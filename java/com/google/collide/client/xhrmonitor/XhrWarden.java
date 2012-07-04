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

package com.google.collide.client.xhrmonitor;

import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringMap.IterationCallback;
import com.google.collide.shared.util.JsonCollections;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.dom.XMLHttpRequest;

/**
 * The warden watches XMLHttpRequests so we can monitor how many are going out
 * and in and log it when it passes some threshold. The request kill feature
 * should not be used in production as it could degrade the user experience.
 */
public class XhrWarden {
  /**
   * If more than {@code WARDEN_WARNING_THRESHOLD} xhr requests are opened the
   * warden will trigger a warning to the warden listener.
   */
  public static final int WARDEN_WARNING_THRESHOLD = 7;
  /**
   * If {@code WARDEN_REQUEST_LIMIT} xhr's are already opened the oldest one
   * gets logged and is killed automatically.
   */
  /*
   * After switching to SPDY, we don't have a hard-limit on simultaneous XHRs so
   * there's no need to kill. Leaving this in just in-case anyone needs to use it
   * for debug purposes.
   */
  public static final int WARDEN_REQUEST_LIMIT = Integer.MAX_VALUE;

  /**
   * The underlying singleton used by the warden implementation.
   */
  private static WardenImpl wardenManager;

  /**
   * Initializes the warden.
   */
  public static WardenRequestManager watch() {
    if (wardenManager == null) {
      wardenManager = new WardenImpl(WARDEN_WARNING_THRESHOLD, WARDEN_REQUEST_LIMIT);
      createWarden(wardenManager);
    }
    return wardenManager;
  }
  
  /**
   * Listener for warden events
   */
  public interface WardenListener {
    /**
     * Called when the warning threshold of XHR requests is reached.
     */
    public void onWarning(WardenRequestManager manager);

    /**
     * Called when the hard request limit is reached and the oldest XHR request has been killed.
     *
     * @param request The request that was killed.
     */
    public void onEmergency(WardenRequestManager manager, WardenXhrRequest request);
  }

  /**
   * Defines public facing methods of a warden request manager.
   */
  public interface WardenRequestManager {
    public int getRequestCount();

    /**
     * Dumps all requests to the console.
     */
    void dumpRequestsToConsole();

    /**
     * Iterates over all open requests objects.
     */
    public void iterate(IterationCallback<WardenXhrRequest> callback);

    /**
     * Adds a custom header to the list of headers to be added by the XhrWarden.
     * This is meant for debugging purposes since this will get added to every
     * XHR request made by the client.
     */
    public void addCustomHeader(String header, String value);
  }

  interface WardenReadyStateHandler {
    public void onRequestOpen(WardenXhrRequest request);

    public void onRequestDone(WardenXhrRequest request);

    public void onRequestOpening(WardenXhrRequest request);

    public void doListRequests();
  }

  /**
   * Receives javascript events from the warden and decides which XHR requests
   * may need to die. Also deals with logging to counselor if things start going
   * awry.
   */
  static class WardenImpl implements WardenReadyStateHandler, WardenRequestManager {
    private final JsonStringMap<WardenXhrRequest> openXhrRequests;
    private final JsonStringMap<String> customHeaders;
    private boolean alreadyLoggedError;
    private final WardenListener eventListener;
    private final int requestWarningLimit;
    private final int requestErrorLimit;

    /**
     * The default event handler for warden events.
     */
    private static class WardenEventHandler implements WardenListener {
      @Override
      public void onEmergency(WardenRequestManager manager, WardenXhrRequest request) {
        String message =
            "The Warden killed an xhr request due to capacity issues: " + request.getUrl();
        Log.info(XhrWarden.class, message);
      }

      @Override
      public void onWarning(WardenRequestManager manager) {
        Log.info(XhrWarden.class, "Warden Warning -- Too Many Open Requests.");
        manager.dumpRequestsToConsole();
      }
    }

    public WardenImpl(int requestWarningLimit, int requestErrorLimit) {
      this(requestWarningLimit, requestErrorLimit, new WardenEventHandler());
    }

