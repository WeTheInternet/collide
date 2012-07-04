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

package com.google.collide.client;

/**
 * Information about the client operating and/or windowing system.
 */
public interface ClientOs {
  
  /**
   * @return a slightly longer string for text describing action keys, i.e.
   * "Command" or "Control"
   */
  public String actionKeyDescription();

  /**
   * The canonical prefix for an action key (e.g. clover for Mac, "Ctrl" for
   * others).
   *
   * @return a very short string suitable for menu shortcuts 
   */
  public String actionKeyLabel();
  
  public boolean isMacintosh();

  /**
   * @return a slightly longer string for text describing shift keys, i.e.
   * "Shift".
   */
  public String shiftKeyDescription();

  /**
   * The canonical prefix for a shift key (e.g. uparrow for Mac, "Shift" for
   * others).
   *
   * @return a very short string suitable for menu shortcuts 
   */
  public String shiftKeyLabel();

  /**
   * @return a string suitable for naming the Alt key
   */
  public String altKeyDescription();

  /**
   * @return a very short string suitable for menu shortcuts needing the Alt key
   */
  public String altKeyLabel();
  
  /**
   * @return a string suitable for naming the Ctrl key
   */
  public String ctrlKeyDescription();

  /**
   * @return a very short string suitable for menu shortcuts needing the Ctrl key
   */
  public String ctrlKeyLabel();
}