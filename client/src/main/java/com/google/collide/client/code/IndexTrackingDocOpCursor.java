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

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

// TODO: clean up the API such that clients do not have to call super
/**
 * {@link DocOpCursor} implementation that tracks the current index.
 *
 * Subclasses must call through to the super methods of each override. This
 * class overrides some {@link DocOpCursor} methods with an empty implementation
 * as a convenience.
 */
public abstract class IndexTrackingDocOpCursor implements DocOpCursor {

  /**
   * The index where the most recently-visited component began.
   */
  private int beginIndex;

  /**
   * The index where the most recently-visited component ended. (Inclusive)
   */
  private int endIndex = -1;

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
  }

  @Override
  public void characters(String chars) {
    beginIndex = endIndex + 1;
    endIndex = beginIndex + chars.length() - 1;
  }

  @Override
  public void deleteCharacters(String chars) {
  }

  @Override
  public void deleteElementEnd() {
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
  }

  @Override
  public void elementEnd() {
    beginIndex = ++endIndex;
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    beginIndex = ++endIndex;
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
  }

  @Override
  public void retain(int itemCount) {
    beginIndex = endIndex + 1;
    endIndex = beginIndex + itemCount - 1;
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
  }

  protected int getBeginIndex() {
    return beginIndex;
  }

  protected int getEndIndex() {
    return endIndex;
  }

}
