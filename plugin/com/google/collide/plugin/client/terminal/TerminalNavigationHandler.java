package com.google.collide.plugin.client.terminal;

import xapi.collect.impl.AbstractInitMap;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.inject.impl.SingletonProvider;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.Pair;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.dto.LogMessage;
import com.google.collide.plugin.client.terminal.TerminalLogView.Resources;
import com.google.collide.plugin.client.terminal.TerminalLogView.ViewEvents;
import com.google.gwt.core.shared.GWT;

public class TerminalNavigationHandler extends
    PlaceNavigationHandler<TerminalPlace.NavigationEvent> 
    implements ConvertsValue<Pair<String, TerminalLogHeader>, TerminalLogView> {

  private final AppContext context;
  private final MultiPanel<? extends PanelModel,?> contentArea;
  private final Place parentPlace;
  private final SingletonProvider<Resources> terminalResources;

  private final AbstractMultiInitMap<String, TerminalLogView, TerminalLogHeader> views;
  
  public TerminalNavigationHandler(AppContext context, final MultiPanel<?,?> masterPanel, Place parentPlace) {
    this.context = context;
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

  protected TerminalLogView initializeView(String from, MultiPanel<?,?> masterPanel) {
    TerminalLogView logger = TerminalLogView.create(terminalResources.get(), new ViewEvents() {
      @Override
      public void run() {
        
      }
    });
    masterPanel.setContent(logger);
    return logger;
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
    views.get(X_String.firstNotEmpty(log.getModule(),"global"), header).addLog(log);
  }

  @Override
  public TerminalLogView convert(Pair<String, TerminalLogHeader> from) {
    TerminalLogView view = initializeView(from.get0(), contentArea);
    view.setHeader(from.get1());
    return view;
  }

  public void setHeader(String module, TerminalLogHeader header) {
    views.get(module, header).setHeader(header);
  }

}
