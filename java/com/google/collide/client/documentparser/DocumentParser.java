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

package com.google.collide.client.documentparser;

import com.google.collide.client.util.BasicIncrementalScheduler;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.client.util.UserActivityManager;
import com.google.collide.codemirror2.Parser;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.Position;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.Anchor.RemovalStrategy;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Parser for a document that delegates to CodeMirror.
 *
 *  This class attaches to a document and re-parses whenever the contents
 * changes. It uses an incremental parser allowing it to resume parsing from the
 * beginning of the changed line.
 *
 */
public class DocumentParser {

  public static DocumentParser create(Document document, Parser codeMirrorParser,
      UserActivityManager userActivityManager) {
    /*
     * Guess that parsing 300 lines takes 50ms, let scheduler balance actual
     * parsing time per machine.
     */
    BasicIncrementalScheduler scheduler = new BasicIncrementalScheduler(
        userActivityManager, 50, 300);
    return create(document, codeMirrorParser, scheduler);
  }

  @VisibleForTesting
  public static DocumentParser create(Document document, Parser codeMirrorParser,
      IncrementalScheduler scheduler) {
    return new DocumentParser(document, codeMirrorParser, scheduler);
  }

  /**
   * A listener that receives a callback as lines of the document get parsed.
   * Can be called synchronously with user keyboard interactions
   * or asynchronously in batch mode, parsing a few lines in a row.
   */
  public interface Listener {
    /**
     * This method is called to mark the start of asynchronous parsing
     * iteration.
     *
     * @param lineNumber number of a line iteration started from
     */
    void onIterationStart(int lineNumber);

    /**
     * This method is called to mark the finish of asynchronous parsing
     * iteration.
     */
    void onIterationFinish();

    /**
     * Note: This may be called synchronously with a user's key press, so do not
     * do too much work synchronously.
     */
    void onDocumentLineParsed(Line line, int lineNumber, @Nonnull JsonArray<Token> tokens);
  }

  private static final AnchorType PARSER_ANCHOR_TYPE = AnchorType.create(DocumentParser.class,
      "parser");

  private Anchor createParserPosition(Document document) {
    Anchor position =
        document.getAnchorManager().createAnchor(PARSER_ANCHOR_TYPE, document.getFirstLine(), 0,
            AnchorManager.IGNORE_COLUMN);
    position.setRemovalStrategy(RemovalStrategy.SHIFT);
    return position;
  }

  private final Parser codeMirrorParser;

