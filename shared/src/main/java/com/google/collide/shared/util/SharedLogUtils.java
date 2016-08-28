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

package com.google.collide.shared.util;

/**
 * Utility methods for logging from shared code.
 */
public class SharedLogUtils {

  public interface Implementation {
    void markTimeline(Class<?> clazz, String label);

    void info(Class<?> clazz, Object... objects);
    
    void warn(Class<?> clazz, Object... objects);

    void error(Class<?> clazz, Object... objects);

    void debug(Class<?> clazz, Object... objects);
  }

  private static class NoopImplementation implements Implementation {
    @Override
    public void markTimeline(Class<?> clazz, String label) {
    }
    
    @Override
    public void info(Class<?> clazz, Object... objects) {
    }

    @Override
    public void debug(Class<?> clazz, Object... objects) {
    }

    @Override
    public void error(Class<?> clazz, Object... objects) {
    }

    @Override
    public void warn(Class<?> clazz, Object... objects) {
    }
  }

  private static Implementation implementation = new NoopImplementation();

  public static void setImplementation(Implementation implementation) {
    SharedLogUtils.implementation = implementation;
  }

  public static void markTimeline(Class<?> clazz, String label) {
    implementation.markTimeline(clazz, label);
  }
  
  public static void info(Class<?> clazz, Object... objects) {
    implementation.info(clazz, objects);
  }
  
  public static void error(Class<?> clazz, Object... objects) {
    implementation.error(clazz, objects);
  }

  public static void debug(Class<?> clazz, Object... objects) {
    implementation.debug(clazz, objects);
  }

  public static void warn(Class<?> clazz, Object... objects) {
    implementation.warn(clazz, objects);
  }
}
