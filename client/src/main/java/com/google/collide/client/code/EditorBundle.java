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

package com.google.collide.client.code;

import collide.client.filetree.FileTreeModel;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.autoindenter.Autoindenter;
import com.google.collide.client.code.autocomplete.integration.AutocompleterFacade;
import com.google.collide.client.code.debugging.DebuggingModel;
import com.google.collide.client.code.debugging.DebuggingModelController;
import com.google.collide.client.code.errorrenderer.EditorErrorListener;
import com.google.collide.client.code.errorrenderer.ErrorReceiver;
import com.google.collide.client.code.errorrenderer.ErrorRenderer;
import com.google.collide.client.code.gotodefinition.GoToDefinitionHandler;
import com.google.collide.client.code.lang.LanguageHelper;
import com.google.collide.client.code.lang.LanguageHelperResolver;
import com.google.collide.client.code.parenmatch.ParenMatchHighlighter;
import com.google.collide.client.code.popup.EditorPopupController;
import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.codeunderstanding.CubeClientWrapper;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.Editor.Css;
import com.google.collide.client.editor.TextActions;
import com.google.collide.client.editor.input.RootActionExecutor;
import com.google.collide.client.history.Place;
import com.google.collide.client.syntaxhighlighter.SyntaxHighlighter;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.util.UserActivityManager;
import com.google.collide.client.util.logging.Log;
import com.google.collide.client.workspace.outline.OutlineController;
import com.google.collide.client.workspace.outline.OutlineModel;
import com.google.collide.codemirror2.CodeMirror2;
import com.google.collide.codemirror2.Parser;
import com.google.collide.shared.document.Document;

import elemental.dom.Element;

/**
 * A class that bundles together all of the editor-related components, such as the editor widget,
 * the collaboration controller, the document parser and syntax highlighter.
 */
public class EditorBundle implements FileContent {

  /**
   * Static factory method for obtaining an instance of the EditorBundle.
   */
  public static EditorBundle create(AppContext appContext,
      Place currentPlace,
      DocumentManager documentManager,
      ParticipantModel participantModel,
      OutlineModel outlineModel,
      FileTreeModel fileTreeModel,
      ErrorReceiver errorReceiver) {

    final Editor editor = Editor.create(appContext);

    EditorErrorListener editorErrorListener = new EditorErrorListener(
        editor, errorReceiver, new ErrorRenderer(appContext.getResources()));

    EditorPopupController editorPopupController = EditorPopupController.create(
        appContext.getResources(), editor);

    // TODO: clean this up when things stabilize.
    CubeClientWrapper cubeClientWrapper = new CubeClientWrapper(
        appContext.getFrontendApi().GET_CODE_GRAPH);
    CubeClient cubeClient = cubeClientWrapper.getCubeClient();
    AutocompleterFacade autocompleter = AutocompleterFacade.create(
        editor, cubeClient, appContext.getResources());

    GoToDefinitionHandler goToDefinition = new GoToDefinitionHandler(currentPlace,
        editor,
        fileTreeModel,
        appContext.getResources(),
        cubeClient,
        editorPopupController);

    // Here is where to add support for rendering links / nested content

    SelectionRestorer selectionRestorer = new SelectionRestorer(editor);

    DebuggingModel debuggingModel = new DebuggingModel();
    DebuggingModelController<?> debuggingModelController =
        DebuggingModelController.create(currentPlace,
            appContext.getResources(),
            debuggingModel,
            editor,
            editorPopupController,
            documentManager);

    WorkspaceLocationBreadcrumbs breadcrumbs = new WorkspaceLocationBreadcrumbs();

    OutlineController outlineController = new OutlineController(outlineModel, cubeClient, editor);

    final EditorBundle editorBundle = new EditorBundle(documentManager,
        editor,
        editorErrorListener,
        autocompleter,
        goToDefinition,
        selectionRestorer,
        debuggingModelController,
        breadcrumbs,
        cubeClientWrapper,
        outlineController,
        appContext.getUserActivityManager(),
        editorPopupController,
        appContext.getResources().workspaceEditorCss());

    return editorBundle;
  }

  private final AutocompleterFacade autocompleter;
  private Autoindenter autoindenter;
  private final DocumentManager documentManager;
  private final Editor editor;
  private final GoToDefinitionHandler goToDefinition;
  private DocumentParser parser;
  private final SelectionRestorer selectionRestorer;
  private final DebuggingModelController<?> debuggingModelController;
  private final WorkspaceLocationBreadcrumbs breadcrumbs;
  private final CubeClientWrapper cubeClientWrapper;
  private final OutlineController outlineController;

  /*
   * TODO: EditorBundle shouldn't have path. It's here to satisfy legacy dependency
   */
  private PathUtil path;
  private SyntaxHighlighter syntaxHighlighter;
  private final EditorErrorListener editorErrorListener;
  private final UserActivityManager userActivityManager;
  private final EditorPopupController editorPopupController;
  private ParenMatchHighlighter matchHighlighter;
  private RootActionExecutor.Remover languageActionsRemover;
  private RootActionExecutor.Remover textActionsRemover;
  private final Css editorCss;
  private final boolean isReadOnly = false;

