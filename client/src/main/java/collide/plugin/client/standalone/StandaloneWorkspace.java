package collide.plugin.client.standalone;

import collide.client.util.Elements;
import collide.plugin.client.common.ZIndexService;
import com.google.collide.client.CollideSettings;
import com.google.collide.client.Resources;
import com.google.collide.client.code.FileContent;
import com.google.collide.client.code.NoFileSelectedPanel;
import com.google.collide.client.code.PluginContent;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.panel.AbstractPanelPositioner;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.Panel;
import com.google.collide.client.ui.panel.Panel.Interpolator;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.ResizeBounds.BoundsBuilder;
import com.google.collide.client.util.ResizeController;
import com.google.collide.client.util.ResizeController.ElementInfo;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.ShowableUiComponent;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DivElement;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import xapi.log.X_Log;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class StandaloneWorkspace extends MultiPanel<StandaloneWorkspace.Model,StandaloneWorkspace.View> {

  private static class Namespace {
    static final String workspace = "Workspace";
    static final String fileNav = "Explorer";
    static final String header = "Header";
  }

  public static class Model extends PanelModel {
    private Model(boolean showHistory, boolean showClose, boolean showSettings, boolean showCollapse,
      boolean clearNavigator) {
      super(showHistory, showClose, showSettings, showCollapse, clearNavigator);
    }

    public static Builder newStandaloneBuilder() {
      return new Builder();
    }

    public static class Builder extends PanelModel.Builder<Model> {

      @Override
      public Model build() {
        return new Model(historyIcon, closeIcon, settingsIcon, collapseIcon, clearNavigator);
      }

    }

  }

  public static class View extends MultiPanel.View<Model> {
    private final Element root, dummy;
    private final Panel<?,?> headerPanel;
    private final Panel<?,?> fileNavPanel;
    private final Panel<?,?> workspacePanel;

//    private final MapFromStringTo<Panel<?,?>> panels;
    private final AbstractPanelPositioner positioner;
    private final Resources resources;
    private final ZIndexService zIndexer;
    private boolean hidden = false;

    public View(Resources res, Element root, CollideSettings settings) {
      super(root, true);
//      panels = Collections.mapFromStringTo();
      positioner = new AbstractPanelPositioner();
      this.resources = res;
      this.root = root;
      this.zIndexer = new ZIndexService();

      dummy = Elements.createDivElement(res.panelCss().hidden());
      root.appendChild(dummy);
      final DivElement header = Elements.createDivElement();
      final DivElement files = Elements.createDivElement();
      final DivElement workspace = Elements.createDivElement();

      header.setId(StandaloneConstants.HEADER_PANEL);
      files.setId(StandaloneConstants.FILES_PANEL);
      workspace.setId(StandaloneConstants.WORKSPACE_PANEL);
      setElement(root);

      Interpolator inter = new Interpolator() {
        @Override
        public float interpolate(float value) {
          return value - 20;
        }
      };

      // add a full size 100%x100% sizing panel to root @ z-index -1.
      // this is so we don't force users to have a bounds-limited body element
      final DivElement caliper = Elements.createDivElement(res.panelCss().caliper());
      root.appendChild(caliper);

      // set a window-resizing method
      EventListener listener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          caliper.getStyle().setWidth(Elements.getWindow().getInnerWidth(), "px");
          caliper.getStyle().setHeight(Elements.getWindow().getInnerHeight(), "px");
          // TODO also fire a redraw event for all attached panels
          positioner.recalculate();
        }
      };
      Elements.getWindow().setOnresize(listener);

      fileNavPanel = getOrMakePanel(Namespace.fileNav, new PanelContent.AbstractContent(files),
        ResizeBounds
          .withMinSize(275, 300)
          .maxSize(375, 0)
          .maxHeight(caliper, inter)
        );
//      fileNavPanel.setPosition(-55, 5);
      // This is to workaround the main collide entry point's default css
      fileNavPanel.getChildAnchor().getParentElement().getStyle().setTop(-28, "px");
      fileNavPanel.getView().getElement().getStyle().setHeight(Elements.getWindow().getInnerHeight()-20, "px");
      fileNavPanel.setHeaderContent(Namespace.fileNav);

      workspacePanel = getOrMakePanel(Namespace.workspace, new PanelContent.AbstractContent(workspace),
        ResizeBounds.withMinSize(450, 300)
        .maxSize(900, -1)
        .maxHeight(caliper, inter));
      workspacePanel.setClosable(true);
//      workspacePanel.setPosition(405, 5);
      workspacePanel.setHeaderContent(Namespace.workspace);
      hideWorkspace();

      headerPanel = getOrMakePanel(Namespace.header, new PanelContent.AbstractContent(header),
        ResizeBounds.withMaxSize(200, 59).minSize(150, 59));
      headerPanel.setAllHeader();
      headerPanel.setPosition(-5, 0);

      workspacePanel.getView().getElement().getStyle().setWidth("100%");
      workspacePanel.getView().getElement().getStyle().setHeight("100%");

      zIndexer.setAlwaysOnTop(headerPanel.getView().getElement());
      // restore state...
      zIndexer.setNextZindex(workspacePanel.getView().getElement());
      zIndexer.setNextZindex(fileNavPanel.getView().getElement());

      // resize immediately
      listener.handleEvent(null);

      if (settings.isHidden()) {
        hide();
      }

    }

    @Override
    public Element getContentElement() {
      return root;
    }

    @Override
    public Element getHeaderElement() {
      return headerPanel.getView().getElement();
    }

    protected Panel<?,?> getOrMakePanel(String namespace, final PanelContent panelContent, BoundsBuilder bounds) {
      final Panel<?,?> panel;
      if (positioner.hasPanel(namespace)) {
        panel = positioner.getPanel(namespace).panel;
        if (hidden || panelContent instanceof PanelContent.HiddenContent) {
          panel.hide();
        }
        if (panel.getView().getElement().getParentElement() == dummy) {
          root.appendChild(panel.getView().getElement());
        }
      }
      else {
        panel = Panel.create(namespace, resources, bounds.build());
        // only top-level panels are fixed.
        panel.getView().getElement().getStyle().setPosition("fixed");
        if (namespace.equals(Namespace.workspace)) {
          panel.hide();
        }
        if (!namespace.equals(Namespace.header))
          positioner.addPanel(panel);
        if (hidden || shouldHide(panelContent.getContentElement().getId())) {
          panel.hide();
          dummy.appendChild(panel.getView().getElement());
        } else {
          root.appendChild(panel.getView().getElement());
        }
        attachResizeHandler(panel, namespace, panelContent);
        // Position this panel in a sane manner
      }
      panel.setContent(panelContent);
      panel.setHeaderContent(namespace);
      zIndexer.setNextZindex(panelContent.getContentElement());
      return panel;
    }

    private void attachResizeHandler(final Panel<?,?> panel, final String namespace, final PanelContent panelContent) {
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
        @Override
        public void execute() {
          panel.addResizeHandler(new ResizeController.ResizeEventHandler() {
            @Override
            public void startDragging(ElementInfo ... elementInfos) {
              zIndexer.setNextZindex(panel.getView().getElement());
            }

            @Override
            public void whileDragging(float deltaW, float deltaH, float deltaX, float deltaY) {
              // Notify the positioner of resizes.
              if (Math.abs(deltaW)>0.001 || Math.abs(deltaX) > 0.001) {
                positioner.adjustHorizontal(panel, deltaX, deltaW);
              }
              if (Math.abs(deltaH)>0.001 || Math.abs(deltaY) > 0.001) {
                positioner.adjustVertical(panel, deltaY, deltaH);
              }
              // TODO periodically run a z-index check and adapt as needed
            }

            @Override
            public void doneDragging(float deltaX, float deltaY, float origX, float origY) {
              // if this panel currently covers another one completely, we should raise the concealed panels
              detectCollisions(panel, namespace, panelContent);
              positioner.recalculate();
            };
          });
        }
      });
    }

    protected void detectCollisions(Panel<?,?> panel, String namespace, PanelContent panelContent) {
      JsoArray<String> keys = positioner.keys();
      ArrayOf<Panel<?,?>> collisions = Collections.arrayOf();
      for (int i = keys.size(); i-->0;) {//count backwards to maintain order when we re-index
        String key = keys.get(i);
        if (!key.equals(namespace)) {
          Panel<?,?> test = positioner.getPanel(key).panel;
          if (covers(panel, test)) collisions.push(test);
        }
      }
      for (int i = 0; i < collisions.length(); i++) {
        zIndexer.setNextZindex(collisions.get(i).getView().getElement());
      }
    }


    private void hide() {
      hidden = true;
      //also do the mapped panels.
      JsoArray<String> keys = positioner.keys();
      while(!keys.isEmpty()){
        positioner.getPanel(keys.pop()).panel.hide();
      }
    }

    private void show() {
      hidden = false;
      //also do the mapped panels.
      JsoArray<String> keys = positioner.keys();
      PositionerBuilder builder = new PositionController.PositionerBuilder();
      while(!keys.isEmpty()) {
        Panel<?,?> panel = positioner.getPanel(keys.pop()).panel;
        panel.show();
      }
    }

    private boolean covers(Panel<?,?> moved, Panel<?,?> covered) {
      Element elMoved = moved.getView().getElement();
      Element elCovered = covered.getView().getElement();

      // first and easiest, if elCovered is a higher zIndex, do nothing.
      if (elCovered.getStyle().getZIndex() - elMoved.getStyle().getZIndex() > 0) {
        return false;
      }

      // now compute overlap. We can be lazy and use offsets because we control the panel wrappers.
      if (elMoved.getOffsetLeft() > elCovered.getOffsetLeft() + 20) {// TODO replace hardcoded 10 w/ snap
                                                                     // values
        return false;
      }
      if (elMoved.getOffsetTop() > elCovered.getOffsetTop() + 20) {
        return false;
      }
      if (elMoved.getOffsetLeft() + elMoved.getOffsetWidth() < elCovered.getOffsetLeft() +
        elCovered.getOffsetWidth() - 20) {
        return false;
      }
      if (elMoved.getOffsetTop() + elMoved.getOffsetHeight() < elCovered.getOffsetTop() +
        elCovered.getOffsetHeight() - 20) {
        return false;
      }
      return true;
    }

    public void hideWorkspace() {
      workspacePanel.hide();
      positioner.removePanel(workspacePanel);
    }
    public void showWorkspace() {
      if (workspacePanel.getView().getElement().getParentElement() == dummy) {
        root.appendChild(workspacePanel.getView().getElement());
      }
      workspacePanel.show();
//      positionPanel(workspacePanel);
    }

  }

  public static interface ViewController {
  }

  @Override
  public void setContent(PanelContent panelContent, PanelModel settings) {
    if (currentContent == panelContent) {
      return;
    }

    if (panelContent == null) {
      //TODO minimize
    } else {
      if (panelContent instanceof FileContent) {
        // When opening file content, we aim to use a new window,
        // if there is room for it.  Otherwise, we minimize the first of:
        // The oldest FileContent currently open
        // The oldest minimizable PanelContent of any kind
        // The file navigator (the default root panel.)


        if (currentContent instanceof FileContent) {
          // There's already one file open.
          currentContent.onContentDestroyed();
          currentContent.getContentElement().removeFromParent();
        }
        getView().workspacePanel.setContent(panelContent);
        currentContent = panelContent;
        currentContent.onContentDisplayed();
        if (panelContent instanceof NoFileSelectedPanel) {
          getView().hideWorkspace();
        } else {
          getView().showWorkspace();
        }
      } else if (panelContent instanceof PluginContent) {
        PluginContent pluggedin = (PluginContent)panelContent;
        // any other command routing through workspace; currently this is where the plugins are living.
        // TODO move them to their own Place
        BoundsBuilder bounds = pluggedin.getBounds();
        Panel<?,?> asPanel = getView().getOrMakePanel(
          pluggedin.getNamespace(), pluggedin, bounds
            .maxHeight(getView().root, Interpolator.NO_OP)
        );
        asPanel.setContent(panelContent);
        panelContent.onContentDisplayed();
      } else {
        Log.warn(getClass(), "Unhandled content of type ", panelContent.getClass(), panelContent);
      }
    }

    setHeaderVisibility(true);
  }

  public static boolean shouldHide(String id) {
    return StandaloneConstants.WORKSPACE_PANEL.equals(id) &&
        !HistoryUtils.getHistoryString().contains(PlaceConstants.WORKSPACE_PLACE_NAME);
  }

  @Override
  public ShowableUiComponent<? extends com.google.collide.mvp.View<?>> getToolBar() {
    return new ShowableUiComponent<View>() {

      @Override
      public void hide() {
        X_Log.trace("Hiding panel");
      }

      @Override
      public void show() {
        X_Log.trace("Showing panel");
      }

    };
  }

  public StandaloneWorkspace(Resources resources, Element root, CollideSettings standaloneSettings) {
    super(new View(resources, root, standaloneSettings));
  }

  public void hide() {
    X_Log.trace("Hiding collide");
    getView().hide();
  }

  public void show() {
    X_Log.trace("Showing collide");
    getView().show();
  }

  public void closeEditor() {
    getView().hideWorkspace();
  }

  public void showEditor() {
    getView().showWorkspace();
  }

  @Override
  public com.google.collide.client.ui.panel.PanelModel.Builder<Model> newBuilder() {
    return new Model.Builder();
  }

}
