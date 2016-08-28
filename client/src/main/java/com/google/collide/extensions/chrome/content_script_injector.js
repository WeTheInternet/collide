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
 * @fileoverview This file provides a utility to programmatically inject content
 * scripts based on manifest details of a Chrome extension. This is useful,
 * for example, to handle the "first installation" case when a Chrome extension
 * gets installed into a browser and it needs to inject its content scripts onto
 * already opened tabs right away.
 *
 * This class provides a simple API to inject all content scripts onto a given
 * tab, that would be otherwise injected by the browser itself, if the extension
 * had been installed before opening the given tab.
 *
 * NOTE! The Chrome extension should have the corresponding cross-domain
 * permissions in the manifest.json file to allow programmatic injections of
 * the content scripts.
 *
 * NOTE! You will probably also need to handle the case of multiple content
 * script injections in your extension (due to programmatic injections and
 * injections via the standard mechanism).
 */

/**
 * Injector of the Chrome extension's content scripts.
 * @constructor
 */
var ContentScriptInjector = function() {
  /**
   * Parsed details about the content scripts.
   * @type {!Array.<Object>}
   * @private
   */
  this.details_ = [];
  this.initDetails_();
};


/**
 * Initializes content scripts details.
 * @private
 */
ContentScriptInjector.prototype.initDetails_ = function() {
  var details;
  try {
    details = chrome.app.getDetails();
  } catch (e) {
    // Ignore.
  }
  details = details || {};

  var contentScriptDetails = details['content_scripts'] || [];
  for (var i = 0, info; info = contentScriptDetails[i]; ++i) {
    this.details_.push(this.parseContentScriptDetails_(info));
  }
};

/**
 * Parses a "stringified" content script detail into an internal format with
 * the {@code RegExp} instances instead of its string representations.
 * @param {!Object} info A "stringified" content script detail info.
 * @return {!Object} Parsed result.
 * @private
 */
ContentScriptInjector.prototype.parseContentScriptDetails_ = function(info) {
  var result = {};
  result['js'] = info['js'];

  var REGEX_PROPERTIES_INFO = {
    'matches': this.parseMatchRegex_,
    'exclude_matches': this.parseMatchRegex_,
    'include_globs': this.parseGlobRegex_,
    'exclude_globs': this.parseGlobRegex_
  };

  for (var key in REGEX_PROPERTIES_INFO) {
    if (!info[key]) {
      continue;
    }
    result[key] = [];
    var parseMethod = REGEX_PROPERTIES_INFO[key];
    for (var i = 0, len = info[key].length; i < len; ++i) {
      var parsedResult = parseMethod.call(this, info[key][i]);
      if (parsedResult) {
        result[key].push(parsedResult);
      }
    }
  }

  if (!result['include_globs']) {
    // No include_globs means no additional restrictions to be applied.
    // For simplicity, we put a regexp that matches any string in this case.
    result['include_globs'] = [/.*/];
  }

  return result;
};


/**
 * Parses the "match" pattern. For more info see:
 * http://code.google.com/chrome/extensions/match_patterns.html
 * @param {string} str String representation of a regular expression.
 * @return {RegExp} Parsed regular expression.
 * @private
 */
ContentScriptInjector.prototype.parseMatchRegex_ = function(str) {
  if (str == '<all_urls>') {
    return /^(https?|ftp|file):\/\/.+$/i;
  }

  // Here's the basic syntax:
  //   <url-pattern> := <scheme>://<host><path>
  //   <scheme> := '*' | 'http' | 'https' | 'file' | 'ftp'
  //   <host> := '*' | '*.' <any char except '/' and '*'>+
  //   <path> := '/' <any chars>
  var urlPatternRegexp = /^([^:\/]+):\/\/([^\/]*)(\/.*)?$/i;
  var result = urlPatternRegexp.exec(str);
  if (!result) {
    return null;
  }

  var scheme = result[1];
  var host = result[2];
  var path = result[3];

  if (scheme == '*') {
    // If the scheme is *, then it matches either http or https.
    scheme = 'https?';
  }

  if (host == '*') {
    // If the host is just *, then it matches any host.
    host = '[^/]+';
  } else if (host.indexOf('*.') === 0) {
    // If the host is *.hostname, then it matches the specified host or any of
    // its subdomains.
    host = '([^/]+\\.)?' + this.escapeRegexCharacters_(host.substr(2));
  } else {
    host = this.escapeRegexCharacters_(host);
  }

  // Add a regexp for a port number inlined into the URL.
  host += '(:\\d+)?';

  // In the path section, each '*' matches 0 or more characters.
  path = this.escapeRegexCharacters_(path).replace(/\\\*/g, '.*');

  try {
    var regexString = '^' + scheme + '://' + host + path + '$';
    return new RegExp(regexString, 'i');
  } catch (e) {
    return null;
  }
};


