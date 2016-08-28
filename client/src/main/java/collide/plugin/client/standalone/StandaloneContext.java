package com.google.collide.plugin.client.standalone;

import collide.client.util.Elements;

import com.google.collide.client.AppContext;
import com.google.collide.client.CollideSettings;

public class StandaloneContext {
  static StandaloneContext create(AppContext ctx) {
    return new StandaloneContext(ctx);
  }

  private final StandaloneWorkspace panel;
  private final AppContext ctx;


  public StandaloneContext(AppContext ctx) {
    this.ctx = ctx;
    this.panel = new StandaloneWorkspace(ctx.getResources(), Elements.getBody(), CollideSettings.get());
  }

  /**
   * @return the panel
   */
  public StandaloneWorkspace getPanel() {
    return panel;
  }

  public AppContext getAppContext() {
    return ctx;
  }

}
