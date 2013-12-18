package collide.gwtc.resources;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import collide.client.filetree.FileTreeNodeRenderer;

// A resource bundle is a container for css, images and other media compiled into the app.
public interface GwtcResources extends 
  ClientBundle, FileTreeNodeRenderer.Resources
  {
  @Source("Gwtc.css")
  GwtcCss panelHeaderCss();
  
  @Source("gwt-logo-small.png")
  ImageResource gwt();
  @Source("gear.png")
  ImageResource gear();
  @Source("green-radar-small.gif")
  ImageResource radarGreenAnim();
  @Source("red-radar-small.gif")
  ImageResource radarRedAnim();
  @Source("yellow-radar-small.gif")
  ImageResource radarYellowAnim();
  @Source("green-radar-still.png")
  ImageResource radarGreenSmall();
  @Source("red-radar-still.png")
  ImageResource radarRedSmall();
  @Source("yellow-radar-still.png")
  ImageResource radarYellowSmall();
  @Source("reload.png")
  ImageResource reload();
  @Source("close.png")
  ImageResource close();
  
}