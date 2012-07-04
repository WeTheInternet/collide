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

import com.google.collide.client.AppContext;
import com.google.collide.client.codeunderstanding.CodeGraphTestUtils.MockCubeClient;
import com.google.collide.client.codeunderstanding.CubeData;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.testing.MockAppContext;
import com.google.collide.client.testutil.SynchronousTestCase;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.CodeReference;
import com.google.collide.dto.CodeReferences;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;

/**
 * Tests for {@link GoToDefinitionHandler}.
 */
public class ReferenceStoreTest extends SynchronousTestCase {

  @Override
  public String getModuleName() {
    return "com.google.collide.TestCode";
  }

  public void testFindReferenceInFileAfterEdit() {
    // Test data.
    PathUtil filePath = new PathUtil("/foo.js");
    Document document = Document.createFromString(""
        + "var defvar = 5;\n"
        + "var myvar = defvar;\n");
    CodeReference codeReference =
        DtoClientImpls.MockCodeReferenceImpl.make()
            .setReferenceStart(DtoClientImpls.FilePositionImpl.make()
                .setLineNumber(1).setColumn(12))
            .setReferenceEnd(DtoClientImpls.FilePositionImpl.make()
                .setLineNumber(1).setColumn(17))
            .setTargetFilePath(filePath.getPathString())
            .setTargetStart(DtoClientImpls.FilePositionImpl.make()
                .setLineNumber(0).setColumn(4))
            .setTargetEnd(DtoClientImpls.FilePositionImpl.make()
                .setLineNumber(0).setColumn(9))
            .setReferenceType(CodeReference.Type.VAR);
    JsoArray<CodeReference> codeReferences = JsoArray.from(codeReference);
    CodeReferences fileReferences =
        DtoClientImpls.CodeReferencesImpl.make().setReferences(codeReferences);

    // Environment.
    AppContext appContext = new MockAppContext();
    Editor editor = Editor.create(appContext);
    editor.setDocument(document);
    MockCubeClient cubeClient = MockCubeClient.create();
    cubeClient.setPath(filePath.getPathString());
    ReferenceStore referenceStore = null;
    try {
      referenceStore = new ReferenceStore(cubeClient);
      referenceStore.onDocumentChanged(document, null);
      referenceStore.updateReferences(
          new CubeData(filePath.getPathString(), null, null, null, null, fileReferences));

      LineInfo line1 = document.getLineFinder().findLine(1);
      // Check that there's reference at positions 12 to 17 inclusive (line 2).
      assertNotNull(referenceStore.findReference(line1, 12, true));
      assertNotNull(referenceStore.findReference(line1, 17, true));

      // Make some edits. Just insert some whitespaces before reference.
      // Now the second line is: "var    myvar = defvar;\n"
      document.insertText(document.getFirstLine().getNextLine(), 3, "   ");

      // Test!
      // Now there's nothing at position 13.
      assertNull(referenceStore.findReference(line1, 13, true));

      // And there's reference at 18.
      assertNotNull(referenceStore.findReference(line1, 18, true));

      // Make some more edits, add whitespace inside reference.
      // This should break it.
      // Now the second line is: "var    myvar = d   efvar;\n"
      document.insertText(document.getFirstLine().getNextLine(), 16, "   ");

      // Now there should be nothing at positions 15-23.
      assertNull(referenceStore.findReference(line1, 15, true));
      assertNull(referenceStore.findReference(line1, 18, true));
      assertNull(referenceStore.findReference(line1, 21, true));

      referenceStore.onDocumentChanged(Document.createEmpty(), null);
    } finally {
      if (referenceStore != null) {
        referenceStore.cleanup();
      }
      cubeClient.cleanup();
    }
  }
}
