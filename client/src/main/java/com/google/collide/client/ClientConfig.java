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

package com.google.collide.client;

import com.google.gwt.core.client.GWT;

/**
 * A deferred-binding class to provide compile-time constant tests for whether a
 * build is a "release" or "debug" build.
 *
 */
public abstract class ClientConfig {

  /**
   * Base type for our deferred bound concrete implementations that statically
   * determine if a build should be a debug or release permutation.
   */
  public static abstract class DebugOrReleaseMode {
    abstract boolean isDebug();
  }

  /**
   * Alternative to {@link ReleaseMode}, substituted by
   * {@link GWT#create(Class)} if gwt.xml properties tell it this is a debug
   * build.
   */
  @SuppressWarnings("unused")
  private static class DebugMode extends DebugOrReleaseMode {
    @Override
    public boolean isDebug() {
      return true;
    }
  }

  /**
   * Mode for "Release" builds, possibly replaced with {@link DebugMode} if the
   * gwt.xml property tells {@link GWT#create(Class)} to do so.
   */
  @SuppressWarnings("unused")
  private static class ReleaseMode extends DebugOrReleaseMode {
    @Override
    public boolean isDebug() {
      return false;
    }
  }

  private static final DebugOrReleaseMode MODE_INSTANCE = GWT.create(DebugOrReleaseMode.class);

  public static boolean isDebugBuild() {
    return MODE_INSTANCE.isDebug();
  }
}
