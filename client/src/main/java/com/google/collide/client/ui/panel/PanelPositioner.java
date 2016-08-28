package com.google.collide.client.ui.panel;

import xapi.util.api.RemovalHandler;

public interface PanelPositioner {

  RemovalHandler addPanel(Panel<?, ?> panel);

  boolean adjustHorizontal(Panel<?, ?> panel, float deltaX, float deltaW);

  boolean adjustVertical(Panel<?, ?> panel, float deltaY, float deltaH);

  void removePanel(Panel<?, ?> panel);

}
