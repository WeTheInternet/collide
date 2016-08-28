package com.google.collide.client.code;

import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.util.ResizeBounds.BoundsBuilder;

public interface PluginContent extends PanelContent{

  /**
   * @return The string key used to identify a given plugin;
   * this value should be human readable, and machine friendly.
   *
   * It will be used as header-groups for content panels
   */
  String getNamespace();
  
  BoundsBuilder getBounds();

}
