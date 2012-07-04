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

package com.google.collide.dto;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;

/**
 * <p>Code graph structure which encapsulates a tree of code blocks,
 * and associations between code blocks.
 *
 * <p>An instance of this object may represent a subgraph, that is, some of
 * its components may be omitted.
 *
 */
public interface CodeGraph {
  /**
   * @return a mapping of file name to the file scope code block
   */
  JsonStringMap<CodeBlock> getCodeBlockMap();

  /**
   * @return the default package code block
   */
  CodeBlock getDefaultPackage();

  /**
   * @return inheritance associations between code blocks
   */
  JsonArray<InheritanceAssociation> getInheritanceAssociations();

  /**
   * @return type associations between code blocks
   */
  JsonArray<TypeAssociation> getTypeAssociations();

  /**
   * @return import associations between code blocks
   */
  JsonArray<ImportAssociation> getImportAssociations();
}
