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

package com.google.collide.clientlibs.invalidation;

import java.util.logging.Level;

import com.google.collide.shared.util.SharedLogUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window;

/**
 * An implementation of the {@link Logger} used by the SimpleListener.
 */
public class InvalidationLogger {

  public static InvalidationLogger create() {
    String value = Window.Location.getParameter("tangoLogging");

    if (StringUtils.isNullOrEmpty(value) || value.equalsIgnoreCase("false")) {
      return new InvalidationLogger(false, false);
    } else if (value.equalsIgnoreCase("fine")) {
      return new InvalidationLogger(true, true);
    } else {
      return new InvalidationLogger(true, false);
    }
  }

  private final boolean enabled;
  private final boolean logFine;

  public InvalidationLogger(boolean enabled, boolean logFine) {
    this.enabled = enabled;
    this.logFine = logFine;
  }

  public void fine(String template, Object... args) {
    if (enabled && logFine) {
      SharedLogUtils.info(getClass(), format(template, args));
    }
  }

  public void info(String template, Object... args) {
    if (enabled) {
      SharedLogUtils.info(getClass(), format(template, args));
    }
  }

  public boolean isLoggable(Level level) {
    return enabled;
  }

  public void log(Level level, String template, Object... args) {
    if (enabled) {
      SharedLogUtils.info(getClass(), format(template, args));
    }
  }

  public void severe(String template, Object... args) {
    if (enabled) {
      SharedLogUtils.error(getClass(), format(template, args));
    }
  }

  public void warning(String template, Object... args) {
    if (enabled) {
      SharedLogUtils.warn(getClass(), format(template, args));
    }
  }

  /** A helper object which simplifies formatting */
  private static class ArgumentFormatHelper {
    private final Object[] args;
    private int current = 0;

    public ArgumentFormatHelper(Object... args) {
      this.args = args;
    }

    public String next() {
      if (args == null || current >= args.length) {
        return "[MISSING ARG]";
      }

      return args[current++].toString();
    }

    public String rest() {
      if (args == null || current >= args.length) {
        return "";
      }

      StringBuilder builder = new StringBuilder();
      for (int i = current; i < args.length; i++) {
        builder.append(" [").append(args[i].toString()).append(']');
      }

      return builder.toString();
    }
  }

  /**
   * GWT does not emulate string formatting so we just do a simple stupid one which looks for %s or
   * %d and replaces it with an arg. If there are less args than %markers we put in [MISSING ARG].
   * If there are more args than %markers we just append them at the end within brackets.
   */
  private static String format(String template, Object... args) {
    StringBuilder builder = new StringBuilder();
    ArgumentFormatHelper helper = new ArgumentFormatHelper(args);

    RegExp formatMatcher = RegExp.compile("(%s)|(%d)", "ig");
    int lastIndex = 0;
    MatchResult result = formatMatcher.exec(template);
    while (result != null) {
      String fragment = template.substring(lastIndex, result.getIndex() - 1);
      builder.append(fragment);
      builder.append(helper.next());

      lastIndex = result.getIndex() + result.getGroup(0).length();
    }

    String lastFragment = template.substring(lastIndex, template.length());
    builder.append(lastFragment);
    builder.append(helper.rest());

    return builder.toString();
  }
}
