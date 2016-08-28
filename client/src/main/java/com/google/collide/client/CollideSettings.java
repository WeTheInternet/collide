package com.google.collide.client;

import com.google.gwt.core.client.JavaScriptObject;

public class CollideSettings extends JavaScriptObject {
  protected CollideSettings() {}
  public static native CollideSettings get()
  /*-{
     return $wnd.collide || {};
   }-*/;

  public final native String getMode()
  /*-{
    return this.mode;
  }-*/;
  
  public final native String getModule()
  /*-{
    return this.module;
  }-*/;
  
  public final native String getOpenFile()
  /*-{
    return this.open;
  }-*/;

  public final boolean isHidden() {
    return "hidden".equals(getMode());
  }
}