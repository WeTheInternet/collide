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

package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

/**
 * A Tango payload indicating that the user was added/removed from a
 * project/workspace.
 */
@RoutingType(type = RoutingTypes.MEMBERSHIPCHANGEDPAYLOAD)
public interface MembershipChangedPayload extends ServerToClientDto {

  /**
   * An object which identifies the type of membership change which occurred.
   */
  public enum MembershipChange {
    ADDED_TO_WORKSPACE, REMOVED_FROM_WORKSPACE, ADDED_TO_PROJECT, REMOVED_FROM_PROJECT
  }

  /** The type of membership change */
  MembershipChange getMembershipChange();

  /**
   * The id of the project or workspace the user was added to or removed from
   */
  String getId();
}
