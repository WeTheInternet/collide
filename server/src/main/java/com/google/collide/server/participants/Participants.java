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

package com.google.collide.server.participants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.collide.dto.server.DtoServerImpls.GetWorkspaceParticipantsResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.ParticipantImpl;
import com.google.collide.dto.server.DtoServerImpls.ParticipantUserDetailsImpl;
import com.google.collide.dto.server.DtoServerImpls.UserDetailsImpl;
import com.google.collide.server.shared.util.Dto;

/**
 * Acts as the authentication provider, with a compatible API subset with the bundled default
 * AuthManager that comes with Vertx.
 * 
 * This one however is in-memory, and also has affordances for broadcasting to joined participants.
 * Also, this implementation allows for a single username to be logged in as multiple different
 * sessions.
 */
public class Participants extends BusModBase {

  public static final String CLIENT_ADDRESS_PREFX = "client";

  public static final String PAYLOAD_TAG = "payload";
  public static final String OMIT_SENDER_TAG = "omitSender";
  public static final String TARGET_SPECIFIC_CLIENT_TAG = "sendToClient";
  public static final String TARGET_USERS_TABS_TAG = "sendToUsersTabs";

  private static final long DEFAULT_LOGIN_TIMEOUT = 60 * 60 * 1000; // 1 hour

  // TODO: This is temporarily set to 30 mins for testing.
  private static final long DEFAULT_KEEP_ALIVE_TIMEOUT = 30 * 1000 * 60; // 30 secs

  /**
   * A single Collide tab for a logged in user that is connected to the eventbus.
   */
  private static final class ConnectedTab {
    final LoggedInUser loginInfo;    
    long timerId;

    ConnectedTab(LoggedInUser loginInfo, long tabDisconnectTimerId) {
      this.loginInfo = loginInfo;
      this.timerId = tabDisconnectTimerId;      
    }
  }

  /**
   * A logged in user that may have multiple tabs open.
   */
  private static final class LoggedInUser {
    final String username;
    
    /** Stable user ID for the lifetime of the server. */
    final String userId;
    long timerId;

    private LoggedInUser(String username) {      
      this.username = username;
      this.userId = getStableUserId(username);
    }

    /** Stable identifier for a username. Stable for the lifetime of the server. */
    static final Map<String, String> usernameToStableIdMap = new HashMap<String, String>();
    static String getStableUserId(String username) {
      String stableId = usernameToStableIdMap.get(username);
      if (stableId == null) {
        stableId = UUID.randomUUID().toString();
        usernameToStableIdMap.put(username, stableId);       
      }
      return stableId;
    }
  }

  private String password;
  private long tabKeepAliveTimeout;
  private long loginSessionTimeout;

  /** Map of per-tab active client IDs to ConnectedTabs. */
  protected final Map<String, ConnectedTab> connectedTabs = new HashMap<String, ConnectedTab>();

  /** Map of per-user session IDs LoggedInUsers. */
  protected final Map<String, LoggedInUser> loggedInUsers = new HashMap<String, LoggedInUser>();

