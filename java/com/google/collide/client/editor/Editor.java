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

package com.google.collide.client.editor;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.parenmatch.ParenMatchHighlighter;
import com.google.collide.client.document.linedimensions.LineDimensionsCalculator;
import com.google.collide.client.editor.Buffer.ScrollListener;
import com.google.collide.client.editor.gutter.Gutter;
import com.google.collide.client.editor.gutter.LeftGutterManager;
import com.google.collide.client.editor.input.InputController;
import com.google.collide.client.editor.renderer.LineRenderer;
import com.google.collide.client.editor.renderer.RenderTimeExecutor;
import com.google.collide.client.editor.renderer.Renderer;
import com.google.collide.client.editor.search.SearchMatchRenderer;
import com.google.collide.client.editor.search.SearchModel;
import com.google.collide.client.editor.selection.CursorView;
import com.google.collide.client.editor.selection.LocalCursorController;
import com.google.collide.client.editor.selection.SelectionLineRenderer;
import com.google.collide.client.editor.selection.SelectionManager;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.FontDimensionsCalculator;
import com.google.collide.client.util.dom.FontDimensionsCalculator.FontDimensions;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerManager.Dispatcher;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import elemental.events.Event;
import elemental.html.Element;

/**
 * The presenter for the Collide editor.
 *
 *  This class composes many of the other classes that together form the editor.
 * For example, the area where the text is displayed, the {@link Buffer}, is a
 * nested presenter. Other components are not presenters, such as the input
 * mechanism which is handled by the {@link InputController}.
 *
 *  If an added element wants native browser selection, you must not inherit the
 * "user-select" CSS property. See
 * {@link CssUtils#setUserSelect(Element, boolean)}.
 */
public class Editor extends UiComponent<Editor.View> {

  /**
   * Static factory method for obtaining an instance of the Editor.
   */
  public static Editor create(AppContext appContext) {

    FontDimensionsCalculator fontDimensionsCalculator =
        FontDimensionsCalculator.get(appContext.getResources().workspaceEditorCss().editorFont());
    RenderTimeExecutor renderTimeExecutor = new RenderTimeExecutor();
    LineDimensionsCalculator lineDimensions =
        LineDimensionsCalculator.create(fontDimensionsCalculator);

    Buffer buffer =
        Buffer.create(appContext, fontDimensionsCalculator.getFontDimensions(), lineDimensions,
            renderTimeExecutor);
    InputController input = new InputController();
    View view =
        new View(appContext.getResources(), buffer.getView().getElement(), input.getInputElement());
    FocusManager focusManager = new FocusManager(buffer, input.getInputElement());
    return new Editor(appContext, view, buffer, input, focusManager, fontDimensionsCalculator,
        renderTimeExecutor);
  }

  /**
   * Animation CSS.
   */
  @CssResource.Shared
  public interface EditorSharedCss extends CssResource {
    String animationEnabled();

    String scrollable();
  }

  /**
   * CssResource for the editor.
   */
  public interface Css extends EditorSharedCss {
    String leftGutter();

    String editorFont();

    String root();

    String scrolled();

    String gutter();
    
    String lineRendererError();
  }

  /**
   * A listener that is called when the user presses a key.
   */
  public interface KeyListener {
    /*
     * The reason for preventDefault() not preventing default behavior is that
     * Firefox does not have support the defaultPrevented attribute, so we have
     * know way of knowing if it was prevented from the native event. We could
     * create a proxy for SignalEvent to note calls to preventDefault(), but
     * this would not catch the case that the implementor interacts directly to
     * the native event.
     */
    /**
     * @param event the event for the key press. Note: Calling preventDefault()
     *        may not prevent the default behavior in some cases. The return
     *        value of this method is a better channel for indicating the
     *        default behavior should be prevented.
     * @return true if the event was handled (the default behavior will not run
     *         in this case), false to proceed with the default behavior. Even
     *         if true is returned, other listeners will still get the callback
     */
    boolean onKeyPress(SignalEvent event);
  }

  /**
   * A listener that is called on "keyup" native event.
   */
  public interface NativeKeyUpListener {

    /**
     * @param event the event for the key up
     * @return true if the event was handled, false to proceed with default
     *         behavior
     */
    boolean onNativeKeyUp(Event event);
  }

  /**
   * ClientBundle for the editor.
   */
  public interface Resources
      extends
      Buffer.Resources,
      CursorView.Resources,
      SelectionLineRenderer.Resources,
      SearchMatchRenderer.Resources,
      ParenMatchHighlighter.Resources {
    @Source({"Editor.css", "constants.css"})
    Css workspaceEditorCss();
    
    @Source("squiggle.gif")
    ImageResource squiggle();
  }

