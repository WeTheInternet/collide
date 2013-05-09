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

package com.google.collide.client.ui.dropdown;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.ui.button.ImageButton2;
import com.google.collide.client.ui.list.SimpleList;
import com.google.collide.client.util.Elements;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.html.InputElement;
import elemental.html.SpanElement;

/**
 * A collection of widgets for creating dropdowns
 */
public class DropdownWidgets {

  public interface Css extends CssResource {

    // Dropdown Button
    public String button();

    public String buttonLabel();

    public String buttonArrow();

    // Dropdown Input
    public String inputContainer();

    public String input();

    public String inputArrow();

    public String inputArrowActive();

    // Split Button
    public String splitButtonLabel();

    public String splitButtonArrow();
  }

  public interface Resources extends BaseResources.Resources, SimpleList.Resources {
    @Source("DropdownWidgets.css")
    Css dropdownWidgetsCss();
  }

  /**
   * Creates a button which functions as a dropdown.
   * 
   */
  public static class DropdownButton {

    private final DivElement button;
    private final DivElement label;

    public DropdownButton(Resources res) {
      Css css = res.dropdownWidgetsCss();

      button = Elements.createDivElement(css.button());
      button.addClassName(res.baseCss().button());

      label = Elements.createDivElement(css.buttonLabel());
      button.appendChild(label);

      Element arrow = Elements.createDivElement(css.buttonArrow());
      button.appendChild(arrow);
    }

    public DivElement getButton() {
      return button;
    }

    public DivElement getLabel() {
      return label;
    }
  }

  /**
   * An input box that displays with a clickable dropdown arrow. This is not modeled as a proper UI
   * component since it is fairly light weight.
   * 
   */
  public static class DropdownInput {
    private final DivElement container;
    private final InputElement inputElement;
    private final SpanElement indicatorArrow;

    public DropdownInput(Resources res) {
      Css css = res.dropdownWidgetsCss();

      container = Elements.createDivElement(css.inputContainer());
      indicatorArrow = Elements.createSpanElement(css.inputArrow());
      indicatorArrow.setTextContent("\u00A0");
      container.appendChild(indicatorArrow);

      inputElement = Elements.createInputTextElement(res.baseCss().textInput());
      inputElement.addClassName(res.dropdownWidgetsCss().input());
      container.appendChild(inputElement);
    }

    public DivElement getContainer() {
      return container;
    }

    public SpanElement getButton() {
      return indicatorArrow;
    }

    public InputElement getInput() {
      return inputElement;
    }
  }

  /**
   * Creates a split button which has an image in the left and right. The left side is considered
   * the label and the right side is typically a trigger for a dropdown menu.
   */
  public static class SplitDropdownButton {
    private final ImageButton2 labelButton;
    private final ImageButton2 triggerButton;

    public SplitDropdownButton(Resources res, Element container, ImageResource labelImage) {
      BaseResources.Css baseCss = res.baseCss();
      Css css = res.dropdownWidgetsCss();

      labelButton = new ImageButton2(res, labelImage);
      labelButton.getButtonElement().addClassName(css.splitButtonLabel());
      container.appendChild(labelButton.getButtonElement());

      triggerButton = new ImageButton2(res, res.disclosureArrowDkGreyDown());
      triggerButton.getButtonElement().addClassName(css.splitButtonArrow());
      container.appendChild(triggerButton.getButtonElement());
    }

    public void initializeAfterAttachedToDom() {
      labelButton.initializeAfterAttachedToDom();
      triggerButton.initializeAfterAttachedToDom();
    }

    public ImageButton2 getLabelButton() {
      return labelButton;
    }

    public ImageButton2 getTriggerButton() {
      return triggerButton;
    }
  }

  private DropdownWidgets() {
    // can't instantiate this class
  }
}
