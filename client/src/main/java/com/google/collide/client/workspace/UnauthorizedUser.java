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

package com.google.collide.client.workspace;

import collide.client.common.CommonResources;
import collide.client.util.Elements;

import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

/**
 * Presenter for the message displayed to a user that accesses a workspace he is
 * not authorized to view.
 */
public class UnauthorizedUser extends UiComponent<UnauthorizedUser.View> {

  /**
   * Static factory method for {@link HeaderMenu}.
   */
  public static UnauthorizedUser create(Resources res) {
    return new UnauthorizedUser(new View(res));
  }

  /**
   * Style names.
   */
  public interface Css extends CssResource {
    String base();

    String title();

    String email();
  }

  /**
   * Images and CssResources.
   */
  public interface Resources extends CommonResources.BaseResources {

    @Source({"UnauthorizedUser.css", "collide/client/common/constants.css"})
    Css unauthorizedUserCss();
  }

  /**
   * The View for the Header.
   */
  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("UnauthorizedUser.ui.xml")
    interface MyBinder extends UiBinder<DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField(provided = true)
    final Resources res;

    @UiField
    SpanElement email;

    public View(Resources res) {
      this.res = res;
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));

      // Set the user's email/name.
      String userEmail = BootstrapSession.getBootstrapSession().getUsername();
      Elements.asJsElement(email).setTextContent(userEmail);

      // Wire up event handlers.
      attachHandlers();
    }

    protected void attachHandlers() {
    }
  }

  /**
   * Events reported by the Header's View.
   */
  private interface ViewEvents {
  }

  private UnauthorizedUser(View view) {
    super(view);
  }
}
