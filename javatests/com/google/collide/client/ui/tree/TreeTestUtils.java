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

package com.google.collide.client.ui.tree;

import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.client.DtoClientImpls.DirInfoImpl;
import com.google.collide.dto.client.DtoClientImpls.FileInfoImpl;
import com.google.collide.json.client.JsoArray;

import java.util.ArrayList;

/**
 * Shared utility code for tree tests.
 *
 */
public class TreeTestUtils {

  public interface NodeInfoFactory {
    DirInfo makeDirInfo();
    FileInfo makeFileInfo();
  }
  
  public static final NodeInfoFactory CLIENT_NODE_INFO_FACTORY = new NodeInfoFactory() {
    @Override
    public DirInfo makeDirInfo() {
      return DirInfoImpl.make();
    }

    @Override
    public FileInfo makeFileInfo() {
      return FileInfoImpl.make();
    }
  };
  
  public static final NodeInfoFactory SERVER_NODE_INFO_FACTORY = new NodeInfoFactory() {
    @Override
    public DirInfo makeDirInfo() {
      return com.google.collide.dto.server.DtoServerImpls.DirInfoImpl.make();
    }

    @Override
    public FileInfo makeFileInfo() {
      return com.google.collide.dto.server.DtoServerImpls.FileInfoImpl.make();
    }
  };
  
  /**
   * This is the GWT Module used for these tests.
   */
  public static final String BUILD_MODULE_NAME =
      "com.google.collide.client.TestCode";
  
  /**
   * Constructs the mock file tree used in each of the tests.
   */
  public static DirInfo createMockTree(NodeInfoFactory nodeInfoFactory) {
    // Root has 3 directories and 2 files.
    DirInfo root = makeEmptyDir(nodeInfoFactory, "Root");

    // This has only files. This subtree is 1 level deep.
    DirInfo AD1 = makeEmptyDir(nodeInfoFactory, "AD1");
    AD1.getFiles().add(makeFile(nodeInfoFactory, "AF2"));
    AD1.getFiles().add(makeFile(nodeInfoFactory, "BF2"));
    AD1.getFiles().add(makeFile(nodeInfoFactory, "CF2"));
    AD1.getFiles().add(makeFile(nodeInfoFactory, "DF2"));

    // This has mixed files and empty directories. this subtree is 1 level deep.
    DirInfo BD1 = makeEmptyDir(nodeInfoFactory, "BD1");
    BD1.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "AD2"));
    BD1.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "BD2"));
    BD1.getFiles().add(makeFile(nodeInfoFactory, "EF2"));
    BD1.getFiles().add(makeFile(nodeInfoFactory, "FF2"));
    BD1.getFiles().add(makeFile(nodeInfoFactory, "GF2"));

    // This has mixed files and directories. The directories then have subfiles.
    // 2 levels deep.
    DirInfo CD1 = makeEmptyDir(nodeInfoFactory, "CD1");
    CD1.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "CD2"));
    CD1.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "DD2"));
    CD1.getFiles().add(makeFile(nodeInfoFactory, "HF2"));
    CD1.getFiles().add(makeFile(nodeInfoFactory, "IF2"));
    CD1.getFiles().add(makeFile(nodeInfoFactory, "JF2"));

    // We must go deeper.
    DirInfo CD2 = CD1.getSubDirectories().get(0);
    CD2.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "AD3"));
    CD2.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "BD3"));
    CD2.getFiles().add(makeFile(nodeInfoFactory, "AF3"));
    CD2.getFiles().add(makeFile(nodeInfoFactory, "BF3"));
    CD2.getFiles().add(makeFile(nodeInfoFactory, "CF3"));

    DirInfo DD2 = CD1.getSubDirectories().get(1);
    DD2.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "CD3"));
    DD2.getSubDirectories().add(makeEmptyDir(nodeInfoFactory, "DD3"));
    DD2.getFiles().add(makeFile(nodeInfoFactory, "DF3"));
    DD2.getFiles().add(makeFile(nodeInfoFactory, "EF3"));
    DD2.getFiles().add(makeFile(nodeInfoFactory, "FF3"));

    // Add them to the root and return it.
    root.getSubDirectories().add(AD1);
    root.getSubDirectories().add(BD1);
    root.getSubDirectories().add(CD1);
    root.getFiles().add(makeFile(nodeInfoFactory, "AF1"));
    root.getFiles().add(makeFile(nodeInfoFactory, "BF1"));
    
    return root;
  }

  private static DirInfo makeEmptyDir(NodeInfoFactory nodeInfoFactory, String name) {
    DirInfo dirInterface = nodeInfoFactory.makeDirInfo();
    
    if (dirInterface instanceof DirInfoImpl) {
      DirInfoImpl dir = (DirInfoImpl) dirInterface;
      dir.setNodeType(TreeNodeInfo.DIR_TYPE);
      dir.setName(name);
      dir.setFiles(JsoArray.<FileInfo>create());
      dir.setSubDirectories(JsoArray.<DirInfo>create());
      dir.setIsComplete(true);
    } else {
      com.google.collide.dto.server.DtoServerImpls.DirInfoImpl dir =
          (com.google.collide.dto.server.DtoServerImpls.DirInfoImpl) dirInterface;
      dir.setNodeType(TreeNodeInfo.DIR_TYPE);
      dir.setName(name);
      dir.setFiles(
          new ArrayList<com.google.collide.dto.server.DtoServerImpls.FileInfoImpl>
          ());
      dir.setSubDirectories(
          new ArrayList<com.google.collide.dto.server.DtoServerImpls.DirInfoImpl>
          ());
      dir.setIsComplete(true);
    }
    
    return dirInterface;
  }

  private static FileInfo makeFile(NodeInfoFactory nodeInfoFactory, String name) {
    FileInfo fileInterface = nodeInfoFactory.makeFileInfo();
    
    if (fileInterface instanceof FileInfoImpl) {
      FileInfoImpl file = (FileInfoImpl) fileInterface;
      file.setNodeType(TreeNodeInfo.FILE_TYPE);
      file.setName(name);
      file.setSize("0");
    } else {
      com.google.collide.dto.server.DtoServerImpls.FileInfoImpl file =
          (com.google.collide.dto.server.DtoServerImpls.FileInfoImpl)
          fileInterface;
      file.setNodeType(TreeNodeInfo.FILE_TYPE);
      file.setName(name);
      file.setSize("0");
    }
    
    return fileInterface;
  }
}
