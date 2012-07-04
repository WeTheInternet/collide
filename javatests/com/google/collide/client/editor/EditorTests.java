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

package com.google.collide.client.editor;

import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.AnchorUtils;
import com.google.collide.shared.document.anchor.InsertionPlacementStrategy;

/**
 * Various test cases for {@link Editor}.
 */
public class EditorTests extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.client.editor.EditorTestModule";
  }

  /**
   * Tests that selection do not affect break replacement.
   *
   * @see AnchorUtils#setTextBetweenAnchors
   */
  public void testSetTextBetweenAnchors() {
    AnchorType bottomType = AnchorType.create(EditorTests.class, "bottom");
    AnchorType topType = AnchorType.create(EditorTests.class, "top");

    Document doc = Document.createFromString("qwerty\nasdfgh\nzxcvbn\n");
    LineFinder lineFinder = doc.getLineFinder();
    AnchorManager anchorManager = doc.getAnchorManager();
    LineInfo lineInfo = lineFinder.findLine(1);

    Editor editor = Editor.create(new MockAppContext());
    editor.setDocument(doc);

    Anchor topAnchor = anchorManager.createAnchor(topType, lineInfo.line(), lineInfo.number(),
        AnchorManager.IGNORE_COLUMN);
    topAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    topAnchor.setInsertionPlacementStrategy(InsertionPlacementStrategy.EARLIER);

    Anchor bottomAnchor = anchorManager.createAnchor(bottomType, lineInfo.line(), lineInfo.number(),
        AnchorManager.IGNORE_COLUMN);
    bottomAnchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    bottomAnchor.setInsertionPlacementStrategy(InsertionPlacementStrategy.LATER);

    SelectionModel selection = editor.getSelection();

    selection.setSelection(lineInfo, 1, lineInfo, 4);

    AnchorUtils.setTextBetweenAnchors(
        "bugaga", topAnchor, bottomAnchor, editor.getEditorDocumentMutator());

    assertEquals("qwerty\nbugaga\nzxcvbn\n", doc.asText());
  }
}
