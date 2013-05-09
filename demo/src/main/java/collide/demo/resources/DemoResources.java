package collide.demo.resources;

import collide.demo.view.SplitPanel;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.ClientBundle;

public interface DemoResources extends ClientBundle{

  final DemoResources GLOBAL = GWT.create(DemoResources.class);
  
  @Source("SplitPanel.css")
  SplitPanel.Css splitPanel();
  
}
