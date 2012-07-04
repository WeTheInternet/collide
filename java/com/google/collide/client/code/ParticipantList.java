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

import com.google.collide.client.util.Elements;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import elemental.html.DivElement;
import elemental.html.SpanElement;

/**
 * Presenter for the participant list in the navigation bar.
 *
 */
public class ParticipantList extends UiComponent<ParticipantList.View>
    implements ParticipantModel.Listener {

  /**
   * Static factory method for obtaining an instance of the ParticipantList.
   */
  public static ParticipantList create(View view, Resources res, ParticipantModel model) {
    ParticipantList participantList = new ParticipantList(view, model);
    participantList.init();

    return participantList;
  }

  /**
   * CSS for the participant list.
   *
   */
  public interface Css extends CssResource {
    String name();

    String root();

    String row();

    String swatch();
  }

  interface Resources extends ClientBundle {
    @Source("ParticipantList.css")
    Css workspaceNavigationParticipantListCss();
  }

  static class View extends CompositeView<Void> {
    final Resources res;

    final Css css;

    private final JsoStringMap<DivElement> rows = JsoStringMap.create();

    View(Resources res) {
      this.res = res;
      this.css = res.workspaceNavigationParticipantListCss();

      setElement(Elements.createDivElement(css.root()));
    }

    private void addParticipant(String userId, String displayEmail, String name, String color) {
      DivElement rowElement = Elements.createDivElement(css.row());

      DivElement swatchElement = Elements.createDivElement(css.swatch());
      swatchElement.setAttribute("style", "background-color: " + color);
      rowElement.appendChild(swatchElement);

      SpanElement nameElement = Elements.createSpanElement(css.name());
      nameElement.setTextContent(name);
      nameElement.setTitle(displayEmail);
      rowElement.appendChild(nameElement);

      getElement().appendChild(rowElement);
      rows.put(userId, rowElement);
    }

    private void removeParticipant(String userId) {
      DivElement row = rows.get(userId);
      if (row != null) {
        row.removeFromParent();
      }
    }
  }

  private final ParticipantModel model;

  private ParticipantList(View view, ParticipantModel model) {
    super(view);
    this.model = model;
  }

  @Override
  public void participantAdded(Participant participant) {
    addParticipantToView(participant);
  }

  @Override
  public void participantRemoved(Participant participant) {
    getView().removeParticipant(participant.getUserId());
  }

  public ParticipantModel getModel() {
    return model;
  }

  private void init() {
    model.addListener(this);
    populateViewFromModel();
  }

  private void addParticipantToView(Participant participant) {
    getView().addParticipant(participant.getUserId(),
        participant.getDisplayEmail(), participant.getDisplayName(), participant.getColor());
  }

  private void populateViewFromModel() {
    JsoStringMap<Participant> participants = getModel().getParticipants();
    participants.iterate(new IterationCallback<Participant>() {
      @Override
      public void onIteration(String userId, Participant participant) {
        addParticipantToView(participant);
      }
    });
  }
}
