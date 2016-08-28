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

package com.google.collide.client.search.awesomebox.components;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.Editor.KeyListener;
import com.google.collide.client.editor.FocusManager;
import com.google.collide.client.editor.search.SearchModel;
import com.google.collide.client.editor.search.SearchModel.MatchCountListener;
import com.google.collide.client.search.awesomebox.host.AbstractAwesomeBoxComponent;
import com.google.collide.client.search.awesomebox.host.ComponentHost;
import com.google.collide.client.search.awesomebox.shared.AwesomeBoxResources;
import com.google.collide.client.search.awesomebox.shared.AwesomeBoxResources.ComponentCss;
import com.google.collide.client.search.awesomebox.shared.MappedShortcutManager;
import com.google.collide.client.search.awesomebox.shared.ShortcutManager;
import com.google.collide.client.search.awesomebox.shared.ShortcutManager.ShortcutPressedCallback;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.HasView;
import com.google.collide.shared.util.ListenerRegistrar;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.events.MouseEvent;

/**
 * Section that displays find and replace controls. This section is meant to be
 * the first item in it's context since it piggy backs off the AwesomeBox input.
 */
public class FindReplaceComponent extends AbstractAwesomeBoxComponent implements HasView<
    FindReplaceComponent.View> {

  public interface ViewEvents {
    public void onFindQueryChanged();

    public void onKeydown(KeyboardEvent event);

    public void onNextClicked();

    public void onPreviousClicked();

    public void onReplaceClicked();

    public void onReplaceAllClicked();

    public void onCloseClicked();
  }

  public enum FindMode {
    FIND, REPLACE
  }

  private final View view;
  private final ShortcutManager shortcutManager = new MappedShortcutManager();

  private SearchModel searchModel;
  private String lastQuery = "";
  private FocusManager focusManager;

  // Editor Listener For Esc
  // TODO: Long term this should be a global clear event that bubbles
  private ListenerRegistrar<KeyListener> editorKeyListenerRegistrar;
  private RemoverManager removerManager = new RemoverManager();
  // TODO: Handle changes to total matches via document mutations
  private final MatchCountListener totalMatchesListener = new MatchCountListener() {
    @Override
    public void onMatchCountChanged(int total) {
      getView().numMatches.setInnerText(String.valueOf(total));
    }
  };

  public FindReplaceComponent(View view) {
    super(HideMode.NO_AUTOHIDE, HiddenBehavior.REVERT_TO_DEFAULT, "Find in this file");
    this.view = view;
    view.setDelegate(new ViewEventsImpl());
  }

  public void setFindMode(FindMode mode) {
    CssUtils.setDisplayVisibility2(
        Elements.asJsElement(getView().totalMatchesContainer), mode == FindMode.FIND);
    CssUtils.setDisplayVisibility2(
        Elements.asJsElement(getView().replaceActions), mode == FindMode.REPLACE);
    CssUtils.setDisplayVisibility2(
        Elements.asJsElement(getView().replaceRow), mode == FindMode.REPLACE);
  }

  /**
   * Attaches to the editor's search model for querying.
   */
  public void attachEditor(Editor editor) {
    this.searchModel = editor.getSearchModel();
    this.focusManager = editor.getFocusManager();
    this.editorKeyListenerRegistrar = editor.getKeyListenerRegistrar();

    searchModel.getMatchCountChangedListenerRegistrar().add(totalMatchesListener);
    setupShortcuts();
  }

  @Override
  public View getView() {
    return view;
  }

  @Override
  public elemental.dom.Element getElement() {
    return getView().getElement();
  }

  /**
   * Sets the query of the find replace component.
   */
  public void setQuery(String query) {
    getView().setQuery(query);
    if (isActive()) {
      Preconditions.checkNotNull(searchModel, "Search model is required to set the query");

      getView().selectQuery();
      searchModel.setQuery(query);
    }
  }

  @Override
  public String getTooltipText() {
    return "Press Ctrl+F to quickly find text in the current file";
  }

  /**
   * Initializes the shortcut manager with our shortcuts of interest. The
   * {@link ComponentHost} will handle actually notifying us of shortcuts being
   * used.
   */
  private void setupShortcuts() {
    shortcutManager.addShortcut(0, KeyCode.ENTER, new ShortcutPressedCallback() {
      @Override
      public void onShortcutPressed(KeyboardEvent event) {
        event.preventDefault();

        if (searchModel != null) {
          searchModel.getMatchManager().selectNextMatch();
        }
      }
    });

    shortcutManager.addShortcut(ModifierKeys.SHIFT, KeyCode.ENTER, new ShortcutPressedCallback() {
      @Override
      public void onShortcutPressed(KeyboardEvent event) {
        event.preventDefault();

        if (searchModel != null) {
          searchModel.getMatchManager().selectPreviousMatch();
        }
      }
    });

    shortcutManager.addShortcut(ModifierKeys.ACTION, KeyCode.G, new ShortcutPressedCallback() {
      @Override
      public void onShortcutPressed(KeyboardEvent event) {
        event.preventDefault();

        if (focusManager != null) {
          focusManager.focus();
        }
      }
    });

    shortcutManager.addShortcut(
        ModifierKeys.ACTION | ModifierKeys.SHIFT, KeyCode.G, new ShortcutPressedCallback() {
          @Override
          public void onShortcutPressed(KeyboardEvent event) {
            event.preventDefault();
            searchModel.getMatchManager().selectPreviousMatch();

            if (focusManager != null) {
              focusManager.focus();
            }
          }
        });
  }

  @Override
  public void onShow(ComponentHost host, ShowReason reason) {
    super.onShow(host, reason);

    String query = getView().getQuery();
    if (StringUtils.isNullOrEmpty(query)) {
      getView().setQuery(lastQuery);
      searchModel.setQuery(lastQuery);
    } else if (!searchModel.getQuery().equals(query)) {
      searchModel.setQuery(query);
    }

    // Listen for esc in the editor while we're showing
    // TODO: Use some sort of event system long term
    removerManager.track(editorKeyListenerRegistrar.add(new KeyListener() {
      @Override
      public boolean onKeyPress(SignalEvent event) {
        if (event.getKeyCode() == KeyCode.ESC) {
          hide();
        }
        return false;
      }
    }));
  }

  @Override
  public void focus() {
    getView().selectQuery();
    getView().focus();
  }

  @Override
  public void onHide() {
    lastQuery = getView().getQuery();
    if (searchModel != null) {
      searchModel.setQuery("");
    }

    removerManager.remove();
    getView().numMatches.setInnerText("0");
  }

  public class ViewEventsImpl implements ViewEvents {
    @Override
    public void onFindQueryChanged() {
      Preconditions.checkNotNull(searchModel, "Search model must be set for find/replace to work");

      String query = getView().getQuery();
      searchModel.setQuery(query);
    }

    @Override
    public void onKeydown(KeyboardEvent event) {
      shortcutManager.onKeyDown(event);
    }

    @Override
    public void onNextClicked() {
      if (searchModel != null && !StringUtils.isNullOrEmpty(searchModel.getQuery())) {
        searchModel.getMatchManager().selectNextMatch();
      }
    }

    @Override
    public void onPreviousClicked() {
      if (searchModel != null && !StringUtils.isNullOrEmpty(searchModel.getQuery())) {
        searchModel.getMatchManager().selectPreviousMatch();
      }
    }

    @Override
    public void onReplaceAllClicked() {
      searchModel.getMatchManager().replaceAllMatches(getView().replaceInput.getValue());
      getView().selectQuery();
      getView().focus();
    }

    @Override
    public void onReplaceClicked() {
      searchModel.getMatchManager().replaceMatch(getView().replaceInput.getValue());
    }

    @Override
    public void onCloseClicked() {
      hide();
    }
  }

  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("FindReplaceComponent.ui.xml")
    interface FindReplaceUiBinder extends UiBinder<Element, View> {
    }

    private static FindReplaceUiBinder uiBinder = GWT.create(FindReplaceUiBinder.class);

    @UiField(provided = true)
    final ComponentCss css;
    @UiField(provided = true)
    final AwesomeBoxResources res;
    @UiField
    InputElement findInput;
    @UiField
    DivElement closeButton;
    @UiField
    DivElement replaceRow;
    @UiField
    InputElement replaceInput;
    @UiField
    AnchorElement prevButton;
    @UiField
    AnchorElement nextButton;
    @UiField
    DivElement replaceActions;
    @UiField
    AnchorElement replaceButton;
    @UiField
    AnchorElement replaceAllButton;
    @UiField
    SpanElement numMatches;
    @UiField
    DivElement totalMatchesContainer;

    public View(AwesomeBoxResources res) {
      this.res = res;
      this.css = res.awesomeBoxComponentCss();

      setElement(Elements.asJsElement(uiBinder.createAndBindUi(this)));
      createTooltips(res);
      handleEvents();
    }

    public String getQuery() {
      return findInput.getValue();
    }

    public void setQuery(String query) {
      findInput.setValue(query);
    }

    public void selectQuery() {
      findInput.select();
    }

    public void focus() {
      findInput.focus();
    }

    private void createTooltips(AwesomeBoxResources res) {
      Tooltip.create(res, Elements.asJsElement(nextButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Next match");
      Tooltip.create(res, Elements.asJsElement(prevButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Previous match");
      Tooltip.create(res, Elements.asJsElement(replaceButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Replace current match");
      Tooltip.create(res, Elements.asJsElement(replaceAllButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Replace all matches");
    }

    private void handleEvents() {
      Elements.asJsElement(findInput).addEventListener(Event.INPUT, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onFindQueryChanged();
          }
        }
      }, false);

      getElement().addEventListener(Event.KEYDOWN, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onKeydown((KeyboardEvent) evt);
          }
        }
      }, false);

      getElement().addEventListener(Event.CLICK, new EventListener() {
        @Override
        public void handleEvent(Event arg0) {
          if (getDelegate() == null) {
            return;
          }

          MouseEvent mouseEvent = (MouseEvent) arg0;
          if (prevButton.isOrHasChild((Node) mouseEvent.getTarget())) {
            getDelegate().onPreviousClicked();
          } else if (nextButton.isOrHasChild((Node) mouseEvent.getTarget())) {
            getDelegate().onNextClicked();
          } else if (replaceButton.isOrHasChild((Node) mouseEvent.getTarget())) {
            getDelegate().onReplaceClicked();
          } else if (replaceAllButton.isOrHasChild((Node) mouseEvent.getTarget())) {
            getDelegate().onReplaceAllClicked();
          } else if (closeButton.isOrHasChild((Node) mouseEvent.getTarget())) {
            getDelegate().onCloseClicked();
          }
        }
      }, false);
    }
  }
}