  private final Document.TextListener documentTextListener = new Document.TextListener() {
    @Override
    public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
      /*
       * Tracks the earliest change in the document, so that can be used as a
       * starting point for the parser
       */
      Line earliestLine = parserPosition.getLine();
      int earliestLineNumber = parserPosition.getLineNumber();

      for (int i = 0, n = textChanges.size(); i < n; i++) {
        TextChange textChange = textChanges.get(i);
        Line line = textChange.getLine();
        int lineNumber = textChange.getLineNumber();

        if (lineNumber < earliestLineNumber) {
          earliestLine = line;
          earliestLineNumber = lineNumber;
        }

        // Synchronously parse this line
        worker.parse(line, lineNumber, 1, null);
      }

      // Queue the earliest
      document.getAnchorManager().moveAnchor(
          parserPosition, earliestLine, earliestLineNumber, AnchorManager.IGNORE_COLUMN);

      scheduler.schedule(parserTask);
    }
  };

  private final ListenerManager<Listener> listenerManager;
  private final Anchor parserPosition;

  private final IncrementalScheduler.Task parserTask = new IncrementalScheduler.Task() {
    @Override
    public boolean run(int workAmount) {
      return executeWorker(workAmount);
    }
  };

  private final IncrementalScheduler scheduler;
  private final DocumentParserWorker worker;
  private final Remover textListenerRemover;

  private DocumentParser(
      Document document, Parser codeMirrorParser, IncrementalScheduler scheduler) {
    Preconditions.checkNotNull(codeMirrorParser);
    Preconditions.checkNotNull(scheduler);
    this.codeMirrorParser = codeMirrorParser;
    this.listenerManager = ListenerManager.create();
    this.scheduler = scheduler;
    this.worker = new DocumentParserWorker(this, codeMirrorParser);
    this.parserPosition = createParserPosition(document);
    this.textListenerRemover = document.getTextListenerRegistrar().add(documentTextListener);
  }

  /**
   * Schedules the parsing of the document from the last parsed position, or the
   * beginning of the document if this is the first time parsing.
   */
  public void begin() {
    scheduler.schedule(parserTask);
  }

  public ListenerRegistrar<Listener> getListenerRegistrar() {
    return listenerManager;
  }

  /**
   * Parses the given line synchronously, returning the tokens on the line.
   *
   * <p>This will NOT schedule parsing of subsequent lines.
   *
   * @return the parsed tokens, or {@code null} if there isn't a snapshot
   *         and it's not the first line
   */
  @Nullable
  public JsonArray<Token> parseLineSync(@Nonnull Line line) {
    return worker.parseLine(line);
  }

  /**
   * @return true if this parser mode supports smart indentation
   */
  public boolean hasSmartIndent() {
    return codeMirrorParser.hasSmartIndent();
  }

  /**
   * Return the indentation for this line, based upon the line above it.
   */
  public int getIndentation(Line line) {
    return worker.getIndentation(line);
  }

  public void teardown() {
    parserPosition.getLine().getDocument().getAnchorManager().removeAnchor(parserPosition);
    textListenerRemover.remove();
    scheduler.teardown();
  }

  void dispatchIterationStart(final int lineNumber) {
    listenerManager.dispatch(new Dispatcher<Listener>() {
      @Override
      public void dispatch(Listener listener) {
        listener.onIterationStart(lineNumber);
      }
    });
  }

  void dispatchIterationFinish() {
    listenerManager.dispatch(new Dispatcher<Listener>() {
      @Override
      public void dispatch(Listener listener) {
        listener.onIterationFinish();
      }
    });
  }

  void dispatch(final Line line, final int lineNumber, @Nonnull final JsonArray<Token> tokens) {
    listenerManager.dispatch(new Dispatcher<Listener>() {
      @Override
      public void dispatch(Listener listener) {
        listener.onDocumentLineParsed(line, lineNumber, tokens);
      }
    });
  }

  private boolean executeWorker(int workAmount) {
    dispatchIterationStart(parserPosition.getLineNumber());
    boolean result = worker.parse(parserPosition.getLine(), parserPosition.getLineNumber(),
        workAmount, parserPosition);
    dispatchIterationFinish();
    return result;
  }

  public SyntaxType getSyntaxType() {
    return codeMirrorParser.getSyntaxType();
  }

  /**
   * Checks if line has been parsed since last changes (in this line or
   * in lines above it).
   *
   * @param lineNumber number of line to check
   * @return {@code true} if line is to be parsed.
   */
  public boolean isLineDirty(int lineNumber) {
    // Without this check last line never becomes "clean".
    if (!scheduler.isBusy()) {
      return false;
    }
    return parserPosition.getLineNumber() <= lineNumber;
  }

  /**
   * Calculates parser mode at the beginning of line.
   *
   * @return {@code null} if previous line is not parsed yet
   */
  @Nullable
  public String getInitialMode(@Nonnull TaggableLine line) {
    return worker.getInitialMode(line);
  }

  /**
   * Synchronously parse beginning of the line and safely cast resulting state.
   *
   * <p>It is explicitly checked that current syntax type corresponds to
   * specified state class.
   *
   * If the parser hasn't asynchronously reached previous line (may be it is
   * appeared too recently) then {@code null} is returned.
   *
   * @see DocumentParserWorker#getParserState
   */
  public <T extends State> ParseResult<T> getState(Class<T> stateClass, Position position,
      @Nullable String appendedText) {
    Preconditions.checkArgument(getSyntaxType().checkStateClass(stateClass));
    Preconditions.checkNotNull(position);
    return worker.getParserState(position, appendedText);
  }
}
