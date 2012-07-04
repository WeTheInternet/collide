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

package com.google.collide.client.ui.list;

import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.util.ImageResourceUtils;
import com.google.collide.client.util.Utils;
import com.google.gwt.resources.client.ImageResource;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

/**
 * A simple list item renderer that renders the {@link Object#toString()} of the
 * given model objects and optionally adds an icon.
 * 
 */
public class BasicListItemRenderer<M> extends ListItemRenderer<M> {

  /**
   * Creates a renderer which renders items with no images.
   */
  public static <M> BasicListItemRenderer<M> create(M[] items) {
    return new BasicListItemRenderer<M>(items, null, 0);
  }
  
  /**
   * Creates a renderer which renders items with icons. Any items without an icon will be rendered
   * with 0 left padding.
   */
  public static <M> BasicListItemRenderer<M> createWithIcons(M[] items, ImageResource[] icons) {
    return new BasicListItemRenderer<M>(items, icons, 0);
  }

  /**
   * @param defaultPadding If no icon is found this is the amount of padding to use for alignment
   *        purposes.
   */
  public static <M> BasicListItemRenderer<M> createWithIcons(M[] items, ImageResource[] icons,
      int defaultPadding) {
    return new BasicListItemRenderer<M>(items, icons, defaultPadding);
  }
  
  private static final int IMAGE_PADDING_LEFT_PX = 15;
  private static final int IMAGE_PADDING_RIGHT_PX = 10;
  
  private final M[] items;
  private final ImageResource[] icons;
  private final int defaultPadding;

  private BasicListItemRenderer(M[] items, ImageResource[] icons, int defaultPadding) {
    this.items = items;
    this.icons = icons;
    this.defaultPadding = defaultPadding;
  }
  
  @Override
  public void render(Element listItemBase, M item) {

    if (icons != null) {
      ImageResource icon = findIcon(item);
      int padding = defaultPadding;
      if (icon != null) {
        ImageResourceUtils.applyImageResource(listItemBase, icon, IMAGE_PADDING_LEFT_PX + "px",
            "center");
        padding = IMAGE_PADDING_LEFT_PX + icon.getWidth() + IMAGE_PADDING_RIGHT_PX;
      }
      
      // By breaking this up we can avoid overriding the listItems hover state
      listItemBase.getStyle().setPaddingLeft(padding, CSSStyleDeclaration.Unit.PX);
    }

    listItemBase.setTextContent(item.toString());
  }
  
  private ImageResource findIcon(M item) {
    for (int i = 0, n = items.length; i < n; i++) {
      if (Utils.equalsOrNull(items[i], item)) {
        return icons[i];
      }
    }
    
    return null;
  }
}
