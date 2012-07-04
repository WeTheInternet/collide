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

package com.google.collide.client.code.autocomplete.html;

import com.google.collide.client.util.collections.SimpleStringBag;
import com.google.collide.client.util.collections.StringMultiset;

/**
 * Object that represents contents of HTML tag entity.
 *
 * <p>Actually, only tag name and attributes bag are stored.
 *
 */
public class HtmlTagWithAttributes extends DirtyStateTracker {

  private final String tagName;

  private final StringMultiset attributes = new SimpleStringBag();


  public HtmlTagWithAttributes(String tagName) {
    this.tagName = tagName;
  }

  public String getTagName() {
    return tagName;
  }

  public StringMultiset getAttributes() {
    return attributes;
  }
}
