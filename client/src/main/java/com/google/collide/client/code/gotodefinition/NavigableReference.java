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

package com.google.collide.client.code.gotodefinition;

import com.google.collide.client.documentparser.AsyncParser;
import com.google.collide.dto.CodeReference;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * A reference in a file that can be navigated somewhere. A reference always has
 * a range in the file and optional snippet that tells something about where it
 * is pointing to.
 *
 */
abstract class NavigableReference implements AsyncParser.LineAware {

  private final int lineNumber;
  private final int startColumn;
  private final int endColumn;
  private final String snippet;

  private NavigableReference(int lineNumber, int startColumn, int endColumn, String snippet) {
    Preconditions.checkArgument(endColumn >= startColumn);
    this.lineNumber = lineNumber;
    this.startColumn = startColumn;
    this.endColumn = endColumn;
    this.snippet = snippet;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndColumn() {
    return endColumn;
  }

  public String getSnippet() {
    return snippet;
  }

  public static FileReference createToFile(CodeReference reference) {
    return new FileReference(reference.getReferenceStart().getLineNumber(),
        reference.getReferenceStart().getColumn(), reference.getReferenceEnd().getColumn(),
        reference.getTargetSnippet(), reference.getTargetFilePath(),
        reference.getTargetStart().getLineNumber(), reference.getTargetStart().getColumn());
  }

  public static FileReference createToFile(int lineNumber, int startColumn, int endColumn,
      String path) {
    return new FileReference(lineNumber, startColumn, endColumn, null, path, 0, 0);
  }

  public static FileReference createToFile(int lineNumber, int startColumn, int endColumn,
      String path, int targetLineNumber, int targetColumn) {
    return new FileReference(
        lineNumber, startColumn, endColumn, null, path, targetLineNumber, targetColumn);
  }

  public static UrlReference createToUrl(int lineNumber, int startColumn, String url) {
    return new UrlReference(lineNumber, startColumn, null, url);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NavigableReference)) {
      return false;
    }

    NavigableReference that = (NavigableReference) o;
    return this.endColumn == that.endColumn && this.lineNumber == that.lineNumber
        && this.startColumn == that.startColumn
        && (this.snippet == null ? that.snippet == null : snippet.equals(that.snippet));
  }

  abstract void navigate(ReferenceNavigator navigator);

  /**
   * @return short target context displayed to user or {@code null} if nothing
   *         to display as context
   */
  abstract String getTargetName();

  @VisibleForTesting
  static final class FileReference extends NavigableReference {
    private final String targetFilePath;
    private final int targetLineNumber;
    private final int targetColumn;

    private FileReference(int lineNumber, int startColumn, int endColumn, String snippet,
        String targetFilePath, int targetLineNumber, int targetColumn) {
      super(lineNumber, startColumn, endColumn, snippet);
      this.targetFilePath = targetFilePath;
      this.targetLineNumber = targetLineNumber;
      this.targetColumn = targetColumn;
    }

    @Override
    void navigate(ReferenceNavigator navigator) {
      navigator.goToFile(targetFilePath, targetLineNumber, targetColumn);
    }

    @Override
    String getTargetName() {
      return targetFilePath;
    }

    public String getTargetFilePath() {
      return targetFilePath;
    }

    public String toString() {
      return "(" + getLineNumber() + "," + getStartColumn() + "-" + getEndColumn() + ") to file \""
          + targetFilePath + "\", target position: (" + targetLineNumber + ", " + targetColumn
          + "\"";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FileReference) || !super.equals(o)) {
        return false;
      }

      FileReference that = (FileReference) o;
      return this.targetColumn == that.targetColumn
          && this.targetLineNumber == that.targetLineNumber
          && (this.targetFilePath == null
              ? that.targetFilePath == null
              : this.targetFilePath.equals(that.targetFilePath));
    }
  }

  @VisibleForTesting
  static final class UrlReference extends NavigableReference {
    private final String url;

    private UrlReference(int lineNumber, int startColumn, String snippet, String url) {
      super(lineNumber, startColumn, startColumn + url.length() - 1, snippet);
      this.url = url;
    }

    @Override
    void navigate(ReferenceNavigator navigator) {
      navigator.goToUrl(url);
    }

    @Override
    String getTargetName() {
      return null;
    }

    public String getUrl() {
      return url;
    }

    public String toString() {
      return "(" + getLineNumber() + "," + getStartColumn() + "-" + getEndColumn() + ") to URL \""
          + url + "\"";
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UrlReference) || !super.equals(o)) {
        return false;
      }

      UrlReference that = (UrlReference) o;
      return this.url == null ? that.url == null : this.url.equals(that.url);
    }
  }
}
