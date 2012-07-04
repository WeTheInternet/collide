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

import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.FontDimensionsCalculator;

import elemental.canvas.CanvasRenderingContext2D;
import elemental.canvas.TextMetrics;
import elemental.html.CanvasElement;

/**
 * A measurement provider which utilizes an in-memory canvas to measure text.
 *
 */
/*
 * TODO: This is currently unused though it would be preferred. There is
 * a rounding bug in webkit which causes zoom levels to minutely change the
 * width of text. So even though 6px text should always be 6px it is in fact off
 * thanks to webkit. The canvas doesn't have this bug and will always correctly
 * report the width of text correctly. So until a big webkit patch lands fixing
 * rounding bugs we can't switch to this significantly faster provider.
 *
 * BUG: https://bugs.webkit.org/show_bug.cgi?id=60318
 *
 * More Specifically: https://bugs.webkit.org/show_bug.cgi?id=71143
 */
public class CanvasMeasurementProvider implements MeasurementProvider {
  
  private final FontDimensionsCalculator calculator;
  private final CanvasElement canvas;

  public CanvasMeasurementProvider(FontDimensionsCalculator calculator) {
    this.calculator = calculator;
    canvas = Elements.createCanvas();
  }

  @Override
  public double getCharacterWidth() {
    return calculator.getFontDimensions().getCharacterWidth();
  }

  @Override
  public double measureStringWidth(String text) {
    CanvasRenderingContext2D context = (CanvasRenderingContext2D) canvas.getContext("2d");
    context.setFont(calculator.getFont());
    TextMetrics metrics = context.measureText(text);
    return metrics.getWidth();
  }
}
