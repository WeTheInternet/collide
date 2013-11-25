package collide.shared.collect;

import java.util.Iterator;

import elemental.util.ArrayOf;

public class ArrayOfIterable <T> implements Iterable<T> {

  private final ArrayOf<T> array;
  
  public ArrayOfIterable(ArrayOf<T> objects) {
    array = objects;
  }

  @Override
  public Iterator<T> iterator() {
    return new ArrayOfIterator<T>(array);
  }
}
