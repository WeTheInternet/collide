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

package com.google.collide.client.search.awesomebox;

import com.google.collide.client.AppContext;
import com.google.collide.client.common.BaseResources;
import com.google.collide.client.search.awesomebox.AwesomeBoxModel.ContextChangeListener;
import com.google.collide.client.search.awesomebox.host.AbstractAwesomeBoxComponent;
import com.google.collide.client.search.awesomebox.host.ComponentHost;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.UserActivityManager;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.HasView;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Timer;

import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.events.MouseEvent;
import elemental.html.HTMLCollection;

/**
 * The main controller and view for the awesome box
 *
 */
/*
 * The autohide component was not used since this component does not strictly
 * utilize a full auto-hide type functionality. The input box is still part of
 * the control (for styling and ui reasons) but does not hide.
 */
// TODO: In the future lets add some sort of query ranking/processor.
public class AwesomeBox extends AbstractAwesomeBoxComponent implements HasView<AwesomeBox.View> {

  /**
   * Creates a new AwesomeBox component and returns it. Though the class
   * instance will be unique, the underlying model will remain consistent among
   * all AwesomeBox's. This allows the AwesomeBox to be added at different
   * places but rely on only one underlying set of data for a consistent
   * experience.
   */
  public static AwesomeBox create(AppContext context) {
    return new AwesomeBox(new AwesomeBox.View(context.getResources()), context.getAwesomeBoxModel(),
        context.getUserActivityManager());
  }

  public interface Css extends CssResource {
    /* Generic Awesome Box and Container Styles */
    String awesomeContainer();

    String dropdownContainer();

    String closeButton();

    String awesomeBox();

    /* Generic Section Styles */
    String section();

    String selected();

    String sectionItem();

    String shortcut();
  }

  public interface SectionCss extends CssResource {
    /* Goto File Actions */
    String fileItem();

    String folder();

    /* Goto Branch Actions */
    String branchIcon();

    /* Find Actions */
    String searchIcon();
  }

  public interface Resources extends BaseResources.Resources, Tooltip.Resources {
    @Source({"AwesomeBox.css", "com/google/collide/client/common/constants.css"})
    public Css awesomeBoxCss();

    @Source("AwesomeBoxSection.css")
    public SectionCss awesomeBoxSectionCss();
  }

  /**
   * Defines an AwesomeBox section which is hidden until the AwesomeBox is
   * focused.
   *
   */
  public interface AwesomeBoxSection {
    /**
     * Actions that can be taken when a section item is selected.
     */
    public enum ActionResult {
      /**
       * An action was performed and the AwesomeBox should be closed.
       */
      CLOSE,
      /**
       * An item was selected and the section should receive selection.
       */
      SELECT_ITEM,
      /**
       * Do nothing.
       */
      DO_NOTHING
    }

    /**
     * Called when the section has been added to a context. Any context related
     * setup should be performed here.
     */
    public void onAddedToContext(AwesomeBoxContext context);

    /**
     * Called when the query in the AwesomeBox is modified and the section may
     * need to be filtered.
     *
     *  If the section currently has a selection it should be removed upon query
     * change.
     *
     * @return true if the section has results and should be visible.
     */
    public boolean onQueryChanged(String query);

    /**
     * Called when the global context has been changed to a context containing
     * this section and any previous state should most likely be removed.
     */
    public void onContextChanged(AwesomeBoxContext context);

    /**
     * Called when the AwesomeBox is focused and in a context that this section
     * is a member of. The section should prepare itself for the AwesomeBox to
     * be empty.
     *
     * @return true if section should be immediately visible in the dropdown.
     */
    public boolean onShowing(AwesomeBox awesomeBox);

    /**
     * Called when the AwesomeBox panel is hidden due to loss of focus or
     * external click.
     */
    public void onHiding(AwesomeBox awesomeBox);

    /**
     * Called to move the selection down or up. The contract for this method
     * specifies that the section will return false if it is at a boundary and
     * unable to move the selection or does not accept selection. When unable to
     * move selection it should not assume it has lost selection until
     * onClearSelection is called.
     *
     * @param moveDown true if the selection is moving down, false if up. If the
     *        section currently has no selection it should select it's first
     *        item when moveDown is true and it's last item if moveDown is
     *        false.
     *
     * @return true if the selection was moved successfully
     */
    /*
     * This method off-loads selection onto the sections with little expectation
     * set forth by the AwesomeBox. This allows for sections which are
     * non-standard and can be selected in different ways (or not at all).
     */
    public boolean onMoveSelection(boolean moveDown);

    /**
     * The selection has been reset. If the section has any item selected it
     * should clear the selection.
     */
    public void onClearSelection();

