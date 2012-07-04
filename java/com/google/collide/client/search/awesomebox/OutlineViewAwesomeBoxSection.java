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

import com.google.collide.client.editor.Editor;
import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.ViewListController;
import com.google.collide.client.workspace.outline.OutlineModel;
import com.google.collide.client.workspace.outline.OutlineNode;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.HasView;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

import elemental.html.Element;
import elemental.html.SpanElement;

/**
 * An awesome box section which displays the classes/functions/variables in your file.
 *
 */
public class OutlineViewAwesomeBoxSection
    extends AbstractAwesomeBoxSection<OutlineViewAwesomeBoxSection.OutlineItem> {

  /**
   * An item in the awesomebox which displays an OutlineNode.
   */
  public static class OutlineItem extends AbstractAwesomeBoxSection.ActionItem
      implements HasView<OutlineItem.View> {

    /** A handler called when a node is selected */
    public interface SelectedHandler {
      void onSelected(OutlineNode node);
    }

    /** A factory which can create an {@link OutlineItem} */
    public static class OutlineItemFactory implements ViewListController.Factory<OutlineItem> {

      private final Resources res;
      private final SelectedHandler handler;

      public OutlineItemFactory(Resources res, SelectedHandler handler) {
        this.res = res;
        this.handler = handler;
      }

      @Override
      public OutlineItem create(Element container) {
        View v = new View(res);
        container.appendChild(v.getElement());
        return new OutlineItem(res, v, handler);
      }
    }

    public static class View extends CompositeView<Void> {
      private final SpanElement name;
      private final SpanElement type;

      public View(Resources res) {
        super(AwesomeBoxUtils.createSectionItem(res));

        name = Elements.createSpanElement();
        type = Elements.createSpanElement();
        type.getStyle().setColor("#AAA");

        getElement().appendChild(name);
        getElement().appendChild(type);
      }
    }

    private final View view;
    private final SelectedHandler handler;
    private OutlineNode node;

    private OutlineItem(Resources res, View view, SelectedHandler handler) {
      super(res, view.getElement());
      Preconditions.checkNotNull(handler, "Handle for outline element cannot be null");
      this.view = view;
      this.handler = handler;
    }

    public void setOutlineNode(OutlineNode node) {
      this.node = node;
      view.name.setTextContent(node.getName());
      view.type.setTextContent(" - " + node.getType().toString().toLowerCase());
    }

    @Override
    public String completeQuery() {
      return node.getName();
    }

    @Override
    public ActionResult doAction(ActionSource source) {
      handler.onSelected(node);
      return ActionResult.CLOSE;
    }

    @Override
    public View getView() {
      return view;
    }
  }

  /** The maximum number of results to display */
  private static final int MAX_RESULTS = 6;
  /** A prefix which can be used to force the awesomebox to show only outline results */
  private static final String QUERY_PREFIX = "@";

  private final ViewListController<OutlineItem> listController;
  private OutlineModel model;
  private Editor editor;

  public OutlineViewAwesomeBoxSection(Resources res) {
    super(res);
    sectionElement = AwesomeBoxUtils.createSectionContainer(res);

    this.listController = new ViewListController<OutlineItem>(
        sectionElement, listItems.asJsonArray(),
        new OutlineItem.OutlineItemFactory(res, new OutlineItem.SelectedHandler() {
          @Override
          public void onSelected(OutlineNode node) {
            if (editor != null) {
              editor.scrollTo(node.getLineNumber(), node.getColumn());
              editor.getFocusManager().focus();
            }
          }
        }));
  }

  @Override
  public boolean onQueryChanged(String query) {
    listController.reset();
    if (!StringUtils.isNullOrWhitespace(query) && !query.equals(QUERY_PREFIX) && model != null) {
      traverseNode(model.getRoot(), query.startsWith(QUERY_PREFIX) ? query.substring(1) : query);
    }
    listController.prune();
    return listController.size() > 0;
  }

  @Override
  public boolean onShowing(AwesomeBox awesomeBox) {
    return false;
  }

  public void setOutlineModelAndEditor(OutlineModel model, Editor editor) {
    this.model = model;
    this.editor = editor;
  }

  private void traverseNode(OutlineNode parent, String query) {
    if (listController.size() >= MAX_RESULTS || parent == null) {
      return;
    }

    JsonArray<OutlineNode> children = parent.getChildren();
    for (int i = 0; listController.size() < MAX_RESULTS && i < children.size(); i++) {
      OutlineNode node = children.get(i);
      if (node.getName().contains(query)) {
        OutlineItem item = listController.next();
        item.setOutlineNode(node);
      }

      traverseNode(children.get(i), query);
    }
  }
}
