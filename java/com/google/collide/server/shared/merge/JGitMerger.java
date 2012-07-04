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

package com.google.collide.server.shared.merge;

import com.google.common.collect.Lists;

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeChunk.ConflictState;

import java.util.List;

/**
 * Merger for performing a 3-way-merge using JGit.
 * 
 */
public class JGitMerger {

  private static MergeAlgorithm mergeAlgorithm = new MergeAlgorithm();
  
  public static MergeResult merge(String base, String child, String parent) {
    // Jgit Merge
    org.eclipse.jgit.merge.MergeResult<RawText> jgitMergeResult =
        mergeAlgorithm.merge(RawTextComparator.DEFAULT, new RawText(base.getBytes()),
            new RawText(child.getBytes()), new RawText(parent.getBytes()));

    return formatMerge(jgitMergeResult);
  }

  public static MergeResult formatMerge(org.eclipse.jgit.merge.MergeResult<RawText> results) {
    int runningIndex = 0;
    int numLines = 0;
    List<MergeChunk> mergeChunks = Lists.newArrayList();
    MergeChunk currentMergeChunk = null;
    StringBuilder fileContent = new StringBuilder();
    int conflictIndex = 0;
    for (org.eclipse.jgit.merge.MergeChunk chunk : results) {
      RawText seq = results.getSequences().get(chunk.getSequenceIndex());
      String chunkContent = seq.getString(chunk.getBegin(), chunk.getEnd(), false);

      if (chunk.getConflictState() == ConflictState.NO_CONFLICT
          || chunk.getConflictState() == ConflictState.FIRST_CONFLICTING_RANGE) {
        if (currentMergeChunk != null) {
          mergeChunks.add(currentMergeChunk);
        }
        currentMergeChunk = new MergeChunk();
      }

      switch (chunk.getConflictState()) {
        case NO_CONFLICT:
          currentMergeChunk.setHasConflict(false);
          currentMergeChunk.setMergedData(chunkContent);
          fileContent.append(chunkContent);
          numLines = chunk.getEnd() - chunk.getBegin();
          runningIndex = setRanges(currentMergeChunk, runningIndex, numLines);
          break;
        case FIRST_CONFLICTING_RANGE:
          currentMergeChunk.setHasConflict(true);
          currentMergeChunk.setChildData(chunkContent);
          fileContent.append(chunkContent);
          numLines = chunk.getEnd() - chunk.getBegin();
          runningIndex = setRanges(currentMergeChunk, runningIndex, numLines);
          break;
        case NEXT_CONFLICTING_RANGE:
          currentMergeChunk.setParentData(chunkContent);
          break;
      }
    }
    mergeChunks.add(currentMergeChunk);

    MergeResult mergeResult =
        new MergeResult(mergeChunks, (conflictIndex > 0) ? "" : fileContent.toString());

    return mergeResult;
  }

  private static int setRanges(MergeChunk mergeChunk, int runningChildIndex, int numLines) {
    mergeChunk.setStartLine(runningChildIndex);
    // JGit end index is exclusive, Collide's is inclusive. numLines can be 0.
    int endLine = runningChildIndex + numLines;
    if (numLines > 0) {
      endLine--;
    }
    mergeChunk.setEndLine(endLine);
    runningChildIndex += numLines;
    return runningChildIndex;
  }
}
