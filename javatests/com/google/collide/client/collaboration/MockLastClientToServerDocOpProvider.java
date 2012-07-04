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

import com.google.collide.dto.client.DtoClientImpls.ClientToServerDocOpImpl;

/**
 * Mock implementation of {@link LastClientToServerDocOpProvider}.
 */
public class MockLastClientToServerDocOpProvider implements LastClientToServerDocOpProvider {

  private ClientToServerDocOpImpl lastClientToServerDocOpMsg;

  @Override
  public ClientToServerDocOpImpl getLastClientToServerDocOpMsg() {
    return lastClientToServerDocOpMsg;
  }

  @Override
  public void clearLastClientToServerDocOpMsg(
      ClientToServerDocOpImpl clientToServerDocOpMsgToDelete) {
    if (clientToServerDocOpMsgToDelete.equals(lastClientToServerDocOpMsg)) {
      lastClientToServerDocOpMsg = null;
    }
  }

  void set(ClientToServerDocOpImpl clientToServerDocOpMsg) {
    lastClientToServerDocOpMsg = clientToServerDocOpMsg;
  }
}
