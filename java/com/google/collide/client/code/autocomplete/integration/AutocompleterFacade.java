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

package com.google.collide.client.code.autocomplete.integration;

import com.google.collide.client.Resources;
import com.google.collide.client.code.autocomplete.Autocompleter;
import com.google.collide.client.code.autocomplete.SignalEventEssence;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.util.PathUtil;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.TaggableLine;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;

import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * This class isolates {@link Autocompleter} from the UI.
 */
public class AutocompleterFacade {

  private final Autocompleter autocompleter;
  private final Editor editor;
  private final RemoverManager instanceListeners = new RemoverManager();
  private final RemoverManager documentListeners = new RemoverManager();

  private final Document.LineListener lineListener = new Document.LineListener() {
    @Override
    public void onLineAdded(Document document, int lineNumber, JsonArray<Line> addedLines) {
      // Do nothing.
    }

    @Override
    public void onLineRemoved(Document document, int lineNumber, JsonArray<Line> removedLines) {
      JsonArray<TaggableLine> deletedLines = JsonCollections.createArray();
      for (final Line line : removedLines.asIterable()) {
        deletedLines.add(line);
      }
      autocompleter.getCodeAnalyzer().onLinesDeleted(deletedLines);
    }
  };

  /**
   * A listener that receives and translates keyboard events.
   */
  private final Editor.KeyListener keyListener = new Editor.KeyListener() {
    @Override
    public boolean onKeyPress(SignalEvent event) {
      return autocompleter.processKeyPress(new SignalEventEssence(event));
    }
  };

  /**
   * A listener that dismisses the autocomplete box when the editor is scrolled.
   */
  private final Buffer.ScrollListener dismissingScrollListener = new Buffer.ScrollListener() {
    @Override
    public void onScroll(Buffer buffer, int scrollTop) {
      autocompleter.dismissAutocompleteBox();
    }
  };

  /**
   * A listener for user modifications in the editor.
   */
  private final Editor.TextListener textListener = new Editor.TextListener() {
    @Override
    public void onTextChange(TextChange textChange) {
      autocompleter.refresh();
    }
  };

  private final DocumentParser.Listener parseListener;

  public static AutocompleterFacade create(
      Editor editor, CubeClient cubeClient, Resources resources) {
    AutocompleteUiController popup = new AutocompleteUiController(editor, resources);
    Autocompleter autocompleter = Autocompleter.create(editor, cubeClient, popup);
    return new AutocompleterFacade(editor, autocompleter);
  }

  public AutocompleterFacade(Editor editor, Autocompleter autocompleter) {
    this.editor = editor;
    this.autocompleter = autocompleter;

    this.parseListener = new DocumentParserListenerAdapter(autocompleter, editor);

    instanceListeners.track(editor.getTextListenerRegistrar().add(textListener));
    instanceListeners.track(editor.getKeyListenerRegistrar().add(keyListener));
    instanceListeners.track(
        editor.getBuffer().getScrollListenerRegistrar().add(dismissingScrollListener));
  }

  public void cleanup() {
    pause();
    instanceListeners.remove();
    autocompleter.cleanup();
  }

  private void pause() {
    documentListeners.remove();
  }

  public void editorContentsReplaced(PathUtil path, DocumentParser parser) {
    pause();
    documentListeners.track(editor.getDocument().getLineListenerRegistrar().add(lineListener));
    documentListeners.track(parser.getListenerRegistrar().add(parseListener));

    autocompleter.reset(path, parser);
  }
}
