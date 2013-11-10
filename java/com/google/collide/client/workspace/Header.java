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

import collide.client.filetree.FileTreeModel;
import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.bootstrap.BootstrapSession;
import com.google.collide.client.history.Place;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.ClientPluginService;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.search.SearchContainer;
import com.google.collide.client.search.awesomebox.AwesomeBox;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponent;
import com.google.collide.client.search.awesomebox.host.AwesomeBoxComponentHost;
import com.google.collide.client.search.awesomebox.shared.AwesomeBoxResources;
import com.google.collide.client.testing.DebugAttributeSetter;
import com.google.collide.client.testing.DebugId;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.clientlibs.model.Workspace;
import com.google.collide.dto.UserDetails;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.ImageElement;

/**
 * The Header for the workspace Shell.
 *
 */
public class Header extends UiComponent<Header.View> {

  /**
   * Creates the default version of the header to be used in the editor shell.
   */
  public static Header create(View view,
      WorkspaceShell.View workspaceShellView,
      Place currentPlace,
      final AppContext appContext,
      FileNameSearch fileNameSearch,
      FileTreeModel fileTreeModel) {
    AwesomeBoxComponentHost awesomeBoxHost = new AwesomeBoxComponentHost(
        view.awesomeBoxComponentHostView, appContext.getAwesomeBoxComponentHostModel());
    // TODO: Sigh, due to tooltip being part of the client glob this
    // lives outside of the component host, need to resolve this when I get a
    // chance should be easy to make tooltip and autohide crud its own thing.
    // TODO: This should never be shown (its a fallback just in
    // case we hit a case where it could in the future).
    final String defaultText = "Access the AwesomeBox for more options";
    Positioner tooltipPositioner = new Tooltip.TooltipPositionerBuilder().buildAnchorPositioner(
        view.awesomeBoxComponentHostView.getElement());
    Tooltip tooltip = new Tooltip.Builder(appContext.getResources(),
        view.awesomeBoxComponentHostView.getElement(), tooltipPositioner).setTooltipRenderer(
        new Tooltip.TooltipRenderer() {
          @Override
          public elemental.dom.Element renderDom() {
            elemental.html.DivElement element = Elements.createDivElement();
            AwesomeBoxComponent component =
                appContext.getAwesomeBoxComponentHostModel().getActiveComponent();
            if (component == null || component.getTooltipText() == null) {
              element.setTextContent(defaultText);
            } else {
              element.setTextContent(component.getTooltipText());
            }
            return element;
          }
        }).build();

    AwesomeBox awesomeBoxComponent = AwesomeBox.create(appContext);
    appContext.getAwesomeBoxComponentHostModel().setDefaultComponent(awesomeBoxComponent);

    RunButtonController runButtonController = RunButtonController.create(appContext,
        view.runButton,
        view.runDropdownButton,
        currentPlace,
        fileNameSearch,
        fileTreeModel);



    Header header = new Header(view,
        currentPlace,
        appContext,
        runButtonController,
        awesomeBoxHost);
    return header;
  }

  /**
   * Style names associated with elements in the header.
   */
  public interface Css extends CssResource {

    String gray();

    String leftButtonGroup();

    String pluginButtonContainer();

    String paddedButton();

    String pluginButtons();

    String pluginIcon();

    String pluginButton();

    String runButtonContainer();

    String runButton();

    String runDropdownButton();

    String runIcon();

    String syncButtons();

    String readOnlyMessage();

    String newWorkspaceButton();

    String selectArrows();

    String selectBg();

    String triangle();

    String awesomeBoxContainer();

    String feedbackButton();

    String shareButton();

    String rightButtonGroup();

    String profileImage();
  }

  /**
   * Images and CssResources consumed by the Header.
   */
  public interface Resources
      extends
      AwesomeBox.Resources,
      AwesomeBoxResources,
      PopupBlockedInstructionalPopup.Resources,
      RunButtonTargetPopup.Resources,
      SearchContainer.Resources{

    @Source("gear.png")
    ImageResource gearIcon();

    @Source("terminal.png")
    ImageResource terminalIcon();

    @Source("play.png")
    ImageResource runIcon();

    @Source("play_dropdown.png")
    ImageResource runDropdownIcon();

    @Source("select_control.png")
    ImageResource selectArrows();

    @Source("read_only_message_icon.png")
    ImageResource readOnlyMessageIcon();

    @Source("trunk_branch_icon.png")
    ImageResource trunkBranchIcon();

    @Source({"Header.css", "constants.css", "collide/client/common/constants.css"})
    Css workspaceHeaderCss();
  }

  /**
   * The View for the Header.
   */
  public static class View extends CompositeView<ViewEvents> {
    @UiTemplate("Header.ui.xml")
    interface MyBinder extends UiBinder<DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField(provided = true)
    final Resources res;

    final Css css;

    @UiField
    DivElement headerMenuElem;

    @UiField
    DivElement pluginContainer;

    @UiField
    DivElement rightButtons;

    @UiField
    AnchorElement runButton;

    @UiField
    AnchorElement runDropdownButton;

