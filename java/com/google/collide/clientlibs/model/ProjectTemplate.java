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

package com.google.collide.clientlibs.model;

/**
 * An object which encapsulates a ProjectTemplate from the server.
 */
/*
 * This is located within WorkspaceManager since this operates at the workspace level not the
 * Project level (contrary to its name). A template is loaded into a branch via a call to
 * loadTemplate.
 */
public class ProjectTemplate {
  public static ProjectTemplate create(String templateName, String templateTag) {
    return new ProjectTemplate(templateName, templateTag, templateName);
  }

  public final String name;
  public final String id;
  public final String description;

  private ProjectTemplate(String name, String id, String description) {
    this.name = name;
    this.id = id;
    this.description = description;
  }
}