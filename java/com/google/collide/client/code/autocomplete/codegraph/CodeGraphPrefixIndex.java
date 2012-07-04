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

package com.google.collide.client.code.autocomplete.codegraph;

import com.google.collide.client.code.autocomplete.PrefixIndex;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.dto.CodeBlockAssociation;
import com.google.collide.dto.CodeGraph;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Implements a prefix index over code graph.
 */
public class CodeGraphPrefixIndex implements PrefixIndex<CodeGraphProposal> {

  private final CodeGraph codeGraph;
  private final JsonStringMap<FileIndex> fileIdToData = JsonCollections.createMap();
  private final JsonStringMap<String> filePathToId = JsonCollections.createMap();
  private final PathUtil contextFilePath;
  private final boolean globalNamespace;

  private static String getFullId(FileIndex fileIndex, CodeBlock cb) {
    return fileIndex.fileCodeBlock.getId() + ":" + cb.getId();
  }

  /**
   * Objects returned as search results.
   */
  private static class CodeGraphProposalImpl extends CodeGraphProposal {
    private final CodeBlock codeBlock;
    private final FileIndex fileData;

    CodeGraphProposalImpl(CodeBlock codeBlock, String qname, FileIndex fileData) {
      super(qname, fileData.path, codeBlock.getBlockType() == CodeBlock.Type.VALUE_FUNCTION);
      this.codeBlock = codeBlock;
      this.fileData = fileData;
    }
  }

  /**
   * Keeps per-file indexing data structures.
   */
  private static class FileIndex {
    /**
     * Code blocks are indexed with breadth-first queue
     */
    private static class IndexQueueItem {
      final String fqnamePrefix;
      final JsonArray<CodeBlock> codeBlocks;

      IndexQueueItem(String prefix, JsonArray<CodeBlock> codeBlocks) {
        this.fqnamePrefix = prefix;
        this.codeBlocks = codeBlocks;
      }
    }

    private static final JsonArray<CodeBlockAssociation> EMPTY_LINKS_ARRAY =
        JsonCollections.createArray();
    private final JsonStringMap<JsonArray<CodeBlockAssociation>> links =
        JsonCollections.createMap();
    private final PathUtil path;
    private final CodeBlock fileCodeBlock;
    private final JsonStringMap<CodeBlock> codeBlocks = JsonCollections.createMap();
    private final JsoArray<IndexQueueItem> indexQueue = JsoArray.create();
    private final JsonStringMap<String> fqnames = JsonCollections.createMap();

    FileIndex(CodeBlock fileCodeBlock, PathUtil path) {
      this.path = path;
      this.fileCodeBlock = fileCodeBlock;
      indexQueue.add(new IndexQueueItem(null, JsonCollections.createArray(fileCodeBlock)));
    }

    void addOutgoingLink(CodeBlockAssociation link) {
      String key = Objects.firstNonNull(link.getSourceLocalId(), "");
      JsonArray<CodeBlockAssociation> linksArray = links.get(key);
      if (linksArray == null) {
        linksArray = JsonCollections.createArray();
        links.put(key, linksArray);
      }
      linksArray.add(link);
    }

    JsonArray<CodeBlockAssociation> getOutgoingLinks(CodeBlock codeBlock) {
      String key = getMapKey(codeBlock);
      JsonArray<CodeBlockAssociation> result = links.get(key);
      return result == null ? EMPTY_LINKS_ARRAY : result;
    }

    CodeBlock findCodeBlock(String localId) {
      String key = localId == null ? "" : localId;
      CodeBlock result = getCodeBlock(key);
      if (result != null) {
        return result;
      }
      return indexUntil(key);
    }

    private CodeBlock getCodeBlock(String localId) {
      String key = localId == null ? "" : localId;
      return codeBlocks.get(key);
    }

    String getFqname(CodeBlock codeBlock) {
      return fqnames.get(getMapKey(codeBlock));
    }

    String getFqname(String localId) {
      String key = localId == null ? "" : localId;
      return fqnames.get(key);
    }

    private CodeBlock indexUntil(String searchKey) {
      while (!indexQueue.isEmpty()) {
        IndexQueueItem head = indexQueue.shift();
        String prefix = head.fqnamePrefix;
        if (!StringUtils.isNullOrEmpty(prefix)) {
          prefix = prefix + ".";
        }

        for (int i = 0; i < head.codeBlocks.size(); i++) {
          CodeBlock codeBlock = head.codeBlocks.get(i);
          String key = getMapKey(codeBlock);
          codeBlocks.put(key, codeBlock);
          String fqname = appendFqname(prefix, codeBlock);
          putFqname(codeBlock, fqname);
          indexQueue.add(new IndexQueueItem(fqname, codeBlock.getChildren()));
        }
        CodeBlock result = codeBlocks.get(searchKey);
        if (result != null) {
          return result;
        }
      }
      return null;
    }

