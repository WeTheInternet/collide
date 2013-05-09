package collide.demo.child.view;

import xapi.inject.impl.SingletonProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;


public class PanelHeader {

  // The elements from our .ui.xml file.  These are filled in by the compiler.
  @UiField Element headerContainer;
  @UiField Element header;
  @UiField Element controls;
  @UiField Element icons;
  // This is our css data; the compiler uses this object when filling in values.
  @UiField(provided=true) final Resources res;

  
  public PanelHeader(Resources res) {
    // Allow users to override default css.
    this.res = res == null ? resources.get() : res;
    // Calls into the generated ui binder, creating html elements and filling in our values.
    binder.createAndBindUi(this);
  }

  /**
   * Creates a new panel header with the given resources, all of which may be null.
   * 
   * @param moduleContainer - The element in which to append the header. 
   * If null, you must attach the header somewhere.
   * @param res - Css resource overrides.  If null, our defaults are applied.
   * @param model - A model for the panel, which contains values like isMaximizable, isClosable, etc.
   * 
   * @return - A PanelHeader widget.
   */
  public static PanelHeader create() {
    PanelHeader mod = new PanelHeader(null);
    return mod;
  }


  // This is a class that binds our ui.xml file to PanelHeader class
  @UiTemplate("PanelHeader.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, PanelHeader> {}

  // This is a generated instance of the above interface. It fills in this class's values.
  static MyBinder binder = GWT.create(MyBinder.class);

  // An interface of css classnames.  These must match .classNames in the .css file.
  public interface Css extends CssResource {
    String headerContainer();
    String header();
    String icons();
    String controls();
  }

  // A resource bundle is a container for css, images and other media compiled into the app.
  public interface Resources extends 
    ClientBundle 
    {
    @Source("PanelHeader.css")
    Css panelHeaderCss();
  }
  
  // A provider for our resources; this object will create one and only one copy of resources,
  // and make sure the css is injected into the hostpage before returning the resource provider.
  private static final SingletonProvider<Resources> resources = new SingletonProvider<Resources>(){
    protected Resources initialValue() {
      Resources res = GWT.create(Resources.class);
      res.panelHeaderCss().ensureInjected();
      return res;
    };
  };


  public elemental.dom.Element getElement() {
    return (elemental.dom.Element)headerContainer;
  }
  
  

}
