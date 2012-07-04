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
package com.google.collide.client.code.autocomplete;

import static org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType.INPUT;

import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.util.IncrementalScheduler;
import com.google.collide.client.util.PathUtil;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Parser;
import com.google.collide.codemirror2.State;
import com.google.collide.codemirror2.Stream;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.codemirror2.Token;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.util.JsonCollections;

/**
 * A set of common test utilities and mock implementations.
 *
 * <p>This code was moved from TestSetupHelper.
 */
public class TestUtils {

  public static final SignalEventEssence CTRL_SPACE = new SignalEventEssence(
      ' ', true, false, false, false, INPUT);

  public static final SignalEventEssence CTRL_SHIFT_SPACE = new SignalEventEssence(
      ' ', true, false, true, false, INPUT);

  /**
   * Implementation that publishes its content.
   */
  public static class MockStream implements Stream {

    /**
     * Flag that indicates "at the end of line".
     *
     * <p>This flag is toggled each time {@link #isEnd} is called. That way the
     * first invocation of {@link #isEnd} always returns {@code false} and
     * second invocation returns {@code true}.
     *
     * <p>Described behavior allows {@link MockParser} implementations to push
     * tokens. To determine what tokens to push, {@link #getText} can be used.
     */
    boolean toggle = true;

    private final String text;

    public MockStream(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    /**
     * @see #toggle
     */
    @Override
    public boolean isEnd() {
      toggle = !toggle;
      return toggle;
    }
  }

  /**
   * Implementation that "collects" schedule requests.
   */
  public static class MockIncrementalScheduler implements IncrementalScheduler {

    public final JsonArray<Task> requests = JsonCollections.createArray();

    @Override
    public void schedule(Task worker) {
      requests.add(worker);
    }

    @Override
    public void cancel() {
      requests.clear();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public boolean isPaused() {
      return false;
    }

    @Override
    public boolean isBusy() {
      return !requests.isEmpty();
    }

    @Override
    public void teardown() {}
  }

  private static class MockState implements State {
    @Override
    public State copy(Parser codeMirrorParser) {
      return createMockState();
    }
  }

  /**
   * Mock {@link Parser} implementation.
   */
  public static class MockParser implements Parser {

    private final SyntaxType type;

    public MockParser(SyntaxType type) {
      this.type = type;
    }

    @Override
    public boolean hasSmartIndent() {
      return false;
    }

    @Override
    public SyntaxType getSyntaxType() {
      return type;
    }

    @Override
    public int indent(State stateBefore, String textAfter) {
      return 0;
    }

    @Override
    public State defaultState() {
      return createMockState();
    }

    @Override
    public void parseNext(Stream stream, State parserState, JsonArray<Token> tokens) {
    }

    @Override
    public Stream createStream(String text) {
      return new MockStream(text);
    }

    @Override
    public String getName(State state) {
      return type.getName();
    }
  }

  public static <T extends AutocompleteProposal> JsonStringSet createNameSet(
      JsonArray<T> proposals) {
    JsonStringSet result = JsonCollections.createStringSet();
    for (int i = 0; i < proposals.size(); i++) {
      result.add(proposals.get(i).name);
    }
    return result;
  }

  public static JsonStringSet createNameSet(AutocompleteProposals proposals) {
    JsonStringSet result = JsonCollections.createStringSet();
    for (int i = 0; i < proposals.size(); i++) {
      result.add(proposals.get(i).name);
    }
    return result;
  }

  public static AbstractTrie<String> createStringTrie(String... items) {
    AbstractTrie<String> result = new AbstractTrie<String>();
    for (String item : items) {
      result.put(item, item);
    }
    return result;
  }

  public static <T extends AutocompleteProposal> String joinNames(JsonArray<T> proposals) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < proposals.size(); i++) {
      if (i > 0) {
        result.append(",");
      }
      result.append(proposals.get(i).name);
    }
    return result.toString();
  }

  public static <T extends AutocompleteProposal> T findProposalByName(
      JsonArray<T> proposals, String name) {
    for (int i = 0; i < proposals.size(); i++) {
      if (proposals.get(i).getName().equals(name)) {
        return proposals.get(i);
      }
    }
    return null;
  }

  public static State createMockState() {
    return new MockState();
  }

  public static DocumentParser createDocumentParser(PathUtil path) {
    return createDocumentParser(
        path, false, new MockIncrementalScheduler(), Document.createEmpty());
  }

  public static DocumentParser createDocumentParser(PathUtil path, boolean setupRealParser,
      IncrementalScheduler scheduler, Document document) {
    Parser parser = setupRealParser ? CodeMirror2.getParser(path)
        : new MockParser(SyntaxType.syntaxTypeByFilePath(path));
    return DocumentParser.create(document, parser, scheduler);
  }

  /**
   * Selects and returns proposal with given name.
   *
   * @return {@code null} if proposal with specified name is not found
   */
  public static ProposalWithContext selectProposalByName(
      AutocompleteProposals proposals, String name) {
    for (int i = 0, n = proposals.size(); i < n; i++) {
      AutocompleteProposal proposal = proposals.get(i);
      if (proposal.getName().equals(name)) {
        return proposals.select(i);
      }
    }
    return null;
  }
}