    @UiField
    DivElement leftButtonGroup;

    @UiField
    DivElement syncButtonsDiv;

    @UiField
    DivElement readOnlyMessage;

    @UiField
    AnchorElement newWorkspaceButton;

    @UiField
    DivElement awesomeBoxContainer;

    @UiField
    AnchorElement feedbackButton;

    @UiField
    AnchorElement shareButton;

    @UiField
    DivElement profileImage;

    private final AwesomeBoxComponentHost.View awesomeBoxComponentHostView;
    private final ImageButton runImageButton;
    private final ImageButton runDropdownImageButton;
    private final ImageButton newWorkspaceImageButton;
    private final ImageButton shareImageButton;

    private final Tooltip newWorkspaceTooltip;

    public View(Resources res, boolean detached) {
      this.res = res;
      this.css = res.workspaceHeaderCss();
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));

      // Determine if we should use the awesome box
      awesomeBoxComponentHostView = new AwesomeBoxComponentHost.View(Elements.asJsElement(
          awesomeBoxContainer), res.awesomeBoxHostCss());

      // Create the run button.
      runImageButton = new ImageButton.Builder(res).setImage(res.runIcon())
          .setElement((elemental.html.AnchorElement) runButton).build();
      runImageButton.getView().getImageElement().addClassName(res.workspaceHeaderCss().runIcon());

      if (detached) {
        CssUtils.setDisplayVisibility2(Elements.asJsElement(rightButtons), false);
      }

      //install our plugins
      ClientPlugin<?>[] plugins = ClientPluginService.getPlugins();
      ImageButton[] buttons = new ImageButton[plugins.length];
      for (int i = 0; i < plugins.length; i++) {
        final ClientPlugin<?> plugin = plugins[i];
        elemental.dom.Element holder = Elements.createDivElement(
          res.workspaceHeaderCss().pluginButtonContainer()
          ,res.workspaceHeaderCss().paddedButton()
          );
        (Elements.asJsElement(pluginContainer)).appendChild(holder);
        elemental.dom.Element link = Elements.createElement("a",res.workspaceHeaderCss().pluginButton());
        holder.getStyle().setWidth(60,"px");
        link.getStyle().setLeft(i*40,"px");
        holder.appendChild(link);

        final ImageButton button = new ImageButton.Builder(res).setImage(plugins[i].getIcon(res))
        .setElement((elemental.html.AnchorElement) link).build();
        buttons[i] = button;
        button.getView().getImageElement().addClassName(res.workspaceHeaderCss().pluginIcon());
        button.setListener(new ImageButton.Listener() {
          @Override
          public void onClick() {
            if (getDelegate() != null) {
              getDelegate().onPluginButtonClicked(plugin, button);
            }
          }
        });
      }
      // Create the gear button.

      // Create the terminal button
//      terminalImageButton = new ImageButton.Builder(res).setImage(res.terminalIcon())
//          .setElement((elemental.html.AnchorElement) terminalButton).build();
//      terminalImageButton.getView().getImageElement().addClassName(res.workspaceHeaderCss().terminalIcon());


      newWorkspaceImageButton = new ImageButton.Builder(res).setImage(res.trunkBranchIcon())
          .setElement((elemental.html.AnchorElement) newWorkspaceButton).setText("Branch & Edit")
          .build();
      newWorkspaceImageButton.getView()
          .getImageElement().addClassName(res.workspaceHeaderCss().newWorkspaceButton());

      new ImageButton.Builder(res).setImage(res.readOnlyMessageIcon())
          .setElement((elemental.html.AnchorElement) readOnlyMessage).setText("Read Only").build();

      // Create the run drop down button.
      runDropdownImageButton = new ImageButton.Builder(res).setImage(res.runDropdownIcon())
          .setElement((elemental.html.AnchorElement) runDropdownButton).build();

      // Create the share button. Wait to set the icon until we know the
      // workspace's visibility
      // settings.
      shareImageButton = new ImageButton.Builder(res).setText("Share")
          .setElement((elemental.html.AnchorElement) shareButton).build();
      setShareButtonVisible(true);
      new DebugAttributeSetter().setId(DebugId.WORKSPACE_HEADER_SHARE_BUTTON)
          .on(shareImageButton.getView().getElement());