  private EditorBundle(DocumentManager documentManager,
      Editor editor,
      EditorErrorListener editorErrorListener,
      AutocompleterFacade autoCompleter,
      GoToDefinitionHandler goToDefinition,
      SelectionRestorer selectionRestorer,
      DebuggingModelController<?> debuggingModelController,
      WorkspaceLocationBreadcrumbs breadcrumbs,
      CubeClientWrapper cubeClientWrapper,
      OutlineController outlineController,
      UserActivityManager userActivityManager,
      EditorPopupController editorPopupController,
      Editor.Css editorCss) {
    this.documentManager = documentManager;
    this.editor = editor;
    this.editorErrorListener = editorErrorListener;
    this.autocompleter = autoCompleter;
    this.goToDefinition = goToDefinition;
    this.selectionRestorer = selectionRestorer;
    this.debuggingModelController = debuggingModelController;
    this.breadcrumbs = breadcrumbs;
    this.cubeClientWrapper = cubeClientWrapper;
    this.outlineController = outlineController;
    this.userActivityManager = userActivityManager;
    this.editorPopupController = editorPopupController;
    this.editorCss = editorCss;
  }

  public Editor getEditor() {
    return editor;
  }

  /**
   * The readonly state of this workspace. The readonly state of the editor can change (i.e. when a
   * file is deleted, the workspace temporarily goes into readonly mode until a current file is
   * open), but the readonly state of the EditorBundle is final based on the workspace. This is now
   * hardcoded to false.
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }

  public WorkspaceLocationBreadcrumbs getBreadcrumbs() {
    return breadcrumbs;
  }

  public PathUtil getPath() {
    return path;
  }

  public DebuggingModelController<?> getDebuggingModelController() {
    return debuggingModelController;
  }

  public void cleanup() {
    reset();
    goToDefinition.cleanup();
    autocompleter.cleanup();
    editorErrorListener.cleanup();
    editor.cleanup();
    outlineController.cleanup();
    cubeClientWrapper.cleanup();
    editorPopupController.cleanup();
    debuggingModelController.cleanup();

    // TODO: remove
    Element readOnlyElement = Elements.getElementById("readOnly");
    if (readOnlyElement != null) {
      readOnlyElement.removeFromParent();
    }
  }

  /**
   * Detach services attached on {@link #setDocument}.
   *
   * <p>
   * These services are constructed on the base of document and path, When active document is
   * changed or editor is closed, they should gracefully cleanup.
   */
  private void reset() {
    if (parser != null) {
      parser.teardown();
      parser = null;
    }

    if (syntaxHighlighter != null) {
      editor.removeLineRenderer(syntaxHighlighter.getRenderer());
      syntaxHighlighter.teardown();
      syntaxHighlighter = null;
    }

    if (autoindenter != null) {
      autoindenter.teardown();
      autoindenter = null;
    }

    if (matchHighlighter != null) {
      matchHighlighter.teardown();
    }

    if (languageActionsRemover != null) {
      languageActionsRemover.remove();
      languageActionsRemover = null;
    }

    if (textActionsRemover != null) {
      textActionsRemover.remove();
      textActionsRemover = null;
    }
  }

  @Override
  public PathUtil filePath() {
    return path;
  }

  /**
   * Replaces the document for the editor and related components.
   */
  public void setDocument(Document document, PathUtil path, String fileEditSessionKey) {
    selectionRestorer.onBeforeDocumentChanged();

    reset();

    this.path = path;

    documentManager.attachToEditor(document, editor);

    Parser codeMirrorParser = CodeMirror2.getParser(path);
    parser = codeMirrorParser == null ? null
        : DocumentParser.create(document, codeMirrorParser, userActivityManager);

    LanguageHelper languageHelper = LanguageHelperResolver.getHelper(parser.getSyntaxType());
    RootActionExecutor actionExecutor = editor.getInput().getActionExecutor();
    languageActionsRemover = actionExecutor.addDelegate(languageHelper.getActionExecutor());
    textActionsRemover = actionExecutor.addDelegate(TextActions.INSTANCE);

    cubeClientWrapper.setDocument(document, path.getPathString());

    goToDefinition.editorContentsReplaced(path, parser);
    selectionRestorer.onDocumentChanged(fileEditSessionKey);
    editorErrorListener.onDocumentChanged(document, fileEditSessionKey);

    debuggingModelController.setDocument(document, path, parser);
    outlineController.onDocumentChanged(parser);

    try {
      autocompleter.editorContentsReplaced(path, parser);
    } catch (Throwable t) {
      Log.error(getClass(), "Autocompletion subsystem failed to accept the changed document", t);
    }

    syntaxHighlighter = SyntaxHighlighter.create(document,
        editor.getRenderer(),
        editor.getViewport(),
        editor.getSelection(),
        parser,
        editorCss);
    editor.addLineRenderer(syntaxHighlighter.getRenderer());
    /*
     * Make sure we open the editor in the right state according to the workspace readonly state.
     * (For example, deleting a file will temporarily set the editor in readonly mode while it's
     * still open).
     */
    editor.setReadOnly(isReadOnly);

    autoindenter = Autoindenter.create(parser, editor);

    parser.begin();

    breadcrumbs.setPath(path);

    if (!isReadOnly) {
      matchHighlighter = ParenMatchHighlighter.create(document,
          editor.getViewport(),
          document.getAnchorManager(),
          editor.getView().getResources(),
          editor.getRenderer(),
          editor.getSelection());
    }
  }

  @Override
  public Element getContentElement() {
    return getEditor().getElement();
  }

  @Override
  public void onContentDisplayed() {
    getEditor().getBuffer().synchronizeScrollTop();
  }

  @Override
  public void onContentDestroyed() {

  }

  public OutlineController getOutlineController() {
    return outlineController;
  }

}
