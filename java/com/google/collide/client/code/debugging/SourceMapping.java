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

/**
 * Mapping between the local files in the project and remote scripts and
 * resource files on the debuggee application.
 */
interface SourceMapping {

  /**
   * @return local {@link PathUtil} path of a given JavaScript source
   */
  public PathUtil getLocalScriptPath(@Nullable OnScriptParsedResponse response);

  /**
   * @return line number in the local source corresponding to a given
   *         {@link Location}
   */
  public int getLocalSourceLineNumber(@Nullable OnScriptParsedResponse response, Location location);

  /**
   * Converts a local {@link Breakpoint} model to it's remote representation.
   *
   * @param breakpoint breakpoint model
   * @return actual breakpoint to be set in the debuggee application
   */
  public BreakpointInfo getRemoteBreakpoint(Breakpoint breakpoint);

  /**
   * @return local {@link PathUtil} path of a given resource
   */
  public PathUtil getLocalSourcePath(String resourceUri);

  /**
   * @return remote resource URI corresponding to a given local path
   */
  public String getRemoteSourceUri(PathUtil path);
}
