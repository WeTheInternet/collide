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

package com.google.collide.client.codeunderstanding;

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeGraphFreshness;
import com.google.collide.dto.CodeGraphRequest;
import com.google.collide.dto.CodeGraphResponse;
import com.google.collide.dto.ImportAssociation;
import com.google.collide.dto.InheritanceAssociation;
import com.google.collide.dto.TypeAssociation;
import com.google.collide.dto.client.DtoClientImpls.CodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphFreshnessImpl;
import com.google.collide.dto.client.DtoClientImpls.CodeGraphImpl;
import com.google.collide.dto.client.DtoClientImpls.ImportAssociationImpl;
import com.google.collide.dto.client.DtoClientImpls.InheritanceAssociationImpl;
import com.google.collide.dto.client.DtoClientImpls.MockCodeBlockImpl;
import com.google.collide.dto.client.DtoClientImpls.MockImportAssociationImpl;
import com.google.collide.dto.client.DtoClientImpls.MockInheritanceAssociationImpl;
import com.google.collide.dto.client.DtoClientImpls.MockTypeAssociationImpl;
import com.google.collide.dto.client.DtoClientImpls.TypeAssociationImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.grok.GrokUtils;
import com.google.collide.shared.util.StringUtils;

/**
 * A set of static methods / mock implementations for tests using code graph components.
 */
public class CodeGraphTestUtils {

  /**
   * Constructs childless {@link CodeBlockImpl} with
   * specified characteristics.
   */
  public static CodeBlockImpl createCodeBlock(String id, String name, CodeBlock.Type type,
      int startLine, int startColumn, int endLine, int endColumn) {
    return MockCodeBlockImpl.make().setId(id).setName(name)
        .setBlockType(type == null ? CodeBlock.Type.VALUE_UNDEFINED : type.value)
        .setStartLineNumber(startLine).setStartColumn(startColumn).setEndLineNumber(endLine)
        .setEndColumn(endColumn).setChildren(JsoArray.<CodeBlock>create());
  }

  /**
   * Constructs {@link CodeBlock} with specified characteristics and adds it
   * as a child to the given block according to the given path.
   */
  public static CodeBlock createCodeBlock(CodeBlock fileBlock, String id, String qname,
      CodeBlock.Type type, int startLine, int startColumn, int endLine, int endColumn) {
    JsonArray<String> path = StringUtils.split(qname, ".");
    CodeBlock result = createCodeBlock(id, path.get(path.size() - 1), type, startLine, startColumn,
        endLine, endColumn);
    CodeBlock container;
    if (path.size() > 1) {
      JsonArray<String> parentPath = path.copy();
      parentPath.remove(parentPath.size() - 1);
      container = GrokUtils.getOrCreateCodeBlock(fileBlock, parentPath, GrokUtils.NO_MISSING_CHILD);
      if (container == null) {
        throw new RuntimeException("Can't create code block in file=" + fileBlock.getName()
            + " by qname=" + qname + ". Create all containers first");
      }
    } else {
      container = fileBlock;
    }
    container.getChildren().add(result);
    return result;
  }

  /**
   * Constructs {@link CodeGraphFreshness} of specified freshness components.
   */
  public static CodeGraphFreshness createFreshness(String libs, String full, String file) {
    CodeGraphFreshnessImpl result = CodeGraphFreshnessImpl.make();
    result.setLibsSubgraph(libs);
    result.setFullGraph(full);
    result.setFileTree(file);
    return result;
  }

  /**
   * Constructs {@link CodeGraphImpl} consisting given {@link CodeBlock}.
   */
  public static CodeGraphImpl createCodeGraph(CodeBlock fileBlock) {
    CodeGraphImpl result = CodeGraphImpl.make();
    JsoStringMap<CodeBlock> codeBlocks = JsoStringMap.create();
    codeBlocks.put("/foo.js", fileBlock);
    result.setCodeBlockMap(codeBlocks);
    return result;
  }

  /**
   * Constructs {@link TypeAssociation} from the given source to the specified
   * destination.
   */
  public static TypeAssociation createTypeAssociation(CodeBlock srcFile, CodeBlock srcCodeBlock,
      CodeBlock targetFile, CodeBlock targetCodeBlock) {
    TypeAssociationImpl result = MockTypeAssociationImpl.make();
    result.setSourceFileId(srcFile.getId()).setSourceLocalId(srcCodeBlock.getId())
        .setTargetFileId(targetFile.getId()).setTargetLocalId(targetCodeBlock.getId())
        .setIsRootAssociation(false);
    return result;
  }

  /**
   * Constructs {@link InheritanceAssociation} from the given source to the
   * specified destination.
   */
  public static InheritanceAssociation createInheritanceAssociation(CodeBlock srcFile,
      CodeBlock srcCodeBlock, CodeBlock targetFile, CodeBlock targetCodeBlock) {
    InheritanceAssociationImpl result = MockInheritanceAssociationImpl.make();
    result.setSourceFileId(srcFile.getId()).setSourceLocalId(srcCodeBlock.getId())
        .setTargetFileId(targetFile.getId()).setTargetLocalId(targetCodeBlock.getId())
        .setIsRootAssociation(false);
    return result;
  }

  public static ImportAssociation createRootImportAssociation(
      CodeBlock srcFile, CodeBlock targetFile) {
    ImportAssociationImpl result = MockImportAssociationImpl.make();
    result.setSourceFileId(srcFile.getId()).setSourceLocalId(null)
        .setTargetFileId(targetFile.getId()).setTargetLocalId(null).setIsRootAssociation(true);
    return result;
  }
  /**
   * A mock implementation of {@link CubeState.CubeResponseDistributor}.
   */
  public static class MockCubeClientDistributor implements CubeState.CubeResponseDistributor {

    public final JsonArray<CubeDataUpdates> collectedNotifications = JsoArray.create();

    @Override
    public void notifyListeners(CubeDataUpdates updates) {
      collectedNotifications.add(updates);
    }
  }

  /**
   * A mock implementation of {@link FrontendApi.RequestResponseApi}.
   */
  public static class MockApi
      implements
        FrontendApi.RequestResponseApi<CodeGraphRequest, CodeGraphResponse> {

    public final JsonArray<FrontendApi.ApiCallback<CodeGraphResponse>> collectedCallbacks;

    public MockApi() {
      collectedCallbacks = JsoArray.create();
    }

    @Override
    public void send(CodeGraphRequest msg, FrontendApi.ApiCallback<CodeGraphResponse> callback) {
      collectedCallbacks.add(callback);
    }
  }

  /**
   * Implementation that uses {@link MockApi} and raises visibility of some
   * methods.
   */
  public static class MockCubeClient extends CubeClient {

    public final MockApi api;

    public static MockCubeClient create() {
      return new MockCubeClient(new MockApi());
    }

    private MockCubeClient(MockApi api) {
      super(api);
      this.api = api;
    }

    @Override
    public void setPath(String filePath) {
      super.setPath(filePath);
    }

    @Override
    public void cleanup() {
      super.cleanup();
    }
  }
}