  /**
   * A listener that is called after the user enters or deletes text and before
   * it is applied to the document.
   */
  public interface BeforeTextListener {
    /**
     * Note: You should not mutate the document within this callback, as this is
     * not supported yet and can lead to other clients having stale position
     * information inside the {@code textChange}.
     * 
     * Note: The {@link TextChange} contains a reference to the live
     * {@link Line} from the document model. If you hold on to a reference after
     * {@link #onBeforeTextChange} returns, beware that the contents of the
     * {@link Line} could change, invalidating some of the state in the
     * {@link TextChange}.
     *
     * @param textChange the text change whose last line will be the same as the
     *        insertion point (since the text hasn't been inserted yet)
     */
    void onBeforeTextChange(TextChange textChange);
  }

  /**
   * A listener that is called when the user enters or deletes text.
   *
   * Similar to {@link Document.TextListener} except is only called when the
   * text is entered/deleted by the local user.
   */
  public interface TextListener {
    /**
     * Note: You should not mutate the document within this callback, as this is
     * not supported yet and can lead to other clients having stale position
     * information inside the {@code textChange}.
     *
     * Note: The {@link TextChange} contains a reference to the live
     * {@link Line} from the document model. If you hold on to a reference after
     * {@link #onTextChange} returns, beware that the contents of the
     * {@link Line} could change, invalidating some of the state in the
     * {@link TextChange}.
     */
    void onTextChange(TextChange textChange);
  }

  /**
   * A listener that is called when the document changes.
   *
   *  This can be used by external clients of the editor; if the client is a
   * component of the editor, use {@link Editor#setDocument(Document)} instead.
   */
  public interface DocumentListener {
    void onDocumentChanged(Document oldDocument, Document newDocument);
  }

  /**
   * A listener that is called when the editor becomes or is no longer
   * read-only.
   */
  public interface ReadOnlyListener {
    void onReadOnlyChanged(boolean isReadOnly);
  }

  /**
   * The view for the editor, containing gutters and the buffer. This exposes
   * only the ability to enable or disable animations.
   */
  public static class View extends CompositeView<Void> {
    private final Element bufferElement;
    final Css css;
    final Resources res;

    private View(Resources res, Element bufferElement, Element inputElement) {

      this.res = res;
      this.bufferElement = bufferElement;
      this.css = res.workspaceEditorCss();

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(bufferElement);
      rootElement.appendChild(inputElement);
      setElement(rootElement);
    }

    private void addGutter(Element gutterElement) {
      getElement().insertBefore(gutterElement, bufferElement);
    }

    private void removeGutter(Element gutterElement) {
      getElement().removeChild(gutterElement);
    }

    public void setAnimationEnabled(boolean enabled) {
      // TODO: Re-enable animations when they are stable.
      if (enabled) {
        // getElement().addClassName(css.animationEnabled());
      } else {
        // getElement().removeClassName(css.animationEnabled());
      }
    }
    
    public Resources getResources() {
      return res;
    }
  }

  public static final int ANIMATION_DURATION = 100;
  private static int idCounter = 0;
  
  private final AppContext appContext;
  private final Buffer buffer;
  private Document document;
  private final ListenerManager<DocumentListener> documentListenerManager =
      ListenerManager.create();
  private final EditorDocumentMutator editorDocumentMutator;
  private final FontDimensionsCalculator editorFontDimensionsCalculator;
  private EditorUndoManager editorUndoManager;
  private final FocusManager focusManager;
  private final MouseHoverManager mouseHoverManager;
  private final int id = idCounter++;

  private final FontDimensionsCalculator.Callback fontDimensionsChangedCallback =
      new FontDimensionsCalculator.Callback() {
        @Override
        public void onFontDimensionsChanged(FontDimensions fontDimensions) {
          handleFontDimensionsChanged();
        }
      };

  private final JsonArray<Gutter> gutters = JsonCollections.createArray();
  private final InputController input;
  private final LeftGutterManager leftGutterManager;
  private LocalCursorController localCursorController;
  private final ListenerManager<ReadOnlyListener> readOnlyListenerManager = ListenerManager
      .create();
  private Renderer renderer;
  private SearchModel searchModel;
  private SelectionManager selectionManager;
  private final EditorActivityManager editorActivityManager;
  private ViewportModel viewport;
  private boolean isReadOnly;
  private final RenderTimeExecutor renderTimeExecutor;

