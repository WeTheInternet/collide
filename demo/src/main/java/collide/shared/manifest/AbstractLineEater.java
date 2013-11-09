package collide.shared.manifest;

import collide.shared.manifest.AbstractLineEater.Stack;

public class AbstractLineEater implements LineEater {

  protected static final class Stack {
    LineEater eater;
    Stack parent;
    Stack next, prev;
  }
  protected final Stack head;
  protected Stack tail;
  protected LineEater eater;
  
  public AbstractLineEater() {
    this(null);
  }
  public AbstractLineEater(Stack parent) {
    tail = head = new Stack();
    head.parent = parent;
    eater = head.eater = newStartSection(head);
  }
  
  private class StartSection implements LineEater {
    private String name;

    @Override
    public boolean eat(String line) {
      String startName = isStartLine(line);
      if (startName != null) {
        name = startName;
        Stack curTail = tail;
        assert curTail.next == null;
        curTail.next = new Stack();
        eater = newChildEater(curTail, name);
        curTail.next.eater = eater;
        curTail.next.prev = tail;
        tail = curTail.next;
        return true;
      }
      String endName = isEndLine(line);
      if (endName != null) {
        if (head.parent != null) {
          tail = head.parent;
          eater = tail.eater;
        }
        if ("".equals(endName)) {
          return true;
        }
        return eater.eat(endName);
      }
      return addValue(line);
    }
    
    @Override
    public String toString() {
      return name+":\n";
    }
  }
  

  @Override
  public final boolean eat(String line) {
    return eater.eat(line);
  }

  protected LineEater newChildEater(Stack tail, String name) {
    return this;
  }
  protected boolean addValue(String line) {
    return false;
  }
  
  protected String isEndLine(String line) {
    return line.trim().isEmpty() ? "" : null;
  }

  protected String isStartLine(String line) {
    return line.endsWith(":") ? line.replaceAll(":$", "").trim() : null;
  }

  protected LineEater newStartSection() {
    return new StartSection();
  }

  protected LineEater newChildEater(String name) {
    return new AbstractLineEater(head);
  }
  
  protected LineEater newStartSection(Stack parent) {
    return new StartSection();
  }
  
}
