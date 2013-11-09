package collide.gwtc.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

// A resource bundle is a container for css, images and other media compiled into the app.
public interface GwtcResources extends 
  ClientBundle 
  {
  @Source("Gwtc.css")
  GwtcCss panelHeaderCss();
  
  @Source("gear.png")
  ImageResource gear();
  @Source("green-radar-small.gif")
  ImageResource radarGreenSmall();
  @Source("red-radar-small.gif")
  ImageResource radarRedSmall();
  @Source("yellow-radar-small.gif")
  ImageResource radarYellowSmall();
  @Source("reload.png")
  ImageResource reload();
  
}