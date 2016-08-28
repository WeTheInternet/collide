package collide.plugin.client.inspector;

import com.google.collide.json.client.JsoArray;
import elemental.client.Browser;
import elemental.dom.Attr;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.js.dom.JsNode;
import elemental.js.events.JsMouseEvent;
import xapi.inject.impl.SingletonProvider;
import xapi.util.api.ReceivesValue;
import xapi.util.api.RemovalHandler;

import com.google.gwt.core.client.JavaScriptObject;

public class ElementInspector extends JavaScriptObject{

  protected ElementInspector() {}

  private static SingletonProvider<ElementInspector> singleton =
  new SingletonProvider<ElementInspector>(){
    protected ElementInspector initialValue() {
      Browser.getWindow().addEventListener("click", new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          JsNode el = ((JsMouseEvent)evt).getToElement();
          ElementInspector inspector = ElementInspector.get();
          JsoArray<Node> withData = JsoArray.create();
          while (el != null && !"html".equalsIgnoreCase(el.getNodeName())) {
            if (inspector.hasData(el)) {
              withData.add(el);
            }
            el = el.getParentNode();
          }
          if (withData.size() > 0) {
            inspector.fireEvent(withData);
          }
        }
      }, false);
      return create();
    }

    private native ElementInspector create()
      /*-{ return {}; }-*/;
  };

  public static ElementInspector get() {
    return singleton.get();
  }

  public final native void monitorInspection(ReceivesValue<JsoArray<Node>> receiver)
  /*-{
     if (!this.monitors)
       this.monitors = [];
     this.monitors.push(receiver);
   }-*/;

  public final native void fireEvent(JsoArray<Node> withData)
  /*-{
     if (this.monitors) {
       for (var i in this.monitors)
         this.monitors[i].@xapi.util.api.ReceivesValue::set(*)(withData);
     }
   }-*/;

  private static int cnt;
  private static String uuid() {
    StringBuilder b = new StringBuilder("x_");
    int id = cnt++;
    while(id > 0) {
      char digit = (char)(id % 32);
      b.append(Character.forDigit(digit, 32));
      id >>= 5;
    }
    return b.toString();
  }

  private final String getDebugId(Node node) {
    Node debug = node.getAttributes().getNamedItem("xapi.debug");
    if (debug == null) {
      Attr attr = Browser.getDocument().createAttribute("xapi.debug");
      attr.setNodeValue(uuid());
      node.getAttributes().setNamedItem((debug=attr));
    }
    return debug.getNodeValue();
  }

  public final RemovalHandler makeInspectable(Node node, final Object data) {
    final String id = getDebugId(node);
    remember(id, data);
    return new RemovalHandler() {
      @Override
      public void remove() {
        forget(id, data);
      }
    };
  }


  public final void forget(Node node, Object data) {
    forget(getDebugId(node), data);
  }

  public final JsoArray<Object> recall(Node node) {
    return recall(getDebugId(node));
  }

  public final boolean hasData(Node node) {
    return hasData(getDebugId(node));
  }

  public final void remember(Node node, Object data) {
    remember(getDebugId(node), data);
  }



  private final native void forget(String nodeId, Object data)
  /*-{
    var arr = this[nodeId];
    if (!arr) return;
    if (data == null) {
      delete this[nodeId];
      return;
    }
    var i = arr.length;
    for (;i --> 0;) {
      if (arr[i] == data)
        arr.splice(i, 1);
    }
    if (arr.length == 0)
      delete this[nodeId];
  }-*/;

  private final native boolean hasData(String id)
  /*-{
    return this[id] && this[id].length > 0;
  }-*/;

  private final native JsoArray<Object> recall(String id)
  /*-{
    // Return a mutable array, for convenience
    if (!this[id]) this[id] = [];
    this [id];
  }-*/;

  private final native void remember(String id, Object data)
  /*-{
    if (!this[id]) this[id] = [];
    this[id].push(data);
  }-*/;

}
