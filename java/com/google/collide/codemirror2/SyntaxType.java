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
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Syntax types (languages / file formats) enumeration.
 */
public enum SyntaxType {

  CPP("clike", "text/x-c++src", State.class, new BasicTokenFactory(), false),
  CSS("css", "text/css", CssState.class, new CssTokenFactory(), true),
  DART("dart", "text/dart", State.class, new BasicTokenFactory(), false),
  GO("go", "text/x-go", State.class, new BasicTokenFactory(), false),
  HTML("html", "text/html", HtmlState.class, new HtmlTokenFactory(), true),
  JAVA("clike", "text/x-java", State.class, new BasicTokenFactory(), false),
  JS("javascript", "text/javascript", JsState.class, new BasicTokenFactory(), true),
  NONE("unknown", "text/plain", State.class, new BasicTokenFactory(), true),
  PHP("php", "text/x-php", State.class, new BasicTokenFactory(), false),
  PY("python", "text/x-python", PyState.class, new PyTokenFactory(), false),
  SVG("xml", "application/xml", State.class, new BasicTokenFactory(), true),
  XML("xml", "application/xml", State.class, new BasicTokenFactory(), true),
  YAML("yaml", "text/x-yaml", State.class, new BasicTokenFactory(), true);

  /**
   * Guesses syntax type by file path.
   */
  @VisibleForTesting
  public static SyntaxType syntaxTypeByFilePath(PathUtil filePath) {
    Preconditions.checkNotNull(filePath);
    return syntaxTypeByFileName(filePath.getBaseName());
  }

  /**
   * Guesses syntax type by file name.
   */
  public static SyntaxType syntaxTypeByFileName(String fileName) {
    Preconditions.checkNotNull(fileName);
    SyntaxType syntaxType = extensionToSyntaxType.get(PathUtil.getFileExtension(
        fileName.toLowerCase()));
    return (syntaxType != null) ? syntaxType : SyntaxType.NONE;
  }

  private final String mimeType;

  private final TokenFactory tokenFactory;

  private final Class stateClass;

  private final String name;

  private final boolean hasGlobalNamespace;

  private static final JsonStringMap<SyntaxType> extensionToSyntaxType =
      createExtensionToSyntaxTypeMap();

  public String getMimeType() {
    return mimeType;
  }

  public String getName() {
    return name;
  }

  @SuppressWarnings("unchecked")
  public <T extends State> TokenFactory<T> getTokenFactory() {
    return tokenFactory;
  }

  SyntaxType(String name, String mimeType, Class<? extends State> stateClass,
      TokenFactory tokenFactory, boolean hasGlobalNamespace) {
    this.name = name;
    this.mimeType = mimeType;
    this.tokenFactory = tokenFactory;
    this.stateClass = stateClass;
    this.hasGlobalNamespace = hasGlobalNamespace;
  }

  /**
   * Checks if specified class can be cast form parser state corresponding to
   * this syntax type.
   *
   * @param stateClass class we are going to (down)cast to
   * @return {@code true} is cast can be performed safely
   */
  public boolean checkStateClass(Class stateClass) {
    return stateClass == this.stateClass || stateClass == State.class;
  }

  /**
   * @return {@code true} if this syntax type has a notion of global namespace, as opposed
   *         to the notion of modules and explicit importing
   */
  public boolean hasGlobalNamespace() {
    return hasGlobalNamespace;
  }

  /**
   * Initializes extension -> syntax type map. <b>All extension keys must be lowercase</b>.
   * @return a new map instance with all supported languages.
   */
  private static JsonStringMap<SyntaxType> createExtensionToSyntaxTypeMap() {
    JsonStringMap<SyntaxType> tmp = JsonCollections.createMap();
    tmp.put("cc", SyntaxType.CPP);
    tmp.put("cpp", SyntaxType.CPP);
    tmp.put("css", SyntaxType.CSS);
    tmp.put("cxx", SyntaxType.CPP);
    tmp.put("dart", SyntaxType.DART);
    tmp.put("go", SyntaxType.GO);
    tmp.put("hpp", SyntaxType.CPP);
    tmp.put("h", SyntaxType.CPP);
    tmp.put("html", SyntaxType.HTML);
    tmp.put("hxx", SyntaxType.CPP);
    tmp.put("java", SyntaxType.JAVA);
    tmp.put("js", SyntaxType.JS);
    tmp.put("php", SyntaxType.PHP);
    tmp.put("py", SyntaxType.PY);
    tmp.put("svg", SyntaxType.SVG);
    tmp.put("xml", SyntaxType.XML);
    tmp.put("yaml", SyntaxType.YAML);
    return tmp;
  }
}
