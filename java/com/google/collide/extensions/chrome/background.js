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

/**
 * @fileoverview This file contains the JavaScript code that will be executed
 * in the background page of the extension.
 *
 * Background page's work is to forward requests from (many) Web IDE frontends
 * to the corresponding debuggee tabs and vise versa. It communicates with the
 * content scripts from one side, and with the DevTools Debugger instances
 * attached to the debuggee tabs from another side.
 */

/**
 * Handles a port connection from a content script.
 * @param {Port} port The connection port.
 * @constructor
 */
var PortHandler = function(port) {
  /**
   * @type {Port}
   * @private
   */
  this.port_ = port;
  port.onMessage.addListener(this.onMessage_.bind(this));
  port.onDisconnect.addListener(this.onDisconnect_.bind(this));

  /**
   * The host tab from which the connection was initiated.
   * @type {Tab}
   * @private
   */
  this.hostTab_ = this.port_.sender.tab;

  /**
   * ID of the window the host tab is contained within.
   * @type {number}
   * @private
   */
  this.hostWindowId_ = this.hostTab_.windowId;

  /**
   * Holds info about the opened tabs which are being debugged.
   * This is a String-to-Object map between the target and an object with the
   * following fields:
   *   - {number} tabId ID of the debuggee tab
   *   - {boolean} debuggerAttached Whether the debugger is attached to the tab
   * @type {!Object}
   * @private
   */
  this.debuggeeTabs_ = {};

  /**
   * Deferred messages to be processed after the debugger attaches to the
   * tab being debugged.
   * This is a String-to-Array map between the target and an array of the raw
   * message strings received from the content scripts to be processed later.
   * @type {!Object}
   * @private
   */
  this.messageQueues_ = {};

  /**
   * Required debugging protocol version. We can only attach to the debugger
   * with matching major version and greater or equal minor version.
   * @type {string}
   * @private
   */
  this.debugProtocolVersion_ = '1.0';
};


/**
 * Whether to open the debuggee app in a popup window instead of a normal one.
 * @type {boolean}
 */
PortHandler.OPEN_POPUP_DEBUGGEE_WINDOW = false;


/**
 * @return ID of the host tab from which the connection was initiated.
 */
PortHandler.prototype.getHostTabId = function() {
  return this.hostTab_.id;
};


/**
 * Checks a given name of a command or event against a white-list.
 * @param {string} name The name to check.
 * @return {boolean} True if it passes the white-list check.
 * @private
 */
PortHandler.prototype.isMethodOrEventAllowed_ = function(name) {
  return /^(Debugger|Runtime|CSS|DOM|Console)\.\w+$/.test(name);
};


/**
 * Checks whether a given method or event changes the global object (global
 * scope).
 * @param {string} name The name to check.
 * @return {boolean} True if it will change the global object.
 * @private
 */
PortHandler.prototype.isGlobalObjectChangeMethod_ = function(name) {
  return name === 'Page.frameNavigated';
};


/**
 * Checks whether a given event resets the debugger.
 * @param {string} name The name to check.
 * @return {boolean} True if the debugger has just been reset.
 * @private
 */
PortHandler.prototype.isDebuggerResetEvent_ = function(name) {
  return name === 'Inspector.reset';
};


/**
 * Parses a message received from a content script.
 * @param {Object} msg The message to parse.
 * @return {Object} The parsed message, or {@code null} if failed to parse.
 * @private
 */
PortHandler.prototype.parseMessage_ = function(msg) {
  if (msg && msg.data) {
    try {
      var parsedMessage = JSON.parse(msg.data);
      if (parsedMessage && typeof parsedMessage === 'object') {
        return parsedMessage;
      }
    } catch (e) {
      console.error('Failed to parse JSON message: ' + msg.data, e);
    }
  }
  return null;
};


/**
 * Handles messages from content scripts.
 * @param {Object} msg The message to process.
 * @private
 */
