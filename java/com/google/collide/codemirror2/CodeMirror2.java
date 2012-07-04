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

import com.google.collide.client.util.PathUtil;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

/**
 * Wraps the CodeMirror2 syntax parser modes.
 *
 */
public class CodeMirror2 {

  /**
   * External parser javascript source.
   */
  public interface Resources extends ClientBundle {
    @Source("codemirror2_parsers.js")
    TextResource parser();

    @Source("codemirror2_base.js")
    TextResource base();
  }

  public static String getJs(Resources resources) {
    return resources.base().getText() + resources.parser().getText();
  }

  public static Parser getParser(PathUtil path) {
    SyntaxType type = SyntaxType.syntaxTypeByFilePath(path);
    CmParser parser = getParserForMime(type.getMimeType());
    Preconditions.checkNotNull(parser);
    parser.setType(type);

    // TODO: testing no smart indentation to see how it feels
    parser.setPreventSmartIndent(type != SyntaxType.PY);
    return parser;
  }

  private static native CmParser getParserForMime(String mime) /*-{
    conf = $wnd.CodeMirror.defaults;
    if (mime == "text/x-python") {
      conf["mode"] = {
        version : 2
      };
    }
    return $wnd.CodeMirror.getMode(conf, mime);
  }-*/;

  /**
   * Mode constant: JavaScript.
   */
  public static final String JAVASCRIPT = "javascript";

  /**
   * Mode constant: HTML.
   */
  public static final String HTML = "html";

  /**
   * Mode constant: CSS.
   */
  public static final String CSS = "css";
}
