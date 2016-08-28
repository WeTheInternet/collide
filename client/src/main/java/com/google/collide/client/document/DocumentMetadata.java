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

package com.google.collide.client.document;

import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.ConflictChunk;
import com.google.collide.dto.NodeConflictDto.ConflictHandle;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;

/**
 * Utility methods for retrieving metadata associated with a document.
 */
public final class DocumentMetadata {

  private static final String TAG_LINKED_TO_FILE = DocumentManager.class.getName()
      + ":LinkedToFile";
  private static final String TAG_FILE_EDIT_SESSION_KEY = DocumentManager.class.getName()
      + ":FileEditSessionKey";
  private static final String TAG_BEGIN_CC_REVISION = DocumentManager.class.getName()
      + ":BeginCcRevision";
  private static final String TAG_PATH = DocumentManager.class.getName() + ":Path";
  
  // TODO: move conflicts and conflict handle out of metadata.
  private static final String TAG_CONFLICTS = DocumentManager.class.getName() + ":Conflicts";
  private static final String TAG_CONFLICT_HANDLE =
      DocumentManager.class.getName() + ":ConflictHandle";
  
  /**
   * Returns whether the document is linked to a file.
   * 
   * If a document is not linked to a file, it is a client-only document.
   * Metadata such as {@link #getFileEditSessionKey(Document)},
   * {@link #getBeginCcRevision(Document)}, {@link #getConflicts(Document)},
   * {@link #getPath(Document)} will be invalid.
   */
  public static boolean isLinkedToFile(Document document) {
    return ((Boolean) document.getTag(TAG_LINKED_TO_FILE)).booleanValue();
  }

  static void putLinkedToFile(Document document, boolean isLinkedToFile) {
    document.putTag(TAG_LINKED_TO_FILE, isLinkedToFile);
  }

  /**
   * Only valid if {@link #isLinkedToFile(Document)} is true.
   */
  public static String getFileEditSessionKey(Document document) {
    ensureLinkedToFile(document);
    return document.getTag(TAG_FILE_EDIT_SESSION_KEY);
  }

  static void putFileEditSessionKey(Document document, String fileEditSessionKey) {
    document.putTag(TAG_FILE_EDIT_SESSION_KEY, fileEditSessionKey);
  }

  /**
   * Only valid if {@link #isLinkedToFile(Document)} is true.
   */
  public static int getBeginCcRevision(Document document) {
    ensureLinkedToFile(document);
    return ((Integer) document.getTag(TAG_BEGIN_CC_REVISION)).intValue();
  }

  static void putBeginCcRevision(Document document, int beginCcRevision) {
    document.putTag(TAG_BEGIN_CC_REVISION, beginCcRevision);
  }

  /**
   * Only valid if {@link #isLinkedToFile(Document)} is true.
   */
  public static PathUtil getPath(Document document) {
    ensureLinkedToFile(document);
    return document.getTag(TAG_PATH);
  }

  static void putPath(Document document, PathUtil path) {
    document.putTag(TAG_PATH, path);
  }

  /**
   * Only valid if {@link #isLinkedToFile(Document)} is true.
   */
  public static JsonArray<ConflictChunk> getConflicts(Document document) {
    ensureLinkedToFile(document);
    return document.getTag(TAG_CONFLICTS);
  }

  static void putConflicts(Document document, JsonArray<ConflictChunk> conflicts) {
    document.putTag(TAG_CONFLICTS, conflicts);
  }
  
  /**
   * Only valid if {@link #isLinkedToFile(Document)} is true.
   */
  public static ConflictHandle getConflictHandle(Document document) {
    ensureLinkedToFile(document);
    return document.getTag(TAG_CONFLICT_HANDLE);
  }

  static void putConflictHandle(Document document, ConflictHandle conflictHandle) {
    document.putTag(TAG_CONFLICT_HANDLE, conflictHandle);
  }  
  
  private static void ensureLinkedToFile(Document document) {
    if (!isLinkedToFile(document)) {
      throw new IllegalStateException("Document must be linked to file");
    }
  }

  public static void clearConflicts(Document document) {
    document.putTag(TAG_CONFLICT_HANDLE, null);
    document.putTag(TAG_CONFLICTS, null);
  }
}
