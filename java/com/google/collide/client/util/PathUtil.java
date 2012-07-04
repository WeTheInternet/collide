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

package com.google.collide.client.util;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

/**
 * Utility class for dealing with File paths on the client.
 *
 * <p>This class is immutable.
 *
 * TODO: We may need the equivalent of this on the server as well.  If we do,
 * might want to drop the use of native collections and put it in shared.
 */
public class PathUtil implements Comparable<PathUtil> {

  public static final String SEP = "/";

  public static final PathUtil EMPTY_PATH = new PathUtil("");

  public static final PathUtil WORKSPACE_ROOT = new PathUtil(SEP);

  /**
   * Creates a PathUtil composed of the components of <code>first</code>
   * followed by the components of <code>second>.
   */
  public static PathUtil concatenate(PathUtil first, PathUtil second) {
    JsonArray<String> components = first.pathComponentsList.copy();
    components.addAll(second.pathComponentsList);
    return new PathUtil(components);
  }

  /**
   * Creates a PathUtil that has all the components of the supplied from
   * PathUtil, excluding some number of path components from the end.
   */
  public static PathUtil createExcludingLastN(PathUtil from, int componentsToExclude) {
    JsonArray<String> result = JsonCollections.createArray();
    for (int i = 0, n = from.getPathComponentsCount() - componentsToExclude; i < n; ++i) {
      result.add(from.getPathComponent(i));
    }
    return new PathUtil(result);
  }

  /**
   * Creates a PathUtil that has all the components of the supplied from
   * PathUtil, excluding some number of path components from the beginning.
   */
  public static PathUtil createExcludingFirstN(PathUtil from, int componentsToExclude) {
    JsonArray<String> result = JsonCollections.createArray();
    for (int i = componentsToExclude, n = from.getPathComponentsCount(); i < n; ++i) {
      result.add(from.getPathComponent(i));
    }
    return new PathUtil(result);
  }

  public static PathUtil createFromPathComponents(JsonArray<String> pathComponentsList) {
    return new PathUtil(pathComponentsList);
  }

  private final JsonArray<String> pathComponentsList;

  private PathUtil(JsonArray<String> pathComponentsList) {
    this.pathComponentsList = pathComponentsList;
  }

  public PathUtil(String path) {
    pathComponentsList = JsonCollections.createArray();

    if (path != null && !path.isEmpty()) {
      JsonArray<String> pieces = StringUtils.split(path, SEP);

      for (int i = 0, n = pieces.size(); i < n; i++) {
        String piece = pieces.get(i);
        /*
         * Ignore empty string components or "." which stands for current directory
         */
        if (!StringUtils.isNullOrEmpty(piece) && !piece.equals(".")) {
          pathComponentsList.add(piece);
        }
      }
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PathUtil) {
      PathUtil other = (PathUtil) obj;
      return getPathString().equals(other.getPathString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getPathString().hashCode();
  }

  public String getBaseName() {
    int numComponents = getPathComponentsCount();
    String baseName = "";
    if (numComponents > 0) {
      baseName = getPathComponent(numComponents - 1);
    }
    return baseName;
  }

  /**
   * @return the String representation of the path. Path Strings, except for the
   *         EMPTY_PATH, always start with a leading "/", implying that the path
   *         is relative to the workspace root.
   */
  public String getPathString() {
    if (this == EMPTY_PATH || pathComponentsList.size() == 0) {
      return "";
    }
    return SEP + pathComponentsList.join(SEP);
  }

  public int getPathComponentsCount() {
    return pathComponentsList.size();
  }

  public String getPathComponent(int index) {
    return pathComponentsList.get(index);
  }

  /**
   * Returns whether the given {@code path} is a child of this path (or is
   * the same as this path). For example, a path "/tmp" contains "/tmp" and
   * it also contains "/tmp/something".
   */
  public boolean containsPath(PathUtil path) {
    Preconditions.checkNotNull(path, "Containing path must not be null");
    JsonArray<String> otherPathComponents = path.pathComponentsList;
    if (otherPathComponents.size() < pathComponentsList.size()) {
      // The path given has less components than ours, it cannot be a child
      return false;
    }

    for (int i = 0; i < pathComponentsList.size(); i++) {
      if (!pathComponentsList.get(i).equals(otherPathComponents.get(i))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns a new path util that is relative to the given parent path. For this to work parent must
   * contain the current path. i.e. if the path is /tmp/alex.txt and you pass in a path of /tmp then
   * you will get /alex.txt.
   *
   * @return null if parent is invalid
   */
  public PathUtil makeRelativeToParent(PathUtil parent) {
    Preconditions.checkNotNull(parent, "Parent path cannot be null");
    JsonArray<String> parentPathComponents = parent.pathComponentsList;
    if (parentPathComponents.size() > pathComponentsList.size()) {
      // The path given has the same or more components, it can't be a parent
      return null;
    }

    for (int i = 0; i < parentPathComponents.size(); i++) {
      // this means that this is not our parent i.e. /a/b/t.txt vs /a/c/
      if (!parentPathComponents.get(i).equals(pathComponentsList.get(i))) {
        return null;
      }
    }

    return new PathUtil(pathComponentsList.slice(
        parentPathComponents.size(), pathComponentsList.size()));
  }

  /**
   * Delegates to {@link #getPathString()} to return the string representation
   * of this object.
   */
  @Override
  public String toString() {
    return getPathString();
  }

  /**
   * Compares two {@link PathUtil}s lexicographically on each path component.
   */
  @Override
  public int compareTo(PathUtil that) {
    JsonArray<String> components1 = this.pathComponentsList;
    JsonArray<String> components2 = that.pathComponentsList;

    // First compare path components except the last one (file's name).
    for (int i = 0; i + 1 < components1.size() && i + 1 < components2.size(); ++i) {
      int result = components1.get(i).compareTo(components2.get(i));
      if (result != 0) {
        return result;
      }
    }

    // Sub-folders go after the parent folder.
    int lengthDiff = components1.size() - components2.size();
    if (lengthDiff != 0) {
      return lengthDiff;
    }

    int lastComponent = components1.size() - 1;
    if (lastComponent < 0) {
      return 0;
    }

    // Finally, compare the file names.
    return components1.get(lastComponent).compareTo(components2.get(lastComponent));
  }

  /**
   * @return file extension or {@code null} if file has no extension
   */
  public String getFileExtension() {
    return getFileExtension(getPathString());
  }

  /**
   * Returns the file extension of a given file path.
   *
   * @param filePath the file path
   * @return file extension or {@code null} if file has no extension
   */
  public static String getFileExtension(String filePath) {
    int lastSlashPos = filePath.lastIndexOf('/');
    int lastDotPos = filePath.lastIndexOf('.');
    // If (lastSlashPos > lastDotPos) then dot is somewhere in parent directory and file has
    // no extension.
    if (lastDotPos < 0 || lastSlashPos > lastDotPos) {
      return null;
    }
    return filePath.substring(lastDotPos + 1);
  }

  /**
   * Builder for {@link PathUtil}.
   */
  public static class Builder {
    private final JsonArray<String> pathComponentsList;

    public Builder() {
      pathComponentsList = JsonCollections.createArray();
    }

    public Builder addPathComponent(String component) {
      pathComponentsList.add(component);
      return this;
    }

    public Builder addPathComponents(JsonArray<String> components) {
      pathComponentsList.addAll(components);
      return this;
    }

    public Builder addPath(PathUtil path) {
      pathComponentsList.addAll(path.pathComponentsList);
      return this;
    }

    public PathUtil build() {
      return new PathUtil(pathComponentsList);
    }
  }
}
