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

package com.google.collide.shared.invalidations;

import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.shared.util.JsonCollections;
import com.google.common.base.Preconditions;

/**
 * Utilities to simplify working with invalidations.
 */
public class InvalidationUtils {

  /** A set to prevent duplicated prefixes */
  public static InvalidationObjectPrefix getInvalidationObjectPrefix(String objectName) {
    for (int i = 1; i <= longestPrefixString; i++) {
      String curPrefixString = objectName.substring(0, i);
      if (PREFIX_BY_STRING.containsKey(curPrefixString)) {
        return PREFIX_BY_STRING.get(curPrefixString);
      }
    }

    throw new IllegalArgumentException("The given object name [" + objectName
        + "] does not match a prefix object from [" + PREFIX_BY_STRING.getKeys().join(", ") + "]");
  }

  /**
   * A value identifying that the version of the object is unknown.
   */
  public static final long UNKNOWN_VERSION = Long.MIN_VALUE;

  /** The version returned when an object doesn't exist in the store */
  public static final long INITIAL_OBJECT_VERSION = 1L;

  /** Map of the prefix string to the */
  private static final JsonStringMap<InvalidationObjectPrefix> PREFIX_BY_STRING =
      JsonCollections.createMap();
  private static int longestPrefixString;

  /**
   * A prefix to prepend to an id which defines a {@link InvalidationObjectId}. Prefixes must be
   * unique so this class ensures that no duplicates are used.
   */
  public enum InvalidationObjectPrefix {
    /**
     * This is a mutation to the file tree. ADD, DELETE, COPY(PASTE), and MOVE.
     */
    FILE_TREE_MUTATION("fm"),
    /**
     * Notifies that the number of participants has changed.
     */
    PARTICIPANTS_UPDATE("p"),
    /**
     * Invalidation of a path, or the entire workspace file tree and conflict listing.
     */
    FILE_TREE_INVALIDATED("fr"),
    /**
     * Notifies that an upload session has ended.
     */
    // TODO: Not sure if we still need this one?
    END_UPLOAD_SESSION_FINISHED("uf");

    private final String prefix;

    private InvalidationObjectPrefix(String prefix) {
      Preconditions.checkArgument(
          isPrefixValid(prefix), "Prefix [" + prefix + "] conflicts with another Tango object");
      this.prefix = prefix;

      InvalidationUtils.longestPrefixString = Math.max(
          InvalidationUtils.longestPrefixString, prefix.length());
      InvalidationUtils.PREFIX_BY_STRING.put(prefix, this);
    }

    public String getPrefix() {
      return prefix;
    }

    /** Helper function to ensure a previous is not currently used */
    private static boolean isPrefixValid(final String prefix) {
      for (String existingPrefix : InvalidationUtils.PREFIX_BY_STRING.getKeys().asIterable()) {
        if (prefix.startsWith(existingPrefix) || existingPrefix.startsWith(prefix)) {
          return false;
        }
      }

      return true;
    }
  }

  static {
    // This ensures that the PREFIX_BY_STRING map is filled in when this class is loaded by the JVM
    if (InvalidationObjectPrefix.FILE_TREE_MUTATION != null) {
      InvalidationObjectPrefix.FILE_TREE_MUTATION.getPrefix();
    }
  }

  private InvalidationUtils() {
    // util class
  }
}
