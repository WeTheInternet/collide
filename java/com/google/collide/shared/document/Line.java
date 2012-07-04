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

package com.google.collide.shared.document;

import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.util.JsonCollections;

/*
 * TODO: prevent leaks by forcing a clear on the tags when a line
 * is detached.
 */
/**
 * Model for a line of text (including the newline character) in a document.
 *
 *  Lines by design do not know their line numbers since that would require a
 * line insertion/deletion to iterate through all of the following lines to
 * update the line numbers, thus making insertion/deletion slower operations
 * than they have to be. The {@link LineFinder} can help to efficiently resolve
 * a line number given the line, or vice versa.
 *
 * Lines can have tags attached to them by clients of this class.
 */
public class Line implements TaggableLine {

  static Line create(Document document, String text) {
    return new Line(document, text);
  }

  private boolean attached;

  private final Document document;

  private Line nextLine;

  private Line previousLine;

  // Not final so we can do O(1) clearTags
  private JsonStringMap<Object> tags;

  private String text;

  private Line(Document document, String text) {
    this.document = document;
    this.text = text;

    tags = JsonCollections.createMap();
  }

  public Document getDocument() {
    return document;
  }

  public Line getNextLine() {
    return nextLine;
  }

  @Override
  public Line getPreviousLine() {
    return previousLine;
  }

  /**
   * Gets a tag set on this line with {@link #putTag}. This serves as storage
   * for arbitrary objects for this line. For example, a document parser may
   * store its snapshot here.
   *
   *  It is the client's responsibility to ensure type safety, this method
   * blindly casts as a convenience.
   *
   * @param key the unique identifier for this tag. In order to prevent
   *        namespace collisions, prefix this with the caller's fully qualified
   *        class name
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getTag(String key) {
    return (T) tags.get(key);
  }

  @SuppressWarnings("unchecked")
  public <T> T removeTag(String key) {
    return (T) tags.remove(key);
  }

  public String getText() {
    return text;
  }

  public boolean hasColumn(int column) {
    return column >= 0 && column < text.length();
  }

  public boolean isAttached() {
    return attached;
  }

  public int length() {
    return text.length();
  }

  /**
   * Puts a tag on this line.
   *
   * @see Line#getTag(String)
   */
  @Override
  public <T> void putTag(String key, T value) {
    tags.put(key, value);
  }
  
  /**
   * This is not public API and will eventually be hidden in the public
   * interface
   */
  public void clearTags() {
    tags = JsonCollections.createMap();
  }

  @Override
  public String toString() {
    String trimmedText = text.trim();
    return (trimmedText.length() > 50 ? trimmedText.substring(0, 50) + "..." : trimmedText);
  }

  @Override
  public boolean isFirstLine() {
    return getPreviousLine() == null;
  }

  @Override
  public boolean isLastLine() {
    return getNextLine() == null;
  }

  void setAttached(boolean attached) {
    this.attached = attached;
  }

  void setNextLine(Line nextLine) {
    this.nextLine = nextLine;
  }

  void setPreviousLine(Line previousLine) {
    this.previousLine = previousLine;
  }

  void setText(String text) {
    this.text = text;
  }
}