    /**
     * The enter key has been pressed and the target is the current selection in
     * this section. The necessary action should be performed.
     *
     * @return Appropriate action to take after action has been performed
     */
    public ActionResult onActionRequested();

    /**
     * Called when the tab completion key is pressed in the AwesomeBox and the
     * section currently has selection.
     *
     * @return A string representing the item currently selected. Null or empty
     *         will cancel the completion.
     */
    public String onCompleteSelection();

    /**
     * Called when a click event is received where the target is a child of this
     * section. Typically a section should call mouseEvent.preventDefault() to
     * prevent native selection and focus from being transferred.
     *
     * @param mouseEvent The location of the click.
     *
     * @return The appropriate action for the AwesomeBox to take.
     */
    public ActionResult onSectionClicked(MouseEvent mouseEvent);

    /**
     * Returns the div element which wraps this section's contents.
     */
    public elemental.html.DivElement getElement();
  }

  /**
   * Callback used when iterating through sections.
   */
  public interface SectionIterationCallback {
    public boolean onIteration(AwesomeBoxSection section);
  }
  
  private static final String NO_QUERY = "";

  private final AwesomeBoxModel model;
  private final UserActivityManager userActivityManager;
  private final View view;
  /** A hacky means to ensure our keyup handler ignores enter after show */
  private boolean ignoreEnterKeyUp = false;

  /**
   * Tracks the last query we have dispatched to ensure that we don't duplicate
   * query changed events.
   */
  private String lastDispatchedQuery = NO_QUERY;

  protected AwesomeBox(View view, AwesomeBoxModel model, UserActivityManager userActivityManager) {
    super(HideMode.AUTOHIDE, HiddenBehavior.STAY_ACTIVE, "Type to find files and features");
    this.view = view;
    this.model = model;
    this.userActivityManager = userActivityManager;

    view.setDelegate(new ViewEventsImpl());
    model.getContextChangeListener().add(new ContextChangeListener() {
      @Override
      public void onContextChanged(boolean contextAlreadyActive) {
        if (contextAlreadyActive) {
          selectQuery();
        } else {
          refreshView();
        }
      }
    });
  }
  
  /**
   * Returns the view for this component.
   */
  @Override
  public View getView() {
    return view;
  }

  /**
   * Refreshes the view by reloading all the section DOM and clearing selection.
   */
  private void refreshView() {
    getView().clearSections();
    getView().awesomeBoxInput.setAttribute(
        "placeholder", getModel().getContext().getPlaceholderText());
    getView().setInputEmptyStyle(getModel().getContext().getAlwaysShowCloseButton());

    JsonArray<AwesomeBoxSection> sections = getModel().getCurrentSections();
    for (int i = 0; i < sections.size(); i++) {
      AwesomeBoxSection section = sections.get(i);
      if (isActive()) {
        boolean showInitial = section.onShowing(this);
        CssUtils.setDisplayVisibility2(section.getElement(),
            StringUtils.isNullOrEmpty(getQuery()) ? showInitial : section.onQueryChanged(
                getQuery()));
      }
      getView().getElement().appendChild(section.getElement());
    }

    // If the view is expanded, try to get back to a default state of some kind.
    if (isActive()) {
      getModel().selectFirstItem();
      getView().awesomeBoxInput.focus();
    }
  }

  @Override
  public elemental.html.Element getElement() {
    return getView().getElement();
  }
  
  @Override
  public String getTooltipText() {
    return "Press Alt+Enter to quickly access the AwesomeBox";
  }

  AwesomeBoxModel getModel() {
    return model;
  }

  /**
   * Retrieves the current query of the AwesomeBox.
   */
  public String getQuery() {
    return getView().awesomeBoxInput.getValue();
  }

  /**
   * Sets the query of the AwesomeBox, this will not trigger a query changed
   * event.
   */
  public void setQuery(String query) {
    getView().awesomeBoxInput.setValue(query);
    getView().setInputEmptyStyle(getModel().getContext().getAlwaysShowCloseButton());
  }

  /**
   * Selects whatever text is in the Awesomebox input.
   */
  public void selectQuery() {
    getView().awesomeBoxInput.select();
  }
  
  @Override
  public void onShow(ComponentHost host, ShowReason reason) {
    super.onShow(host, reason);
    // We assume the alt+enter shortcut focused us (and its a keydown listener).
    ignoreEnterKeyUp = reason == ShowReason.OTHER;
    
    JsonArray<AwesomeBoxSection> sections = getModel().getCurrentSections();
    for (int i = 0; i < sections.size(); i++) {
      AwesomeBoxSection section = sections.get(i);
      CssUtils.setDisplayVisibility2(section.getElement(), section.onShowing(AwesomeBox.this));
      getView().getElement().appendChild(section.getElement());
    }

    getModel().selectFirstItem();

    // Show the panel
    getView().setInputEmptyStyle(getModel().getContext().getAlwaysShowCloseButton());
  }

