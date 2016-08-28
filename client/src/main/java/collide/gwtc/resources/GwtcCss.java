package collide.gwtc.resources;

import com.google.gwt.resources.client.CssResource;

// An interface of css classnames.  These must match .classNames in the .css file.
public interface GwtcCss extends CssResource {
  String close();
  String gwt();
  String gear();
  String headerContainer();
  String header();
  String icons();
  String controls();
  String reloadIcon();
  
  String radarGreen();
  String radarYellow();
  String radarRed();
  
  String success();
  String warn();
  String fail();
  
  String bottomHeader();
  String topHeader();
}