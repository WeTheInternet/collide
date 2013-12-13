package com.google.collide.plugin.client.standalone;

import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.CodePanelBundle;
import com.google.collide.client.code.CodePerspective.View;
import com.google.collide.client.code.ParticipantModel;
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

}