PortHandler.prototype.onMessage_ = function(msg) {
  console.log('Received message from the content script: ' + (msg && msg.data));

  var parsedMessage = this.parseMessage_(msg);
  if (!parsedMessage) {
    console.error('Failed to process a message: ', msg);
    return;
  }

  var msgId = parsedMessage.id;
  var method = parsedMessage.method;
  var target = parsedMessage.target;
  var params = parsedMessage.params;

  if (method === CustomEvents.WINDOW_OPEN) {
    this.openDebuggeeWindow_(parsedMessage);
  } else if (method === CustomEvents.WINDOW_CLOSE) {
    this.maybeCloseDebuggeeWindow_(parsedMessage);
  } else {
    if (!target) {
      this.sendResponse_(msgId, method, target, params, null,
          'Target is missing or empty');
      return;
    }
    var tabInfo = this.debuggeeTabs_[target];
    if (!tabInfo) {
      var messageQueue = this.messageQueues_[target];
      if (messageQueue) {
        // The window is being opened, just wait.
        messageQueue.push(msg);
      } else {
        this.sendResponse_(msgId, method, target, params, null,
            'No such target: ' + target);
      }
      return;
    }
    if (!this.isMethodOrEventAllowed_(method)) {
      this.sendResponse_(msgId, method, target, params, null,
          'Unsupported method: ' + method);
      return;
    }

    if (!tabInfo.debuggerAttached) {
      this.sendResponse_(msgId, method, target, params, null,
          'Debugger is not attached');
      return;
    }

    chrome.debugger.sendCommand(
        {tabId: tabInfo.tabId},
        method,
        params,
        function(result) {
          var error = chrome.extension.lastError &&
              chrome.extension.lastError.message;
          this.sendResponse_(msgId, method, target, params, result,
              error);
        }.bind(this));
  }
};


/**
 * Called if a content script disconnects (e.g. the web page it was running
 * upon was closed), but not if the extension itself was uninstalled, disabled
 * or reloaded.
 * @private
 */
PortHandler.prototype.onDisconnect_ = function() {
  // Close all open "debuggee" tabs.
  for (var target in this.debuggeeTabs_) {
    chrome.tabs.remove(this.debuggeeTabs_[target].tabId);
  }
  this.debuggeeTabs_ = {};
  this.messageQueues_ = {};
  delete this.port_;
  delete this.hostTab_;
};


/**
 * Attaches the debugger to the debuggee tab, if needed.
 * @param {string} target The target to attach the debugger to.
 * @private
 */
PortHandler.prototype.maybeAttachDebugger_ = function(target) {
  var tabInfo = this.debuggeeTabs_[target];
  if (tabInfo.debuggerAttached) {
    return;
  }
  tabInfo.debuggerAttached = true;
  console.log('Attaching debugger on target ' + target);
  chrome.debugger.attach(
      {tabId: tabInfo.tabId},
      this.debugProtocolVersion_,
      function() {
        if (chrome.extension.lastError) {
          console.error('Debugger attach failed: ' +
              chrome.extension.lastError.message);
          delete tabInfo.debuggerAttached;
          this.sendResponse_(null, CustomEvents.ON_DETACH, target);
        } else {
          this.sendResponse_(null, CustomEvents.ON_ATTACH, target);
        }
      }.bind(this));
};


/**
 * Sends a response to the content script.
 * @param {string?} id ID of the response (should be the same as in the
 *     corresponding request).
 * @param {string} method Message or event name.
 * @param {string} target Target to send the message to.
 * @param {Object} request Request data, if any.
 * @param {Object} result Response data, if any.
 * @param {string?} error Not empty if an error occurred.
 * @private
 */
PortHandler.prototype.sendResponse_ = function(
    id, method, target, request, result, error) {
  var response = createCustomEventResponse(id, method, target, request,
      result, error);
  if (error) {
    console.error('===> Sending response ' + response.method +
        ' with ERROR: ', response);
  } else {
    console.log('===> Sending response ' + response.method, response);
  }
  this.port_.postMessage(response);
};


/**
 * Returns an absolute URL relative to the host tab's base URL.
 * @param {string} url An absolute or relative URL to process.
 * @return {string} The absolute URL.
 * @private
 */
PortHandler.prototype.getAbsoluteUrl_ = function(url) {
  if (/^\w+:\/\//.test(url)) {
    return url; // Already an absolute URL.
  }
  var baseUrl = this.hostTab_.url;
  if (/^[\/\\]/.test(url)) {
    // Relative from the host address.
    var hostRegex = /^\w+:\/\/[^\/\\]+/;
    var res = hostRegex.exec(baseUrl);
    if (res != null) {
      return res[0] + url;
    }
  }
  // Relative from the base URL.
  return baseUrl + '/' + url;
};


