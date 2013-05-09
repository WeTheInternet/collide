package collide.demo.child;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepMethod;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.util.X_Runtime;
import xapi.util.X_Util;
import collide.demo.child.view.PanelHeader;
import collide.demo.shared.SharedClass;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;

@KeepClass
public class ChildModule implements EntryPoint {
static class Jso extends JavaScriptObject{
protected Jso(){}
}
  public static void main(String[] args) {
    new ChildModule().onModuleLoad();
  }
  @Override
  public void onModuleLoad() {
    PanelHeader head = PanelHeader.create();
    
//    Browser.getDocument().appendChild(head.getElement());
//    head.getElement().setOnclick(new EventListener() {
      
//      @Override
//      public void handleEvent(Event evt) {
//        Window.alert("Oh hai");
//      }
//    });
    
    try {
      out("Hello world!!!\n\n");
      // First, enhance our classes.
      X_Reflect.magicClass(SharedClass.class);
      // Log to console for inspection
      out(SharedClass.class);
      out("\n");
      // Now, play with it a little
      SharedClass instance = new SharedClass();
      fieldTest(instance);
      methodTest(instance);
      constructorTest(instance);
      codesourceTest(instance);
      sharedObjectTest(instance);
    } catch (Throwable e) {
      fail(e);
    }
  }

  private final Method out;

  ChildModule() {
    try {
      out = X_Runtime.isJava() 
          ? X_Log.class.getMethod("info", Object[].class)
          : X_Reflect.magicClass(ChildModule.class)
            .getMethod("jsOut", Object[].class);
    } catch (Exception e) {
      throw X_Util.rethrow(e);
    }
  }

  private void out(Object o) throws Exception {
    o = new Object[] { o };
    out.invoke(this, o);
  }

  @KeepMethod
  private native void jsOut(Object[] o)
  /*-{ 
    $wnd.console.log(o[0])
    var pre = $doc.createElement('pre')
    pre.innerHTML = o[0]
    $doc.body.appendChild(pre)
  }-*/;

  void fieldTest(SharedClass test) throws Exception {
    Field field = SharedClass.class.getField("sharedInt");
    int was = test.sharedInt;
    out("Changing " + field + " from " + field.get(test) + " to 3;");
    field.set(test, 3);
    out("See: " + test.sharedInt + " != " + was);
    out("\n");
  }

  void methodTest(SharedClass test) throws Exception {
    Method doStuff = test.getClass().getDeclaredMethod("doStuff");
    doStuff.setAccessible(true);
    doStuff.invoke(test);
    out(doStuff);
    out("\n");
  }

  void constructorTest(SharedClass test) throws Exception {
    Constructor<SharedClass> ctor = SharedClass.class.getConstructor();
    out(ctor);
    SharedClass other = ctor.newInstance();
    if (other == null)
      throw new RuntimeException("New instance returned null");
    assert other != test;
    out("\n");
  }

  void codesourceTest(SharedClass test) throws Exception {
    out("Compiled from:\n"
        + test.getClass().getProtectionDomain().getCodeSource().getLocation()
            .toExternalForm());
  }

  void sharedObjectTest(SharedClass test) {
    
  }

  native void sendObject(Class<SharedClass> cls, SharedClass test)
  /*-{
    $wnd.top.Callback(cls, test);
  }-*/;

  void fail(Throwable e) {
    try{
      while (e != null) {
        out(e.toString());
        for (StackTraceElement el : e.getStackTrace()) {
          out(el.toString());
        }
        e = e.getCause();
      }
    } catch (Exception ex){ex.printStackTrace();}
  }

}
