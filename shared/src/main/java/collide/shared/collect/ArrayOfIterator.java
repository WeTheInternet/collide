package collide.shared.collect;

import java.util.Iterator;

import elemental.util.ArrayOf;

public class ArrayOfIterator<T> implements Iterator<T> {

  private final ArrayOf<T> array;
  private int pos;

  public ArrayOfIterator(ArrayOf<T> array) {
    this.array = array;
  }

  @Override
  public boolean hasNext() {
    return pos < array.length();
  }

  @Override
  public T next() {
    assert pos >= 0;
    assert pos < array.length();
    return array.get(pos++);
  }

  @Override
  public void remove() {
    assert pos > 0;
    assert pos <= array.length();
    array.removeByIndex(--pos);
  }

}
