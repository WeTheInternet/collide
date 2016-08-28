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

package com.google.collide.client.code.debugging;

import javax.annotation.Nullable;

import com.google.collide.client.code.debugging.DebuggerApiTypes.BreakpointInfo;
import com.google.collide.client.code.debugging.DebuggerApiTypes.Location;
import com.google.collide.client.code.debugging.DebuggerApiTypes.OnScriptParsedResponse;
import com.google.collide.client.util.PathUtil;
import com.google.collide.shared.util.RegExpUtils;
import com.google.common.base.Preconditions;

/**
 * Identity source mapping to be used for static files.
 *
 */
public class StaticSourceMapping implements SourceMapping {

  private final String resourceBaseUri;

  public static StaticSourceMapping create(String baseUri) {
    Preconditions.checkNotNull(baseUri, "Base URI is NULL!");
    return new StaticSourceMapping(baseUri);
  }

  private StaticSourceMapping(String resourceBaseUri) {
    this.resourceBaseUri = ensureNoTrailingSlash(resourceBaseUri).toLowerCase();
  }

  @Override
  public PathUtil getLocalScriptPath(@Nullable OnScriptParsedResponse response) {
    if (response == null) {
      return null;
    }
    return getLocalSourcePath(response.getUrl());
  }

  @Override
  public int getLocalSourceLineNumber(@Nullable OnScriptParsedResponse response,
      Location location) {
    return location.getLineNumber();
  }

  @Override
  public BreakpointInfo getRemoteBreakpoint(Breakpoint breakpoint) {
    String url = getRemoteSourceUri(breakpoint.getPath());
    // We allow arbitrary query and anchor parts in the breakpoint URLs for
    // static resources.
    String urlRegex = "^" + RegExpUtils.escape(url) + "([?#].*)?$";
    return new BreakpointInfoImpl(urlRegex,
        breakpoint.getLineNumber(), 0, breakpoint.getCondition());
  }

  @Override
  public PathUtil getLocalSourcePath(String resourceUri) {
    Preconditions.checkNotNull(resourceBaseUri, "Base URI is NULL! Impossible?!");

    // TODO: stopgap to get more
    // stable build to rehearse his demo with. Suggest tracking down actual
    // source of why resourceBaseUri could be null.
    if (resourceUri == null || resourceBaseUri == null) {
      return null;
    }
    if (resourceUri.toLowerCase().startsWith(resourceBaseUri)) {
      String relativePath = resourceUri.substring(resourceBaseUri.length());

      // We drop any query and anchor part, since we assume this is a static
      // resource.
      int pos = relativePath.indexOf('?');
      if (pos != -1) {
        relativePath = relativePath.substring(0, pos);
      }
      pos = relativePath.indexOf('#');
      if (pos != -1) {
        relativePath = relativePath.substring(0, pos);
      }

      return new PathUtil(relativePath);
    }
    return null;
  }

  @Override
  public String getRemoteSourceUri(PathUtil path) {
    return resourceBaseUri + path.getPathString();
  }

  private static String ensureNoTrailingSlash(String path) {
    while (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }

  private static class BreakpointInfoImpl implements BreakpointInfo {
    private final String urlRegex;
    private final int lineNumber;
    private final int columnNumber;
    private final String condition;

    private BreakpointInfoImpl(
        String urlRegex, int lineNumber, int columnNumber, String condition) {
      this.urlRegex = urlRegex;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
      this.condition = condition;
    }

    @Override
    public String getUrl() {
      return null; // urlRegex will be used instead.
    }

    @Override
    public String getUrlRegex() {
      return urlRegex;
    }

    @Override
    public int getLineNumber() {
      return lineNumber;
    }

    @Override
    public int getColumnNumber() {
      return columnNumber;
    }

    @Override
    public String getCondition() {
      return condition;
    }
  }
}
