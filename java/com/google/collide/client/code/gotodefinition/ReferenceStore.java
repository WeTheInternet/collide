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

import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.codeunderstanding.CubeData;
import com.google.collide.client.codeunderstanding.CubeDataUpdates;
import com.google.collide.client.codeunderstanding.CubeUpdateListener;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.CodeReference;
import com.google.collide.dto.CodeReferences;
import com.google.collide.dto.FilePosition;
import com.google.collide.dto.client.ClientDocOpFactory;
import com.google.collide.dto.client.DtoClientImpls;
import com.google.collide.json.client.Jso;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.LineNumberAndColumn;
import com.google.collide.shared.ot.PositionMigrator;
import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;

/**
 * Storage and lookup system for cubeReferences from source ranges to
 * another file or URLs. Collects cubeReferences from various sources like Cube
 * or dynamic reference provider.
 *
 */
class ReferenceStore {

  // Tracks docops since last time we received information from Cube.
  // These docops are used when we need to find reference at given position
  // if the position has changed as the result of user edits.
  private final PositionMigrator positionMigrator;
  private final CubeClient cubeClient;
  private final CubeUpdateListener cubeListener = new CubeUpdateListener() {
    @Override
    public void onCubeResponse(CubeData data, CubeDataUpdates updates) {
      if (!updates.isFileReferences()) {
        return;
      }
      updateReferences(data);
    }
  };

  // References that come from Cube.
  private CodeReferences cubeReferences;

  // References that come from client parser.
  private DynamicReferenceProvider dynamicReferenceProvider;

  public ReferenceStore(CubeClient cubeClient) {
    this.cubeClient = cubeClient;
    this.positionMigrator = new PositionMigrator(ClientDocOpFactory.INSTANCE);
    cubeClient.addListener(cubeListener);
  }

  /**
   * Finds reference at given position.
   *
   * @param lineInfo position line info
   * @param column position column
   * @param blocking whether to block until given line is parsed for references
   * @return reference found at given position, or {@code null} if nothing there
   */
  @VisibleForTesting
  NavigableReference findReference(LineInfo lineInfo, int column, boolean blocking) {
    // TODO: Optimize this search.
    // Seach for reference at position where the cursor would have been if there were
    // no text changes.
    int line = lineInfo.number();
    NavigableReference result = null;
    if (cubeReferences != null && cubeReferences.getReferences().size() > 0) {
      // TODO: Optimize this search.
      // Seach for reference at position where the cursor would have been if there were
      // no text changes.
      LineNumberAndColumn oldPosition = positionMigrator.migrateFromNow(line, column);
      for (int i = 0; i < cubeReferences.getReferences().size(); i++) {
        CodeReference reference = cubeReferences.getReferences().get(i);
        if (reference.getReferenceStart().getLineNumber() > oldPosition.lineNumber) {
          // We've gone too far, nothing to look further.
          break;
        }
        if (isFilePositionBefore(reference.getReferenceStart(), oldPosition.lineNumber,
                oldPosition.column)
            && isFilePositionAfter(reference.getReferenceEnd(), oldPosition.lineNumber,
                oldPosition.column)) {
          // Migrate old reference to new position after edits.
          CodeReference newReference = migrateCubeReference(reference);
          if (newReference != null) {
            result = NavigableReference.createToFile(newReference);
          }
          break;
        }
      }
    }

    if (result == null && dynamicReferenceProvider != null) {
      result = dynamicReferenceProvider.getReferenceAt(lineInfo, column, blocking);
    }

    if (result == null) {
      Log.debug(getClass(), "Found no references at: (" + line + "," + column + ")");
    } else {
      Log.debug(getClass(),
          "Found reference at (" + line + "," + column + "):" + result.toString());
    }
    return result;
  }

