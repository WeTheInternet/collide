package collide.junit.cases;

import java.util.Arrays;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

public class SourceVisitor {


  protected boolean arraysEqualPrimitive(Object v1, Object v2) {
    // Fast at runtime; correct at test time.
    assert v1 != null : new NullPointerException();
    assert v1.getClass().getComponentType().isPrimitive() :
      "Non primitive array sent as first arg to arrayEqualPrimitive in "+getClass()+": "+v1;
    assert v2 != null : new NullPointerException();
    assert v2.getClass().getComponentType().isPrimitive() :
      "Non primitive array sent as second arg to arrayEqualPrimitive in "+getClass()+": "+v2;
    return nativePrimitivesEqual(v1, v2);
  }

  protected native boolean nativePrimitivesEqual(Object v1, Object v2)
  /*-{
    var i = v1.length; // v1 never null
    if (v2 == null || v2.length != i) return false;
    for (; i-- > 0;) {
      if (v1[i] != v2[i]) {// we're only sending non-long primitives here
        return false;
      }
    }
    return true;
  }-*/;

  protected boolean arraysEqualLong(Object zero, Object one) {
    assert zero instanceof long[] : "Non-long array as first argument to arraysEqualLong in "+getClass()
      +":\n"+zero;
    assert one instanceof long[] : "Non-long array as second argument to arraysEqualLong in "+getClass()
      +":\n"+one;
    return Arrays.equals((long[])zero, (long[])one);
  }

  protected boolean arraysEqualObject(Object zero, Object one) {
    assert zero instanceof Object[] : "Non-Object array as first argument to arraysEqualObject in "+getClass()
      +":\n"+zero+" "+zero.getClass();
    assert one instanceof Object[] : "Non-Object array as second argument to arraysEqualObject in "+getClass()
      +":\n"+one+" "+one.getClass();
    return Arrays.equals((Object[])zero, (Object[])one);
  }


  @UnsafeNativeLong
  protected static native long getLong(JavaScriptObject object, String member)
  /*-{
    return object[member];
  }-*/;



}
