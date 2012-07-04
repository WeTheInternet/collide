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

import com.google.collide.client.ui.list.SimpleList.ListEventDelegate;
import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.ui.list.SimpleList.View;
import com.google.collide.client.util.Elements;

import elemental.html.AnchorElement;
import elemental.html.Element;
import elemental.html.UListElement;

/**
 * Sidebar list item renderer. It is meant to render anchor tags which include an
 * href. Feel free to set the href to javascript:; if that functionality is unneeded.
 *
 * @param <M> The underlying data type
 */
public class SidebarListItemRenderer<M> extends ListItemRenderer<M> {

  public static <M> SimpleList<M> create(
      SimpleList.Resources res, LinkRenderer<M> renderer, ListEventDelegate<M> delegate) {
    UListElement container = (UListElement) Elements.createElement("ul");
    return create(container, res, renderer, delegate);
  }

  public static <M> SimpleList<M> create(UListElement container, SimpleList.Resources res,
      LinkRenderer<M> renderer, ListEventDelegate<M> delegate) {
    SimpleList.View view = (View) container;
    ListItemRenderer<M> itemRenderer =
        new SidebarListItemRenderer<M>(res.sidebarListCss(), renderer);
    return SimpleList.create(view, res.sidebarListCss(), itemRenderer, delegate);
  }

  /**
   * A renderer which can provide the details for rendering an anchor tag.
   */
  public interface LinkRenderer<M> {
    public String getText(M item);

    public String getHref(M item);
  }

  /**
   * A helper class which encapsulates the text and href of an anchor tag for rendering purposes.
   */
  public static class SidebarLink {
    private final String text;
    private final String href;

    public SidebarLink(String text, String href) {
      this.text = text;
      this.href = href;
    }

    public String getText() {
      return text;
    }

    public String getHref() {
      return href;
    }
  }

  /**
   * A renderer which can render a {@link SidebarLink}.
   */
  public static class SidebarLinkRenderer implements LinkRenderer<SidebarLink> {
    @Override
    public String getText(SidebarLink item) {
      return item.getText();
    }

    @Override
    public String getHref(SidebarLink item) {
      return item.getHref();
    }

  }

  public interface Css extends SimpleList.Css {
    @Override
    String listBase();

    @Override
    String listContainer();

    @Override
    String listItem();

    String anchor();
  }

  private final LinkRenderer<M> renderer;
  private final Css css;

  public SidebarListItemRenderer(Css css, LinkRenderer<M> renderer) {
    this.css = css;
    this.renderer = renderer;
  }

  @Override
  public void render(Element listItemBase, M itemData) {
    AnchorElement anchor = Elements.createAnchorElement(css.anchor());
    anchor.setTextContent(renderer.getText(itemData));
    anchor.setHref(renderer.getHref(itemData));
    listItemBase.appendChild(anchor);
  }

  @Override
  public Element createElement() {
    return Elements.createElement("li");
  }
}
