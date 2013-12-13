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

import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.TestHelper;
import com.google.collide.dto.ProjectInfo;
import com.google.collide.dto.client.DtoClientImpls.ProjectInfoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;

import elemental.html.DivElement;

/**
 * A base class for tests exploiting Collide's Place infrastructure.
 */
public abstract class PlaceGwtTestCase extends CommunicationGwtTestCase {

  private static final String PROJECT_ID = "projectid";
  protected static final String WS_ID = "1234";
  private boolean initialized = false;

  /**
   * This gwtSetUp builds on the super class's, by setting the expectations
   * needed to get the places installed, then installing the places, and thus
   * draining those expectations.  We do assert that the drainage is complete
   * here.
   */
  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    if (!initialized) {
      initialized = true;
      // some of the place handlers assume there's an element with the GWT_ROOT
      // id... which is true in the app, but not in tests.  Make one: 
      if (Elements.getElementById(AppContext.GWT_ROOT) == null) {
        DivElement gwt_root = Elements.createDivElement();
        gwt_root.setId(AppContext.GWT_ROOT);
        Elements.getBody().appendChild(gwt_root);
      }      
    }
    // expectations for setupPlaces()...
    MockFrontendApi frontend = context.getFrontendApi();
    JsoArray<ProjectInfo> projects = JsoArray.create();
    projects.add(
        ProjectInfoImpl.make().setId(PROJECT_ID).setName("projectname").setSummary("summary"));
    JsoStringMap<String> templates = JsoStringMap.create(); // no templates
  
    // and do the setupPlaces, which will also drain those expectations. Unlike
    // EasyMock, we don't have any record/replay modality to mess with... so the
    // user's test expectations can be "recorded" even after this.
    TestHelper.setupPlaces(context);
    context.assertIsDrained();
  }
}
