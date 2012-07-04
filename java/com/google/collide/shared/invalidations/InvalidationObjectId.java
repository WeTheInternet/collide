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

import com.google.collide.shared.invalidations.InvalidationUtils.InvalidationObjectPrefix;

/**
 * An object which defines a tango object which can receive notifications or have notifications
 * published.
 *
 * @param <T> The type of the payload for this object {@link Void} if no payload is required.
 */
public final class InvalidationObjectId<T> {

  /**
   * An enum which defines the version number and persistence requirements for a
   * {@link InvalidationObjectId}. Currently these aren't really used but may be used in future
   * implementations.
   */
  public enum VersioningRequirement {
    /**
     * No version or payload data will be persisted and {@link System#currentTimeMillis()} will be
     * used to assign version numbers.
     * <p>
     * Recommended for objects which are sending notifications to the client which do not require
     * the ability to determine a notification was missed. These clients typically do not have
     * payloads or have payloads which carry extra information and can be lost or dropped.
     */
    NONE,
    /**
     * No payload data will be persisted; however, versions will be sequential.
     * <p>
     * Recommended for objects which require the ability to determine that a notification was missed
     * but do not require the ability to retrieve any missed payloads.
     */
    VERSION_ONLY,
    /**
     * Versions will be sequential and payloads will be persisted for each invalidation.
     * <p>
     * Recommended for objects which require the ability to determine that a notification was missed
     * and the ability to retrieve any missed payloads.
     */
    PAYLOADS
  }

  private final InvalidationObjectPrefix prefix;
  private final String id;
  private final String name;
  private final VersioningRequirement versioningRequirement;
  /**
   * A string which indicates that there was no payload, this is only used when a
   * {@link InvalidationObjectId} has uses {@link InvalidationUtils.VersioningRequirement#PAYLOADS}.
   */
  public static final String EMPTY_PAYLOAD = "\0\3";

  public InvalidationObjectId(
      InvalidationObjectPrefix prefix, String id, VersioningRequirement versioningRequirement) {
    this.prefix = prefix;
    // TODO: if the id is a number pack it into an integer
    this.id = id;
    this.versioningRequirement = versioningRequirement;
    this.name = prefix.getPrefix() + id;
  }

  /**
   * @return the name of this object for use in registration.
   */
  public String getName() {
    return name;
  }

  public InvalidationObjectPrefix getPrefix() {
    return prefix;
  }

  /**
   * @return the id portion of this object, typically a workspaceId or projectId.
   */
  public String getId() {
    return id;
  }

  public VersioningRequirement getVersioningRequirement() {
    return versioningRequirement;
  }

  /**
   * Useful for logging so the full name is logged, use #getName for registering.
   */
  @Override
  public String toString() {
    return prefix.toString() + "(" + name + ")";
  }
}
