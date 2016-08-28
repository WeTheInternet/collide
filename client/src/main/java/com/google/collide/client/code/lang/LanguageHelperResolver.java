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

package com.google.collide.client.code.lang;

import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Object thad holds instances of {@link LanguageHelper} and returns
 * corresponding instance by given {@link SyntaxType}.
 */
public class LanguageHelperResolver {

  private static LanguageHelperResolver instance;

  private final JsonStringMap<LanguageHelper> mapping;

  private LanguageHelperResolver() {
    mapping = JsonCollections.createMap();
    mapping.put(SyntaxType.CSS.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.CPP.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.DART.getName(), new DartLanguageHelper());
    mapping.put(SyntaxType.GO.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.HTML.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.NONE.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.JAVA.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.JS.getName(), new JsLanguageHelper());
    mapping.put(SyntaxType.PHP.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.PY.getName(), new PyLanguageHelper());
    mapping.put(SyntaxType.XML.getName(), new NoneLanguageHelper());
    mapping.put(SyntaxType.YAML.getName(), new NoneLanguageHelper());
  }

  public static LanguageHelper getHelper(SyntaxType type) {
    if (instance == null) {
      instance = new LanguageHelperResolver();
    }
    return instance.resolve(type.getName());
  }

  private LanguageHelper resolve(String typeName) {
    LanguageHelper result = mapping.get(typeName);
    Preconditions.checkNotNull(result, "can't resolve language helper");
    return result;
  }
}