  @Override
  public void onHide() {
    super.onHide();
    
    getModel().clearSelection();

    JsonArray<AwesomeBoxSection> sections = getModel().getCurrentSections();
    for (int i = 0; i < sections.size(); i++) {
      AwesomeBoxSection section = sections.get(i);
      section.onHiding(AwesomeBox.this);
      CssUtils.setDisplayVisibility2(section.getElement(), false);
    }

    lastDispatchedQuery = NO_QUERY;
    
    getView().awesomeBoxInput.setValue("");
    getView().setInputEmptyStyle(false);
  }

  /**
   * Focuses the AwesomeBox view.
   */
  @Override
  public void focus() {
    getView().awesomeBoxInput.focus();
  }

  private interface ViewEvents {
    public void onCloseClicked();

    /**
     * The AwesomeBox input has lost focus.
     */
    public void onBlur();

    /**
     * Called when the AwesomeBox panel is clicked.
     */
    public void onClick(MouseEvent mouseEvent);

    /**
     * Fired when a key down event occurs on the AwesomeBox input.
     */
    public void onInputKeyDown(KeyboardEvent keyEvent);

    public void onKeyUp(KeyboardEvent keyEvent);
  }

  private class ViewEventsImpl implements ViewEvents {
    @Override
    public void onBlur() {
      hide();
    }

    @Override
    public void onClick(MouseEvent mouseEvent) {
      mouseEvent.stopPropagation();

      boolean isInInput = getView().mainInput.isOrHasChild((Element) mouseEvent.getTarget());
      boolean isInDropDown = getView().getElement().contains((Node) mouseEvent.getTarget());

      if (mouseEvent.getButton() == MouseEvent.Button.PRIMARY && isInDropDown) {
        sectionClicked(mouseEvent);
      } else if (!isInInput) {
        mouseEvent.preventDefault();
      }
    }

    private void sectionClicked(MouseEvent mouseEvent) {
      JsonArray<AwesomeBoxSection> sections = getModel().getCurrentSections();
      for (int i = 0; i < sections.size(); i++) {
        AwesomeBoxSection section = sections.get(i);
        if (section.getElement().contains((Node) mouseEvent.getTarget())) {
          switch (section.onSectionClicked(mouseEvent)) {
            case CLOSE:
              hide();
              break;
            case SELECT_ITEM:
              /**
               * We assume the section has internally handled selection, set
               * selection only clears selection if it was on another section
               * previously.
               */
              getModel().setSelection(section);
              break;
          }
          return;
        }
      }
    }

    @Override
    public void onKeyUp(KeyboardEvent keyEvent) {
      if (keyEvent.getKeyCode() == KeyCode.ENTER) {
        if (ignoreEnterKeyUp) {
          ignoreEnterKeyUp = false;
          return;
        }
        
        AwesomeBoxSection section =
            getModel().getSelection(AwesomeBoxModel.SelectMode.TRY_AUTOSELECT_FIRST_ITEM);
        if (section != null
            && section.onActionRequested() == AwesomeBoxSection.ActionResult.CLOSE) {
          hide();
        }
      } else if (!lastDispatchedQuery.equals(getQuery())) {
        lastDispatchedQuery = getQuery();
        // TODO: allow context to choose rather query change is batched.
        deferQueryChangeTimer.cancel();
        deferQueryChangeTimer.schedule(30);
      }
    }

    /**
     * Timer which defers query change until 50ms after the user has stopped
     * typing. This prevents spamming the event if the user is typing in a large
     * amount of text quickly.
     */
    private final Timer deferQueryChangeTimer = new Timer() {
      @Override
      public void run() {
        dispatchQueryChangeEvent();
      }
    };

    private void dispatchQueryChangeEvent() {
      // force selection to be cleared before a query change
      getModel().clearSelection();

      JsonArray<AwesomeBoxSection> sections = getModel().getCurrentSections();
      for (int i = 0; i < sections.size(); i++) {
        AwesomeBoxSection section = sections.get(i);
        CssUtils.setDisplayVisibility2(
            section.getElement(), section.onQueryChanged(getView().awesomeBoxInput.getValue()));
      }

      // Select the first item in our list
      getModel().selectFirstItem();
    }

    private void changeSelection(final boolean moveDown) {
      AwesomeBoxSection section = getModel().getSelection(AwesomeBoxModel.SelectMode.DEFAULT);

      if (section == null) {
        // if null then we should reset selection to the top or the bottom
        getModel().selectFirstItem();
      } else if (!section.onMoveSelection(moveDown)) {
        getModel().iterateFrom(section, moveDown, new SectionIterationCallback() {
          @Override
          public boolean onIteration(AwesomeBoxSection curSection) {
            return !getModel().trySetSelection(curSection, moveDown);
          }
        });
        // if nothing is selected on iteration then the selection doesn't change
      }
    }

