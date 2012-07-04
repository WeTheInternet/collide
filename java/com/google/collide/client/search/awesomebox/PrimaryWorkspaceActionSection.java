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
import com.google.collide.client.history.Place;
import com.google.collide.client.search.SearchPlace;
import com.google.collide.client.search.awesomebox.components.FindReplaceComponent.FindMode;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.common.base.Preconditions;

/**
 * Contains the primary actions for a workspace that are always shown and quickly accesible by a
 * user.
 *
 */
public class PrimaryWorkspaceActionSection
    extends AbstractActionSection<AbstractActionSection.FilteredActionItem> {

  /**
   * The abstract action section usually filters actions so it expects a maximum number of actions
   * to display.
   */
  public static final int NUMBER_OF_ACTIONS = 2;

  public interface FindActionSelectedCallback {
    public void onSelected(FindMode mode);
  }

  private final ListenerManager<FindActionSelectedCallback> listenerManager;
  private boolean showFindAndReplace = false;

  private Place currentPlace;

  public PrimaryWorkspaceActionSection(AwesomeBox.Resources res) {
    super(res, NUMBER_OF_ACTIONS);
    this.listenerManager = ListenerManager.create();
  }

  public void registerOnFileOpenedHandler(Place currentPlace) {
    this.currentPlace = currentPlace;
    currentPlace.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
      @Override
      public void onFileOpened(boolean isEditable, PathUtil filePath) {
        // If the file is editable then we can find/replace through it
        showFindAndReplace = isEditable;
      }
    });
  }

  public ListenerManager<FindActionSelectedCallback> getFindActionSelectionListener() {
    return listenerManager;
  }

  @Override
  protected JsonArray<AbstractActionSection.FilteredActionItem> getAllActions() {
    JsonArray<AbstractActionSection.FilteredActionItem> allActions = JsonCollections.createArray();
    allActions.add(new FilteredActionItem(res, "Find in this file...", ModifierKeys.ACTION, "F") {
      @Override
      public void initialize() {
        getElement().addClassName(res.awesomeBoxSectionCss().searchIcon());
      }

      @Override
      public boolean onQueryChanged(String query) {
        return showFindAndReplace;
      }

      @Override
      public boolean onShowing() {
        return showFindAndReplace;
      }

      @Override
      public ActionResult doAction(ActionSource source) {
        listenerManager.dispatch(new Dispatcher<FindActionSelectedCallback>() {
          @Override
          public void dispatch(FindActionSelectedCallback listener) {
            listener.onSelected(FindMode.FIND);
          }
        });
        return ActionResult.DO_NOTHING;
      }
    });

    allActions.add(new FilteredActionItem(res, "Replace in this file...", ModifierKeys.ACTION
        | ModifierKeys.SHIFT, "F") {
      @Override
      public boolean onQueryChanged(String query) {
        return showFindAndReplace;
      }

      @Override
      public boolean onShowing() {
        return showFindAndReplace;
      }

      @Override
      public ActionResult doAction(ActionSource source) {
        listenerManager.dispatch(new Dispatcher<FindActionSelectedCallback>() {
          @Override
          public void dispatch(FindActionSelectedCallback listener) {
            listener.onSelected(FindMode.REPLACE);
          }
        });
        return ActionResult.DO_NOTHING;
      }
    });

    if (false) {
      // disabled since find in this branch is not ready for launch
      allActions.add(new FilteredActionItem(res, "Find in this branch...") {
        private String lastQuery = "";

        @Override
        public boolean onQueryChanged(String query) {
          lastQuery = query;
          return true;
        }

        @Override
        public boolean onShowing() {
          return true;
        }

        @Override
        public ActionResult doAction(ActionSource source) {
          Preconditions.checkNotNull(currentPlace, "Place cannot be null");
          currentPlace.fireChildPlaceNavigation(SearchPlace.PLACE.createNavigationEvent(lastQuery));
          return ActionResult.CLOSE;
        }
      });
    }

    /**
     * This assert is just here as a handy reminder to update assumptions if the situation changes
     * and to prevent someone from pulling their hair out.
     */
    assert allActions.size() == NUMBER_OF_ACTIONS;
    return allActions;
  }

  @Override
  protected String getTitle() {
    return "Actions";
  }

}