    void putFqname(CodeBlock codeBlock, String fqname) {
      fqnames.put(getMapKey(codeBlock), fqname);
    }

    private String getMapKey(CodeBlock codeBlock) {
      return codeBlock == fileCodeBlock ? "" : codeBlock.getId();
    }

    private String appendFqname(String prefix, CodeBlock codeBlock) {
      return (codeBlock == fileCodeBlock) ? "" : prefix + codeBlock.getName();
    }
  }

  CodeGraphPrefixIndex(CodeGraph codeGraph, SyntaxType mode) {
    this(codeGraph, mode, null);
  }

  CodeGraphPrefixIndex(CodeGraph codeGraph, SyntaxType mode, PathUtil contextFilePath) {
    Preconditions.checkNotNull(codeGraph);
    Preconditions.checkNotNull(mode);
    this.codeGraph = codeGraph;
    this.contextFilePath = contextFilePath;
    this.globalNamespace = mode.hasGlobalNamespace() || contextFilePath == null;

    JsonStringMap<CodeBlock> codeBlockMap = codeGraph.getCodeBlockMap();
    JsonArray<String> keys = codeBlockMap.getKeys();
    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      CodeBlock fileCodeBlock = codeBlockMap.get(key);
      SyntaxType fileMode = SyntaxType.syntaxTypeByFileName(fileCodeBlock.getName());
      if (mode.equals(fileMode)) {
        FileIndex fileIndex = new FileIndex(fileCodeBlock, new PathUtil(fileCodeBlock.getName()));
        fileIdToData.put(fileCodeBlock.getId(), fileIndex);

        String filePath = fileIndex.path.getPathString();
        String idList = filePathToId.get(filePath);
        if (idList != null) {
          idList += "," + fileCodeBlock.getId();
        } else {
          idList = fileCodeBlock.getId();
        }
        filePathToId.put(filePath, idList);
      }
    }

