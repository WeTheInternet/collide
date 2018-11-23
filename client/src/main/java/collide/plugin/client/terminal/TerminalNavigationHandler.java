package collide.plugin.client.terminal;

import xapi.collect.impl.AbstractInitMap;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.fu.In1Out1;
import xapi.inject.impl.SingletonProvider;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.HasId;
import xapi.util.api.Pair;
import collide.client.util.Elements;
import collide.demo.view.TabPanel;
import collide.demo.view.TabPanel.TabView;
import collide.demo.view.TabPanelResources;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.dto.LogMessage;
import com.google.collide.mvp.View;
import collide.plugin.client.terminal.TerminalLogView.Resources;
import collide.plugin.client.terminal.TerminalLogView.ViewEvents;
import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;
import elemental.html.DivElement;

@SuppressWarnings("ALL")
public class TerminalNavigationHandler extends
    PlaceNavigationHandler<TerminalPlace.NavigationEvent>
    implements In1Out1<Pair<String, TerminalLogHeader>, TerminalLogView> {

  private final AppContext context;
  private final MultiPanel<? extends PanelModel,?> contentArea;
  private final Place parentPlace;
  private final SingletonProvider<Resources> terminalResources;

  private final AbstractMultiInitMap<String, TerminalLogView, TerminalLogHeader> views;
  private final TabPanel logTabs;

  public TerminalNavigationHandler(AppContext context, final MultiPanel<?,?> masterPanel, Place parentPlace) {
    this.context = context;
    logTabs = TabPanel.create(getTabPanelResources());
    masterPanel.setContent(logTabs);
    this.contentArea = masterPanel;
    this.parentPlace = parentPlace;
    views = new AbstractMultiInitMap<String, TerminalLogView, TerminalLogHeader>(AbstractInitMap.PASS_THRU, this);

    //create and inject our resources lazily
    this.terminalResources = new SingletonProvider<Resources>(){
      @Override
      protected Resources initialValue() {
        Resources res = GWT.create(Resources.class);
        res.terminalLogCss().ensureInjected();
        return res;
      };
    };
  }

  protected TabPanelResources getTabPanelResources() {
    return TabPanel.DEFAULT_RESOURCES.get();
  }

  protected TerminalLogView initializeView(String from, MultiPanel<?,?> masterPanel) {
    final TabView[] tab = new TabView[1];
    final TerminalLogView[] view = new TerminalLogView[1];
    view[0] = TerminalLogView.create(from, terminalResources.get(), new ViewEvents() {
      @Override
      public void onLogsFlushed() {
        if (logTabs.unhide(tab[0])) {
          view[0].clear();
        }
      }
    });
    tab[0] = logTabs.addContent(view[0].getView());
    logTabs.select(tab[0]);
    return view[0];
  }

  @Override
  public void cleanup() {
    contentArea.getToolBar().show();
  }

  @Override
  protected void enterPlace(TerminalPlace.NavigationEvent navigationEvent) {
    contentArea.clearNavigator();

    contentArea.setHeaderVisibility(false);
    String module = navigationEvent.getModule();
    PanelContent panelContent = views.get(module, null);
    contentArea.setContent(panelContent,
      contentArea.newBuilder().setCollapseIcon(true).setClearNavigator(true).build());
    contentArea.getToolBar().hide();
  }

  public Place getCurrentPlace() {
    return parentPlace;
  }

  public void addLog(LogMessage log, TerminalLogHeader header) {
    String id = X_String.firstNotEmpty(log.getModule(),"global");

    views.get(id, header).addLog(log);
  }

  @Override
  public TerminalLogView io(Pair<String, TerminalLogHeader> from) {
    TerminalLogView view = initializeView(from.get0(), contentArea);
    view.setHeader(from.get1());
    return view;
  }

  public void setHeader(String module, TerminalLogHeader header) {
    views.get(module, header).setHeader(header);
  }

  public void setRename(String from, String to) {
    TerminalLogView is = views.getValue(from);
    views.setValue(to, is);
  }

}
