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

import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.dto.Participant;
import com.google.collide.dto.client.DtoClientImpls.ParticipantImpl;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * A variant of GWTTestCase providing baked-in mockery of the bootstrap code
 * injected onto our hosting page, such that code like
 * {@link com.google.collide.client.bootstrap.BootstrapSession
 * BootstrapSession} can work in tests.
 *
 */
public abstract class BootstrappedGwtTestCase extends GWTTestCase {

  public static final String TESTUSER_EMAIL = "testuser@test.org";
  public static final String TESTUSER_USERID = "12345";
  
  public static Participant makeParticipant() {
    return makeParticipant(TESTUSER_USERID);
  }
  
  public static Participant makeParticipant(String userId) {
    ParticipantImpl result = ParticipantImpl.make();
    result.setUserId(userId);
    return result;
  }

  protected void assertVisibility(boolean visibility, com.google.gwt.dom.client.Element elem) {
    assertVisibility(visibility, Elements.asJsElement(elem));
  }

  protected void assertVisibility(boolean visibility, elemental.html.Element elem) {
    assertEquals(visibility, CssUtils.isVisible(elem));
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    injectBootstrapSession();
  }
  
  protected native void injectBootstrapSession() /*-{
    var email =
        @com.google.collide.client.testing.BootstrappedGwtTestCase::TESTUSER_EMAIL;
    var userId =
        @com.google.collide.client.testing.BootstrappedGwtTestCase::TESTUSER_USERID;
    $wnd['__session'] = {
        "activeClient": userId + "345",
        "userId": userId,
        "email": email,
        "isAdmin": false,
      };
    $wnd['__channelKeepAliveIntervalMs'] = 10000;
  }-*/;
}