    public WardenImpl(int requestWarningLimit, int requestErrorLimit, WardenListener listener) {
      this.requestWarningLimit = requestWarningLimit;
      this.requestErrorLimit = requestErrorLimit;
      openXhrRequests = JsonCollections.createMap();
      customHeaders = JsonCollections.createMap();
      alreadyLoggedError = false;
      eventListener = listener;
    }

    @Override
    public int getRequestCount() {
      return openXhrRequests.getKeys().size();
    }

    @Override
    public void iterate(IterationCallback<WardenXhrRequest> callback) {
      openXhrRequests.iterate(callback);
    }

    public WardenXhrRequest getLongestIdleRequest() {
      WardenXhrRequest oldest = null;
      for (int i = 0; i < openXhrRequests.getKeys().size(); i++) {
        WardenXhrRequest wrapper = openXhrRequests.get(openXhrRequests.getKeys().get(i));
        if (oldest == null || oldest.getTime() > wrapper.getTime()) {
          oldest = wrapper;
        }
      }
      return oldest;
    }
    
    @Override
    public void dumpRequestsToConsole() {
      final StringBuilder builder = new StringBuilder();
      builder.append("\n -- ");
      builder.append(getRequestCount());
      builder.append(" Open XHR Request(s) --\n");
      iterate(new IterationCallback<WardenXhrRequest>() {
        @Override
        public void onIteration(String key, WardenXhrRequest value) {
          builder.append('(');
          builder.append(key);
          builder.append(") ");
          builder.append(value.getUrl());
          builder.append(" -- last activity on ");
          builder.append(value.getDateString());
          builder.append('\n');
        }
      });

      Log.info(getClass(), builder.toString());
    }

    @Override
    public void onRequestOpen(WardenXhrRequest request) {
      if (openXhrRequests.get(request.getId()) != null) {
        // strange state to be in
        return;
      }
      openXhrRequests.put(request.getId(), request);

      /*
       * If we haven't notified the server of our state we will let them know
       * now. In an effort to not flood ourselves we will only re-notify them
       * once we go back below the warning threshold.
       */
      if (getRequestCount() >= requestWarningLimit && !alreadyLoggedError) {
        eventListener.onWarning(this);
        alreadyLoggedError = true;
      }
      
      final XMLHttpRequest xhr = request.getRequest();
      customHeaders.iterate(new IterationCallback<String>() {
        @Override
        public void onIteration(String header, String value) {
          xhr.setRequestHeader(header, value);
        }
      });
    }

    @Override
    public void onRequestOpening(WardenXhrRequest request) {
      /*
       * We are trying to open up a new xhr request but are at the limit we will
       * kill the oldest inactive request so that we can make room
       */
      if (openXhrRequests.get(request.getId()) == null && getRequestCount() >= requestErrorLimit) {
        WardenXhrRequest oldest = getLongestIdleRequest();
        oldest.kill();
        openXhrRequests.remove(oldest.getId());

        eventListener.onEmergency(this, oldest);
      } 
    }

    @Override
    public void onRequestDone(WardenXhrRequest request) {
      if (openXhrRequests.get(request.getId()) != null) {
        openXhrRequests.remove(request.getId());
      }

      if (getRequestCount() < requestWarningLimit) {
        alreadyLoggedError = false;
      }
    }

    @Override
    public void doListRequests() {
      dumpRequestsToConsole();
    }

    @Override
    public void addCustomHeader(String header, String value) {
      customHeaders.put(header, value);
    }
  }

  /**
   * Models a warden HTTP request which effectively wraps a XMLHttpRequest
   */
  public interface WardenXhrRequest {
    /**
     * Kills the request immediately.
     */
    public void kill();

    /**
     * Retrieves the time of the last activity for this request.
     */
    public double getTime();

    /**
     * Returns the date and time of this requests last activity as a String.
     */
    public String getDateString();

    /**
     * Gets the id for this request that was assigned by the warden.
     */
    public String getId();

    /**
     * Gets the URL this request tried to open.
     */
    public String getUrl();

