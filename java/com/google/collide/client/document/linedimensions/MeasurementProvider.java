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

package com.google.collide.client.document.linedimensions;

/**
 * An object which exposes methods which can be used to obtain the width of a
 * string.
 *
 */
interface MeasurementProvider {
  /**
   * Measures a string of text and returns its rendered width in pixels.
   */
  public double measureStringWidth(String text);

  /**
   * Returns the width in pixels of a single-standard latin character.
   */
  public double getCharacterWidth();
}
