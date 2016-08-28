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

import javax.annotation.Nullable;

import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.util.DeferredCommandExecutor;
import com.google.collide.client.util.logging.Log;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Lazily parses source lines. That is, defers parsing for some time.
 * 2 possible states are - line is parsed, parse results are ready, and
 * parsing is not finished, no results available.
 *
 */
public class DeferringLineParser {

  private final DocumentParser parser;

  private LineInfo scheduledParseLineInfo;
  private int parsedLineNumber = -1;
  private JsonArray<Token> parsedLineTokens;

  private final DeferredCommandExecutor parseExecutor = new DeferredCommandExecutor(25) {
    @Override
    protected boolean execute() {
      parseScheduledLine();
      return false;
    }
  };

  private void parseScheduledLine() {
    Preconditions.checkState(scheduledParseLineInfo != null);
    parsedLineTokens = parseLine(scheduledParseLineInfo);
    parsedLineNumber = scheduledParseLineInfo.number();
    scheduledParseLineInfo = null;
  }

  private JsonArray<Token> parseLine(LineInfo lineInfo) {
    JsonArray<Token> tokens = parser.parseLineSync(lineInfo.line());
    if (tokens == null) {
      tokens = JsonCollections.createArray();
    }
    Log.debug(getClass(),
        "Line " + lineInfo.number() + " parsed, number of tokens: " + tokens.size());
    return tokens;
  }

  /**
   * Creates a new deferring parser.
   *
   * @param parser document parser to use
   */
  public DeferringLineParser(DocumentParser parser) {
    this.parser = parser;
  }

  /**
   * Returns available parse results or schedules parsing. Does not collect
   * parse results, i.e. at given time parse results only for a single line
   * are available.
   *
   * @param lineInfo line to parse
   * @param blocking whether to block until the line is parsed
   * @return parsed tokens or {@code null} if no results available now
   */
  JsonArray<Token> getParseResult(LineInfo lineInfo, boolean blocking) {
    // TODO: We should get parser state here.
    if (parsedLineNumber == lineInfo.number()) {
      Preconditions.checkState(parsedLineTokens != null);
      return parsedLineTokens;
    } else if (blocking) {
      scheduleParse(null);  // Cancel anything currently scheduled.
      parsedLineTokens = parseLine(lineInfo);
      parsedLineNumber = lineInfo.number();
      return parsedLineTokens;
    } else {
      scheduleParse(lineInfo);
      return null;
    }
  }

  private void scheduleParse(@Nullable LineInfo lineInfo) {
    if (lineInfo == null && scheduledParseLineInfo == null) {
      return;
    }
    if (scheduledParseLineInfo != null && lineInfo != null
        && scheduledParseLineInfo.number() == lineInfo.number()) {
      return;
    }
    if (scheduledParseLineInfo != null) {
      parseExecutor.cancel();  // Cancel anything currently scheduled.
    }
    scheduledParseLineInfo = lineInfo;
    if (scheduledParseLineInfo == null) {
      return;
    }
    parseExecutor.schedule(2);
    Log.debug(getClass(),
        "Scheduled line parse for line number " + scheduledParseLineInfo.number());
  }
}