  @Override
  public void start() {
    super.start();
 
    this.password = getOptionalStringConfig("password", "");
    this.loginSessionTimeout = getOptionalLong("session_timeout", DEFAULT_LOGIN_TIMEOUT);
    this.tabKeepAliveTimeout = getOptionalLong("keep_alive_timeout", DEFAULT_KEEP_ALIVE_TIMEOUT);
    String addressBase = getOptionalStringConfig("address", "participants");   

    eb.registerHandler(addressBase + ".login", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        doLogin(message);
      }
    });

    eb.registerHandler(addressBase + ".logout", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        doLogout(message);
      }
    });

    eb.registerHandler(addressBase + ".authorise", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        doAuthorise(message);
      }
    });

    eb.registerHandler(addressBase + ".keepAlive", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        doKeepAlive(event);
      }      
    });

    eb.registerHandler(addressBase + ".getParticipants", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        doGetParticipants(event);
      }      
    });

    eb.registerHandler(addressBase + ".broadcast", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        doBroadcast(event);
      }   
    });

    eb.registerHandler(addressBase + ".sendTo", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> event) {
        doSendTo(event);
      }
    });
  }

  private long getOptionalLong(String fieldName, long defaultVal) {
    Number val = config.getNumber(fieldName);
    if (val == null) {
      return defaultVal;
    }
    return val instanceof Integer ? (Integer) val : (Long) val;    
  }

  void doBroadcast(Message<JsonObject> event) {
    String payload = event.body.getString(PAYLOAD_TAG);
    String senderActiveClientId = event.body.getString(OMIT_SENDER_TAG);
    Set<Entry<String, ConnectedTab>> entries = connectedTabs.entrySet();
    for (Entry<String, ConnectedTab> entry : entries) {
      String activeClientId = entry.getKey();
      String address = CLIENT_ADDRESS_PREFX + "." + activeClientId;

      // Send to everyone except the optionally specified sender that we wish to ignore.
      if (!activeClientId.equals(senderActiveClientId)) {        
        vertx.eventBus().send(address, Dto.wrap(payload));
      }
    }
  }

  void doSendTo(Message<JsonObject> event) {
    String payload = event.body.getString(PAYLOAD_TAG);

    List<String> clientsToMessage = new ArrayList<String>();
    String activeClientId = event.body.getString(TARGET_SPECIFIC_CLIENT_TAG);
    if (activeClientId != null) {

      // Send to a specific tab.
      ConnectedTab tab = connectedTabs.get(activeClientId);
      if (tab != null) {        
        clientsToMessage.add(activeClientId);
      }
    } else {
      String username = event.body.getString(TARGET_USERS_TABS_TAG);
      if (username != null) {

        // Collect the ids of all this user's open tabs.
        Set<Entry<String, ConnectedTab>> allClients = connectedTabs.entrySet();
        for (Entry<String, ConnectedTab> entry : allClients) {
          if (username.equals(entry.getValue().loginInfo.username)) {
            clientsToMessage.add(entry.getKey());
          }
        }
      }
    }

    // Message the clients.
    for (String cid :  clientsToMessage) {
      vertx.eventBus().send(CLIENT_ADDRESS_PREFX + "." + cid, Dto.wrap(payload));
    }
  }

  /**
   * Returns all the connected tabs, as well as the user information for the user that owns each
   * tab.
   */
  void doGetParticipants(Message<JsonObject> event) {
    GetWorkspaceParticipantsResponseImpl resp = GetWorkspaceParticipantsResponseImpl.make();
    List<ParticipantUserDetailsImpl> collaboratorsArr = new ArrayList<ParticipantUserDetailsImpl>();

    Set<Entry<String, ConnectedTab>> collaborators = connectedTabs.entrySet();
    for (Entry<String, ConnectedTab> entry : collaborators) {
      String userId = entry.getValue().loginInfo.userId;
      String username = entry.getValue().loginInfo.username;
      ParticipantUserDetailsImpl participantDetails = ParticipantUserDetailsImpl.make();
      ParticipantImpl participant = ParticipantImpl.make().setId(entry.getKey()).setUserId(userId);
      UserDetailsImpl userDetails = UserDetailsImpl.make()
          .setUserId(userId).setDisplayEmail(username).setDisplayName(username)
          .setGivenName(username);

      participantDetails.setParticipant(participant);
      participantDetails.setUserDetails(userDetails);      
      collaboratorsArr.add(participantDetails);
    }

    resp.setParticipants(collaboratorsArr);        
    event.reply(Dto.wrap(resp));
  }

  void doKeepAlive(Message<JsonObject> event) {
    final String activeClientId = event.body.getString("activeClient");
    if (activeClientId != null) {
      ConnectedTab loginInfo = connectedTabs.get(activeClientId);
      if (loginInfo != null) {
        vertx.cancelTimer(loginInfo.timerId);
        loginInfo.timerId = vertx.setTimer(tabKeepAliveTimeout, new Handler<Long>() {
          @Override
          public void handle(Long timerID) {
            connectedTabs.remove(activeClientId);                
          }
        });
      }
    }
  }

  void doLogin(final Message<JsonObject> message) {
    final String username = message.body.getString("username", null);
    if (username == null) {
      sendStatus("denied", message);
      return;
    }
    String password = message.body.getString("password", null);
    if (password == null && !"".equals(this.password)) {      
      sendStatus("denied", message, new JsonObject().putString("reason", "needs-pass"));
      return;
    }

    if(!authenticate(username, password)) {
      sendStatus("denied", message);
      return;
    }

    // Passed authentication. Create a logged in user and a timer to expire his session.
    if (alreadyLoggedIn(username)) {
      // Cancel the previous session logout timer.
      LoggedInUser existing = loggedInUsers.remove(LoggedInUser.getStableUserId(username));
      vertx.cancelTimer(existing.timerId);
    }

    final LoggedInUser user = new LoggedInUser(username);    
    loggedInUsers.put(user.userId, user);
    user.timerId = vertx.setTimer(loginSessionTimeout, new Handler<Long>() {
      @Override
      public void handle(Long event) {
        logout(user.userId);
      }
    });

    // The spelling "sessionID" is needed to work with the vertx eventbus bridge whitelist.
    JsonObject jsonReply = new JsonObject().putString("sessionID", user.userId);
    sendOK(message, jsonReply);
  }

  /**
   * This is NOT the same as authenticating. This just checks to see if we are tracking a login
   * session for this username already.
   */
  private boolean alreadyLoggedIn(String username) {
    return loggedInUsers.get(LoggedInUser.getStableUserId(username)) != null;
  }
  
  private String createActiveTab(LoggedInUser user) {
    final String activeClient = UUID.randomUUID().toString();
    long timerId = vertx.setTimer(tabKeepAliveTimeout, new Handler<Long>() {
      @Override
       public void handle(Long timerId) {
         connectedTabs.remove(activeClient);        
       }
     });
    connectedTabs.put(activeClient, new ConnectedTab(user, timerId));
    return activeClient;
  }

  private boolean authenticate(String username, String password) {
    return "".equals(this.password) || password.equals(this.password);
  }

  void doLogout(final Message<JsonObject> message) {
    final String sessionID = getMandatoryString("sessionID", message);
    if (sessionID != null) {
      if (logout(sessionID)) {
        sendOK(message);
      } else {
        super.sendError(message, "Not logged in");
      }
    }
  }

  private boolean logout(String userId) {
    LoggedInUser user = loggedInUsers.remove(userId);
    if (user != null) {
      List<Entry<String, ConnectedTab>> usersTabs = new ArrayList<Entry<String, ConnectedTab>>();
      Set<Entry<String, ConnectedTab>> entries = connectedTabs.entrySet();
      for (Entry<String, ConnectedTab> entry : entries) {
        if (userId.equals(entry.getValue().loginInfo.userId)) {
          usersTabs.add(entry);
        }
      }

      for (int i=0;i<usersTabs.size();i++) {
        connectedTabs.remove(usersTabs.get(i).getKey());
        vertx.cancelTimer(usersTabs.get(i).getValue().timerId);
      }
      return true;
    } else {
      return false;
    }
  }

  void doAuthorise(Message<JsonObject> message) {
    String userId = getMandatoryString("sessionID", message);
    if (userId == null) {
      sendStatus("denied", message);
      return;
    }

    LoggedInUser user = loggedInUsers.get(userId);
    if (user != null || "".equals(password)) {
      String username = message.body.getString("username", "anonymous");
      if (user != null) {
        username = user.username;
      } else {
        user = new LoggedInUser(username);        
      }
      JsonObject reply = new JsonObject().putString("username", username);
      if (message.body.getBoolean("createClient", false)) {
        String activeClient = createActiveTab(user);
        reply.putString("activeClient", activeClient);
      }
      sendOK(message, reply);
    } else {      
      sendStatus("denied", message);
    }
  }
}
