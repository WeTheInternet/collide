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

package com.google.collide.client.code;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Element;

/**
 * Presenter for a section in the navigation bar. A section consists of a header
 * and a content area.
 *
 */
public class WorkspaceNavigationSection<V extends WorkspaceNavigationSection.View<?>> extends
    UiComponent<V> {

  public interface Css extends CssResource {
    String closeX();

    String contentArea();

    String contentAreaScrollable();

    String header();

    String root();

    String stretch();

    String blue();

    String underlineHeader();
    
    String headerLink();
    
    String menuButton();
  }
  
  public interface ViewEvents {
    void onTitleClicked();

    void onCloseClicked();
    
    void onMenuButtonClicked();
  }
  
  public static abstract class AbstractViewEventsImpl implements ViewEvents {
    @Override
    public void onTitleClicked() {
    }

    @Override
    public void onCloseClicked() {
    }

    @Override
    public void onMenuButtonClicked() {
    }
  }

  @VisibleForTesting
  public static class View<D extends ViewEvents> extends CompositeView<D> {

    @UiTemplate("WorkspaceNavigationSection.ui.xml")
    interface MyBinder extends UiBinder<DivElement, View<?>> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField(provided = true)
    final BaseResources.Css baseCss;

    @UiField(provided = true)
    final Css css;
    
    @UiField(provided = true)
    final Resources res;

    @UiField
    DivElement header;

    @UiField
    AnchorElement closeX;

    @UiField
    DivElement contentArea;
    
    @UiField
    AnchorElement menuButton;

    @UiField
    AnchorElement title;

    private Element contentElement;

    protected View(Resources res) {
      this.css = res.workspaceNavigationSectionCss();
      this.baseCss = res.baseCss();
      this.res = res;

      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      attachEventHandlers();

      setCloseable(false);
      setShowMenuButton(false);
    }

    private void attachEventHandlers() {
      Elements.asJsElement(closeX).setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event arg0) {
          if (getDelegate() != null) {
            getDelegate().onCloseClicked();
          }
        }
      });
      Elements.asJsElement(title).setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event arg0) {
          if (getDelegate() != null) {
            getDelegate().onTitleClicked();
          }
        }
      });
      Elements.asJsElement(menuButton).setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event arg0) {
          if (getDelegate() != null) {
            getDelegate().onMenuButtonClicked();
          }
        }
      });
    }

    @VisibleForTesting
    public Element getContentElement() {
      return contentElement;
    }

    protected void setContent(Element newContentElement) {
      if (contentElement == newContentElement) {
        return;
      }
      
      if (contentElement != null) {
        Elements.asJsElement(contentArea).replaceChild(newContentElement, contentElement);
      } else {
        Elements.asJsElement(contentArea).appendChild(newContentElement);
      }

      contentElement = newContentElement;
    }

    protected void setContentAreaScrollable(boolean scrollable) {
      Element contentAreaElement = Elements.asJsElement(contentArea);
      CssUtils.setClassNameEnabled(contentAreaElement, css.contentAreaScrollable(), scrollable);
    }

    protected void setStretch(boolean stretch) {
      CssUtils.setClassNameEnabled(getElement(), css.stretch(), stretch);
    }

    protected void setBlue(boolean blue) {
      CssUtils.setClassNameEnabled(getElement(), css.blue(), blue);
    }

    public void setTitle(String title) {
      Elements.asJsElement(this.title).setTextContent(title);
    }

    public void setCloseable(boolean closeable) {
      CssUtils.setDisplayVisibility(Elements.asJsElement(closeX), closeable);
    }
    
    public void setShowMenuButton(boolean visible) {
      CssUtils.setDisplayVisibility(Elements.asJsElement(menuButton), visible);
    }

    protected void setUnderlineHeader(boolean underline) {
      CssUtils.setClassNameEnabled(Elements.asJsElement(header), css.underlineHeader(), underline);
    }
  }

  public interface Resources extends BaseResources.Resources {
    @Source({"WorkspaceNavigationSection.css", "constants.css",
        "com/google/collide/client/common/constants.css"})
    Css workspaceNavigationSectionCss();
  }

  public void setVisible(boolean visible) {
    CssUtils.setDisplayVisibility(getView().getElement(), visible);
  }
  
  public void makeTitleLink() {
  }

  protected WorkspaceNavigationSection(V view) {
    super(view);
  }
}
