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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.dto.CodeBlock;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Represents parsed chunk of the source code. Encapsulates chunk boundaries,
 * identifiers found in the chunk and their positions.
 */
public class Scope {

  private final CodeBlock rootCodeBlock;
  private final JsonArray<Scope> subscopes = JsonCollections.createArray();
  private Position endPos;
  private Position beginPos;

  Scope(CodeBlock codeBlock) {
    Preconditions.checkNotNull(codeBlock);
    this.rootCodeBlock = codeBlock;

    setBegin(Position.from(codeBlock.getStartLineNumber(), codeBlock.getStartColumn()));
    setEnd(Position.from(codeBlock.getEndLineNumber(), codeBlock.getEndColumn()));
  }

  @Override
  public String toString() {
    String bounds = " [(" + getBeginLineNumber() + "," + getBeginColumn() + "), ("
        + getEndLineNumber() + "," + getEndColumn() + ")]";
    return CodeBlock.Type.valueOf(rootCodeBlock.getBlockType())
        + " " + rootCodeBlock.getName() + bounds;
  }

  int getBeginColumn() {
    return beginPos.getColumn();
  }

  int getBeginLineNumber() {
    return beginPos.getLine();
  }

  CodeBlock getCodeBlock() {
    return rootCodeBlock;
  }

  int getEndColumn() {
    return endPos.getColumn();
  }

  int getEndLineNumber() {
    return endPos.getLine();
  }

  JsonArray<Scope> getSubscopes() {
    return subscopes;
  }

  public void setBegin(Position pos) {
    beginPos = pos;
  }

  public void setEnd(Position pos) {
    endPos = pos;
  }
}
