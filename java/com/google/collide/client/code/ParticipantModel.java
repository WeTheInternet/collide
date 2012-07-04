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

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.client.util.QueryCallbacks.SimpleCallback;
import com.google.collide.dto.GetWorkspaceParticipants;
import com.google.collide.dto.GetWorkspaceParticipantsResponse;
import com.google.collide.dto.ParticipantUserDetails;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.UserDetails;
import com.google.collide.dto.client.DtoClientImpls.GetWorkspaceParticipantsImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

/**
 * Model for the participants in the current workspace.
 */
// TODO: Pass the initial list of participants from the workspace bootstrap response
public class ParticipantModel {

  /**
   * Listener for changes in the participant model.
   *
   */
  public interface Listener {
    void participantAdded(Participant participant);

    void participantRemoved(Participant participant);
  }

  private static class ColorGenerator {

    private static final String[] COLORS = new String[] {"#FC9229", // Orange
        "#51D13F", // Green
        "#B744D1", // Purple
        "#3BC9D1", // Cyan
        "#D13B45", // Pinky Red
        "#465FE6", // Blue
        "#F41BDB", // Magenta
        "#B7AC4A", // Mustard
        "#723226" // Brown
    };

    private int previousColorIndex = -1;

    private String nextColor() {
      previousColorIndex = (previousColorIndex + 1) % COLORS.length;
      return COLORS[previousColorIndex];
    }
  }

  public static ParticipantModel create(FrontendApi frontendApi, MessageFilter messageFilter) {
    ParticipantModel model = new ParticipantModel(frontendApi);
    model.registerForInvalidations(messageFilter);
    model.requestAllParticipants();
    return model;
  }

  private final JsoArray<Listener> listeners;

  /**
   * The last callback created when requesting all participants. If this is null, then the last
   * request was for specific participants.
   */
  private SimpleCallback<JsonArray<ParticipantUserDetails>> lastRequestAllCallback;

  /**
   * The set of all active participant ids, including participants who have not been added to the
   * participant list because we don't have user details yet.
   */
  private JsonStringSet presentParticipantsTracker = JsoStringSet.create();

  private final JsoStringMap<Participant> participantsByUserId = JsoStringMap.create();

  private final JsoStringMap<String> clientIdToUserId = JsoStringMap.create();

  /**
   * A map of user ID to the {@link UserDetails} for the participant. We cache participant info in
   * case users connect and disconnect rapidly. We keep the cache here so we can discard it when the
   * user leaves the workspace.
   */
  // TODO: Should we persist user details for the entire session?
  private final JsoStringMap<UserDetails> participantUserDetails = JsoStringMap.create();

  /**
   * Tracks the number of participants (optimization for otherwise having to iterate participants to
   * get its size).
   */
  private int count;
  private final ColorGenerator colorGenerator;
  private Participant self;
  private final FrontendApi frontendApi;

  private ParticipantModel(FrontendApi frontendApi) {
    this.frontendApi = frontendApi;
    colorGenerator = new ColorGenerator();
    listeners = JsoArray.create();
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public int getCount() {
    return count;
  }

  public String getUserId(final String clientId) {
    return clientIdToUserId.get(clientId);
  }

  public Participant getParticipantByUserId(final String id) {
    return participantsByUserId.get(id);
  }

  /**
   * Gets the participants keyed by user id. Do not modify the returned map (not enforced for
   * performance reasons).
   */
  public JsoStringMap<Participant> getParticipants() {
    return participantsByUserId;
  }

  public Participant getSelf() {
    return self;
  }

  private void registerForInvalidations(MessageFilter messageFilter) {
    messageFilter.registerMessageRecipient(RoutingTypes.GETWORKSPACEPARTICIPANTSRESPONSE,
        new MessageRecipient<GetWorkspaceParticipantsResponse>() {

            @Override
          public void onMessageReceived(GetWorkspaceParticipantsResponse message) {
            handleParticipantUserDetails(message.getParticipants(), true);
          }
        });
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void createAndAddParticipant(
      com.google.collide.dto.Participant participantDto, UserDetails userDetails) {
    boolean isSelf =
        participantDto.getUserId().equals(BootstrapSession.getBootstrapSession().getUserId());
    String color = isSelf ? "black" : colorGenerator.nextColor();
    Participant participant = Participant.create(
        participantDto, userDetails.getDisplayName(), userDetails.getDisplayEmail(), color, isSelf);
    participantsByUserId.put(participantDto.getUserId(), participant);
    count++;

    if (isSelf) {
      self = participant;
    }

    dispatchParticipantAdded(participant);
  }

  private void dispatchParticipantAdded(Participant participant) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      listeners.get(i).participantAdded(participant);
    }
  }

