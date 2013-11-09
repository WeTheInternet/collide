package collide.gwtc.view;

import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import collide.gwtc.GwtCompileStatus;
import collide.gwtc.GwtcController;
import collide.gwtc.resources.GwtcResources;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Window;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.js.dom.JsElement;


public class GwtcModuleControlView {

  // This is a class that binds our ui.xml file to GwtcModuleControlView class
  @UiTemplate("GwtcModuleControlView.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, GwtcModuleControlView> {}

  // This is a generated instance of the above interface. It fills in this class's values.
  static MyBinder binder = GWT.create(MyBinder.class);
  
  // A provider for our resources; this object will create one and only one copy of resources,
  // and make sure the css is injected into the hostpage before returning the resource provider.
  private static final SingletonProvider<GwtcResources> resources = new SingletonProvider<GwtcResources>(){
    protected GwtcResources initialValue() {
      GwtcResources res = GWT.create(GwtcResources.class);
      res.panelHeaderCss().ensureInjected();
      return res;
    };
  };
  /**
   * Creates a new panel header with the given resources, all of which may be null.
   * 
   * @param moduleContainer - The element in which to append the header. 
   * If null, you must attach the header somewhere.
   * @param res - GwtcCss resource overrides.  If null, our defaults are applied.
   * @param model - A model for the panel, which contains values like isMaximizable, isClosable, etc.
   * 
   * @return - A GwtcModuleControlView widget.
   */
  public static GwtcModuleControlView create(GwtcController controller) {
    return new GwtcModuleControlView(null, controller);
  }


  // The elements from our .ui.xml file.  These are filled in by the compiler.
  @UiField public Element header;
  @UiField Element headerContainer;
  @UiField Element controls;
  @UiField Element close;
  @UiField Element icons;
  @UiField Element reload;
  @UiField Element status;
  // This is our css data; the compiler uses this object when filling in values.
  @UiField(provided=true) final GwtcResources res;

  private GwtCompileStatus currentStatus = GwtCompileStatus.Pending;

  public GwtcModuleControlView(GwtcResources res, final GwtcController controller) {
    // Allow users to override default css.
    this.res = res == null ? resources.get() : res;
    // Calls into the generated ui binder, creating html elements and filling in our values.
    binder.createAndBindUi(this);
    
    reload.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        controller.onRefreshClicked();
      }
    });
    close.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        controller.onCloseClicked();
      }
    });
    status.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (currentStatus == null) {
          controller.onReloadClicked();
          return;
        }
        switch (currentStatus) {
          case PartialSuccess:
          case Pending:
          case Success:
          case Fail:
            controller.onReloadClicked();
          default:
            // TODO warn that a compile is in progress
        }
      }
    });
  }

  public elemental.dom.Element getElement() {
    return (elemental.dom.Element)headerContainer;
  }
  
  public void setCompileStatus(GwtCompileStatus compileStatus) {
    assert compileStatus !=  null;
    X_Log.info("Setting compile status", compileStatus.name(), this);
    if (currentStatus == compileStatus) {
      return;
    }
    status.removeClassName(classFor(currentStatus));
    status.addClassName(classFor(compileStatus));
    currentStatus = compileStatus;
  }

  private String classFor(GwtCompileStatus status) {
    switch (status) {
      case Pending:
        return res.panelHeaderCss().gear();
      case Success:
        return res.panelHeaderCss().success();
      case PartialSuccess:
        return res.panelHeaderCss().warn();
      case Good:
        return res.panelHeaderCss().radarGreen();
      case Warn:
        return res.panelHeaderCss().radarYellow();
      case Error:
        return res.panelHeaderCss().radarRed();
      case Fail:
        return res.panelHeaderCss().fail();
    }
    throw new IllegalStateException();
  }

  public void setHeader(String id) {
    header.setInnerHTML(id);
  }
  
}
