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

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.util.AnimationController;

/**
 * The Presenter for the Collaboration/People section at the bottom of the
 * Workspace Navigator.
 */
public class CollaborationSection extends WorkspaceNavigationSection<CollaborationSection.View>
    implements ParticipantModel.Listener {

  /**
   * Static factory method for obtaining an instance of a CollaborationSection.
   */
  public static CollaborationSection create(
      CollaborationSection.View view, ParticipantModel participantModel, AppContext appContext) {

    // Create sub-presenters.
    ShareWorkspacePane shareWorkspacePane =
        ShareWorkspacePane.create(view.shareWorkspacePaneView, appContext);
    ParticipantList participantList = ParticipantList.create(
        view.participantListView, appContext.getResources(), participantModel);

    // Create and initialize the CollaborationSection.
    CollaborationSection presenter =
        new CollaborationSection(view, shareWorkspacePane, participantList,
            new AnimationController.Builder().setFade(true).setCollapse(true).build());
    presenter.init();
    participantModel.addListener(presenter);

    return presenter;
  }

  /**
   * Styles and images.
   */
  public interface Resources extends WorkspaceNavigationSection.Resources,
      ShareWorkspacePane.Resources, ParticipantList.Resources {
  }

  /**
   * The View for the CollaborationSection.
   */
  public static class View extends WorkspaceNavigationSection.View<WorkspaceNavigationSection.ViewEvents> {
    ShareWorkspacePane.View shareWorkspacePaneView;
    ParticipantList.View participantListView;

    public View(Resources res) {
      super(res);

      // Create subviews.
      this.shareWorkspacePaneView = new ShareWorkspacePane.View(res);
      this.participantListView = new ParticipantList.View(res);

      // Initialize View;
      setTitle("Collaborate");
      setStretch(true);
      setBlue(true);
      setContent(Elements.createDivElement());
      getContentElement().appendChild(participantListView.getElement());
      getContentElement().appendChild(shareWorkspacePaneView.getElement());
    }
  }

  private final ShareWorkspacePane shareWorkspacePane;
  private final ParticipantList participantList;
  private final AnimationController animator;

  CollaborationSection(View view, ShareWorkspacePane shareWorkspacePane,
      ParticipantList participantList, AnimationController animator) {
    super(view);

    this.shareWorkspacePane = shareWorkspacePane;
    this.participantList = participantList;
    this.animator = animator;
  }

  @Override
  public void participantAdded(Participant participant) {
    updateViewFromModel();
  }

  @Override
  public void participantRemoved(Participant participant) {
    updateViewFromModel();
  }

  private void init() {
    updateViewFromModel();
  }

  /**
   * Update the visibility of view components based on the number of
   * participants.
   */
  private void updateViewFromModel() {
    boolean isCollaborative = participantList.getModel().getCount() > 1;
    shareWorkspacePane.setInstructionsVisible(!isCollaborative, animator);
    // TODO: follow up CL will properly clean all of this up
    CssUtils.setDisplayVisibility2(participantList.getView().getElement(), true);
  }
}
