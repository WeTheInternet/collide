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

package com.google.collide.client.ui.list;

/**
 * An interface which describes an object which maintains selection.
 */
public interface HasSelection<M> {
  
  public void clearSelection();

  /**
   * Selects the next item in the list, returning false if selection cannot be
   * moved for any reason.
   */
  public boolean selectNext();

  /**
   * Selects the previous item in the list, returning false if selection cannot
   * be moved for any reason.
   */
  public boolean selectPrevious();

  public int size();
  
  public boolean setSelectedItem(int index);

  public boolean setSelectedItem(M item);

  public boolean selectNextPage();
  
  public boolean selectPreviousPage();
  
  /**
   * Indicates that the currently selected item should be clicked and the
   * appropriate action must be executed.
   */
  public void handleClick();
  
  /**
   * Returns the selected item, or null.
   */
  public M getSelectedItem();
  
  public int getSelectedIndex();
}