/**
 * Opens a new debuggee window, or puts in front an existing one.
 * @param {Object} parsedMessage The parsed message from the content script.
 * @private
 */
PortHandler.prototype.openDebuggeeWindow_ = function(parsedMessage) {
  var target = parsedMessage.target;
  var url = parsedMessage.params && parsedMessage.params.url;

  console.log('openDebuggeeWindow_: url=' + url + ', target=' + target);
  if (!url || !target) {
    this.sendResponse_(parsedMessage.id, parsedMessage.method, target,
        parsedMessage.params, null,
        'Failed to create a window with empty URL or target');
    return;
  }

  url = this.getAbsoluteUrl_(url);

  if (this.debuggeeTabs_[target]) {
    this.updateWindow_(url, target);
    this.sendResponse_(parsedMessage.id, parsedMessage.method, target,
        parsedMessage.params);
    return;
  }

  this.messageQueues_[target] = [];

  chrome.windows.get(this.hostWindowId_, function(hostWin) {
    var positions = this.calculateWindowPosition_(hostWin);
    var createData = {
      left: positions.left,
      top: positions.top,
      width: positions.width,
      height: positions.height,
      url: url,
      type: PortHandler.OPEN_POPUP_DEBUGGEE_WINDOW ? 'popup': 'normal',
      focused: true,
      incognito: this.hostTab_.incognito
    };

    chrome.windows.create(createData, function(win) {
      var tab = win.tabs && win.tabs[0];
      if (!tab) {
        this.sendResponse_(parsedMessage.id, parsedMessage.method, target,
            parsedMessage.params, null,
            'Internal Error: Failed to create a window');
        // Send error responses for any queued messages.
        this.applyMessageQueue_(target);
        return;
      }
      this.debuggeeTabs_[target] = {
        tabId: tab.id
      };
      this.maybeAttachDebugger_(target);
      this.applyMessageQueue_(target);
      this.sendResponse_(parsedMessage.id, parsedMessage.method, target,
          parsedMessage.params);
    }.bind(this));
  }.bind(this));
};


/**
 * Calculates an optimal position for a new window, so that it should pop up
 * nicely "nearby" the host window, without obscuring it if possible.
 * @param {Window} hostWin Chrome host window to place the new window nearby.
 * @return {!Object} Position of the new window.
 * @private
 */
PortHandler.prototype.calculateWindowPosition_ = function(hostWin) {
  // Minimum width for the target window, in pixels.
  var MIN_ACCEPTABLE_WIDTH = 500;
  // Estimation for the window border width, in pixels.
  var BORDER_WIDTH = 2;
  // Estimation for the window header's height, in pixels.
  var WINDOW_HEADER_HEIGHT = PortHandler.OPEN_POPUP_DEBUGGEE_WINDOW ? 32 : 0;

  var hostWinRight = hostWin.left + hostWin.width;
  var availWidthAtLeft = hostWin.left;
  var availWidthAtRight = screen.availWidth - hostWinRight;

  // Try to position the new window at the right if there is enough place, or if
  // there is no place at the right *and* at the left (choose right as default).
  if (availWidthAtRight >= MIN_ACCEPTABLE_WIDTH
      || availWidthAtLeft < MIN_ACCEPTABLE_WIDTH) {
    var width = Math.min(hostWin.width, availWidthAtRight);
    var left = hostWinRight;
    if (width < MIN_ACCEPTABLE_WIDTH) {
      width = MIN_ACCEPTABLE_WIDTH;
      left = screen.availWidth - width - BORDER_WIDTH * 2;
    }
  } else {
    var width = Math.min(hostWin.width, availWidthAtLeft);
    var left = Math.max(0, hostWin.left - width - BORDER_WIDTH * 2);
  }

  var top = hostWin.top;
  var height = hostWin.height - WINDOW_HEADER_HEIGHT;

  return {
    left: left,
    top: top,
    width: width,
    height: height
  };
};


/**
 * Closes a debuggee window, if it is open.
 * @param {Object} parsedMessage The parsed message from the content script.
 * @private
 */
