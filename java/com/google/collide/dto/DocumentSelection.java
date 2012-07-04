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

package com.google.collide.dto;

/**
 * A DTO that models a selection within a document.
 */
public interface DocumentSelection {

  /** Returns the position of the anchor of the selection */
  FilePosition getBasePosition();

  /** Returns the position of the cursor */
  FilePosition getCursorPosition();
  
  /** Returns the user's ID */
  String getUserId();
}
