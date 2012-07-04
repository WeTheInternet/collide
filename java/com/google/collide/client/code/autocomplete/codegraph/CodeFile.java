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

import static com.google.collide.shared.document.util.LineUtils.comparePositions;

import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.CodeBlock;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates code structure data of the file opened in the editor.
 *
 */
class CodeFile {
  static class CodeBlockReferences {
    private final Map<CodeBlock, CodeBlock> childParentRefs = new HashMap<CodeBlock, CodeBlock>();

    void addChildParentRef(CodeBlock child, CodeBlock parent) {
      childParentRefs.put(child, parent);
    }

    CodeBlock getParent(CodeBlock child) {
      return childParentRefs.get(child);
    }

    void clear() {
      childParentRefs.clear();
    }
  }

  private static Scope findScope(Scope scope, int lineNumber, int column, boolean endInclusive) {
    int relativeToBegin =
        comparePositions(lineNumber, column, scope.getBeginLineNumber(), scope.getBeginColumn());

    // When we say that cursor column is X, we mean, that there are X chars
    // before cursor in this line.
    // But when we say that scope ends at column X, we mean that X-th char is
    // the last char that belongs to the scope.
    // That is why we do +1 to make this function work as designed.
    int scopeEndColumn = scope.getEndColumn() + 1;

    int relativeToEnd =
        comparePositions(lineNumber, column, scope.getEndLineNumber(), scopeEndColumn);
    if (relativeToBegin < 0 || relativeToEnd > 0) {
      return null;
    }
    if (!endInclusive && relativeToEnd == 0) {
      return null;
    }

    for (int i = 0; i < scope.getSubscopes().size(); ++i) {
      Scope subScope = findScope(scope.getSubscopes().get(i), lineNumber, column, endInclusive);
      if (subScope != null) {
        return subScope;
      }
    }
    return scope;
  }


  private final CodeBlockReferences refs = new CodeBlockReferences();
  private final PathUtil filePath;

  private CodeBlock rootCodeBlock;
  private Scope rootScope;

  CodeFile(PathUtil filePath) {
    Preconditions.checkNotNull(filePath);
    this.filePath = filePath;
  }

  private void buildSubscopes(Scope rootScope, CodeBlock codeBlock, JsonArray<CodeBlock> queue) {
    JsonArray<Scope> subscopes = rootScope.getSubscopes();
    for (int i = 0, size = codeBlock.getChildren().size(); i < size; i++) {
      CodeBlock child = codeBlock.getChildren().get(i);
      refs.addChildParentRef(child, codeBlock);
      if (isTextuallyNested(child, codeBlock)) {
        Scope childScope = new Scope(child);
        subscopes.add(childScope);
      } else {
        queue.add(child);
      }
    }
    for (int i = 0; i < subscopes.size(); i++) {
      Scope child = subscopes.get(i);
      buildSubscopes(child, child.getCodeBlock(), queue);
    }
  }

  private boolean isTextuallyNested(CodeBlock child, CodeBlock parent) {
    return
            LineUtils.comparePositions(child.getStartLineNumber(), child.getStartColumn(),
                parent.getStartLineNumber(), parent.getStartColumn()) >= 0
        &&
            LineUtils.comparePositions(child.getEndLineNumber(), child.getEndColumn(),
                parent.getEndLineNumber(), parent.getEndColumn()) <= 0;
  }

  /**
   * Finds the most suitable scope for a given position.
   *
   * @param lineNumber position line number
   * @param column position column
   * @param endInclusive when {@code true} then scopes are suitable if they
   *        can be expanded by adding something at the given position
   * @return scope found
   */
  Scope findScope(int lineNumber, int column, boolean endInclusive) {
    if (rootScope == null) {
      return null;
    }
    return findScope(rootScope, lineNumber, column, endInclusive);
  }

  PathUtil getFilePath() {
    return filePath;
  }

  CodeBlockReferences getReferences() {
    return refs;
  }

  CodeBlock getRootCodeBlock() {
    return rootCodeBlock;
  }

  @VisibleForTesting
  Scope getRootScope() {
    return this.rootScope;
  }

  void setRootCodeBlock(CodeBlock codeBlock) {
    this.rootCodeBlock = codeBlock;
    if (codeBlock == null) {
      return;
    }

    refs.clear();
    rootScope = new Scope(codeBlock);
    JsonArray<CodeBlock> queue = JsonCollections.createArray();
    buildSubscopes(rootScope, rootCodeBlock, queue);
    while (!queue.isEmpty()) {
      JsonArray<CodeBlock> newQueue = JsonCollections.createArray();
      for (int i = 0; i < queue.size(); i++) {
        CodeBlock queued = queue.get(i);
        Scope lexicalContainer = findScope(queued.getStartLineNumber(), queued.getStartColumn(),
            false);
        if (lexicalContainer != null) {
          Scope lexicalScope = new Scope(queued);
          lexicalContainer.getSubscopes().add(lexicalScope);
          buildSubscopes(lexicalScope, queued, newQueue);
        }
      }
      queue = newQueue;
    }
  }
}
