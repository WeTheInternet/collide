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

import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.NodeConflictDto.ConflictedPath;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;

import elemental.css.CSSStyleDeclaration;
import elemental.html.Element;

/**
 * A trail of "breadcrumbs" representing the current file path and the current
 * code scope where the cursor is placed. If the cursor is inside a function
 * foo() {...} inside class bar, with the current file being
 * /www/widgets/widget.js, then the breadcrumb trail would look like: www >
 * widgets > widget.js > bar > foo.
 */
public class WorkspaceLocationBreadcrumbs extends UiComponent<WorkspaceLocationBreadcrumbs.View> {

  /**
   * Css resources for breadcrumbs.
   */
  public interface Css extends CssResource {
    String breadcrumbBar();

    String breadcrumbWrap();

    String breadcrumbSlash();

    String breadcrumb();

    String start();

    String hide();

    /** Icons */
    String directory();

    String file();

    String cls();

    String function();

    String field();
  }

  /**
   * Client bundle for breadcrumbs.
   */
  /*
   * TODO: Figure out a way to break the css reliance on data resources
   * then use the folder() and file() from base.
   */
  public interface Resources extends ClientBundle {
    @Source("WorkspaceLocationBreadcrumbs.css")
    Css workspaceLocationBreadcrumbsCss();

    @Source("com/google/collide/client/common/folder_breadcrumb.png")
    DataResource directoryImg();

    @Source("com/google/collide/client/common/file_breadcrumb.png")
    DataResource fileImg();

    @Source("path_slash.png")
    DataResource pathSlashImg();
  }

  private static enum PathType {
    FOLDER, FILE, CLASS, FUNCTION, FIELD
  }

  /**
   * A class representing a single breadcrumb.
   */
  static class Breadcrumb extends UiComponent<Breadcrumb.View> {

    /**
     * The view for a single breadcrumb.
     */
    class View extends CompositeView<Void> {

      private Element breadcrumb;
      private final Css css;

      View(Resources res, String name, String iconClass) {
        css = res.workspaceLocationBreadcrumbsCss();
        Element breadcrumbSlash = Elements.createSpanElement(css.breadcrumbSlash());
        breadcrumbSlash.setTextContent("/");

        breadcrumb = Elements.createSpanElement(css.breadcrumb());
        breadcrumb.addClassName(iconClass);
        breadcrumb.setTextContent(name);

        Element breadcrumbWrap = Elements.createSpanElement(css.breadcrumbWrap());
        breadcrumbWrap.appendChild(breadcrumbSlash);
        breadcrumbWrap.appendChild(breadcrumb);

        // Start hidden
        breadcrumb.addClassName(css.start());
        setElement(breadcrumbWrap);
      }

      /**
       * Show by fading+sliding in from the left.
       */
      void show(int startDelay, int duration) {
        CSSStyleDeclaration style = breadcrumb.getStyle();
        // TODO: extract constants
        style.setProperty("-webkit-transition-duration", duration + "ms");
        style.setProperty("-webkit-transition-delay", startDelay + "ms");
        breadcrumb.removeClassName(css.start());
      }

      /**
       * Hide by fading+sliding out to the left.
       */
      void hide(int startDelay) {
        CSSStyleDeclaration style = breadcrumb.getStyle();
        style.setProperty("-webkit-transition-delay", startDelay + "ms");
        getElement().addClassName(css.hide());
      }

      void detach() {
        getElement().removeFromParent();
      }
    }

    private String name;
    private String iconClass;

