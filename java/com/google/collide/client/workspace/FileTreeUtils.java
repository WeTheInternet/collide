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

package com.google.collide.client.workspace;

import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.DirInfo;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.MutationImpl;
import com.google.collide.dto.client.DtoClientImpls.TreeNodeInfoImpl;
import com.google.collide.json.shared.JsonArray;

/**
 * Utility methods for file tree.
 *
 */
public class FileTreeUtils {
  private FileTreeUtils() {
  }

  /**
   * Allocates a name String that is guaranteed not to exist in the children of
   * the specified DirInfo.
   *
   * @param parentDir the directory we will be scanning.
   * @param seedName the initial name we want to try to use.
   */
  static String allocateName(DirInfo parentDir, String seedName) {
    if (parentDir == null) {
      return seedName;
    }
    String uniqueName = ensureUniqueChildName(parentDir.getSubDirectories(), seedName);
    return ensureUniqueChildName(parentDir.getFiles(), uniqueName);
  }

  private static String ensureUniqueChildName(
      JsonArray<? extends TreeNodeInfo> children, String seedName) {

    String uniqueName = null;
    boolean foundUnique = false;
    int retryNumber = 0;

    while (!foundUnique) {

      // Generate a name to try.
      uniqueName = (retryNumber > 0) ? generateName(seedName, retryNumber) : seedName;

      // Test the name.
      foundUnique = isNameUnique(children, null, uniqueName);

      retryNumber++;
    }

    return uniqueName;
  }

  private static String generateName(String name, int retryNumber) {
    int dotIndex = name.indexOf('.');
    dotIndex = (dotIndex == -1) ? name.length() : dotIndex;
    String base = name.substring(0, dotIndex);

    // This will either be the extension including the '.', or the empty String
    // if dotIndex is == to the size of the String.
    String extension = name.substring(dotIndex, name.length());

    return base + "(" + retryNumber + ")" + extension;
  }

  /**
   * Test if there is a sibling node (file or directory) with specified name.
   *
   * <p>
   * Specified node (whose siblings are checked) itself is not checked.
   */
  static boolean hasNoPeerWithName(FileTreeNode nodeToCheck, String name) {
    DirInfoImpl parentDir = nodeToCheck.getParent().cast();
    return isNameUnique(parentDir.getSubDirectories(), nodeToCheck, name) && isNameUnique(
        parentDir.getFiles(), nodeToCheck, name);
  }

  private static boolean isNameUnique(
      JsonArray<? extends TreeNodeInfo> children, FileTreeNode nodeToCheck, String name) {
    for (int i = 0, n = children.size(); i < n; i++) {
      FileTreeNode child = (FileTreeNode) children.get(i);

      if (child != nodeToCheck && name.equals(child.getName())) {

        // Whoops. We collided.
        return false;
      }
    }
    return true;
  }

  static Mutation makeMutation(Mutation.Type mutatonType, PathUtil oldPath, PathUtil newPath,
      boolean isDirectory, String resourceId) {

    // We make a placeholder for the new node solely to install information
    // about whether or not this is a directory.
    TreeNodeInfoImpl placeHolderNode = TreeNodeInfoImpl.make().setNodeType(
        isDirectory ? TreeNodeInfo.DIR_TYPE
            : TreeNodeInfo.FILE_TYPE).setFileEditSessionKey(resourceId);

    return MutationImpl.make()
        .setMutationType(mutatonType)
        .setNewNodeInfo(placeHolderNode)
        .setOldPath(oldPath == null ? null : oldPath.getPathString())
        .setNewPath(newPath == null ? null : newPath.getPathString());
  }


}
