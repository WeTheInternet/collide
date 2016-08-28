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
import com.google.collide.client.search.awesomebox.AwesomeBox.SectionIterationCallback;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerManager.Dispatcher;

/**
 * The underlying AwesomeBox model used accross all AwesomeBox instances.
 */
// TODO: Provide push/pop context functionality so its much easier to go
// into a temporarily restricted context with limited actions.
public class AwesomeBoxModel {
  /**
   * Called when the context has been updated in the AwesomeBox model.
   */
  public interface ContextChangeListener {
    /**
     * @param contextAlreadyActive true if
     *        {@link AwesomeBoxModel#changeContext(AwesomeBoxContext)} was
     *        called with a context which is already active. This case is just
     *        for informational purposes and no changes are actually performed.
     */
    public void onContextChanged(boolean contextAlreadyActive);
  }

  /**
   * Modes which change the behavior of getSelection.
   */
  public enum SelectMode {
    /**
     * Returns null if there is no selection.
     */
    DEFAULT,
    /**
     * Will attempt to select the first selectable item in the drop-down if
     * there isn't currently a selection.
     */
    TRY_AUTOSELECT_FIRST_ITEM
  }

  /**
   * Modes which change the hide behavior of the dialog.
   */
  public enum HideMode {
    /**
     * The component will autohide when the user clicks outside of the
     * AwesomeBox container or the actual input loses focus.
     */
    AUTOHIDE,
    /**
     * The component will autohide only if user clicks outside of the AwesomeBox
     * container. This allows the AwesomeBox input to be hidden but the popup to
     * stay visible.
     */
    DONT_HIDE_ON_INPUT_LOSE_FOCUS,
    /**
     * The component must be manually closed or programatically closed.
     */
    NO_AUTOHIDE,
  }

  private AwesomeBoxContext currentContext;
  private AwesomeBoxSection selectedSection;
  private final ListenerManager<ContextChangeListener> listener;

  public AwesomeBoxModel() {
    currentContext = AwesomeBoxContext.DEFAULT;
    listener = ListenerManager.create();
  }

  /**
   * Retrieves the current AwesomeBox autohide behavior.
   */
  public HideMode getHideMode() {
    return currentContext.getHideMode();
  }

  /**
   * Attempts to change the selection to the new section, if the section refuses
   * it will return false. If the section accepts selection any old selection
   * will be cleared. In the special case where the section is already selected
   * it will clear the selection and select the first or last item depending on
   * selectFirstItem.
   *
   * @param selectFirstItem True to select the first item, false to select the
   *        last.
   *
   * @return true if the selection is set to the new section.
   */
  boolean trySetSelection(AwesomeBoxSection section, boolean selectFirstItem) {
    if (selectedSection == section) {
      selectedSection.onClearSelection();
      selectedSection.onMoveSelection(selectFirstItem);
    } else if (section.onMoveSelection(selectFirstItem)) {
      if (selectedSection != null) {
        selectedSection.onClearSelection();
      }
      selectedSection = section;
    } else {
      return false;
    }
    return true;
  }

  /**
   * Retrieves the currently selected section.
   *
   * @return null if there is no selection.
   */
  AwesomeBoxSection getSelection(SelectMode mode) {
    if (selectedSection == null && mode == SelectMode.TRY_AUTOSELECT_FIRST_ITEM) {
      selectFirstItem();
    }
    return selectedSection;
  }

  /**
   * Updates the model selection. Without checking if the section will accept
   * selection. TrySetSelection should be preferred if you can't be sure the
   * section will accept selection.
   */
  void setSelection(AwesomeBoxSection section) {
    if (selectedSection != section && selectedSection != null) {
      selectedSection.onClearSelection();
    }
    selectedSection = section;
  }

  void clearSelection() {
    if (selectedSection != null) {
      selectedSection.onClearSelection();
    }
    selectedSection = null;
  }

  /**
   * Will iterate through the sections until it finds the first section which
   * will accept selection and returns it.
   *
   * @return null if there are no no sections or no section accepts selection.
   */
  AwesomeBoxSection selectFirstItem() {
    if (selectedSection != null) {
      selectedSection.onClearSelection();
    }

    JsonArray<AwesomeBoxSection> sections = currentContext.getSections();
    for (int i = 0; i < sections.size(); i++) {
      if (sections.get(i).onMoveSelection(true)) {
        selectedSection = sections.get(i);
        return sections.get(i);
      }
    }

    return null;
  }

  public ListenerManager<ContextChangeListener> getContextChangeListener() {
    return listener;
  }

  /**
   * @return the current context of the AwesomeBox.
   */
  public AwesomeBoxContext getContext() {
    return currentContext;
  }

  /**
   * Changes to the specified context.
   */
  public void changeContext(AwesomeBoxContext context) {
    if (currentContext == context) {
      listener.dispatch(new Dispatcher<ContextChangeListener>() {
        @Override
        public void dispatch(ContextChangeListener listener) {
          listener.onContextChanged(true);
        }
      });
      return;
    }

    clearSelection();
    currentContext = context;

    // Notify contexts of change event
    JsonArray<AwesomeBoxSection> sections = context.getSections();
    for (int i = 0; i < sections.size(); i++) {
      sections.get(i).onContextChanged(currentContext);
    }

    // Dispatch the onContextChanged event
    listener.dispatch(new Dispatcher<ContextChangeListener>() {
      @Override
      public void dispatch(ContextChangeListener listener) {
        listener.onContextChanged(false);
      }
    });
  }

  /**
   * @return The sections in the current context.
   */
  public JsonArray<AwesomeBoxSection> getCurrentSections() {
    return currentContext.getSections();
  }

  /**
   * Iterates through the section list starting at the specified section
   * (exclusive). This is a helper function that simplifies finding the next or
   * previous section.
   *
   * @param startSection Section to start iterating from (exclusive).
   * @param forward Direction to iterate.
   * @param sectionIterationCallback Callback to call for each iteration.
   */
  void iterateFrom(AwesomeBoxSection startSection, boolean forward,
      SectionIterationCallback sectionIterationCallback) {
    JsonArray<AwesomeBoxSection> sections = currentContext.getSections();

    for (int i = 0; i < sections.size(); i++) {
      if (startSection == sections.get(i)) {
        if (forward) {
          iterateForward(i + 1, sectionIterationCallback);
        } else {
          iterateBackwards(i - 1, sectionIterationCallback);
        }
        return;
      }
    }
  }

  /**
   * Iterates the section list starting at the given index moving backwards to
   * the beginning.
   */
  private void iterateBackwards(int index, SectionIterationCallback sectionIterationCallback) {
    JsonArray<AwesomeBoxSection> sections = currentContext.getSections();
    for (int i = index; i >= 0; i--) {
      if (!sectionIterationCallback.onIteration(sections.get(i))) {
        return;
      }
    }
  }

  /**
   * Iterates the section list starting at the given index and moving forward to
   * the end.
   */
  private void iterateForward(int index, SectionIterationCallback sectionIterationCallback) {
    JsonArray<AwesomeBoxSection> sections = currentContext.getSections();
    for (int i = index; i < sections.size(); i++) {
      if (!sectionIterationCallback.onIteration(sections.get(i))) {
        return;
      }
    }
  }
}
