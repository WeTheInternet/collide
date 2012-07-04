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

package com.google.collide.client.util.logging;

import com.google.collide.client.util.ExceptionUtils;
import com.google.collide.client.util.logging.LogConfig.LogLevel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Simple Logging class that logs to the browser's console and to the DevMode
 * console (if you are in DevMode).
 *
 *  So long as generating the parameters to pass to the logging methods is free
 * of side effects, all Logging code should compile out of your application if
 * logging is disabled.
 */
public class Log {

  public static void debug(Class<?> clazz, Object... args) {
    if (LogConfig.isLoggingEnabled()) {
      // DEBUG is the lowest log level, but we use <= for consistency, and in
      // case we ever decide to introduce a SPAM level.
      if (LogConfig.getLogLevel().ordinal() <= LogLevel.DEBUG.ordinal()) {
        log(clazz, LogLevel.DEBUG, args);
      }
    }
  }

  public static void error(Class<?> clazz, Object... args) {
    if (LogConfig.isLoggingEnabled()) {
      log(clazz, LogLevel.ERROR, args);
    }
  }

  public static void info(Class<?> clazz, Object... args) {
    if (LogConfig.isLoggingEnabled()) {
      if (LogConfig.getLogLevel().ordinal() <= LogLevel.INFO.ordinal()) {
        log(clazz, LogLevel.INFO, args);
      }
    }
  }

  public static boolean isLoggingEnabled() {
    return LogConfig.isLoggingEnabled();
  }

  public static void warn(Class<?> clazz, Object... args) {
    if (LogConfig.isLoggingEnabled()) {
      if (LogConfig.getLogLevel().ordinal() <= LogLevel.WARNING.ordinal()) {
        log(clazz, LogLevel.WARNING, args);
      }
    }
  }

  public static void markTimeline(Class<?> clazz, String label) {
    if (LogConfig.isLoggingEnabled()) {
      markTimelineUnconditionally(label + "(" + clazz.getName() + ")");
    }
  }

  // TODO: markTimeLine is deprecated; remove it someday.
  public static native void markTimelineUnconditionally(String label) /*-{
    if ($wnd.console) {
      if ($wnd.console.timeStamp) {
        $wnd.console.timeStamp(label);
      } else if ($wnd.console.markTimeline) {
        $wnd.console.markTimeline(label);
      }
    }
  }-*/;

  private static native void invokeBrowserLogger(String logFuncName, Object o) /*-{
    if ($wnd.console && $wnd.console[logFuncName]) {
      $wnd.console[logFuncName](o);
    }
    return;
  }-*/;


  private static void log(Class<?> clazz, LogLevel logLevel, Object... args) {
    String prefix = new StringBuilder(logLevel.toString())
        .append(" (")
        .append(clazz.getName())
        .append("): ")
        .toString();

    for (Object o : args) {
      if (o instanceof String) {
        logToDevMode(prefix + (String) o);
        logToBrowser(logLevel, prefix + (String) o);
      } else if (o instanceof Throwable) {
        Throwable t = (Throwable) o;
        logToDevMode(prefix + "(click for stack)", t);
        logToBrowser(logLevel, prefix + ExceptionUtils.getStackTraceAsString(t));
      } else if (o instanceof JavaScriptObject) {
        logToDevMode(prefix + "(JSO, see browser's console log for details)");
        logToBrowser(logLevel, prefix + "(JSO below)");
        logToBrowser(logLevel, o);
      } else {
        logToDevMode(prefix + (o != null ? o.toString() : "(null)"));
        logToBrowser(logLevel, prefix + (o != null ? o.toString() : "(null)"));
      }
    }
  }

  private static void logToBrowser(LogLevel logLevel, Object o) {
    switch (logLevel) {
      case DEBUG:
        invokeBrowserLogger("debug", o);
        break;
      case INFO:
        invokeBrowserLogger("info", o);
        break;
      case WARNING:
        invokeBrowserLogger("warn", o);
        break;
      case ERROR:
        invokeBrowserLogger("error", o);
        break;
      default:
        invokeBrowserLogger("log", o);
    }
  }

  private static void logToDevMode(String msg) {
    if (!GWT.isScript()) {
      GWT.log(msg);
    }
  }

  private static void logToDevMode(String msg, Throwable t) {
    if (!GWT.isScript()) {
      GWT.log(msg, t);
    }
  }
}