/**
 * Parses the "glob" pattern. For more info see:
 * http://code.google.com/chrome/extensions/content_scripts.html#match-patterns-globs
 *
 * <p>Acceptable glob strings are URLs that may contain "wildcard" asterisks
 * and question marks. The asterisk (*) matches any string of any length
 * (including the empty string); the question mark (?) matches any single
 * character.
 *
 * @param {string} str String representation of a regular expression.
 * @return {RegExp} Parsed regular expression.
 * @private
 */
ContentScriptInjector.prototype.parseGlobRegex_ = function(str) {
  var escapedString = this.escapeRegexCharacters_(str)
      .replace(/\\\*/g, '.*')
      .replace(/\\\?/g, '.');
  try {
    var regexString = '^' + escapedString + '$';
    return new RegExp(regexString, 'i');
  } catch (e) {
    return null;
  }
};


/**
 * Escapes all special regex characters (.*+?|()[]{}\) from a string.
 * @param {string} str The string to escape.
 * @return {string} Escaped string suitable to pass into a RegExp constructor.
 * @private
 */
ContentScriptInjector.prototype.escapeRegexCharacters_ = function(str) {
  return str.replace(/[.*+?|()\[\]\{\}\\]/g, '\\$&');
};


/**
 * Checks whether a given string matches any given regular expression.
 * @param {Array.<RegExp>?} regexes Array of {@code RegExp}s to check against.
 * @param {string} str The pattern to check.
 * @return {boolean} True, if there is any {@code RegExp} matching the pattern.
 * @private
 */
ContentScriptInjector.prototype.matchesAnyRegexp_ = function(regexes, str) {
  if (!regexes) {
    return false;
  }
  for (var i = 0, regex; regex = regexes[i]; ++i) {
    if (regex.test(str)) {
      return true;
    }
  }
  return false;
};


/**
 * Injects given content scripts onto a given tab.
 * @param {number} tabId ID of the tab to inject the content scripts.
 * @param {Array.<string>} scripts Array of content script names to inject.
 * @private
 */
ContentScriptInjector.prototype.injectContentScripts_ = function(
    tabId, scripts) {
  for (var i = 0, script; script = scripts[i]; ++i) {
    chrome.tabs.executeScript(tabId, {file: script});
  }
};


/**
 * Injects all content scripts (if any) according to the manifest.json rules
 * onto a given tab.
 * @param {number} tabId ID of the tab to inject the content scripts.
 * @param {string} tabUrl URL of the tab to check the URL patterns.
 * @return {boolean} True, if some content scripts were injected.
 */
ContentScriptInjector.prototype.injectAllContentScripts = function(
    tabId, tabUrl) {
  var result = false;

  // The content script will be injected into a page if its URL matches any
  // matches pattern and any include_globs pattern, as long as the URL doesn't
  // also match an exclude_matches or exclude_globs pattern.
  for (var i = 0, detail; detail = this.details_[i]; ++i) {
    if (this.matchesAnyRegexp_(detail['matches'], tabUrl) &&
        this.matchesAnyRegexp_(detail['include_globs'], tabUrl) &&
        !this.matchesAnyRegexp_(detail['exclude_matches'], tabUrl) &&
        !this.matchesAnyRegexp_(detail['exclude_globs'], tabUrl)) {
      this.injectContentScripts_(tabId, detail['js']);
      result = true;
    }
  }

  return result;
};
