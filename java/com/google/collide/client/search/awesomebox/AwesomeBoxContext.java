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

package com.google.collide.client.search.awesomebox;

import com.google.collide.client.search.awesomebox.AwesomeBox.AwesomeBoxSection;
import com.google.collide.client.search.awesomebox.AwesomeBoxModel.HideMode;
import com.google.collide.client.search.awesomebox.shared.MappedShortcutManager;
import com.google.collide.client.search.awesomebox.shared.ShortcutManager;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Context class which manages sections.
 */
public class AwesomeBoxContext {

  /**
   * Builds an AwesomeBox Context
   */
  public static class Builder {
    private HideMode hideMode = HideMode.AUTOHIDE;
    private String headerText = "";
    private boolean preventTab = true;
    private String watermarkText = "";
    private String placeholderText = "";
    private boolean alwaysShowClose = false;
    private AwesomeBoxContext fallbackContext = null;

    public Builder setHideMode(HideMode hideMode) {
      this.hideMode = hideMode;
      return this;
    }

    public Builder setPlaceholderText(String placeholderText) {
      this.placeholderText = placeholderText;
      return this;
    }

    public Builder setHeaderText(String headerText) {
      this.headerText = headerText;
      return this;
    }

    public Builder setPreventTab(boolean preventTab) {
      this.preventTab = preventTab;
      return this;
    }

    public Builder setWaterMarkText(String watermarkText) {
      this.watermarkText = watermarkText;
      return this;
    }

    public Builder setAlwaysShowCloseButton(boolean alwaysShow) {
      this.alwaysShowClose = alwaysShow;
      return this;
    }

    /**
     * If a fallback context is set, then this context can only be visible for
     * one showing of the AwesomeBox. As soon as the AwesomeBox is closed, the
     * context will automatically change to this fallback context.
     */
    public Builder setFallbackContext(AwesomeBoxContext context) {
      this.fallbackContext = context;
      return this;
    }
  }

  /**
   * The default context of the AwesomeBox, most likely this is empty.
   */
  public static final AwesomeBoxContext DEFAULT = new AwesomeBoxContext(new Builder());

  private final JsonArray<AwesomeBoxSection> sections;
  private final ShortcutManager shortcutManager;

  private final HideMode hideMode;
  private final String headerText;
  private final boolean preventTab;
  private final String watermarkText;
  private final String placeholderText;
  private final boolean alwaysShowClose;
  private final AwesomeBoxContext fallbackContext;

  public AwesomeBoxContext(Builder builder) {
    sections = JsonCollections.createArray();
    shortcutManager = new MappedShortcutManager();

    hideMode = builder.hideMode;
    headerText = builder.headerText;
    preventTab = builder.preventTab;
    watermarkText = builder.watermarkText;
    placeholderText = builder.placeholderText;
    alwaysShowClose = builder.alwaysShowClose;
    fallbackContext = builder.fallbackContext;
  }

  public void addSection(AwesomeBoxSection section) {
    sections.add(section);
    section.onAddedToContext(this);
  }

  public void addAllSections(JsonArray<? extends AwesomeBoxSection> sections) {
    this.sections.addAll(sections);
  }

  public JsonArray<AwesomeBoxSection> getSections() {
    return sections;
  }

  public ShortcutManager getShortcutManager() {
    return shortcutManager;
  }

  public HideMode getHideMode() {
    return hideMode;
  }

  public String getHeaderText() {
    return headerText;
  }

  public String getWaterMarkText() {
    return watermarkText;
  }

  public boolean getPreventTab() {
    return preventTab;
  }

  public String getPlaceholderText() {
    return placeholderText;
  }

  public boolean getAlwaysShowCloseButton() {
    return alwaysShowClose;
  }

  public AwesomeBoxContext getFallbackContext() {
    return fallbackContext;
  }

  /**
   * @return The number of sections in the context.
   */
  public int size() {
    return sections.size();
  }

  /**
   * Removes all sections from the internal list for this context.
   */
  public void clearSections() {
    sections.clear();
  }

}
