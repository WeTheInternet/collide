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

import com.google.collide.client.code.FileSelectionController.FileOpenedEvent;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.history.Place;
import com.google.collide.client.search.awesomebox.AbstractActionSection.FilteredActionItem;
import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * An AwesomeBox section which is responsible for presenting the user with
 * possible actions which affect the current editor document.
 */
public class GotoActionSection extends AbstractActionSection<FilteredActionItem> {

  /**
   * The maximum number of results to display when filtering.
   */
  public static final int MAX_RESULTS = 3;
  private Editor editor;
  private boolean isEditableFileOpened = false;

  /**
   * Instantiates a new EditorActionSection
   */
  public GotoActionSection(Resources res) {
    super(res, MAX_RESULTS);
  }

  /**
   * Attach the editor to this awesome box section.
   *
   * @param editor The Collide editor .
   */
  public void attachEditorAndPlace(Place place, Editor editor) {
    this.editor = editor;

    place.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
      @Override
      public void onFileOpened(boolean isEditable, PathUtil filePath) {
        isEditableFileOpened = isEditable;
      }
    });
  }

  @Override
  public boolean onQueryChanged(String query) {
    if (!isEditableFileOpened) {
      return false;
    }

    return super.onQueryChanged(query);
  }

  @Override
  public boolean onShowing(AwesomeBox awesomebox) {
    // no reason to call up to our parent, we don't show.
    return false;
  }

  @Override
  protected JsonArray<FilteredActionItem> getAllActions() {
    JsonArray<FilteredActionItem> actions = JsonCollections.createArray();
    actions.add(new FilteredActionItem(res, "goto line #", ModifierKeys.ACTION, "g") {
      private RegExp numbers = RegExp.compile("(\\d+)");
      private String lastQuery;

      @Override
      public boolean onQueryChanged(String query) {
        if (numbers.test(query)) {
          String number = numbers.exec(query).getGroup(1);
          getElement().setTextContent("goto line " + number);
          lastQuery = query;
          // TODO: An actual disabled style
          getElement().getStyle().setColor("black");
          return true;
        }
        // we defer to testing our goto line label
        getElement().setTextContent("goto line #");
        lastQuery = "";
        getElement().getStyle().setColor("gray");
        return super.onQueryChanged(query);
      }

      @Override
      public boolean onShowing() {
        return false;
      }

      @Override
      public ActionResult doAction(ActionSource source) {
        Preconditions.checkNotNull(editor, "Editor cannot be null");

        MatchResult match = numbers.exec(lastQuery);
        // if the user clicks us without specifying a line
        if (match == null) {
          return ActionResult.DO_NOTHING;
        }

        int line = Integer.parseInt(match.getGroup(1));
        int realLine = Math.min(editor.getDocument().getLineCount() - 1, Math.max(line - 1, 0));
        editor.scrollTo(realLine, 0);
        editor.getFocusManager().focus();

        return ActionResult.CLOSE;
      }
    });
    actions.add(new FilteredActionItem(res, "goto top") {
      @Override
      public ActionResult doAction(ActionSource source) {
        Preconditions.checkNotNull(editor, "Editor cannot be null");
        editor.scrollTo(0, 0);
        editor.getFocusManager().focus();

        return ActionResult.CLOSE;
      }

      @Override
      public boolean onShowing() {
        return false;
      }
    });
    actions.add(new FilteredActionItem(res, "goto end") {
      @Override
      public ActionResult doAction(ActionSource source) {
        Preconditions.checkNotNull(editor, "Editor cannot be null");
        editor.scrollTo(editor.getDocument().getLineCount() - 1, 0);
        editor.getFocusManager().focus();

        return ActionResult.CLOSE;
      }

      @Override
      public boolean onShowing() {
        return false;
      }
    });
    return actions;
  }

  @Override
  protected String getTitle() {
    return "Goto";
  }
}
