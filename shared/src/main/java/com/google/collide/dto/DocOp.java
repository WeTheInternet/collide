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

// TODO: These should be moved to an Editor2-specific package
/**
 * Models a document operation for the Collide code editor.
 * 
 * A DocOp is a description for an operation to be performed on the document. It
 * consists of one or more components that together must span the entire length
 * of the document.
 * 
 * For example, consider the following DocOp which spans the document with three
 * lines: {(RetainLine:1), (Insert:"Hello"), (Retain:5, true), (RetainLine:1)}.
 * It retains the first line, inserts "Hello" at the beginning of the second
 * line, retains the remaining 5 characters on that second line (including the
 * newline), and then retains the last line.
 * 
 */
public interface DocOp {
  JsonArray<DocOpComponent> getComponents();
}