    Breadcrumb(Resources res, String name, String iconClass) {
      View view = new View(res, name, iconClass);
      setView(view);
      this.name = name;
      this.iconClass = iconClass;
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof Breadcrumb)) {
        return false;
      }
      Breadcrumb crumb = (Breadcrumb) other;
      return crumb.name.equals(name) && crumb.iconClass.equals(iconClass);
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 37 * result + name.hashCode();
      result = 37 * result + iconClass.hashCode();
      return result;
    }
  }

  /**
   * Number of milliseconds to delay between each breadcrumb being animated, to
   * create a path buildup/teardown effect.
   */
  private static final int DELAY_SHOW_TIME_MIN = 100;
  private static final int DELAY_SHOW_TIME_MAX = 200;
  private static final int DELAY_HIDE_TIME = 150;
  private PathUtil currentFilePath = null;
  private JsonArray<Breadcrumb> currentPathCrumbs = JsonCollections.createArray();

  /**
   * The view for the breadcrumbs.
   */
  public static class View extends CompositeView<Void> {

    private final Css css;
    private final Resources res;

    View(Resources res) {
      this.res = res;
      this.css = res.workspaceLocationBreadcrumbsCss();
      Element breadcrumbBar = Elements.createDivElement(css.breadcrumbBar());
      setElement(breadcrumbBar);
    }

    public Breadcrumb createBreadcrumb(String name, PathType type) {
      String iconStyle = "";
      switch (type) {
        case FOLDER:
          iconStyle = css.directory();
          break;
        case FILE:
          iconStyle = css.file();
          break;
        case CLASS:
          iconStyle = css.cls();
          break;
        case FUNCTION:
          iconStyle = css.function();
          break;
        case FIELD:
          iconStyle = css.field();
          break;
      }
      return new Breadcrumb(res, name, iconStyle);
    }

    /**
     * First fade out all elements in remove, then fade in elements in add.
     */
    void updateElements(
        final JsonArray<Breadcrumb.View> remove, final JsonArray<Breadcrumb.View> add) {
      for (int i = remove.size() - 1; i >= 0; i--) {
        remove.get(i).hide(0);
      }
      // Detach elements after animation finishes, then start the add animation
      Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
        @Override
        public boolean execute() {
          for (int i = 0, n = remove.size(); i < n; i++) {
            remove.get(i).detach();
          }
          int n = add.size();
          if (n > 0) {
            for (int i = 0; i < n; i++) {
              getElement().appendChild(add.get(i).getElement());
            }
            // Trigger a browser layout so CSS3 transitions fire
            add.get(0).getElement().getClientWidth();
            int showDuration = Math.min(n * DELAY_SHOW_TIME_MIN, DELAY_SHOW_TIME_MAX) / n;
            for (int i = 0; i < n; i++) {
              add.get(i).show(showDuration * i, showDuration);
            }
          }
          return false;
        }
      }, DELAY_HIDE_TIME);
    }
  }

  /**
   * Calculate the difference between the currently displayed path and the new
   * file+scope paths and animate the change.
   */
  void changeBreadcrumbPath(JsonArray<Breadcrumb> newPath) {
    // find path difference
    JsonArray<Breadcrumb.View> remove = JsonCollections.createArray();
    JsonArray<Breadcrumb.View> add = JsonCollections.createArray();
    /*
     * Walk from the existing beginning to the end, and once a difference is
     * found, mark the rest of the existing path to be removed and the rest of
     * the new path to be added.
     */
    Breadcrumb newBreadcrumb;
    Breadcrumb curBreadcrumb;
    boolean isMatchingPath = true;
    int i = 0;
    for (int n = currentPathCrumbs.size(), m = newPath.size(); i < n || i < m; i++) {
      if (i < n && i < m) {
        // Check for path conflict, add and remove
        curBreadcrumb = currentPathCrumbs.get(i);
        newBreadcrumb = newPath.get(i);
        if (!isMatchingPath || !curBreadcrumb.equals(newBreadcrumb)) {
          isMatchingPath = false;
          remove.add(curBreadcrumb.getView());
          add.add(newBreadcrumb.getView());
        } else {
          // Equal, update new list with old Breadcrumb
          newPath.set(i, curBreadcrumb);
        }
      } else if (i >= n) {
        // No current path here, add new path
        add.add(newPath.get(i).getView());
      } else if (i >= m) {
        // No new path here, remove current path
        remove.add(currentPathCrumbs.get(i).getView());
      }
    }
    currentPathCrumbs = newPath;
    getView().updateElements(remove, add);
  }

  /**
   * Clears the current breadcrumb path.
   */
  public void clearPath() {
    currentFilePath = null;
    changeBreadcrumbPath(JsonCollections.<Breadcrumb>createArray());
  }

  /**
   * Returns the current breadcrumb path.
   */
  public PathUtil getPath() {
    return currentFilePath;
  }

  /**
   * Sets the path that these breadcrumbs display.
   */
  public void setPath(PathUtil newPath) {
    // TODO: Uncomment when onCursorLocationChanged is implemented.
    // editor.getSelection().getCursorListenerRegistrar().add(cursorListener);
    JsonArray<Breadcrumb> newFilePath = JsonCollections.createArray();
    for (int i = 0, n = newPath.getPathComponentsCount(); i < n; i++) {
      if (i == n - 1) {
        newFilePath.add(getView().createBreadcrumb(newPath.getPathComponent(i), PathType.FILE));
      } else {
        newFilePath.add(getView().createBreadcrumb(newPath.getPathComponent(i), PathType.FOLDER));
      }
    }
    currentFilePath = newPath;
    changeBreadcrumbPath(newFilePath);
  }

  /**
   * Sets the path and type that these breadcrumbs display.
   */
  public void setPath(ConflictedPath conflictedPath) {
    PathUtil newPath = new PathUtil(conflictedPath.getPath());
    JsonArray<Breadcrumb> newFilePath = JsonCollections.createArray();
    for (int i = 0, n = newPath.getPathComponentsCount(); i < n; i++) {
      if (i == n - 1 && conflictedPath.getNodeType() == TreeNodeInfo.FILE_TYPE) {
        newFilePath.add(getView().createBreadcrumb(newPath.getPathComponent(i), PathType.FILE));
      } else {
        newFilePath.add(getView().createBreadcrumb(newPath.getPathComponent(i), PathType.FOLDER));
      }
    }
    currentFilePath = newPath;
    changeBreadcrumbPath(newFilePath);
  }

  /*
   * TODO: Implement to show the current scope of the cursor.
   */
  public void onCursorLocationChanged(int lineNumber, int column) {
    // TODO: Do this async to avoid cursor lag.
    JsonArray<Breadcrumb> newPath = currentPathCrumbs.copy();
    // JsoArray<String> scope = autocompleter.getLocationScope(lineNumber,
    // column);
    newPath.add(getView().createBreadcrumb("Line " + lineNumber, PathType.CLASS));
    newPath.add(getView().createBreadcrumb("Column " + column, PathType.FUNCTION));
    changeBreadcrumbPath(newPath);
  }
}