PortHandler.prototype.maybeCloseDebuggeeWindow_ = function(parsedMessage) {
  var target = parsedMessage.target;

  console.log('maybeCloseDebuggeeWindow_: target=' + target);
  if (!target || !this.debuggeeTabs_[target]) {
    this.sendResponse_(parsedMessage.id, parsedMessage.method, target,
        parsedMessage.params, null, 'No debuggee window to close');
    return;
  }

  // This will trigger the chrome.tabs.onRemoved event, where we will do the
  // corresponding cleanup.
  chrome.tabs.remove(this.debuggeeTabs_[target].tabId);
};


/**
 * Brings to front an existing debuggee tab, and sets a new URL to it.
 * @param {string} url The new URL to set.
 * @param {string} target The target corresponding to the debuggee tab.
 * @private
 */
PortHandler.prototype.updateWindow_ = function(url, target) {
  var tabInfo = this.debuggeeTabs_[target];
  var tabUpdate = {
    url: url,
    selected: true
  };
  var windowUpdate = {
    focused: true
  };
  chrome.tabs.update(tabInfo.tabId, tabUpdate, function(tab) {
    this.maybeAttachDebugger_(target);
    chrome.windows.update(tab.windowId, windowUpdate);
  }.bind(this));
};


/**
 * Applies the deferred messages (if any) for a given target.
 * @param {string} target The target to apply the messages for.
 * @private
 */
PortHandler.prototype.applyMessageQueue_ = function(target) {
  var messageQueue = this.messageQueues_[target] || [];
  delete this.messageQueues_[target];
  for (var i = 0; i < messageQueue.length; ++i) {
    this.onMessage_(messageQueue[i]);
  }
};


/**
 * Gets a target name corresponding to a given tab ID.
 * @param {number} tabId The tab ID to find the target for.
 * @return {string?} The corresponding target name, or {@code null}
 *     if not found.
 * @private
 */
PortHandler.prototype.getTargetByTabId_ = function(tabId) {
  for (var target in this.debuggeeTabs_) {
    if (this.debuggeeTabs_[target].tabId === tabId) {
      return target;
    }
  }
  return null;
};


/**
 * Should be called when a tab was closed.
 * @param {number} tabId The ID of the closed tab.
 * @return {boolean} True if the tab was found.
 */
PortHandler.prototype.handleTabRemoved = function(tabId) {
  var target = this.getTargetByTabId_(tabId);
  if (!target) {
    return false;
  }
  delete this.debuggeeTabs_[target];
  this.sendResponse_(null, CustomEvents.WINDOW_CLOSE, target);
  return true;
};


/**
 * Should be called when a tab was updated.
 * @param {Tab} tab The updated tab.
 * @return {boolean} True if the tab was found.
 */
PortHandler.prototype.handleTabUpdated = function(tab) {
  if (this.hostTab_.id !== tab.id) {
    return false;
  }
  this.hostTab_ = tab;
  this.hostWindowId_ = tab.windowId;
  return true;
};


/**
 * Should be called when a tab was attached to a window.
 * @param {number} tabId ID of the attached tab.
 * @param {number} windowId ID of the window the tab was attached to.
 * @return {boolean} True if the tab was found.
 */
PortHandler.prototype.handleHostWindowChanged = function(tabId, windowId) {
  if (this.hostTab_.id !== tabId) {
    return false;
  }
  this.hostWindowId_ = windowId;
  return true;
};


/**
 * Should be called when the debugger was detached from a tab.
 * @param {number} tabId The ID of the tab the debugger was detached from.
 * @return {boolean} True if the tab was found.
 */
PortHandler.prototype.handleDebuggerDetached = function(tabId) {
  var target = this.getTargetByTabId_(tabId);
  if (!target) {
    return false;
  }
  delete this.debuggeeTabs_[target].debuggerAttached;
  this.sendResponse_(null, CustomEvents.ON_DETACH, target);
  return true;
};


/**
 * Should be called when a debugger event is received for a given tab.
 * @param {number} tabId The ID of the tab the debugger event was received from.
 * @param {string} method Event name.
 * @param {Object} result Event data, if any.
 * @return {boolean} True if the tab was found.
 */
