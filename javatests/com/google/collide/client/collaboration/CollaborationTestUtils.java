// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.collaboration;

import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer.Receiver;
import com.google.collide.client.collaboration.cc.RevisionProvider;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.testing.MockFrontendApi;
import com.google.collide.client.testing.MockFrontendApi.MockApi;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.RecoverFromMissedDocOps;
import com.google.collide.dto.RecoverFromMissedDocOpsResponse;
import com.google.collide.dto.ServerToClientDocOp;
import com.google.collide.dto.client.DtoClientImpls.ClientToServerDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.DocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.MockRecoverFromMissedDocOpsResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.MockServerToClientDocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.RecoverFromMissedDocOpsImpl;
import com.google.collide.dto.client.DtoClientImpls.RecoverFromMissedDocOpsResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.ServerToClientDocOpImpl;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.ot.DocOpBuilder;
import com.google.collide.shared.util.ErrorCallback;
import com.google.collide.shared.util.Reorderer.TimeoutCallback;

import junit.framework.Assert;

/**
 * Test utilities for that are generally applicable to many different types of collaboration tests.
 */
public class CollaborationTestUtils {

  static class ReceiverListener implements DocOpReceiver.Listener<DocOp>, RevisionProvider {
    private int revision;

    ReceiverListener(int startingRevision) {
      revision = startingRevision;
    }

    @Override
    public int revision() {
      return revision;
    }

    @Override
    public void onMessage(int resultingRevision, String sid, DocOp mutation) {
      Assert.assertEquals(++revision, resultingRevision);
    }

    @Override
    public void onError(Throwable e) {
      Assert.fail();
    }
  }

  static class Objects {
    final DocOpReceiver receiver;
    final IncomingDocOpDemultiplexer.Receiver transportSink;
    final MockLastClientToServerDocOpProvider sender;
    final DocOpRecoverer recoverer;
    final RevisionProvider version;
    final MockApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse> api;
    final ReceiverListener receiverListener;

    public Objects(DocOpReceiver receiver,
        Receiver transportSink,
        MockApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse> api,
        ReceiverListener receiverListener,
        MockLastClientToServerDocOpProvider sender,
        DocOpRecoverer recoverer,
        @SuppressWarnings("unused") MessageFilter messageFilter) {
      this.receiver = receiver;
      this.transportSink = transportSink;
      this.api = api;
      this.recoverer = recoverer;
      version = this.receiverListener = receiverListener;
      this.sender = sender;
    }
  }

  public static final String FILE_EDIT_SESSION_KEY = "2";

  public static final ErrorCallback FAIL_ERROR_CALLBACK = new ErrorCallback() {
      @Override
    public void onError() {
      Assert.fail();
    }
  };

  public static final TimeoutCallback FAIL_TIMEOUT_CALLBACK = new TimeoutCallback() {
      @Override
    public void onTimeout(int lastVersionDispatched) {
      Assert.fail();
    }
  };

  static final DocOpImpl DOC_OP =
      (DocOpImpl) new DocOpBuilder(ClientDocOpFactory.INSTANCE, false).insert("a")
          .retainLine(1).build();

  static ClientToServerDocOpImpl newOutgoingDocOpMsg(int version, int docOpCount) {
    return ClientToServerDocOpImpl.make()
        .setFileEditSessionKey(FILE_EDIT_SESSION_KEY)
        .setCcRevision(version)
        .setSelection(null)
        .setDocOps2(getRepeatedDocOps(docOpCount));
  }

  static ServerToClientDocOpImpl newIncomingDocOpMsg(int appliedVersion) {
    return MockServerToClientDocOpImpl.make().setAppliedCcRevision(appliedVersion).setDocOp2(DOC_OP)
        .setFileEditSessionKey(FILE_EDIT_SESSION_KEY);
  }

  static RecoverFromMissedDocOpsImpl newRecoverMsg(int version, int unackedDocOpCount) {
    return RecoverFromMissedDocOpsImpl.make()
        .setCurrentCcRevision(version)
        .setDocOps2(getRepeatedDocOps(unackedDocOpCount))
        .setFileEditSessionKey(FILE_EDIT_SESSION_KEY);
  }

  static RecoverFromMissedDocOpsResponseImpl newRecoverResponseMsg(
      int appliedVersion, int historyDocOpCount) {

    JsoArray<ServerToClientDocOp> docOpMsgs = JsoArray.create();
    for (int i = 0; i < historyDocOpCount; i++) {
      docOpMsgs.add(MockServerToClientDocOpImpl.make()
          .setFileEditSessionKey(FILE_EDIT_SESSION_KEY)
          .setDocOp2(DOC_OP)
          .setAppliedCcRevision(appliedVersion++));
    }
    return MockRecoverFromMissedDocOpsResponseImpl.make().setDocOps(docOpMsgs);
  }

  private static JsoArray<String> getRepeatedDocOps(int count) {
    JsoArray<String> docOps = JsoArray.create();
    while (docOps.size() < count) {
      docOps.add(DOC_OP.serialize());
    }
    return docOps;
  }

  static Objects createObjects(
      int startVersion, TimeoutCallback outOfOrderTimeoutCallback, int outOfOrderTimeoutMs) {
    
    MessageFilter messageFilter = new MessageFilter();

    DocOpReceiver docOpReceiver = new DocOpReceiver(
        IncomingDocOpDemultiplexer.create(messageFilter), FILE_EDIT_SESSION_KEY,
        outOfOrderTimeoutCallback, outOfOrderTimeoutMs);

    MockApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse> api =
        (MockApi<RecoverFromMissedDocOps, RecoverFromMissedDocOpsResponse>) new MockFrontendApi().RECOVER_FROM_MISSED_DOC_OPS;

    ReceiverListener receiverListener = new ReceiverListener(startVersion);
    docOpReceiver.setRevisionProvider(receiverListener);

    MockLastClientToServerDocOpProvider lastDocOpProvider =
        new MockLastClientToServerDocOpProvider();
    
    DocOpRecoverer docOpRecoverer = new DocOpRecoverer(FILE_EDIT_SESSION_KEY,
        api,
        docOpReceiver,
        lastDocOpProvider,
        receiverListener);

    docOpReceiver.connect(startVersion, receiverListener);

    return new Objects(docOpReceiver,
        docOpReceiver.unorderedDocOpReceiver,
        api,
        receiverListener,
        lastDocOpProvider,
        docOpRecoverer,
        messageFilter);
  }
}