    CodeBlock defaultPackage = codeGraph.getDefaultPackage();
    if (defaultPackage != null) {
      filePathToId.put("", defaultPackage.getId());
      fileIdToData.put(defaultPackage.getId(), new FileIndex(defaultPackage, new PathUtil("")));
    }
  }

  @Override
  public JsonArray<? extends CodeGraphProposal> search(final String query) {
    return searchRoot(query);
  }

  /**
   * Runs a query against all files in the code graph.
   *
   * @param query query to run
   * @return an array of code graph proposals matching the query
   */
  private JsonArray<? extends CodeGraphProposal> searchRoot(final String query) {
    final JsonArray<CodeGraphProposalImpl> result = JsonCollections.createArray();
    if (globalNamespace) {
      fileIdToData.iterate(new IterationCallback<FileIndex>() {
        @Override
        public void onIteration(String key, FileIndex value) {
          search(query, value.fileCodeBlock, value, result);
        }
      });
    } else {
      String idList = filePathToId.get(contextFilePath.getPathString());
      if (idList != null) {
        for (String id : StringUtils.split(idList, ",").asIterable()) {
          FileIndex fileIndex = fileIdToData.get(id);
          search(query, fileIndex.fileCodeBlock, fileIndex, result);
        }
      }
    }
    if (codeGraph.getDefaultPackage() != null) {
      search(query, codeGraph.getDefaultPackage(),
          fileIdToData.get(codeGraph.getDefaultPackage().getId()), result);
    }
    return result;
  }

  private void search(String query, CodeBlock root, FileIndex fileData,
      JsonArray<CodeGraphProposalImpl> result) {
    collectOutgoingLinks(codeGraph.getTypeAssociations());
    collectOutgoingLinks(codeGraph.getInheritanceAssociations());
    collectOutgoingLinks(codeGraph.getImportAssociations());

    JsonArray<CodeGraphProposalImpl> linkSourceCandidates = JsonCollections.createArray();
    linkSourceCandidates.add(new CodeGraphProposalImpl(root, "", fileData));
    searchTree(query, "", root, fileData, false, linkSourceCandidates, result);
    searchLinks(query, linkSourceCandidates, result);
  }

  private void searchLinks(String query, JsonArray<CodeGraphProposalImpl> linkSourceCandidates,
      JsonArray<CodeGraphProposalImpl> result) {
    JsonStringSet visited = JsonCollections.createStringSet();

    while (!linkSourceCandidates.isEmpty()) {
      JsonArray<CodeGraphProposalImpl> newCandidates = JsonCollections.createArray();
      for (CodeGraphProposalImpl candidate : linkSourceCandidates.asIterable()) {
        CodeBlock codeBlock = candidate.codeBlock;

        JsonArray<CodeGraphProposalImpl> zeroBoundary = JsonCollections.createArray();
        JsonArray<CodeGraphProposalImpl> epsilonBoundary = JsonCollections.createArray();
        createBoundary(codeBlock, candidate.fileData, zeroBoundary, epsilonBoundary, visited);

        String linkAccessPrefix = candidate.getName();
        if (linkAccessPrefix == null) {
          linkAccessPrefix = "";
        }
        if (!StringUtils.isNullOrEmpty(linkAccessPrefix)) {
          linkAccessPrefix += ".";
        }

        for (CodeGraphProposalImpl zeroNeighbor : zeroBoundary.asIterable()) {
          searchTree(query, linkAccessPrefix, zeroNeighbor.codeBlock, zeroNeighbor.fileData,
              false, newCandidates, result);
        }

        for (CodeGraphProposalImpl epsilonNeighbor : epsilonBoundary.asIterable()) {
          String epsilonAccessPrefix = linkAccessPrefix;
          CodeBlock targetCodeBlock = epsilonNeighbor.codeBlock;
          if (targetCodeBlock.getBlockType() == CodeBlock.Type.VALUE_FILE) {
            epsilonAccessPrefix += truncateExtension(targetCodeBlock.getName());
          } else {
            epsilonAccessPrefix += targetCodeBlock.getName();
          }
          epsilonAccessPrefix += ".";
          searchTree(query, epsilonAccessPrefix, targetCodeBlock, epsilonNeighbor.fileData,
              false, newCandidates, result);
        }
      }
      linkSourceCandidates = newCandidates;
    }
  }


  /**
   * <p>This function recursively walks a code block tree from the given root
   * and matches its code blocks against the query.
   *
   * <p>Depending on whether strict or partial match is needed, it writes
   * to the output array {@code matched} those code blocks which have an access prefix
   * either exactly matching the query or starting with the query.
   *
   * <p>Code blocks which potentially may have matches in their subtrees are
   * collected in {@code visited} output array for further processing of their
   * outgoing links.
   */
  private void searchTree(String query, String accessPrefix, CodeBlock root, FileIndex fileData,
      boolean strictMatch, JsonArray<CodeGraphProposalImpl> visited,
      JsonArray<CodeGraphProposalImpl> matched) {
    String lcQuery = query.toLowerCase();
    String rootFqname = fileData.getFqname(root);
    JsonArray<CodeBlock> children = root.getChildren();
    for (int i = 0; i < children.size(); i++) {
      CodeBlock child = children.get(i);
      if (fileData.getFqname(child) == null) {
        // It is just a side-effect for performance reasons. Since we're traversing
        // the tree anyway, why don't we fill in fqnames table at the same time?
        String childFqname = (rootFqname == null)
            ? child.getName() : rootFqname + "." + child.getName();
        fileData.putFqname(child, childFqname);
      }

      final String childAccessPrefix = accessPrefix + child.getName();
      final String lcChildAccessPrefix = childAccessPrefix.toLowerCase();
      CodeGraphProposalImpl childProposal = new CodeGraphProposalImpl(
          child, childAccessPrefix, fileData);

      if (strictMatch && lcChildAccessPrefix.equals(lcQuery)) {
        // If we want a strict match then "foo.bar" child will match
        // "foo.bar" query but will not match "foo.b"
        matched.add(childProposal);
      }

      if (!strictMatch && lcChildAccessPrefix.startsWith(lcQuery)) {
        // If we don't need a strict match then "foo.bar." and "foo.baz."
        // both match query "foo.b"
        matched.add(childProposal);
      }

      if (lcQuery.startsWith(lcChildAccessPrefix + ".")) {
        // Children of "foo.bar." may or may not have matches for query "foo.bar.b"
        // but children of "foo.bar.baz" are all already matching "foo.bar.b" (and
        // we don't need to go deeper) while children of "foo.baz" can't match
        // "foo.bar.b" at all.
        if (visited != null) {
          visited.add(childProposal);
        }
        searchTree(query, childAccessPrefix + ".", child, fileData, strictMatch, visited, matched);
      }
    }
  }

  /**
   * Useful for debugging
   */
  @SuppressWarnings("unused")
  private String linkToString(CodeBlockAssociation link) {
    FileIndex sourceFile = fileIdToData.get(link.getSourceFileId());
    FileIndex targetFile = fileIdToData.get(link.getTargetFileId());

    if (sourceFile == null || targetFile == null) {
      return "invalid link. source file=" + link.getSourceFileId()
          + " target file=" + link.getTargetFileId();
    }
    return sourceFile.fileCodeBlock.getName() + ":" + sourceFile.getFqname(link.getSourceLocalId())
        + " ==(" + link.getType() + ")==> "
        + targetFile.fileCodeBlock.getName() + ":" + targetFile.getFqname(link.getTargetLocalId());
  }

  /**
   * Creates a boundary of a {@code root} code block. Boundary is a closure of
   * code blocks accessible from {@code root} code blocks via associations.
   * Since we have two types of associations, one where association target
   * is included into access path, and one where it is not, we partition
   * boundary code blocks into "epsilon" boundary and "zero" boundary
   * correspondingly.
   */
  private void createBoundary(CodeBlock root, FileIndex fileData,
      final JsonArray<CodeGraphProposalImpl> zeroBoundary,
      JsonArray<CodeGraphProposalImpl> epsilonBoundary, JsonStringSet visited) {
    visited.add(getFullId(fileData, root));
    final JsonArray<CodeBlockAssociation> queue = fileData.getOutgoingLinks(root).copy();
    while (!queue.isEmpty()) {
      CodeBlockAssociation link = queue.splice(0, 1).pop();
      FileIndex targetFileData = fileIdToData.get(link.getTargetFileId());
      if (targetFileData == null) {
        continue;
      }
      CodeBlock targetCodeBlock = targetFileData.findCodeBlock(link.getTargetLocalId());
      if (targetCodeBlock == null) {
        continue;
      }

      final String targetFqname = targetFileData.getFqname(targetCodeBlock);
      if (targetFqname == null) {
        if (targetCodeBlock.getBlockType() != CodeBlock.Type.VALUE_FILE) {
          throw new IllegalStateException(
              "type=" + CodeBlock.Type.valueOf(targetCodeBlock.getBlockType()));
        }
        continue;
      }

      String fullTargetId = getFullId(targetFileData, targetCodeBlock);
      if (visited.contains(fullTargetId)) {
        continue;
      }
      visited.add(fullTargetId);


      // We have found a CodeBlock which is a target of the concrete link.
      // We want to match children of this code block against the query,
      // but the problem is that the children may be defined in another file
      // or can even be spread over many files (which is the case in JS where
      // one can add a method to Document.prototype from anywhere).
      // So we need to run a tree query to find all code blocks with the
      // same fqname (called representatives below).
      if (link.getIsRootAssociation()) {
        epsilonBoundary.add(new CodeGraphProposalImpl(
            targetCodeBlock, targetFqname, targetFileData));
      } else {
        final JsonArray<CodeGraphProposalImpl> fqnameRepresentatives =
            JsonCollections.createArray();
        fileIdToData.iterate(new IterationCallback<FileIndex>() {
          @Override
          public void onIteration(String key, FileIndex value) {
            searchTree(
                targetFqname, "", value.fileCodeBlock, value, true, null, fqnameRepresentatives);
            for (int i = 0; i < fqnameRepresentatives.size(); i++) {
              CodeGraphProposalImpl representative = fqnameRepresentatives.get(i);
              queue.addAll(representative.fileData.getOutgoingLinks(representative.codeBlock));
              zeroBoundary.add(representative);
            }
          }
        });
      }
    }
  }

  private static String truncateExtension(String name) {
    PathUtil path = new PathUtil(name);
    String basename = path.getBaseName();
    int lastDot = basename.lastIndexOf('.');
    return (lastDot == -1) ? basename : basename.substring(0, lastDot);
  }

  /**
   * Scans the links array and distributes its elements over the files they
   * are going from. Clears the array when finished.
   *
   * @param links links array
   */
  private void collectOutgoingLinks(JsonArray<? extends CodeBlockAssociation> links) {
    if (links == null) {
      return;
    }
    for (int i = 0, size = links.size(); i < size; i++) {
      CodeBlockAssociation link = links.get(i);
      FileIndex fileData = fileIdToData.get(link.getSourceFileId());
      if (fileData != null) {
        fileData.addOutgoingLink(link);
      }
    }
    links.clear();
  }

  public boolean isEmpty() {
    return fileIdToData.isEmpty();
  }
}
