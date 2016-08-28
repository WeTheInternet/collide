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

package com.google.collide.client.codeunderstanding;

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.dto.CodeGraphRequest;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.common.base.Preconditions;

/**
 * Wrapper that isolates {@link CubeClient} from UI.
 *
 */
public class CubeClientWrapper {

  private final CubeClient cubeClient;

  private ListenerRegistrar.Remover textListenerRemover;

  /**
   * Constructs instance of and registers {@link #cubeClient} as an appropriate
   * Tango invalidation receiver.
   *
   * @param api Cube API
   */
  public CubeClientWrapper(FrontendApi.RequestResponseApi<CodeGraphRequest,
      CodeGraphResponse> api) {    
    this.cubeClient = new CubeClient(api);

    // USED TO REGISTER WITH TANGO
  }

  public void cleanup() {
    unsubscribeTextListener();
    cubeClient.cleanup();
  }

  public void setDocument(Document document, String filePath) {
    Preconditions.checkNotNull(document);
    unsubscribeTextListener();
    textListenerRemover = document.getTextListenerRegistrar().add(new Document.TextListener() {
      @Override
      public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
        cubeClient.refresh();
      }
    });
    cubeClient.setPath(filePath);
  }

  private void unsubscribeTextListener() {
    if (textListenerRemover != null) {
      textListenerRemover.remove();
    }
    textListenerRemover = null;
  }

  public CubeClient getCubeClient() {
    return cubeClient;
  }
}