    @Override
    public void onInputKeyDown(KeyboardEvent keyEvent) {
      if (keyEvent.getKeyCode() == KeyCode.UP) {
        changeSelection(false);
      } else if (keyEvent.getKeyCode() == KeyCode.DOWN) {
        changeSelection(true);
      } else if (keyEvent.getKeyCode() == KeyCode.TAB) {
        handleTabComplete();

        if (getModel().getContext().getPreventTab()) {
          keyEvent.preventDefault();
        }
      }
      dispatchEmptyAwesomeBoxCheck();

      userActivityManager.markUserActive();

      // For a few keys the default is always prevented.
      if (keyEvent.getKeyCode() == KeyCode.UP || keyEvent.getKeyCode() == KeyCode.DOWN) {
        keyEvent.preventDefault();
      } else {
        getModel().getContext().getShortcutManager().onKeyDown(keyEvent);
      }
    }

    /**
     * Handles tab based query completion.
     */
    private void handleTabComplete() {
      AwesomeBoxSection section = getModel().getSelection(AwesomeBoxModel.SelectMode.DEFAULT);
      if (section != null) {
        String completion = section.onCompleteSelection();
        if (completion != null) {
          // TODO: Potentially highlight completed part of the query
          getView().awesomeBoxInput.setValue(completion);
        }
      }
    }

    private void dispatchEmptyAwesomeBoxCheck() {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          getView().setInputEmptyStyle(getModel().getContext().getAlwaysShowCloseButton());
        }
      });
    }

    @Override
    public void onCloseClicked() {
      hide();
    }
  }

  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("AwesomeBox.ui.xml")
    interface AwesomeBoxUiBinder extends UiBinder<Element, View> {
    }

    private static AwesomeBoxUiBinder uiBinder = GWT.create(AwesomeBoxUiBinder.class);

    @UiField(provided = true)
    final Resources res;
    @UiField
    InputElement awesomeBoxInput;
    @UiField
    DivElement closeButton;
    @UiField
    DivElement mainInput;

    public View(Resources res) {
      this.res = res;
      setElement(Elements.asJsElement(uiBinder.createAndBindUi(this)));

      attachHandlers();
    }

    /**
     * Attaches several handlers to the awesome box input and the container.
     */
    private void attachHandlers() {
      Elements.asJsElement(awesomeBoxInput).setOnBlur(new EventListener() {
        @Override
        public void handleEvent(Event event) {
          // blur removes the focus then we hide the actual panel
          if (getDelegate() != null) {
            getDelegate().onBlur();
          }
        }
      });

      Elements.asJsElement(awesomeBoxInput).setOnKeyDown(new EventListener() {
        @Override
        public void handleEvent(Event event) {
          KeyboardEvent keyEvent = (KeyboardEvent) event;

          if (getDelegate() != null) {
            getDelegate().onInputKeyDown(keyEvent);
          }
        }
      });

      Elements.asJsElement(closeButton).setOnClick(new EventListener() {
        @Override
        public void handleEvent(Event arg0) {
          getDelegate().onCloseClicked();
        }
      });

      getElement().setOnKeyUp(new EventListener() {
        @Override
        public void handleEvent(Event event) {
          KeyboardEvent keyEvent = (KeyboardEvent) event;
          if (getDelegate() != null) {
            getDelegate().onKeyUp(keyEvent);
          }
        }
      });

      getElement().setOnMouseDown(new EventListener() {
        @Override
        public void handleEvent(Event event) {
          MouseEvent mouseEvent = (MouseEvent) event;
          if (getDelegate() != null) {
            getDelegate().onClick(mouseEvent);
          }
        }
      });
    }

    /**
     * Removes all sections DOM from the AwesomeBox.
     */
    private void clearSections() {
      HTMLCollection elements = getElement().getChildren();
      for (int l = elements.getLength() - 1; l >= 0; l--) {
        if (elements.item(l) != mainInput) {
          elements.item(l).removeFromParent();
        }
      }
    }

    /**
     * Sets the empty or non-empty styles of the AwesomeBox.
     */
    private void setInputEmptyStyle(boolean alwaysShowClose) {
      boolean isEmpty = StringUtils.isNullOrEmpty(awesomeBoxInput.getValue());
      // Intentional use of setDisplayVisibility
      CssUtils.setDisplayVisibility(Elements.asJsElement(closeButton), !isEmpty || alwaysShowClose);
    }
  }
}