      // Tooltips
      Tooltip.create(res, Elements.asJsElement(runButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Preview file or application");

      Tooltip.create(res, Elements.asJsElement(runDropdownButton), VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE, "Set custom preview target");

      newWorkspaceTooltip = Tooltip.create(res,
          Elements.asJsElement(newWorkspaceButton),
          VerticalAlign.BOTTOM,
          HorizontalAlign.MIDDLE,
          "In Collide, code changes happen in branches.",
          "Click here to create your own editable branch of the top-level project source code.");
      newWorkspaceTooltip.setMaxWidth("150px");

      // Wire up event handlers.
      attachHandlers();
    }

    public AwesomeBoxComponentHost.View getAwesomeBoxView() {
      return awesomeBoxComponentHostView;
    }

    public void createReadOnlyMessageTooltip(Workspace workspace) {
//      boolean isTrunk = WorkspaceType.TRUNK == workspace.getWorkspaceType();
//      String text;
//      if (isTrunk) {
//        text = "Click 'Branch & Edit' to make changes to Trunk";
//      } else if (WorkspaceType.SUBMITTED == workspace.getWorkspaceType()) {
//        text = "This branch has been submitted to trunk and cannot be modified";
//      } else {
//        text = "You have read access to this branch. Contact the owner to gain contributor access.";
//      }
//      Tooltip readOnlyMessageTooltip = Tooltip.create(
//          res, Elements.asJsElement(readOnlyMessage), VerticalAlign.BOTTOM, HorizontalAlign.MIDDLE,
//          text);
//      if (isTrunk) {
//        readOnlyMessageTooltip.setMaxWidth("150px");
//      }
    }

    protected void attachHandlers() {

      runImageButton.setListener(new ImageButton.Listener() {
        @Override
        public void onClick() {
          if (getDelegate() != null) {
            getDelegate().onRunButtonClicked();
          }
        }
      });

      runDropdownImageButton.setListener(new ImageButton.Listener() {
        @Override
        public void onClick() {
          if (getDelegate() != null) {
            getDelegate().onRunDropdownButtonClicked();
          }
        }
      });

      shareImageButton.setListener(new ImageButton.Listener() {
        @Override
        public void onClick() {
          if (getDelegate() != null) {
            getDelegate().onShareButtonClicked();
          }
        }
      });

      getElement().setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          ViewEvents delegate = getDelegate();
          if (delegate == null) {
            return;
          }

          Element target = (Element) evt.getTarget();
          if (feedbackButton.isOrHasChild(target)) {
            delegate.onFeedbackButtonClicked();
          } else if (newWorkspaceButton.isOrHasChild(target)) {
            delegate.onNewWorkspaceButtonClicked();
          }
        }
      });
    }


    void setReadOnly(boolean isReadOnly) {
      if (isReadOnly) {
        CssUtils.setDisplayVisibility2(
            Elements.asJsElement(readOnlyMessage), true, false, "inline-block");
      } else {
        CssUtils.setDisplayVisibility2(Elements.asJsElement(readOnlyMessage), false);
      }
    }

    void setShareButtonVisible(boolean isVisible) {
      CssUtils.setDisplayVisibility2(Elements.asJsElement(shareButton), isVisible);
    }

    void setRunButtonVisible(boolean isVisible) {
      CssUtils.setDisplayVisibility2(Elements.asJsElement(runButton), isVisible);
      CssUtils.setDisplayVisibility2(Elements.asJsElement(runDropdownButton), isVisible);
    }

    public Element getProfileImage() {
      return profileImage;
    }
  }

  /**
   * Events reported by the Header's View.
   */
  private interface ViewEvents {
    void onPluginButtonClicked(ClientPlugin<?> plugin, ImageButton button);

    void onRunButtonClicked();

    void onRunDropdownButtonClicked();

    void onFeedbackButtonClicked();

    void onShareButtonClicked();

    void onNewWorkspaceButtonClicked();
  }

  /**
   * The delegate implementation for handling events reported by the View.
   */
  private class ViewEventsImpl implements ViewEvents {

    @Override
    public void onRunButtonClicked() {
      runButtonController.onRunButtonClicked();
    }

    @Override
    public void onRunDropdownButtonClicked() {
      runButtonController.onRunButtonDropdownClicked();
    }

    @Override
    public void onPluginButtonClicked(ClientPlugin<?> plugin, ImageButton button) {
      plugin.onClicked(button);
    }

    @Override
    public void onFeedbackButtonClicked() {
      // TODO: something.
    }

    @Override
    public void onShareButtonClicked() {
      // was show manage membership
    }

    @Override
    public void onNewWorkspaceButtonClicked() {
      // was new workspace overlay
    }
  }

  private final AwesomeBoxComponentHost awesomeBoxComponentHost;
  private final RunButtonController runButtonController;

  private static final int PORTRAIT_SIZE_HEADER = 28;

  private Header(View view,
      Place currentPlace,
      AppContext appContext,
      RunButtonController runButtonController,
      AwesomeBoxComponentHost awesomeBoxComponentHost) {
    super(view);
    this.runButtonController = runButtonController;
    this.awesomeBoxComponentHost = awesomeBoxComponentHost;

    setProfileImage();

    view.setDelegate(new ViewEventsImpl());
  }

  public AwesomeBoxComponentHost getAwesomeBoxComponentHost() {
    return awesomeBoxComponentHost;
  }

  private void setProfileImage() {
    String url = BootstrapSession.getBootstrapSession().getProfileImageUrl();
    if (url == null)
      return;
    ImageElement pic = Elements.createImageElement(getView().css.profileImage());
    pic.setSrc(UserDetails.Utils.getSizeSpecificPortraitUrl(
        url , PORTRAIT_SIZE_HEADER));
    Elements.asJsElement(getView().getProfileImage()).appendChild(pic);
  }
}
