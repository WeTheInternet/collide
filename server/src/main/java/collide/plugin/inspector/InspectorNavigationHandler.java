package collide.plugin.inspector;

import collide.gwtc.ui.GwtCompilerShell.Resources;
import collide.plugin.client.inspector.InspectorPlace;
import collide.plugin.client.inspector.InspectorPlace.NavigationEvent;
import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelModel;
import xapi.fu.Lazy;

import com.google.gwt.core.shared.GWT;

public class InspectorNavigationHandler extends
    PlaceNavigationHandler<InspectorPlace.NavigationEvent> {

  private final AppContext context;
  private final MultiPanel<? extends PanelModel,?> contentArea;
  private final Place currentPlace;
  private final Lazy<Resources> inspectorResources;

  public InspectorNavigationHandler(AppContext context, MultiPanel<?,?> masterPanel, Place currentPlace) {
    this.context = context;
    this.contentArea = masterPanel;
    this.currentPlace = currentPlace;

    //create our view lazily
    this.inspectorResources = Lazy.deferred1(() -> {
        Resources res = GWT.create(Resources.class);
        res.gwtCompilerCss().ensureInjected();
        res.gwtLogCss().ensureInjected();
        res.gwtClasspathCss().ensureInjected();
        res.gwtModuleCss().ensureInjected();
        return res;
    });
  }

  @Override
  public void cleanup() {
    contentArea.getToolBar().show();
  }

  @Override
  protected void reEnterPlace(NavigationEvent navigationEvent,
      boolean hasNewState) {
    super.reEnterPlace(navigationEvent, hasNewState);
  }

  @Override
  protected void enterPlace(InspectorPlace.NavigationEvent navigationEvent) {
    contentArea.clearNavigator();

    contentArea.setHeaderVisibility(false);

//    String module = navigationEvent.getModule();
//    PanelContent panelContent = views.get(module, null);
//    contentArea.setContent(panelContent,
//      contentArea.newBuilder().setCollapseIcon(true).setClearNavigator(true).build());
    contentArea.getToolBar().hide();
  }

  public Place getCurrentPlace() {
    return currentPlace;
  }

}
