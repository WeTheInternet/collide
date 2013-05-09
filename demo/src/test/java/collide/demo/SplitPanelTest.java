package collide.demo;

import collide.demo.view.SplitPanel;

import com.google.gwt.core.client.EntryPoint;

import elemental.client.Browser;
import elemental.dom.Element;

public class SplitPanelTest implements EntryPoint{

  
  @Override
  public void onModuleLoad() {
    
    SplitPanel outer = new SplitPanel(false);
    SplitPanel inner = new SplitPanel(true);
    
    Element 
    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("grey");
    outer.addChild(e, 0.25);
    
    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("yellow");
    inner.addChild(e, 0.25);
    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("red");
    inner.addChild(e, 0.25);
    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("blue");
    inner.addChild(e, 0.25);
    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("green");
    inner.addChild(e, 0.25);
    
    outer.addChild(inner.getElement(), 0.5);

    e = Browser.getDocument().createDivElement();
    e.getStyle().setBackgroundColor("cyan");
    outer.addChild(e, 0.25);
    
    Browser.getDocument().getElementById("gwt_root").appendChild(inner.getElement());
  }
  
}
