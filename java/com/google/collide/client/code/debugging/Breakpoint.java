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

import com.google.collide.client.util.PathUtil;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * Represents a breakpoint. This class is immutable.
 */
public class Breakpoint {

  private final PathUtil path;
  private final int lineNumber;
  private final String condition;
  private final boolean active;

  private Breakpoint(Builder builder) {
    this.path = builder.path;
    this.lineNumber = builder.lineNumber;
    this.condition = Strings.nullToEmpty(builder.condition);
    this.active = builder.active;
  }

  public PathUtil getPath() {
    return path;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getCondition() {
    return condition;
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Breakpoint) {
      Breakpoint that = (Breakpoint) obj;
      return Objects.equal(this.lineNumber, that.lineNumber)
          && Objects.equal(this.active, that.active)
          && Objects.equal(this.path, that.path)
          && Objects.equal(this.condition, that.condition);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, lineNumber, condition, active);
  }

  @Override
  public String toString() {
    return "{Breakpoint @" + path + " #" + lineNumber + "}";
  }

  /**
   * Builder class for the {@link Breakpoint}.
   */
  public static class Builder {
    private PathUtil path;
    private int lineNumber;
    private String condition;
    private boolean active = true; // Active by default.

    public Builder(Breakpoint breakpoint) {
      this.path = breakpoint.path;
      this.lineNumber = breakpoint.lineNumber;
      this.condition = breakpoint.condition;
      this.active = breakpoint.active;
    }

    public Builder(PathUtil path, int lineNumber) {
      this.path = path;
      this.lineNumber = lineNumber;
    }

    public Builder setPath(PathUtil path) {
      this.path = path;
      return this;
    }

    public Builder setLineNumber(int lineNumber) {
      this.lineNumber = lineNumber;
      return this;
    }

    public Builder setCondition(String condition) {
      this.condition = condition;
      return this;
    }

    public Builder setActive(boolean active) {
      this.active = active;
      return this;
    }

    public Breakpoint build() {
      return new Breakpoint(this);
    }
  }
}
