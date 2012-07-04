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
package com.google.collide.client.testutil;

import com.google.collide.client.Resources;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.gwt.core.client.GWT;

import elemental.html.Element;
import elemental.js.JsBrowser;
import elemental.js.html.JsDocument;

/**
 * GWT test case that cleans up DOM body before and after test.
 *
 */
public abstract class CodeMirrorTestCase extends SynchronousTestCase {

  private static final String INJECTED_CODE_MIRROR_JS = "injectedCodeMirrorJs";

  private static native boolean codeMirrorIsLoaded() /*-{
    if ($wnd.CodeMirror) {
      return true;
    }
    return false
  }-*/;

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    if (!codeMirrorIsLoaded()) {
      Resources resources = GWT.create(Resources.class);
      String js = CodeMirror2.getJs(resources);

      JsDocument jsDocument = JsBrowser.getDocument();
      Element scriptElem = jsDocument.createElement("script");
      scriptElem.setId(INJECTED_CODE_MIRROR_JS);
      scriptElem.setAttribute("language", "javascript");
      scriptElem.setTextContent(js);
      jsDocument.getBody().appendChild(scriptElem);
    }
  }

  @Override
  public void gwtTearDown() throws Exception {
    JsBrowser.getDocument().getElementById(INJECTED_CODE_MIRROR_JS).removeFromParent();
    super.gwtTearDown();
  }
}
