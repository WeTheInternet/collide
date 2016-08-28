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

package com.google.collide.codemirror2;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A wrapper around the JSO state mutated by a parser mode. This is usually a
 * dictionary of mode-specific state information, such as a stack of HTML tags
 * to close or the next expected character.
 */
public class CmState extends JavaScriptObject implements State {
  protected CmState() {
  }

  @Override
  public final native State copy(Parser mode) /*-{
    var copiedState = $wnd.CodeMirror.copyState(mode, this);

    // Workaround for Chrome devmode: Remove the devmode workaround for Chrome
    // object identity. For 100% correctness, this should iterate through
    // and remove all instances of __gwtObjectId, but the top-level was
    // sufficient from my testing.
    delete copiedState.__gwt_ObjectId;

    return copiedState;
  }-*/;
}
