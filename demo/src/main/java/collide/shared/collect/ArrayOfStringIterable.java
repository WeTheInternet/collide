package collide.shared.collect;

import java.util.Iterator;

import xapi.log.X_Log;

import com.google.gwt.reflect.client.GwtReflect;

import elemental.js.util.JsArrayOf;
import elemental.js.util.JsArrayOfString;
import elemental.util.ArrayOf;
import elemental.util.ArrayOfString;
import elemental.util.impl.JreArrayOfString;

public class ArrayOfStringIterable implements Iterable<String> {

  ArrayOf<String> array;
  
  public ArrayOfStringIterable(ArrayOfString strings) {
    if (strings == null) {
      throw new NullPointerException("Strings array cannot be null");
    }
    if (strings instanceof JsArrayOfString) {
      array = ((JsArrayOfString) strings).<JsArrayOf<String>>cast();
    } else {
      try {
        array = GwtReflect.fieldGet(JreArrayOfString.class, "array", strings);
      } catch (Exception e) {
        X_Log.error("Could not get inner array field of "+strings.getClass(), e);
      }
    }
  }

  @Override
  public Iterator<String> iterator() {
    return new ArrayOfIterator<String>(array);
  }

}
