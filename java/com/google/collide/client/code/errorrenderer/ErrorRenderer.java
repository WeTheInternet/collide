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
package com.google.collide.client.code.errorrenderer;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.CodeError;
import com.google.collide.dto.FilePosition;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineNumberAndColumn;
import com.google.collide.shared.ot.PositionMigrator;
import com.google.collide.shared.util.SortedList;

/**
 * Renders code errors in the editor.
 */
public class ErrorRenderer implements LineRenderer {

  private static final SortedList.Comparator<CodeError> ERROR_COMPARATOR =
      new SortedList.Comparator<CodeError>() {
        @Override
        public int compare(CodeError error1, CodeError error2) {
          int startLineDiff =
              error1.getErrorStart().getLineNumber() - error2.getErrorStart().getLineNumber();
          if (startLineDiff != 0) {
            return startLineDiff;
          }
          int startColumnDiff =
              error1.getErrorStart().getColumn() - error2.getErrorStart().getColumn();
          if (startColumnDiff != 0) {
            return startColumnDiff;
          }
          int endLineDiff =
              error1.getErrorEnd().getLineNumber() - error2.getErrorEnd().getLineNumber();
          if (endLineDiff != 0) {
            return endLineDiff;
          } else {
            return error1.getErrorEnd().getColumn() - error2.getErrorEnd().getColumn();
          }
        }
  };

  public JsonArray<CodeError> getCodeErrors() {
    return codeErrors;
  }

  private final Editor.Css css;

  private int currentLineNumber;
  private int currentLineLength;
  // Current render start position.
  private int linePosition;
  // Errors that are visible at current line. They may start on the previous line
  // (or even earlier) or end in one of the next lines.
  private SortedList<CodeError> lineErrors;
  // Index of next error to render in lineErrors array.
  private int nextErrorIndex;

  // List of errors for a file.
  private JsonArray<CodeError> codeErrors;
  private PositionMigrator positionMigrator;

  public ErrorRenderer(Editor.Resources res) {
    this.css = res.workspaceEditorCss();
    codeErrors = JsoArray.create();
  }

  @Override
  public void renderNextChunk(Target target) {
    CodeError nextError = getNextErrorToRender();
    if (nextError == null) {
      // No errors to render. So render the rest of the line with null.
      renderNothingAndProceed(target, currentLineLength - linePosition);
    } else if (nextError.getErrorStart().getLineNumber() < currentLineNumber ||
               nextError.getErrorStart().getColumn() == linePosition) {
      int errorLength;
      if (nextError.getErrorEnd().getLineNumber() > currentLineNumber) {
        errorLength = currentLineLength - linePosition;
      } else {
        // Error ends at current line.
        errorLength = nextError.getErrorEnd().getColumn() + 1 - linePosition;
      }
      renderErrorAndProceed(target, errorLength);
    } else {
      // Wait until we get to the next error.
      renderNothingAndProceed(target, nextError.getErrorStart().getColumn() - linePosition);
    }
  }

  @Override
  public boolean shouldLastChunkFillToRight() {
    return false;
  }

  private void renderErrorAndProceed(Target target, int characterCount) {
    Log.debug(getClass(), "Rendering " + characterCount
        + " characters with error style at position " + linePosition + ", next line position: "
        + (linePosition + characterCount));
    target.render(characterCount, css.lineRendererError());
    linePosition += characterCount;
    nextErrorIndex++;
  }

  private void renderNothingAndProceed(Target target, int characterCount) {
    target.render(characterCount, null);
    linePosition += characterCount;
  }

  private CodeError getNextErrorToRender() {
    while (nextErrorIndex < lineErrors.size()) {
      CodeError nextError = lineErrors.get(nextErrorIndex);
      if (nextError.getErrorEnd().getLineNumber() == currentLineNumber &&
          nextError.getErrorEnd().getColumn() < linePosition) {
        // This may happen if errors overlap.
        nextErrorIndex++;
        continue;
      } else {
        return nextError;
      }
    }
    return null;
  }

  @Override
  public boolean resetToBeginningOfLine(Line line, int lineNumber) {
    // TODO: Convert to anchors so that error positions are updated when text edits happen.
    this.lineErrors = getErrorsAtLine(lineNumber);
    if (lineErrors.size() > 0) {
      Log.debug(getClass(), "Rendering line: " + lineNumber, ", errors size: " + lineErrors.size());
    } else {
      return false;
    }
    this.currentLineNumber = lineNumber;
    this.currentLineLength = line.getText().length();
    this.nextErrorIndex = 0;
    this.linePosition = 0;
    return true;
  }

  private SortedList<CodeError> getErrorsAtLine(int lineNumber) {
    int oldLineNumber = migrateLineNumber(lineNumber);
    SortedList<CodeError> result = new SortedList<CodeError>(ERROR_COMPARATOR);
    for (int i = 0; i < codeErrors.size(); i++) {
      CodeError error = codeErrors.get(i);
      if (error.getErrorStart().getLineNumber() <= oldLineNumber &&
          error.getErrorEnd().getLineNumber() >= oldLineNumber) {
        result.add(migrateError(error));
      }
    }
    return result;
  }

  private int migrateLineNumber(int lineNumber) {
    if (positionMigrator == null) {
      return lineNumber;
    } else {
      return positionMigrator.migrateFromNow(lineNumber, 0).lineNumber;
    }
  }

  private CodeError migrateError(CodeError oldError) {
    FilePosition newErrorStart = migrateFilePositionToNow(oldError.getErrorStart());
    FilePosition newErrorEnd = migrateFilePositionToNow(oldError.getErrorEnd());
    if (newErrorStart == oldError.getErrorStart() && newErrorEnd == oldError.getErrorEnd()) {
      return oldError;
    }
    DtoClientImpls.CodeErrorImpl newError = DtoClientImpls.CodeErrorImpl.make()
        .setErrorStart(newErrorStart)
        .setErrorEnd(newErrorEnd)
        .setMessage(oldError.getMessage());
    Log.debug(getClass(), "Migrated error [" + codeErrorToString(oldError)
        + "] to [" + codeErrorToString(newError) + "]");
    return newError;
  }

  private FilePosition migrateFilePositionToNow(FilePosition filePosition) {
    if (!positionMigrator.haveChanges()) {
      return filePosition;
    }
    LineNumberAndColumn newPosition =
        positionMigrator.migrateToNow(filePosition.getLineNumber(), filePosition.getColumn());
    return DtoClientImpls.FilePositionImpl.make()
        .setLineNumber(newPosition.lineNumber)
        .setColumn(newPosition.column);
  }

  public void setCodeErrors(JsonArray<CodeError> codeErrors, PositionMigrator positionMigrator) {
    this.codeErrors = codeErrors;
    this.positionMigrator = positionMigrator;
  }

  private static String filePositionToString(FilePosition position) {
    return "(" + position.getLineNumber() + "," + position.getColumn() + ")";
  }
  private static String codeErrorToString(CodeError codeError) {
    if (codeError == null) {
      return "null";
    } else {
      return filePositionToString(codeError.getErrorStart()) + "-"
          + filePositionToString(codeError.getErrorEnd());
    }
  }
}