  private Editor(AppContext appContext, View view, Buffer buffer, InputController input,
      FocusManager focusManager, FontDimensionsCalculator editorFontDimensionsCalculator,
      RenderTimeExecutor renderTimeExecutor) {
    super(view);
    this.appContext = appContext;
    this.buffer = buffer;
    this.input = input;
    this.focusManager = focusManager;
    this.editorFontDimensionsCalculator = editorFontDimensionsCalculator;
    this.renderTimeExecutor = renderTimeExecutor;

    Gutter leftGutter = createGutter(
        false, Gutter.Position.LEFT, appContext.getResources().workspaceEditorCss().leftGutter());
    leftGutterManager = new LeftGutterManager(leftGutter, buffer);

    editorDocumentMutator = new EditorDocumentMutator(this);
    mouseHoverManager = new MouseHoverManager(this);

    editorActivityManager =
        new EditorActivityManager(appContext.getUserActivityManager(),
            buffer.getScrollListenerRegistrar(), getKeyListenerRegistrar());

    // TODO: instantiate input from here
    input.initializeFromEditor(this, editorDocumentMutator);

    setAnimationEnabled(true);
    addBoxShadowOnScrollHandler();
    editorFontDimensionsCalculator.addCallback(fontDimensionsChangedCallback);
  }

  private void handleFontDimensionsChanged() {
    buffer.repositionAnchoredElementsWithColumn();
    if (renderer != null) {
      /*
       * TODO: think about a scheme where we don't have to rerender
       * the whole viewport (currently we do because of the right-side gap
       * fillers)
       */
      renderer.renderAll();
    }
  }

  /**
   * Adds a scroll handler to the buffer scrollableElement so that a drop shadow
   * can be added and removed when scrolled.
   */
  private void addBoxShadowOnScrollHandler() {
    if (true) {
      // TODO: investigate why this kills performance
      return;
    }
    
    this.buffer.getScrollListenerRegistrar().add(new ScrollListener() {

      @Override
      public void onScroll(Buffer buffer, int scrollTop) {
        if (scrollTop < 20) {
          getElement().removeClassName(getView().css.scrolled());
        } else {
          getElement().addClassName(getView().css.scrolled());
        }
      }
    });
  }

  public void addLineRenderer(LineRenderer lineRenderer) {
    /*
     * TODO: Because the line renderer is document-scoped, line
     * renderers have to re-add themselves whenever the document changes. This
     * is unexpected.
     */
    renderer.addLineRenderer(lineRenderer);
  }

  public Gutter createGutter(boolean overviewMode, Gutter.Position position, String cssClassName) {
    Gutter gutter = Gutter.create(overviewMode, position, cssClassName, buffer);
    if (viewport != null && renderer != null) {
      gutter.handleDocumentChanged(viewport, renderer);
    }

    gutters.add(gutter);

    gutter.getGutterElement().addClassName(getView().css.gutter());
    getView().addGutter(gutter.getGutterElement());
    return gutter;
  }

  public void removeGutter(Gutter gutter) {
    getView().removeGutter(gutter.getGutterElement());
    gutters.remove(gutter);
  }

  public void setAnimationEnabled(boolean enabled) {
    getView().setAnimationEnabled(enabled);
  }

  public ListenerRegistrar<BeforeTextListener> getBeforeTextListenerRegistrar() {
    return editorDocumentMutator.getBeforeTextListenerRegistrar();
  }

  public Buffer getBuffer() {
    return buffer;
  }

  /*
   * TODO: if left gutter manager gets public API, expose that
   * instead of directly exposign the gutter. Or, if we don't want to expose
   * Gutter#setWidth publicly for the left gutter, make LeftGutterManager the
   * public API.
   */
  public Gutter getLeftGutter() {
    return leftGutterManager.getGutter();
  }

  public Document getDocument() {
    return document;
  }

  /**
   * Returns a document mutator that will also notify editor text listeners.
   */
  public EditorDocumentMutator getEditorDocumentMutator() {
    return editorDocumentMutator;
  }

  public Element getElement() {
    return getView().getElement();
  }

  public FocusManager getFocusManager() {
    return focusManager;
  }

  public MouseHoverManager getMouseHoverManager() {
    return mouseHoverManager;
  }

  public ListenerRegistrar<KeyListener> getKeyListenerRegistrar() {
    return input.getKeyListenerRegistrar();
  }

  public ListenerRegistrar<NativeKeyUpListener> getNativeKeyUpListenerRegistrar() {
    return input.getNativeKeyUpListenerRegistrar();
  }

  public Renderer getRenderer() {
    return renderer;
  }

