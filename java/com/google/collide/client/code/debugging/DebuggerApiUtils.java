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

import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.json.client.Jso;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Objects;

/**
 * Utility helper class that contains browser independent methods on the
 * Debugger API.
 *
 */
class DebuggerApiUtils {

  /**
   * Casts a {@link RemoteObject} to a boolean value.
   *
   * @param remoteObject the remote object to cast
   * @return boolean value
   */
  public static boolean castToBoolean(@Nullable RemoteObject remoteObject) {
    if (remoteObject == null || remoteObject.getType() == null) {
      return false;
    }

    switch (remoteObject.getType()) {
      case BOOLEAN:
        return "true".equals(remoteObject.getDescription());
      case FUNCTION:
        return true;
      case NUMBER:
        if (isNonFiniteNumber(remoteObject)) {
          return !"NaN".equals(remoteObject.getDescription());
        } else {
          return !StringUtils.isNullOrEmpty(remoteObject.getDescription())
              && Double.parseDouble(remoteObject.getDescription()) != 0;
        }
      case OBJECT:
        return remoteObject.getSubType() != RemoteObjectSubType.NULL;
      case STRING:
        return !StringUtils.isNullOrEmpty(remoteObject.getDescription());
      case UNDEFINED:
        return false;
      default:
        return false;
    }
  }

  /**
   * Adds a new field to a {@link Jso} object from a primitive
   * {@link RemoteObject}.
   *
   * <p>NOTE: Non finite numbers will not be added!
   *
   * @param jso object to add the new value to
   * @param key key name
   * @param remoteObject primitive remote object to extract the value from
   * @return true if the field was added successfully, false otherwise (for
   *         example, if the {@code remoteObject} did not represent a primitive
   *         value)
   */
  public static boolean addPrimitiveJsoField(Jso jso, String key, RemoteObject remoteObject) {
    if (remoteObject == null || remoteObject.getType() == null) {
      return false;
    }

    switch (remoteObject.getType()) {
      case BOOLEAN:
        jso.addField(key, "true".equals(remoteObject.getDescription()));
        return true;
      case FUNCTION:
        return false;
      case NUMBER:
        if (!isNonFiniteNumber(remoteObject)) {
          jso.addField(key, Double.parseDouble(remoteObject.getDescription()));
          return true;
        }
        return false;
      case OBJECT:
        if (remoteObject.getSubType() == RemoteObjectSubType.NULL) {
          jso.addNullField(key);
          return true;
        }
        return false;
      case STRING:
        jso.addField(key, remoteObject.getDescription());
        return true;
      case UNDEFINED:
        jso.addUndefinedField(key);
        return true;
      default:
        return false;
    }
  }

  /**
   * Checks whether the given {@link RemoteObject}s are equal.
   *
   * @param a first remote object
   * @param b second remote object
   * @return true if both of the arguments point to the same remote object in
   *         the Debugger VM, or if they are both {@code null}s
   */
  public static boolean equal(@Nullable RemoteObject a, @Nullable RemoteObject b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return Objects.equal(a.getDescription(), b.getDescription())
        && Objects.equal(a.hasChildren(), b.hasChildren())
        && Objects.equal(a.getObjectId(), b.getObjectId())
        && Objects.equal(a.getType(), b.getType())
        && Objects.equal(a.getSubType(), b.getSubType());
  }

  /**
   * Checks whether a given {@link RemoteObject} represents a non finite number
   * ({@code NaN}, {@code Infinity} or {@code -Infinity}).
   *
   * @param remoteObject the object to check
   * @return true if the remote object represents a non finite number
   */
  public static boolean isNonFiniteNumber(RemoteObject remoteObject) {
    if (remoteObject.getType() != RemoteObjectType.NUMBER) {
      return false;
    }
    String description = remoteObject.getDescription();
    return "NaN".equals(description)
        || "Infinity".equals(description)
        || "-Infinity".equals(description);
  }

  public static RemoteObject createRemoteObject(final String value) {
    return new RemoteObject() {

      @Override
      public String getDescription() {
        return value;
      }

      @Override
      public boolean hasChildren() {
        return false;
      }

      @Override
      public RemoteObjectId getObjectId() {
        return null;
      }

      @Override
      public RemoteObjectType getType() {
        return RemoteObjectType.STRING;
      }

      @Override
      public RemoteObjectSubType getSubType() {
        return null;
      }
    };
  }
}