PortHandler.prototype.handleDebuggerEvent = function(tabId, method, result) {
  var target = this.getTargetByTabId_(tabId);
  if (!target) {
    return false;
  }
  if (this.isGlobalObjectChangeMethod_(method)) {
    this.sendResponse_(null, CustomEvents.ON_GLOBAL_OBJECT_CHANGED, target);
  }
  if (this.isDebuggerResetEvent_(method)) {
    // TODO: Probably this should be done in the DevTools backend.
    this.sendResponse_(null, 'Debugger.resumed', target);
  }
  if (this.isMethodOrEventAllowed_(method)) {
    this.sendResponse_(null, method, target, null, result, null);
  } else {
    console.log('Ignoring debugger event: ' + method);
  }
  return true;
};

////////////////////////////////////////////////////////////////////////////////
// Background main class.

/**
 * Main code to be run in the Chrome extension's background page.
 * Listens for connections from the content scripts, and creates
 * a {@code PortHandler} to handle each connection.
 * @constructor
 */
var Background = function() {
  /**
   * Port handlers.
   * @type {!Array.<PortHandler>}
   * @private
   */
  this.portHandlers_ = [];
};


/**
 * Runs the script.
 */
Background.prototype.run = function() {
  var injector = new ContentScriptInjector();

  // First try to inject the content scripts on Web IDE pages, if any.
  chrome.windows.getAll({ populate: true }, function(windows) {
    for (var i = 0, win; win = windows[i]; ++i) {
      var tabs = win.tabs;
      for (var j = 0, tab; tab = tabs[j]; ++j) {
        injector.injectAllContentScripts(tab.id, tab.url);
      }
    }

    this.setupListeners_();
  }.bind(this));
};


/**
 * Sets up all internal listeners.
 * @private
 */
Background.prototype.setupListeners_ = function() {
  chrome.extension.onConnect.addListener(function(port) {
    // Accept only one port connection from a content script in case of multiple
    // content script injections (e.g. on browser startup).
    var hostTabId = port.sender.tab.id;
    for (var i = 0, portHandler; portHandler = this.portHandlers_[i]; ++i) {
      if (portHandler.getHostTabId() == hostTabId) {
        return;
      }
    }

    console.log('Connected to port: ', port);
    var handler = new PortHandler(port);
    this.portHandlers_.push(handler);
    port.onDisconnect.addListener(this.removeHandler_.bind(this, handler));
  }.bind(this));

  chrome.tabs.onRemoved.addListener(function(tabId) {
    for (var i = 0; i < this.portHandlers_.length; ++i) {
      if (this.portHandlers_[i].handleTabRemoved(tabId)) {
        break;
      }
    }
  }.bind(this));

  chrome.tabs.onUpdated.addListener(function(tabId, changeInfo, tab) {
    for (var i = 0; i < this.portHandlers_.length; ++i) {
      if (this.portHandlers_[i].handleTabUpdated(tab)) {
        break;
      }
    }
  }.bind(this));

  chrome.tabs.onAttached.addListener(function(tabId, attachInfo) {
    for (var i = 0; i < this.portHandlers_.length; ++i) {
      if (this.portHandlers_[i].handleHostWindowChanged(
          tabId, attachInfo['newWindowId'])) {
        break;
      }
    }
  }.bind(this));

  chrome.debugger.onDetach.addListener(function(source) {
    var tabId = source.tabId;
    for (var i = 0; i < this.portHandlers_.length; ++i) {
      if (this.portHandlers_[i].handleDebuggerDetached(tabId)) {
        break;
      }
    }
  }.bind(this));

  chrome.debugger.onEvent.addListener(function(
      source, method, params) {
    var tabId = source.tabId;
    for (var i = 0; i < this.portHandlers_.length; ++i) {
      if (this.portHandlers_[i].handleDebuggerEvent(
          tabId, method, params)) {
        break;
      }
    }
  }.bind(this));
};


/**
 * Removes a port handler from the internal collection.
 * @param {PortHandler} handler The handler to remove.
 * @private
 */
Background.prototype.removeHandler_ = function(handler) {
  for (var i = 0; i < this.portHandlers_.length; ++i) {
    if (this.portHandlers_[i] === handler) {
      this.portHandlers_.splice(i, 1);
      break;
    }
  }
};


// The "entry point".
new Background().run();
