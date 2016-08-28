package collide.demo.view;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface TabPanelResources extends ClientBundle {

  @Source("TabView.css")
  TabPanelCss css();
  
  @Source("close.png")
  ImageResource closeIcon();
}
