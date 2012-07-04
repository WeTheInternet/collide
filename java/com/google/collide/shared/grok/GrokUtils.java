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

package com.google.collide.shared.grok;

import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraph;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.StringUtils;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;

/**
 * Shared utilities for working with grok-related data structures.
 */
@GwtCompatible
public class GrokUtils {
  private static final String QNAME_DELIMITER = ".";

  /**
   * A pair of (container, missing child name)
   */
  public static class MissingChildCodeBlock {
    public final CodeBlock container;
    public final String childName;

    private MissingChildCodeBlock(CodeBlock container, String name) {
      this.container = container;
      this.childName = name;
    }
  }

  public static final Function<MissingChildCodeBlock, CodeBlock> NO_MISSING_CHILD =
      new Function<MissingChildCodeBlock, CodeBlock>() {
        @Override
        public CodeBlock apply(MissingChildCodeBlock input) {
          return null;
        }
      };

  /**
   * Finds code block by its qualified name relative to the {@code root} code
   * block.
   *
   * @param root code block to search from
   * @param qname dot-separated qualified name
   * @return code block accessible by {@code qname} from {@code root} or {@code
   *         null}
   */
  public static CodeBlock findCodeBlockByQname(CodeBlock root, String qname) {
    return getOrCreateCodeBlock(root, StringUtils.split(qname, QNAME_DELIMITER), NO_MISSING_CHILD);
  }

  /**
   * Finds code block by its qualified name relative to the {@code root} code
   * block and creates missing blocks if necessary using a factory function. If
   * factory function returns {@code null} at some point, this function also
   * returns {@code null}, otherwise it inserts code blocks created by a factory
   * function into the tree (like mkdir -p does for directories).
   *
   * @param root code block to search from
   * @param qname dot-separated qualified name
   * @param createCodeBlock factory function
   * @return code block accessible by {@code qname} from {@code root} or {@code
   *         null} if the path was missing in the original tree and factory
   *         function refused to create some part of it
   */
  public static CodeBlock getOrCreateCodeBlock(CodeBlock root, JsonArray<String> qname,
      Function<MissingChildCodeBlock, CodeBlock> createCodeBlock) {
    for (int nameIdx = 0, end = qname.size(); nameIdx < end; nameIdx++) {
      CodeBlock newRoot = null;
      for (int i = 0; i < root.getChildren().size(); i++) {
        if (root.getChildren().get(i).getName().equals(qname.get(nameIdx))) {
          newRoot = root.getChildren().get(i);
          break;
        }
      }
      if (newRoot == null) {
        newRoot = createCodeBlock.apply(new MissingChildCodeBlock(root, qname.get(nameIdx)));
        if (newRoot == null) {
          return null;
        }
        root.getChildren().add(newRoot);
      }
      root = newRoot;
    }
    return root;
  }

  /**
   * Search in graph blocks map for block with specific file path.
   *
   * @return {@code null} if filePath or graph is {@code null}, or there is
   *         no code block with specified path
   */
  public static CodeBlock findFileCodeBlock(CodeGraph graph, String filePath) {
    if (filePath == null) {
      return null;
    }
    if (graph == null) {
      return null;
    }
    JsonStringMap<CodeBlock> blockMap = graph.getCodeBlockMap();
    if (blockMap == null) {
      return null;
    }
    JsonArray<String> keys = blockMap.getKeys();
    final int l = keys.size();
    for (int i = 0; i < l; i++) {
      String key = keys.get(i);
      CodeBlock codeBlock = blockMap.get(key);
      if (CodeBlock.Type.VALUE_FILE == codeBlock.getBlockType()) {
        if (filePath.equals(codeBlock.getName())) {
          return codeBlock;
        }
      }
    }
    return null;
  }
}
