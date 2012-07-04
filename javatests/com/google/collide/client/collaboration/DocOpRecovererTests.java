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

import static com.google.collide.client.collaboration.CollaborationTestUtils.*;

import com.google.collide.shared.util.Reorderer.TimeoutCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Timer;

/**
 * General tests for recovering from missed doc op scenarios.
 *
 * This mocks out a few classes, but purposefully does not mock out {@link DocOpReceiver} since it
 * is a critical component of recovery too.
 *
 */
public class DocOpRecovererTests extends GWTTestCase {

  private final TimeoutCallback finishTestTimeoutCallback = new TimeoutCallback() {
    @Override
    public void onTimeout(int lastVersionDispatched) {
      finishTest();
    }
  };
  
  /**
   * Non-collaborative session where a single user is typing and gets his acks out-of-order.
   */
  public void testAcksOutOfOrderWithoutCollaborators() {
    final int startVersion = 1, docOpCount = 3;
    CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, FAIL_TIMEOUT_CALLBACK, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, docOpCount));

    // Receives acks, but out-of-order
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(3), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    
    // Ensure doc op receiver is fed the right data
    assertEquals(startVersion + 3, o.receiverListener.revision());
  }

  /**
   * Non-collaborative session where a single user is typing and his client doesn't get an ack.
   */
  public void testAckNotReceivedWithoutCollaborators() {
    final int startVersion = 1, docOpCount = 3;
    CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, FAIL_TIMEOUT_CALLBACK, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, docOpCount));

    // Imagine we never received an ack, trigger recovery
    o.api.expectAndReturn(newRecoverMsg(startVersion, docOpCount),
        newRecoverResponseMsg(startVersion + 1, docOpCount));
    o.recoverer.recover(FAIL_ERROR_CALLBACK);

    // Ensure doc op receiver is fed the right data
    assertEquals(startVersion + 3, o.receiverListener.revision());
  }

  /**
   * Non-collaborative session where a single user is typing and his client doesn't get an ack so he
   * does a recovery. However, after the recovery, the slow ack finally arrives, so make sure it
   * doesn't blow up because it's already seen the doc op by then.
   */
  public void testAckReceivedAfterRecoveryWithoutCollaborators() {
    final int startVersion = 1, docOpCount = 3;
    CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, FAIL_TIMEOUT_CALLBACK, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, docOpCount));

    // Imagine we never received an ack, trigger recovery
    o.api.expectAndReturn(newRecoverMsg(startVersion, docOpCount),
        newRecoverResponseMsg(startVersion + 1, docOpCount));
    o.recoverer.recover(FAIL_ERROR_CALLBACK);

    // Ensure doc op receiver is fed the right data
    assertEquals(startVersion + 3, o.receiverListener.revision());
    
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(3), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
  }

  /**
   * Collaborative session where a user is typing and his client doesn't get an ack. While he is
   * recovering, he gets collaborator doc ops.
   */
  public void testAckNotReceivedWithCollaboratorsTypingDuringRecovery() {
    final int startVersion = 1, userDocOpCount = 3, collaboratorsDocOpCount = 6;
    final CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, FAIL_TIMEOUT_CALLBACK, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, userDocOpCount));

    // Imagine we never received an ack, trigger recovery
    o.api.expectAndReturnAsync(newRecoverMsg(startVersion, userDocOpCount),
        newRecoverResponseMsg(2, userDocOpCount + collaboratorsDocOpCount));
    o.recoverer.recover(FAIL_ERROR_CALLBACK);

    // Recover is waiting on XHR response, imagine the collaborator doc ops arrive right now
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(3), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(5), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(6), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(7), DOC_OP);
    
    delayTestFinish(100);
    
    new Timer() {
      @Override
      public void run() {
        // By now, the recovery XHR response should have come back
        
        // Ensure doc op receiver is fed the right data
        assertEquals(
            startVersion + userDocOpCount + collaboratorsDocOpCount, o.receiverListener.revision());
        
        finishTest();
      }
    }.schedule(1);
  }
  
  /**
   * Collaborative session where a user is typing and his client doesn't get an ack. While he is
   * recovering, he gets collaborator doc ops, but they are out-of-order. All of the collaborator
   * doc ops arrive in the recovery response (imagine they had been applied before the server got
   * the recovery XHR).
   */
  public void testAckNotReceivedWithCollaboratorsTypingButOutOfOrderDuringRecovery() {
    final int startVersion = 1, userDocOpCount = 3, collaboratorsDocOpCount = 6;
    final CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, FAIL_TIMEOUT_CALLBACK, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, userDocOpCount));

    // Imagine we never received an ack, trigger recovery
    o.api.expectAndReturnAsync(newRecoverMsg(startVersion, userDocOpCount),
        newRecoverResponseMsg(2, userDocOpCount + collaboratorsDocOpCount));
    o.recoverer.recover(FAIL_ERROR_CALLBACK);

    // Recover is waiting on XHR response, imagine the collaborator doc ops arrive right now
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(5), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(7), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(3), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(6), DOC_OP);
    
    delayTestFinish(100);
    
    new Timer() {
      @Override
      public void run() {
        // By now, the recovery XHR response should have come back
        
        // Ensure doc op receiver is fed the right data
        assertEquals(
            startVersion + userDocOpCount + collaboratorsDocOpCount, o.receiverListener.revision());
        
        finishTest();
      }
    }.schedule(1);
  }
  
  /**
   * Non-collaborative session where a single user is typing and gets his acks out-of-order but is
   * missing one, so it times out.
   */
  public void testAcksOutOfOrderAndTimesOutWithoutCollaborators() {
    final int startVersion = 1, docOpCount = 3;
    CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, finishTestTimeoutCallback, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, docOpCount));

    // Receives acks, but out-of-order
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    
    delayTestFinish(100);
  }
  
  /**
   * Collaborative session where a user is typing and his client doesn't get an ack. While he is
   * recovering, he gets collaborator doc ops, but they are out-of-order. Some of the collaborator
   * doc ops were applied after the XHR got to the server, but suppose some weird network latency
   * existed so those arrived before the client got the XHR. One of those was dropped, so it should
   * timeout.
   */
  public void testAckNotReceivedWithCollaboratorsTypingButOutOfOrderWithDropDuringRecovery() {
    final int startVersion = 1, userDocOpCount = 3, collaboratorsDocOpDuringXhrCount = 4;
    final CollaborationTestUtils.Objects o =
        CollaborationTestUtils.createObjects(startVersion, finishTestTimeoutCallback, 1);

    // User types something
    o.sender.set(newOutgoingDocOpMsg(startVersion, userDocOpCount));

    // Imagine we never received an ack, trigger recovery
    o.api.expectAndReturnAsync(newRecoverMsg(startVersion, userDocOpCount),
        newRecoverResponseMsg(2, userDocOpCount + collaboratorsDocOpDuringXhrCount));
    o.recoverer.recover(FAIL_ERROR_CALLBACK);

    // Recover is waiting on XHR response, imagine the collaborator doc ops arrive right now
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(2), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(5), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(3), DOC_OP);
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(4), DOC_OP);
    
    /*
     * Server gets XHR, it applies my 3 doc ops (v6, v7, v8), and also 2 more collaborator doc ops
     * (v9, v10). Imagine one of the collaborator doc ops is dropped (v9). Like we mentioned in
     * javadoc, client<->server has weird latency issues so the XHR response arrives later than the
     * collaborator doc ops.
     */
    o.transportSink.onDocOpReceived(newIncomingDocOpMsg(10), DOC_OP);
    
    delayTestFinish(100);
    
    new Timer() {
      @Override
      public void run() {
        // By now, the recovery XHR response should have come back
        
        // Ensure doc op receiver is fed the right data
        assertTrue(startVersion + userDocOpCount + collaboratorsDocOpDuringXhrCount
            <= o.receiverListener.revision());
        
        // We will now wait for the timeout (waiting for v6) to finish the test
      }
    }.schedule(1);
  }
  
  @Override
  public String getModuleName() {
    return "com.google.collide.client.collaboration.TestCollaboration";
  }
}
