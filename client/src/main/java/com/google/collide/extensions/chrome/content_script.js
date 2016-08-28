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
 * on the Web IDE frontend web page.
 *
 * Content script's work is to receive requests from the Web IDE frontend and
 * forward them to the background page for further processing, as well as to
 * dispatch the responses from the background page back to the Web IDE.
 *
 * Content script and Web IDE frontend run in isolated environments. Thus for
 * communication {@code CustomEvent} DOM events are used to send serialized
 * JSON messages.
 *
 * NOTE! There may be multiple content script injections onto a single Web IDE
 * frontend page due to extension install/uninstall, reload or update actions.
 * Thus we should preserve only one {@code CustomEvent} listener (on the most
 * recently injected content script).
 */

/**
 * Main code to be run in the Chrome extension's content script.
 * @constructor
 */
var ContentScript = function() {
  /**
   * Connection port to the background page.
   * @type {Port?}
   * @private
   */
  this.port_ = chrome.extension.connect();
  this.port_.onMessage.addListener(
      this.sendResponseToEmbeddingPage_.bind(this));
  this.port_.onDisconnect.addListener(this.onDisconnect_.bind(this));

  /**
   * Targets (or debugging sessions) initiated by this content script.
   * We track these to fire the {@code onDetach} event for every target when
   * the connection port to the background page is terminated unexpectedly.
   * @type {!Object}
   * @private
   */
  this.targets_ = {};

  /**
   * The listener function for the {@code DebuggerExtensionRequest} events.
   * @type {!Function}
   * @private
   */
  this.debuggerExtensionRequestHandler_ = function(e) {
    this.port_.postMessage({ data: e.detail });
  }.bind(this);
};


/**
 * Called when disconnected from the background page (e.g. the extension was
 * uninstalled, disabled or reloaded).
 * @private
 */
ContentScript.prototype.onDisconnect_ = function() {
  window.removeEventListener(
      'DebuggerExtensionRequest', this.debuggerExtensionRequestHandler_, true);
  // Send onDetach event response for all targets.
  for (var target in this.targets_) {
    this.sendResponseToEmbeddingPage_(
        createCustomEventResponse(null, CustomEvents.ON_DETACH, target));
  }
  this.notifyDebuggerExtensionInstalled_(2000, false);
  this.port_ = null;
};


/**
 * Sends a response from this content script further to the embedding page.
 * @param {Object} response The response to send.
 * @private
 */
ContentScript.prototype.sendResponseToEmbeddingPage_ = function(response) {
  if (response.target && !response.error) {
    if (response.method === CustomEvents.WINDOW_OPEN) {
      this.targets_[response.target] = true;
    } else if (response.method === CustomEvents.WINDOW_CLOSE) {
      delete this.targets_[response.target];
    }
  }

  var customEvent = document.createEvent('CustomEvent');
  customEvent.initCustomEvent('DebuggerExtensionResponse', true, true,
      JSON.stringify(response));
  window.dispatchEvent(customEvent);
};


/**
 * This will set a boolean flag in the context of the Web IDE frontend to
 * indicate whether this extension is installed or not.
 * @param {number} timeBudget Available time budget to delay the method's
 *     execution, if needed.
 * @param {boolean} isInstalled Whether the extension is installed.
 * @private
 */
ContentScript.prototype.notifyDebuggerExtensionInstalled_ = function(
    timeBudget, isInstalled) {
  try {
    var scriptElement = document.createElement('script');
    scriptElement.type = 'text/javascript';
    scriptElement.appendChild(document.createTextNode(
        'window.__DebuggerExtensionInstalled=' +
            (isInstalled ? 'true' : 'false')));
    var parent = document.documentElement;
    parent.appendChild(scriptElement);
    parent.removeChild(scriptElement);
    this.sendResponseToEmbeddingPage_(createCustomEventResponse(
        null, CustomEvents.ON_EXTENSION_INSTALLED_CHANGED, null));
  } catch (e) {
    if (timeBudget > 0) {
      setTimeout(this.notifyDebuggerExtensionInstalled_.bind(this,
          timeBudget - 100, isInstalled), 100);
    }
  }
};


/**
 * Runs the script.
 */
ContentScript.prototype.run = function() {
  this.notifyDebuggerExtensionInstalled_(2000, true);
  window.addEventListener(
      'DebuggerExtensionRequest', this.debuggerExtensionRequestHandler_, true);
};


// The "entry point".
new ContentScript().run();
