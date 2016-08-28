package collide.plugin.client.common;

import collide.client.util.Elements;
import com.google.common.base.Preconditions;
import elemental.dom.Element;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.util.api.ReceivesValue;
import xapi.util.api.RemovalHandler;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

public class ZIndexService {

  private static class ElementalReceiver implements ReceivesValue<Integer> {

    private Element element;
    public ElementalReceiver(Element e) {
      Preconditions.checkNotNull(e,"Do not send null elements to a ZIndexService.");
      this.element = e;
    }
    @Override
    public void set(Integer value) {
      if (element.getStyle().getZIndex() < value)
        element.getStyle().setZIndex(value);
    }

  }

  private ScheduledCommand recalc;
  private final ArrayOf<String> zIndexes;
  private final ArrayOf<ReceivesValue<Integer>> alwaysOnTop;

  public ZIndexService() {
    alwaysOnTop = Collections.arrayOf();
    zIndexes = Collections.arrayOf();
    // always pack 0, so we can ignore it and any elements w/out zindex
    zIndexes.push(Elements.getOrSetId(Elements.getBody()));
  }

  public void setNextZindex(Element e) {
    int zindex = e.getStyle().getZIndex();
    String id = Elements.getOrSetId(e);
    int nextZ = zIndexes.length();// one higher than anything still attached

    if (zindex > 0) {
      if (nextZ == zindex + 1) {
        //already highest. just quit.
        return;
      }
      if (id.equals(zIndexes.get(zindex))) {
        // only remove index when owned by this known element; to avoid user-set zindex collision woes
        if (nextZ == zindex + 2) {
          return;
        }
        zIndexes.removeByIndex(zindex);
      }else {
        // run cleanup to remove leaked ids, so we can trace leaked classes
        zIndexes.remove(id);
      }
    }
    // returned value is one more than the highest index in our ArrayOf<String id>
    zindex = nextZ;
    zIndexes.set(zindex, id);// store the id, not the element

    e.getStyle().setZIndex(zindex);
//    Log.info(getClass(), zIndexes, id+": "+zindex);


    // applies zindex+1 to anything marked AlwaysOnTop
    applyAlwaysOnTop();
  }

  protected void applyAlwaysOnTop() {
    if (recalc == null) {
      recalc = new ScheduledCommand() {
        @Override
        public void execute() {
          recalc = null;
          int zindex = zIndexes.length()+1;
          for (int i = alwaysOnTop.length(); i-- > 0;) {
            alwaysOnTop.get(i).set(zindex);
          }
        }
      };
      Scheduler.get().scheduleFinally(recalc);
    }
  }

  public RemovalHandler setAlwaysOnTop(Element receiver) {
    return setAlwaysOnTop(new ElementalReceiver(receiver));
  }

  public RemovalHandler setAlwaysOnTop(final ReceivesValue<Integer> receiver) {
    assert !alwaysOnTop.contains(receiver) : "You are adding the same receiver to always on top more than once;"
      + " you DO have the returned handler on tap to do a null check, don't you? ;)";
    alwaysOnTop.push(receiver);
    applyAlwaysOnTop();
    return new RemovalHandler() {
      @Override
      public void remove() {
        alwaysOnTop.remove(receiver);
      }
    };
  }

  public void destroy() {
    if (X_Log.loggable(LogLevel.TRACE)) {
      // detect leaks:
      for (int i = 1; // starting at one because we pad 0 with document.body
      i < zIndexes.length(); i++) {
        String id = zIndexes.get(i);
        if (id == null) continue;
        Element attached = Elements.getDocument().getElementById(id);
        if (attached == null) {
          X_Log.trace("Leaked zindex assignment; possible memory leak in detached element w/ id " + id);
        } else {
          X_Log.trace("Leaked zindex assignment; possible memory leak in element w/ id " + id, attached);
        }
      }
      for (int i = 0; i < alwaysOnTop.length(); i++) {
        ReceivesValue<Integer> listener = alwaysOnTop.get(i);
        if (listener != null)
          X_Log.trace("Leaked zindex always on top; possible memory leak in receiver: ", listener.getClass(),
            listener);
      }
    }
    zIndexes.setLength(0);
    alwaysOnTop.setLength(0);
  }

}
