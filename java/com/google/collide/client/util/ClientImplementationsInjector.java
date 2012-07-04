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

package com.google.collide.client.util;

import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.Jso;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.client.JsoStringSet;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonIntegerMap;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.SharedLogUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.core.client.GWT;

/**
 * Injects delegates for optimized client implementations.
 */
public final class ClientImplementationsInjector {
  public static void inject() {
    SharedLogUtils.setImplementation(new SharedLogUtils.Implementation() {
      @Override
      public void markTimeline(Class<?> clazz, String label) {
        Log.markTimeline(clazz, label);
      }
      
      @Override
      public void info(Class<?> clazz, Object... objects) {
        Log.info(clazz, objects);
      }

      @Override
      public void debug(Class<?> clazz, Object... objects) {
        Log.debug(clazz, objects);
      }

      @Override
      public void error(Class<?> clazz, Object... objects) {
        Log.error(clazz, objects);
      }

      @Override
      public void warn(Class<?> clazz, Object... objects) {
        Log.warn(clazz, objects);
      }
    });

    if (GWT.isScript()) {
      JsonCollections.setImplementation(new JsonCollections.Implementation() {
        @Override
        public <T> JsonStringMap<T> createMap() {
          return JsoStringMap.create();
        }

        @Override
        public JsonStringSet createStringSet() {
          return JsoStringSet.create();
        }

        @Override
        public <T> JsonArray<T> createArray() {
          return Jso.createArray().<JsoArray<T>>cast();
        }

        @Override
        public <T> JsonIntegerMap<T> createIntegerMap() {
           return JsIntegerMap.create();
        }
      });

      /*
       * Only use the faster native JS collections if running as compiled output
       * (so, use JRE collections in dev mode)
       */
      StringUtils.setImplementation(new StringUtils.Implementation() {
        @Override
        public JsonArray<String> split(String string, String separator) {
          return ClientStringUtils.split(string, separator).<JsoArray<String>>cast();
        }
      });
    }
  }

  private ClientImplementationsInjector() {
  }
}
