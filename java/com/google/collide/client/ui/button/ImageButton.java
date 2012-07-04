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

package com.google.collide.client.ui.button;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.ImageResourceUtils;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.ImageResource;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;
import elemental.html.DivElement;
import elemental.html.Element;

/**
 * A button containing and image and text.
 */
public class ImageButton extends UiComponent<ImageButton.View> {

  /**
   * Creates an {@link ImageButton}.
   */
  public static class Builder {
    private final BaseResources.Resources res;
    private ImageResource image;
    private String text;
    private AnchorElement element;

    public Builder(BaseResources.Resources res) {
      this.res = res;
    }

    public Builder setImage(ImageResource image) {
      this.image = image;
      return this;
    }

    public Builder setText(String text) {
      this.text = text;
      return this;
    }

    public Builder setElement(AnchorElement element) {
      this.element = element;
      return this;
    }

    public ImageButton build() {
      // Create an element if one is not specified.
      AnchorElement anchor = element;
      if (anchor == null) {
        anchor = Elements.createAnchorElement(res.baseCss().button());
        anchor.setHref("javascript:;");
      }

      View view = new View(res, anchor);
      ImageButton button = new ImageButton(view);
      button.setImageAndText(image, text);
      return button;
    }
  }

  /**
   * Called when the button is clicked.
   */
  public interface Listener {
    void onClick();
  }


  /**
   * The View for the Header.
   */
  public static class View extends CompositeView<ViewEvents> {

    /**
     * The spacing between the image and the text.
     */
    private static final int ICON_SPACING = 8;

    private final BaseResources.Css baseCss;
    private final DivElement imageElem;
    private final DivElement imagePositioner;
    private final DivElement textElem;

    public View(BaseResources.Resources res, AnchorElement button) {
      baseCss = res.baseCss();

      // Create an outer element.
      setElement(button);
      button.getStyle().setPosition("relative");

      // Create a centered element to position the image.
      imagePositioner = Elements.createDivElement();
      button.appendChild(imagePositioner);
      imagePositioner.getStyle().setPosition("absolute");
      imagePositioner.getStyle().setTop("50%");

      // Add the image.
      imageElem = Elements.createDivElement();
      imagePositioner.appendChild(imageElem);
      imageElem.addClassName(baseCss.buttonImage());
      imageElem.getStyle().setPosition("absolute");

      // Override the margins set in base.css. Those margins are a hack.
      imageElem.getStyle().setMargin("0");

      // Add the text.
      textElem = Elements.createDivElement();
      textElem.getStyle().setHeight("100%");
      button.appendChild(textElem);

      attachEventListeners();
    }

    @Override
    public AnchorElement getElement() {
      return (AnchorElement) super.getElement();
    }

    public Element getImageElement() {
      return imageElem;
    }

    @SuppressWarnings("null")
    private void setImageAndText(ImageResource image, String text) {
      // Update the image.
      boolean hasImage = (image != null);
      CssUtils.setDisplayVisibility2(imageElem, hasImage);
      if (hasImage) {
        ImageResourceUtils.applyImageResource(imageElem, image);
      }

      // Update the text.
      boolean hasText = (text != null && text.length() > 0);
      textElem.setTextContent(hasText ? text : "");

      // Position the image.
      if (hasImage) {
        imageElem.getStyle().setTop(image.getHeight() / -2, "px");
        if (hasText) {
          // Left align the image if we have text.
          imagePositioner.getStyle().setLeft(baseCss.buttonHorizontalPadding(), "px");
          imageElem.getStyle().setLeft(0, "px");
        } else {
          // Horizontally center the image if we do not have text.
          imagePositioner.getStyle().setLeft("50%");
          imageElem.getStyle().setLeft(image.getWidth() / -2, "px");
        }
      }

      /*
       * Position the text. Even if there is no text, we use the text element to
       * force the button element to be wide enough to contain the image.
       */
      int left = 0;
      if (hasImage) {
        left += image.getWidth();
      }
      if (hasText) {
        left += ICON_SPACING;
      }
      textElem.getStyle().setPaddingLeft(left, "px");
    }

    private void attachEventListeners() {
      getElement().addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onButtonClicked();
          }
        }
      }, false);
    }
  }

  /**
   * Events reported by the View.
   */
  private interface ViewEvents {
    void onButtonClicked();
  }

  private Listener listener;
  private ImageResource image;
  private boolean isImageHidden;
  private String text;

  private ImageButton(View view) {
    super(view);
    handleEvents();
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void setImage(ImageResource image) {
    setImageAndText(image, this.text);
  }

  public void clearImage() {
    setImageAndText(null, this.text);
  }

  public void setText(String text) {
    setImageAndText(this.image, text);
  }

  public void clearText() {
    setImageAndText(this.image, null);
  }

  public void setImageAndText(ImageResource image, String text) {
    this.image = image;
    this.text = text;
    updateView();
  }

  /**
   * Shows or hides the image.
   */
  public void setImageHidden(boolean isHidden) {
    this.isImageHidden = isHidden;
    updateView();
  }

  private void updateView() {
    getView().setImageAndText(isImageHidden ? null : image, text);
  }

  private void handleEvents() {
    getView().setDelegate(new ViewEvents() {
      @Override
      public void onButtonClicked() {
        if (listener != null) {
          listener.onClick();
        }
      }
    });
  }
}
