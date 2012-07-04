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

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.util.PathUtil;

/**
 * Extended structure: also holds if proposal case is "function"-case.
 */
public class CodeGraphProposal extends AutocompleteProposal {
  private final boolean isFunction;
  private String labelCache;

  public CodeGraphProposal(String name) {
    super(name);
    isFunction = false;
  }

  public CodeGraphProposal(String name, PathUtil path, boolean function) {
    super(name, path);
    this.isFunction = function;
  }

  public boolean isFunction() {
    return isFunction;
  }

  @Override
  public String getLabel() {
    if (labelCache == null) {
      labelCache = name + (isFunction ? "()" : "");
    }
    return labelCache;
  }
}
