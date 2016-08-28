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
 * @fileoverview This file contains the shared code between the content scripts
 * and background page.
 */

/**
 * Custom event names.
 * @enum {string}
 */
var CustomEvents = {
  WINDOW_OPEN: 'window.open',
  WINDOW_CLOSE: 'window.close',
  ON_ATTACH: 'onAttach',
  ON_DETACH: 'onDetach',
  ON_GLOBAL_OBJECT_CHANGED: 'onGlobalObjectChanged',
  ON_EXTENSION_INSTALLED_CHANGED: 'onExtensionInstalledChanged'
};


/**
 * Creates the response to be sent via a custom event.
 * @param {string?} id ID of the response (should be the same as in the
 *     corresponding request).
 * @param {string} method Message or event name.
 * @param {string} target Target to send the message to.
 * @param {Object} opt_request Request data, if any.
 * @param {Object} opt_result Response data, if any.
 * @param {string?} opt_error Not empty if an error occurred.
 * @return {!Object} New object.
 */
function createCustomEventResponse(id, method, target, opt_request, opt_result,
    opt_error) {
  var response = {
    id: id,
    method: method,
    target: target
  };
  if (opt_request) {
    response.request = opt_request;
  }
  if (opt_error) {
    response.error = opt_error;
  } else {
    response.result = opt_result || {};
  }
  return response;
}