  /**
   * Migrates old reference (its position as it was when we received cube data
   * before any edits) to new reference (new position after edits) if possible.
   * Reference cannot be migrated if some characters disappeared inside it or
   * newline was inserted in the middle.
   *
   * @param reference reference to migrate
   * @return migrated reference or {@code null} if reference cannot be migrated
   */
  private CodeReference migrateCubeReference(CodeReference reference) {
    FilePosition oldStartPosition = reference.getReferenceStart();
    FilePosition oldEndPosition = reference.getReferenceEnd();
    LineNumberAndColumn newStartPosition = positionMigrator.migrateToNow(
        oldStartPosition.getLineNumber(), oldStartPosition.getColumn());
    LineNumberAndColumn newEndPosition = positionMigrator.migrateToNow(
        oldEndPosition.getLineNumber(), oldEndPosition.getColumn());
    int newLength = newEndPosition.column - newStartPosition.column;
    int oldLength = oldEndPosition.getColumn() - oldStartPosition.getColumn();
    if (newStartPosition.lineNumber != newEndPosition.lineNumber
        || newLength != oldLength || newLength < 0) {
      // TODO: Make the method return null if text has changed inside the reference.
      return null;
    }
    DtoClientImpls.CodeReferenceImpl newReference = Jso.create().cast();
    return newReference
        .setReferenceType(reference.getReferenceType())
        .setReferenceStart(toDtoPosition(newStartPosition))
        .setReferenceEnd(toDtoPosition(newEndPosition))
            // TODO: Target may be in this file, in this case update target too.
        .setTargetStart(reference.getTargetStart())
        .setTargetEnd(reference.getTargetEnd())
        .setTargetFilePath(reference.getTargetFilePath())
        .setTargetSnippet(reference.getTargetSnippet());
  }

  private static FilePosition toDtoPosition(LineNumberAndColumn position) {
    return DtoClientImpls.FilePositionImpl.make()
        .setLineNumber(position.lineNumber)
        .setColumn(position.column);
  }

  private static boolean isFilePositionBefore(FilePosition position, int line, int column) {
    return position.getLineNumber() < line
        || (position.getLineNumber() == line && position.getColumn() <= column);
  }

  private static boolean isFilePositionAfter(FilePosition position, int line, int column) {
    return position.getLineNumber() > line
        || (position.getLineNumber() == line && position.getColumn() >= column);
  }

  private void logAllReferences() {
    if (cubeReferences == null) {
      Log.debug(getClass(), "No references info yet.");
      return;
    }
    Log.debug(getClass(), "All references in current file:");
    for (int i = 0; i < cubeReferences.getReferences().size(); i++) {
      CodeReference reference = cubeReferences.getReferences().get(i);
      Log.debug(getClass(), "reference at: " + referenceToString(reference));
    }
  }

  private static String filePositionToString(FilePosition position) {
    return "(" + position.getLineNumber() + "," + position.getColumn() + ")";
  }

  private static String referenceToString(CodeReference reference) {
    return filePositionToString(reference.getReferenceStart()) + " to file \""
        + reference.getTargetFilePath() + "\", target start: "
        + filePositionToString(reference.getTargetStart()) + ", target end: "
        + filePositionToString(reference.getTargetEnd());
  }

  public void onDocumentChanged(Document document,
      @Nullable DynamicReferenceProvider dynamicReferenceProvider) {
    updateReferences(cubeClient.getData());
    this.positionMigrator.start(document.getTextListenerRegistrar());
    this.dynamicReferenceProvider = dynamicReferenceProvider;
  }

  @VisibleForTesting
  void updateReferences(CubeData data) {
    positionMigrator.reset();
    this.cubeReferences = data.getFileReferences();
    // referenceRenderer.resetReferences(editor.getRenderer(), lineFinder);
    if (Log.isLoggingEnabled()) {
      Log.debug(getClass(), "Received code references");
      logAllReferences();
    }
  }

  public void cleanup() {
    cubeClient.removeListener(cubeListener);
    positionMigrator.stop();
  }
}