  private void dispatchParticipantRemoved(Participant participant) {
    for (int i = 0, n = listeners.size(); i < n; i++) {
      listeners.get(i).participantRemoved(participant);
    }
  }

  /**
   * Requests all participants.
   */
  private void requestAllParticipants() {
    lastRequestAllCallback =
      new SimpleCallback<JsonArray<ParticipantUserDetails>>("Failed to retrieve participants") {
      @Override
      public void onQuerySuccess(JsonArray<ParticipantUserDetails> result) {
        /*
         * If there is still an outstanding request for all participants, we should replace all
         * participants with these results. Even if this request isn't the last request for all
         * participants, we should still replace all participants or we might flail in a busy
         * workspace and never update the list.
         *
         * If we've received a tango message containing the updated list of participants,
         * lastRequestAllCallback will be null and we should not replace all participants.
         */
        boolean replaceAll = (lastRequestAllCallback != null);

        /*
         * If this is the last callback, then set lastRequestAllCallback to null so older
         * lastRequestAllCallbacks received out of order do not overwrite the most recent callback.
         */
        if (this == lastRequestAllCallback) {
          lastRequestAllCallback = null;
        }

        handleParticipantUserDetails(result, replaceAll);
      }
    };

    GetWorkspaceParticipants req = GetWorkspaceParticipantsImpl.make();
    frontendApi.GET_WORKSPACE_PARTICIPANTS.send(
        req, new ApiCallback<GetWorkspaceParticipantsResponse>() {

          @Override
          public void onMessageReceived(GetWorkspaceParticipantsResponse message) {
            lastRequestAllCallback.onQuerySuccess(message.getParticipants());
          }

          @Override
          public void onFail(FailureReason reason) {
            // Do nothing.
          }
        });
  }

  /**
   * Updates the model with the participant user details.
   *
   * @param isAllParticipants true if the result contains the complete list of participants
   */
  private void handleParticipantUserDetails(
      JsonArray<ParticipantUserDetails> result, boolean isAllParticipants) {
    // Reset the tracker if the result is all inclusive.
    if (isAllParticipants) {
      presentParticipantsTracker = JsonCollections.createStringSet();
    }

    for (int i = 0; i < result.size(); i++) {
      ParticipantUserDetails item = result.get(i);
      UserDetails userDetails = item.getUserDetails();
      String userId = userDetails.getUserId();

      // Cache the participants' user details.
      participantUserDetails.put(userId, userDetails);
      clientIdToUserId.put(item.getParticipant().getId(), userId);

      if (isAllParticipants) {
        presentParticipantsTracker.add(userId);
        if (!participantsByUserId.containsKey(userId)) {
          createAndAddParticipant(item.getParticipant(), userDetails);
        }
      } else {
        /*
         * Add the participant to the list. If the user is not in presentParticipantsTracker set,
         * then the participant has since disconnected. If the user is in the participants map, then
         * the user was already added to the view.
         */
        if (presentParticipantsTracker.contains(userId)
            && !participantsByUserId.containsKey(userId)) {
          createAndAddParticipant(item.getParticipant(), userDetails);
        }
      }
    }

    // Sweep through participants to find removed participants.
    removeOldParticipants();
  }

  /**
   * Removes users who have left the workspace from the participant list.
   */
  private void removeOldParticipants() {
    // NOTE: Iterating collection that is not affected by removing.
    for (String userId : participantsByUserId.getKeys().asIterable()) {
      if (!presentParticipantsTracker.contains(userId)) {
        Participant participant = participantsByUserId.remove(userId);
        if (participant != null) {
          count--;
          dispatchParticipantRemoved(participant);
        }

        for (String clientId : clientIdToUserId.getKeys().asIterable()) {
          if (clientIdToUserId.get(clientId).equals(userId)) {
            clientIdToUserId.remove(clientId);
          }
        }
      }
    }
  }
}
