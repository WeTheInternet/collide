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

package com.google.collide.client.code;

import collide.client.util.Elements;

import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.FileContents.ContentType;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.html.ImageElement;

/**
 * A display widget for uneditable things.
 */
public class UneditableDisplay extends UiComponent<UneditableDisplay.View>
    implements FileContent {

  public static UneditableDisplay create(View view) {
    return new UneditableDisplay(view);
  }

  public interface Css extends CssResource {
    String top();

    String image();
  }

  public interface Resources extends ClientBundle {
    @Source("UneditableDisplay.css")
    Css uneditableDisplayCss();
  }

  public static class View extends CompositeView<Void> {
    private final Resources res;

    public View(Resources res) {
      super(Elements.createDivElement(res.uneditableDisplayCss().top()));
      this.res = res;
    }
  }

  private UneditableDisplay(View view) {
    super(view);
  }
  
  @Override
  public PathUtil filePath() {
    return null;
  }

  public void displayUneditableFileContents(FileContents uneditableFile) {
    getView().getElement().setInnerHTML("");
    if (uneditableFile.getContentType() == ContentType.IMAGE) {
      ImageElement image =
          Elements.createImageElement(getView().res.uneditableDisplayCss().image());
      image.setSrc("data:" + uneditableFile.getMimeType() + ";base64,"
          + uneditableFile.getContents());
      getView().getElement().appendChild(image);
    } else {
      getView().getElement().setTextContent("This file cannot be edited or displayed.");
    }
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
  }

  @Override
  public void onContentDestroyed() {

  }
}
