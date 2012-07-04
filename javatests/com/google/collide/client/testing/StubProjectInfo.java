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

package com.google.collide.client.testing;

import com.google.collide.dto.ProjectInfo;
import com.google.collide.dto.Role;

/**
 * A {@link ProjectInfo} which can be customized for testing.
 *
 */
public class StubProjectInfo implements ProjectInfo {

  public static StubProjectInfo make() {
    String idAndName = String.valueOf(ID++);
    return new StubProjectInfo(idAndName, idAndName);
  }
  
  public static StubProjectInfo make(String idAndName) {
    return new StubProjectInfo(idAndName, idAndName);
  }

  /**
   * Static field to get an id and name from.
   */
  private static int ID = 10;
  private final String id;
  private String name;
  
  private StubProjectInfo(String id, String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public Role getCurrentUserRole() {
    // if you need this add it
    throw new UnsupportedOperationException();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getLogoUrl() {
    // if you need this add it
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getRootWsId() {
    // if you need this add it
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSummary() {
    // if you need this add it
    throw new UnsupportedOperationException();
  }
  
  public StubProjectInfo setName(String name) {
    this.name = name;
    return this;
  }
}
