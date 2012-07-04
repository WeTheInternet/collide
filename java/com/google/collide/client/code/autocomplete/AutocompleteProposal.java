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

package com.google.collide.client.code.autocomplete;

import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.Utils;

import java.util.Comparator;

/**
 * Structure holding autocomplete proposal and additional metainformation
 * (currently filename where the proposal comes from).
 */
public class AutocompleteProposal {

  public static final Comparator<AutocompleteProposal> LEXICOGRAPHICAL_COMPARATOR =
      new Comparator<AutocompleteProposal>() {
    @Override
    public int compare(AutocompleteProposal first, AutocompleteProposal second) {
      // We assume that proposal never return null as label.
      return first.getLabel().compareToIgnoreCase(second.getLabel());
    }
  };

  protected final String name;
  protected final PathUtil path;

  public AutocompleteProposal(String name) {
    this(name, PathUtil.EMPTY_PATH);
  }

  public AutocompleteProposal(String name, PathUtil path) {
    assert name != null;
    this.name = name;
    this.path = path;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof AutocompleteProposal) {
      AutocompleteProposal that = (AutocompleteProposal) obj;
      if (!this.name.equals(that.name)) {
        return false;
      }
      if (Utils.equalsOrNull(this.path, that.path)) {
        return true;
      }
      return false;
    } else {
      return false;
    }
  }

  public String getName() {
    return name;
  }

  public PathUtil getPath() {
    return path;
  }

  @Override
  public int hashCode() {
    return this.name.hashCode() + (31 * this.path.hashCode());
  }

  @Override
  public String toString() {
    return this.name + " :" + this.path.getPathString();
  }

  public String getLabel() {
    return name;
  }
}
