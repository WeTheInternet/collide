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

package com.google.collide.client.code;

import com.google.collide.client.AppContext;
import com.google.collide.client.collaboration.DocumentCollaborationController;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.BootstrappedGwtTestCase;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.util.JsonCollections;

/**
 * Tests for the {@link DocumentCollaborationController}.
 */
public class DocumentCollaborationControllerTest extends BootstrappedGwtTestCase {

  AppContext context;
  ParticipantModel model;
  Editor editor;

  @Override
  public String getModuleName() {
    return TestUtils.BUILD_MODULE_NAME;
  }

  public void testCreate() {
    context = new MockAppContext();
    Document document = Document.createEmpty();
    IncomingDocOpDemultiplexer demux =
        IncomingDocOpDemultiplexer.create(context.getMessageFilter());
    DocumentCollaborationController controller = new DocumentCollaborationController(
        context, model, demux, document, JsonCollections.<DocumentSelection> createArray());
  }
}
