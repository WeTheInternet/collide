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

package com.google.collide.client.editor.input;

import elemental.js.util.JsArrayOfInt;

/**
 * Represents a shortcut activated by a stream of text characters, such as ":q!"
 * 
 *
 */
public abstract class StreamShortcut implements Shortcut {

  private JsArrayOfInt stream;
  
  /**
   * Constructors for passing shortcuts as ascii, integer array or JsoArray
   */
  public StreamShortcut(String stream) {
    this.stream = JsArrayOfInt.create();
    for (int i = 0, n = stream.length(); i < n; i++) {
      this.stream.set(i, stream.charAt(i));
    }
  }

  /**
   * Return the String that causes this shortcut to be called
   */
  public JsArrayOfInt getActivationStream() {
    return stream;
  }
}