    /**
     * Retrieves the underlying XMLHttpRequest.
     */
    public XMLHttpRequest getRequest();
  }

  /**
   * Wraps a warden jso which is essentially an XMLHttpRequest plus a few
   * special properties.
   *
   */
  static class WardenXhrRequestImpl extends JavaScriptObject implements WardenXhrRequest {
    protected WardenXhrRequestImpl() {
    }

    public final native void kill() /*-{
      this.abort();
    }-*/;

    public final native double getTime() /*-{
      return this.wardenTime;
    }-*/;

    public final native String getDateString() /*-{
      return new Date(this.wardenTime).toString();
    }-*/;

    public final native String getId() /*-{
      return "" + this.wardenId;
    }-*/;

    public final native String getUrl() /*-{
      // Devmode optimization (Browser Channel Weirdness)
      if (typeof this.wardenUrl != "string") {
        return "Browser Channel";
      }
      return this.wardenUrl;
    }-*/;

    @Override
    public final native XMLHttpRequest getRequest() /*-{
      return this;
    }-*/;
  }

  static WardenRequestManager getInstance() {
    return wardenManager;
  }

  static void setInstance(WardenImpl manager) {
    wardenManager = manager;
  }

  /**
   * If the warden is currently enabled, it will disable the warden; otherwise,
   * this is a no-op.
   */
  public static void stopWatching() {
    wardenManager = null;
    removeWarden();
  }
  
  /**
   * Dumps a list of open requests to the console.
   */
  public static void dumpRequestsToConsole() {
    if (getInstance() != null) {
      getInstance().dumpRequestsToConsole();
    }
  }

  /**
   * Creates the warden and substitutes it for the XMLHttpRequest object.
   * Basically creates an XMLHttpRequest factory with an automatic event
   * listener for onreadystatechange and an overridden open function. This
   * proved one of the few fully working ways to override the native object.
   */
  private static native void createWarden(WardenReadyStateHandler handler) /*-{

    $wnd.XMLHttpRequest = (function(handler) {
      var requestid = 0;
      var xmlhttp = $wnd.xmlhttp = $wnd.XMLHttpRequest;

      return function() {
        var request = new xmlhttp();
        request.wardenId = requestid++;

        request.addEventListener("readystatechange", function() {
          // On Open
          if (this.readyState == 1) {
            handler.
              @com.google.collide.client.xhrmonitor.XhrWarden.WardenReadyStateHandler::onRequestOpen(Lcom/google/collide/client/xhrmonitor/XhrWarden$WardenXhrRequest;)
              (this);
          } else if (this.readyState == 3) {
            // this indicates progress so a send or a receive
            this.wardenTime = (new Date()).getTime();
          } else if (this.readyState == 4) {
            // indicates we ended due to failure or otherwise
            handler.
              @com.google.collide.client.xhrmonitor.XhrWarden.WardenReadyStateHandler::onRequestDone(Lcom/google/collide/client/xhrmonitor/XhrWarden$WardenXhrRequest;)
              (this);
          }
        }, true);

        // Override the xml http request open command
        request.xhrOpen = request.open;
        request.open = function(method, url) {
          this.wardenUrl = url;
          this.wardenTime = (new Date()).getTime();
          handler.
            @com.google.collide.client.xhrmonitor.XhrWarden.WardenReadyStateHandler::onRequestOpening(Lcom/google/collide/client/xhrmonitor/XhrWarden$WardenXhrRequest;)
            (this);
          this.xhrOpen.apply(this,arguments);
        };

        return request;
      }
    })(handler);

    // Calls the warden to list any open xhr requests
    $wnd.XMLHttpRequest.list = function() {
      handler.
        @com.google.collide.client.xhrmonitor.XhrWarden.WardenReadyStateHandler::doListRequests()();
    };

    if ($wnd.console && $wnd.console.info) {
      $wnd.console.info("The warden is watching.");
    }
  }-*/;

  private static native void removeWarden() /*-{
    $wnd.XMLHttpRequest = $wnd.xmlhttp || $wnd.XMLHttpRequest;
  }-*/;
}