  public SearchModel getSearchModel() {
    return searchModel;
  }

  public SelectionModel getSelection() {
    return selectionManager.getSelectionModel();
  }

  public LocalCursorController getCursorController() {
    return localCursorController;
  }

  public ListenerRegistrar<TextListener> getTextListenerRegistrar() {
    return editorDocumentMutator.getTextListenerRegistrar();
  }

  public ListenerRegistrar<DocumentListener> getDocumentListenerRegistrar() {
    return documentListenerManager;
  }

  // TODO: need a public interface and impl
  public ViewportModel getViewport() {
    return viewport;
  }

  public boolean isMutatingDocumentFromUndoOrRedo() {
    return editorUndoManager.isMutatingDocument();
  }

  public void removeLineRenderer(LineRenderer lineRenderer) {
    renderer.removeLineRenderer(lineRenderer);
  }

  public void setDocument(final Document document) {
    final Document oldDocument = this.document;

    if (oldDocument != null) {
      // Teardown the objects depending on the old document
      renderer.teardown();
      viewport.teardown();
      selectionManager.teardown();
      localCursorController.teardown();
      editorUndoManager.teardown();
      searchModel.teardown();
    }

    this.document = document;

    /*
     * TODO: dig into each component, figure out dependencies,
     * break apart components so we can reduce circular dependencies which
     * require the multiple stages of initialization
     */
    // Core editor components
    buffer.handleDocumentChanged(document);
    leftGutterManager.handleDocumentChanged(document);
    selectionManager =
        SelectionManager.create(document, buffer, focusManager, appContext.getResources());

    SelectionModel selection = selectionManager.getSelectionModel();
    viewport = ViewportModel.create(document, selection, buffer);
    input.handleDocumentChanged(document, selection, viewport);
    renderer = Renderer.create(document,
        viewport,
        buffer,
        getLeftGutter(),
        selection,
        focusManager,
        this,
        appContext.getResources(),
        renderTimeExecutor);

    // Delayed core editor component initialization
    viewport.initialize();
    selection.initialize(viewport);
    selectionManager.initialize(renderer);
    buffer.handleComponentsInitialized(viewport, renderer);
    for (int i = 0, n = gutters.size(); i < n; i++) {
      gutters.get(i).handleDocumentChanged(viewport, renderer);
    }

    // Non-core editor components
    editorUndoManager = EditorUndoManager.create(this, document, selection);
    searchModel = SearchModel.create(appContext,
        document,
        renderer,
        viewport,
        selection,
        editorDocumentMutator);
    localCursorController =
        LocalCursorController.create(appContext, focusManager, selection, buffer, this);

    documentListenerManager.dispatch(new Dispatcher<Editor.DocumentListener>() {
      @Override
      public void dispatch(DocumentListener listener) {
        listener.onDocumentChanged(oldDocument, document);
      }
    });
  }

  public void undo() {
    editorUndoManager.undo();
  }

  public void redo() {
    editorUndoManager.redo();
  }

  public void scrollTo(int lineNumber, int column) {
    if (document != null) {
      LineInfo lineInfo = document.getLineFinder().findLine(lineNumber);
      /*
       * TODO: the cursor will be the last line in the viewport,
       * fix this
       */
      SelectionModel selectionModel = getSelection();
      selectionModel.deselect();
      selectionModel.setCursorPosition(lineInfo, column);
    }
  }

  public void cleanup() {
    editorFontDimensionsCalculator.removeCallback(fontDimensionsChangedCallback);
    editorActivityManager.teardown();
  }

  public void setReadOnly(final boolean isReadOnly) {

    if (this.isReadOnly == isReadOnly) {
      return;
    }

    this.isReadOnly = isReadOnly;

    readOnlyListenerManager.dispatch(new Dispatcher<Editor.ReadOnlyListener>() {
      @Override
      public void dispatch(ReadOnlyListener listener) {
        listener.onReadOnlyChanged(isReadOnly);
      }
    });
  }

  public boolean isReadOnly() {
    return isReadOnly;
  }

  public ListenerRegistrar<ReadOnlyListener> getReadOnlyListenerRegistrar() {
    return readOnlyListenerManager;
  }

  public int getId() {
    return id;
  }
  
  @VisibleForTesting
  public InputController getInput() {
    return input;
  }
  
  public void setLeftGutterVisible(boolean visible) {
    Element gutterElement = leftGutterManager.getGutter().getGutterElement();
    if (visible) {
      getView().addGutter(gutterElement);
    } else {
      getView().removeGutter(gutterElement);
    }
  }
}
