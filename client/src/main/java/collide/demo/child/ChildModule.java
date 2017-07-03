package collide.demo.child;

import collide.demo.shared.SharedClass;
import xapi.annotation.reflect.KeepMethod;
import xapi.log.X_Log;
import xapi.util.X_Util;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;
import static xapi.util.X_Runtime.isJava;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.ProtectionDomain;

@ReflectionStrategy(debug=ReflectionStrategy.ALL_MEMBERS | ReflectionStrategy.ALL_ANNOTATIONS)
public class ChildModule implements EntryPoint {

  public static void main(String[] args) {
    new ChildModule().onModuleLoad();
  }
  @Override
  public void onModuleLoad() {
    try {
      out("Hello world!!!!!\n");
      // First, enhance our classes.
      magicClass(SharedClass.class);
      // Log to console for inspection
      out(SharedClass.class);
      out("\n");
      // Now, play with it a little
      SharedClass instance = new SharedClass();
      fieldTest(instance);
      methodTest(instance);
      constructorTest(instance);
      codesourceTest(instance);
      out("Annotation: ");
      out(SharedClass.class.getAnnotation(ReflectionStrategy.class));
      sharedObjectTest(instance);
    } catch (Throwable e) {
      fail(e);
    }
  }

  private final Method out;

  ChildModule() {
    try {
      if (isJava()) {
        out = X_Log.class.getMethod("info", Object[].class);
      } else {
        out = magicClass(ChildModule.class).getDeclaredMethod("jsOut", Object[].class);
      }
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
    out("Field:");
    out(field);
    out("\n");
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
    out("Method:");
    out(doStuff);
    out("\n");
  }

  void constructorTest(SharedClass test) throws Exception {
    Constructor<SharedClass> ctor = SharedClass.class.getConstructor();
    out("Constructor:");
    out(ctor);
    SharedClass other = ctor.newInstance();
    if (other == null)
      throw new RuntimeException("New instance returned null");
    assert other != test;
    out("\n");
  }

  void codesourceTest(SharedClass test) throws Exception {
    ProtectionDomain pd = test.getClass().getProtectionDomain();
    CodeSource cs = pd.getCodeSource();
    String loc = cs.getLocation().toExternalForm();
    out("Compiled from:");
    out(loc);
    out("\n");
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
