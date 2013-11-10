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

package com.google.collide.client.testing;

import java.util.HashMap;
import java.util.Map;

import collide.client.util.Elements;

import com.google.collide.client.ClientConfig;

import elemental.dom.Element;

/**
 * Setter for setting debug ID and/or attributes.
 * <p>
 * Examples:
 * <p>
 * <code>new DebugAttributeSetter().add("wsId";, "2323").on(element); </code>
 * <p>
 * <code>new DebugAttributeSetter()
 *     .setId(DebugId.CONTEXT_MENU)
 *     .add(&quot;wsId&quot;, &quot;2323&quot;)
 *     .add(&quot;owner&quot;, &quot;b&quot;)
 *     .on(element);</code>
 */
public class DebugAttributeSetter {
  private Map<String, String> keyValues;
  private DebugId debugId;

  public DebugAttributeSetter() {
    if (ClientConfig.isDebugBuild()) {
      keyValues = new HashMap<String, String>();
    }
  }

  /**
   * Sets debug ID to {@code debugId} if this is debug build. In release mode,
   * this method will be dead code eliminated at compile time
   */
  public DebugAttributeSetter setId(DebugId debugId) {
    if (ClientConfig.isDebugBuild()) {
      if (this.debugId != null) {
        throw new IllegalArgumentException("DebugId was already set to " + this.debugId.name());
      }
      this.debugId = debugId;
    }
    return this;
  }

  /**
   * Adds an attribute specified by {key, value} if this is debug build. In
   * release mode, this method will be dead code eliminated at compile time
   *
   * Note that "collideid_" is added to {@code key} as prefix.
   */
  public DebugAttributeSetter add(String key, String value) {
    if (ClientConfig.isDebugBuild()) {
      if (key == null) {
        throw new IllegalArgumentException("null key");
      }
      if (value == null) {
        throw new IllegalArgumentException("null value");
      }
      keyValues.put(key, value);
    }
    return this;
  }

  /**
   * Applies debug ID and attributes to {@code element}.
   */
  public void on(Element element) {
    if (ClientConfig.isDebugBuild()) {
      if (element == null) {
        throw new IllegalArgumentException("null element");
      }

      if (debugId != null) {
        element.setAttribute(DebugId.getIdKey(), debugId.name());
      }
      for (Map.Entry<String, String> entry : keyValues.entrySet()) {
        element.setAttribute(DebugId.getAttributeKey(entry.getKey()), entry.getValue());
      }
    }
  }

  /**
   * See {@code #on(Element)}
   */
  public void on(com.google.gwt.dom.client.Element element) {
    on(Elements.asJsElement(element));
  }
}
