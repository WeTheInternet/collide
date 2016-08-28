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

import collide.client.util.Elements;

import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.code.FileSelectionController.FileOpenedEvent;
import com.google.collide.client.history.Place;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.search.awesomebox.AwesomeBox.Resources;
import com.google.collide.client.search.awesomebox.FileNameNavigationSection.FileNavItem;
import com.google.collide.client.util.ClientStringUtils;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.RegExpUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.RegExp;

import elemental.html.SpanElement;

/**
 * Section that performs navigation via file names in the AwesomeBox
 */
public class FileNameNavigationSection extends AbstractAwesomeBoxSection<FileNavItem> {

  /*
   * Maximum number of recently opened files to display in the dropdown list.
   * The list contains at most MAX_RECENT_FILES+1 since the most recent file
   * will be the currently opened file and thus not displayed.
   */
  private static final int MAX_RECENT_FILES = 3;

  private final FileNameSearch searchIndex;
  private final JsonArray<PathUtil> recentFiles;

  // TODO: When code place is gone look into compile time injection.
  private Place currentPlace;

  public FileNameNavigationSection(AwesomeBox.Resources res, FileNameSearch searchIndex) {
    super(res);
    this.searchIndex = searchIndex;
    recentFiles = JsonCollections.createArray();

    createDom();
  }

  public void registerOnFileOpenedHandler(Place currentPlace) {
    this.currentPlace = currentPlace;
    // Subscribe to file notifications for our recent file list
    currentPlace.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
      @Override
      public void onFileOpened(boolean isEditable, PathUtil filePath) {
        // in case it is already there, remove it.
        recentFiles.remove(filePath);

        // check to ensure we aren't over our file limit and then insert
        if (recentFiles.size() > MAX_RECENT_FILES) {
          recentFiles.pop();
        }
        recentFiles.splice(0, 0, filePath);
      }
    });
  }

  /**
   * Encapsulates a file path that is displayed in the awesome box.
   *
   */
  static class FileNavItem extends AbstractAwesomeBoxSection.ActionItem {

    private final SpanElement fileNameElement;
    private final SpanElement folderNameElement;
    private PathUtil filePath;
    private final Place place;

    public FileNavItem(Resources res, Place place) {
      super(res, AwesomeBoxUtils.createSectionItem(res));
      Preconditions.checkNotNull(place, "Place cannot be null");
      this.place = place;
      element.addClassName(res.awesomeBoxSectionCss().fileItem());

      fileNameElement = Elements.createSpanElement();
      folderNameElement = Elements.createSpanElement(res.awesomeBoxSectionCss().folder());

      element.appendChild(fileNameElement);
      element.appendChild(folderNameElement);
    }

    public void setPath(PathUtil path) {
      int size = path.getPathComponentsCount();
      if (size == 1) {
        fileNameElement.setTextContent(path.getPathComponent(0));
        folderNameElement.setTextContent("");
      } else {
        fileNameElement.setTextContent(path.getPathComponent(size - 1));
        folderNameElement.setTextContent(" - " + path.getPathComponent(size - 2));
      }

      filePath = path;
    }

    /**
     * Navigates to this file.
     */
    @Override
    public ActionResult doAction(ActionSource source) {
      place.fireChildPlaceNavigation(FileSelectedPlace.PLACE.createNavigationEvent(filePath));
      return ActionResult.CLOSE;
    }

    /**
     * @return The filename portion of the path
     */
    @Override
    public String completeQuery() {
      return filePath.getBaseName();
    }
  }

  /**
   * Creates the basic DOM for this section
   */
  private void createDom() {
    sectionElement = AwesomeBoxUtils.createSectionContainer(res);
  }

  @Override
  public boolean onQueryChanged(String query) {
    // the most recent file in our list is the current opened one, don't show it
    JsonArray<PathUtil> files = recentFiles.slice(1, MAX_RECENT_FILES+1);
    if (searchIndex != null && !StringUtils.isNullOrEmpty(query)) {
      // TODO: Results come back in the order they appear in the tree
      // there needs to be some sort of twiddler that re-ranks the results so
      // that filenames that are better matches appear higher.
      RegExp reQuery = RegExpUtils.createRegExpForWildcardPattern(
          query, ClientStringUtils.containsUppercase(query) ? "" : "i");
      files = searchIndex.getMatches(reQuery, 5);
    }

    // we don't have anything to display
    if (files.size() == 0) {
      return false;
    }

    showFiles(files);
    return true;
  }

  private void showFiles(JsonArray<PathUtil> files) {
    // Reuse any fileNavItems that are currently out there, don't worry about
    // selection, clearSelection will be called by the AwesomeBox after this
    int i = reuseAnyExistingElements(files);
    for (; i < files.size(); i++) {
      FileNavItem item = new FileNavItem(res, currentPlace);
      item.setPath(files.get(i));
      listItems.add(item);

      sectionElement.appendChild(item.getElement());
    }
  }

  /**
   * Reuses any existing FileNavItem elements in the dropdown section for speed.
   * If the number of existing elements is greater than the number of new
   * elements then the remaining items are removed from the dropdown and our
   * array.
   *
   * @param newItems
   */
  private int reuseAnyExistingElements(JsonArray<PathUtil> newItems) {
    int i = 0;
    for (; i < newItems.size() && i < listItems.size(); i++) {
      listItems.get(i).setPath(newItems.get(i));
    }

    JsonArray<FileNavItem> removed = listItems.asJsonArray().splice(i, listItems.size() - i);
    for (int r = 0; r < removed.size(); r++) {
      removed.get(r).remove();
    }

    return i;
  }

  /**
   * The file name navigation section is never shown initially.
   */
  @Override
  public boolean onShowing(AwesomeBox awesomeBox) {
    // Never show the first file in the list, since it's the current file.
    if (recentFiles.size() > 1) {
      showFiles(recentFiles.slice(1, MAX_RECENT_FILES+1));
      return true;
    }
    return false;
  }
}
