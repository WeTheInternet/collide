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

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document.LineListener;
import com.google.collide.shared.util.StringUtils;

import junit.framework.Assert;

/**
 * Test utility methods for document mutation.
 */
public final class DocumentTestUtils {

  public static void deleteAndAssertEquals(Line line, int column, int deleteCount,
      String expectedText) {
    Document doc = line.getDocument();
    String deletedText = doc.getText(line, column, deleteCount);
    final int numberOfNewlinesInDeletedRange =
        StringUtils.countNumberOfOccurrences(deletedText, "\n");
    class MyLineListener implements LineListener {
      boolean called = false;

      @Override
      public void onLineAdded(Document document, int lineNumber,
          JsonArray<Line> addedLines) {
        Assert.fail();
      }

      @Override
      public void onLineRemoved(Document document, int lineNumber,
          JsonArray<Line> removedLines) {
        Assert.assertEquals(numberOfNewlinesInDeletedRange, removedLines.size());
        called = true;
      }
    }

    MyLineListener lineListener = new MyLineListener();

    doc.getLineListenerRegistrar().add(lineListener);
    try {
      doc.deleteText(line, column, deleteCount);
    } finally {
      doc.getLineListenerRegistrar().remove(lineListener);
    }

    if (numberOfNewlinesInDeletedRange != 0) {
      Assert.assertTrue(lineListener.called);
    }

    Assert.assertEquals(expectedText, line.getText());
  }

}
