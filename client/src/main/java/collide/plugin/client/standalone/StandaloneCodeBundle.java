package collide.plugin.client.standalone;

import collide.client.editor.EditorToolbar;
import collide.client.editor.MarkupToolbar;
import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.*;
import com.google.collide.client.code.CodePerspective.View;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.history.Place;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.workspace.WorkspaceShell;

public class StandaloneCodeBundle extends CodePanelBundle {

  public StandaloneCodeBundle(AppContext appContext,
      WorkspaceShell shell,
      FileTreeController<?> fileTreeController,
      FileTreeModel fileTreeModel,
      FileNameSearch searchIndex,
      DocumentManager documentManager,
      ParticipantModel participantModel,
      IncomingDocOpDemultiplexer docOpReceiver,
      Place place) {
    super(appContext, shell, fileTreeController, fileTreeModel,
        searchIndex, documentManager, participantModel, docOpReceiver, place);
  }

  @Override
  protected void attachShellToDom(final WorkspaceShell shell, final View codePerspectiveView) {
    Elements.replaceContents(StandaloneConstants.WORKSPACE_PANEL, codePerspectiveView.detach());
    shell.setPerspective(codePerspectiveView.getElement());
  }

  @Override
  protected EditableContentArea initContentArea(View codePerspectiveView,
      AppContext appContext, EditorBundle editorBundle, FileTreeSection fileTreeSection) {
    EditableContentArea.View view = codePerspectiveView.getContentView();
    final EditorToolbar toolBar = new DefaultEditorToolBar(view.getEditorToolBarView(), FileSelectedPlace.PLACE, appContext, editorBundle);
    // Hook presenter in the editor bundle to the view in the header
    editorBundle.getBreadcrumbs().setView(view.getBreadcrumbsView());
    return new EditableContentArea(view, toolBar, fileTreeSection.getFileTreeUiController());

  }
}
