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

package com.google.collide.shared.ot;

import com.google.collide.dto.DocOp;

/**
 * A pair of document operations.
 */
public final class OperationPair {

  private final DocOp clientOp;
  private final DocOp serverOp;

  /**
   * Constructs an OperationPair from a client operation and a server operation.
   *
   * @param clientOp The client's operation.
   * @param serverOp The server's operation.
   */
  public OperationPair(DocOp clientOp, DocOp serverOp) {
    this.clientOp = clientOp;
    this.serverOp = serverOp;
  }

  /**
   * @return The client's operation.
   */
  public DocOp clientOp() {
    return clientOp;
  }

  /**
   * @return The server's operation.
   */
  public DocOp serverOp() {
    return serverOp;
  }
}
