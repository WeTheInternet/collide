package collide.demo.view;

import xapi.util.api.RemovalHandler;
import collide.demo.resources.DemoResources;

import com.google.collide.client.util.CssUtils;
import com.google.collide.json.client.JsoArray;
import com.google.collide.plugin.client.inspector.ElementInspector;
import com.google.gwt.resources.client.CssResource;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.MouseEvent;
import elemental.util.Timer;

public class SplitPanel {

  public static interface Css extends CssResource{
    String splitPanel();
    String splitPanelChild();
    String panelContent();
    String verticalSplit();
    String horizontalSplit();
    String bottomSplit();
    String leftSplit();
    String rightSplit();
    String topSplit();
  }
  
  class PanelNode {
    Element el;
    PanelNode next;
    double width;
  }
  
  final PanelNode head;
  PanelNode tail;
  int size;
  private boolean vertical;
  private Timer refresh;
  private Css css;
  
  public SplitPanel(boolean vertical) {
    this.vertical = vertical;
    css = createCss();
    tail = head = new PanelNode();
    head.el = Browser.getDocument().createDivElement();
    head.el.addClassName(css.splitPanel());
    head.el.addClassName(vertical ? css.verticalSplit() : css.horizontalSplit());
  }
  
  public Element getElement() {
    return head.el;
  }

  public RemovalHandler addChild(Element child, double width) {
    return addChild(child, width, -1);
  }
  
  public RemovalHandler addChild(final Element child, double width, int index) {
    if (exists(child)) {
      // TODO adjust index
      return RemovalHandler.DoNothing;
    }
    final PanelNode node = new PanelNode();
    node.width = width;
    node.el = wrapChild(child, node);
    getElement().appendChild(node.el);
    RemovalHandler remover = new RemovalHandler() {
      @Override
      public void remove() {
        PanelNode target = head;
        while (target.next != null) {
          if (target.next == node) {
            target.next = node.next;
            size--;
            return;
          }
          target = target.next;
        }
      }
    };
    size ++;
    if (index < 0) { 
      // Negative index = add-to-end, the simplest and fastest case.
      tail.next = node;
      tail = node;
    } else {
      PanelNode target = head;
      while (index --> 0) {
        if (target.next==null)
          break;
        target = target.next;
      }
      if (target.next == null) {
        // We hit the end, add to end and update tail
        target.next = node;
        tail = node;
      } else {
        // An actual insert, just update pointers
        node.next = target.next;
        target.next = node;
      }
    }
    if (refresh == null) {
      refresh = new Timer() {
        @Override
        public void run() {
          refresh = null;
          refresh();
        }
      };
      refresh.schedule(1);
    }
    return remover;
  }
  
  private boolean exists(Element child) {
    PanelNode search = head.next;
    while (search != null) {
      if (search.el == child)
        return true;
      search = search.next;
    }
    return false;
  }

  protected int getMaxDimension() {
    return vertical ? head.el.getClientHeight() : head.el.getClientWidth();
  }

  protected void refresh() {
    if (head.next == null)
      return;
    // Layout panels.
    PanelNode node = head;
    double percents = 0;
    double px = 0;
    float max = getMaxDimension();
    JsoArray<PanelNode> fills = JsoArray.create();
    JsoArray<PanelNode> fixed = JsoArray.create();
    while (node.next != null) {
      node = node.next;
      if (node.width < 0)
        fills.add(node);
      else {
        fixed.add(node);
        if (node.width < 1){
          assert node.width != 0;
          percents += node.width;
        }
        else
          px += node.width;
      }
    }
    final int numFills = fills.size();
    final int numFixed = fixed.size();
    double x = 0;
    boolean overflow = false;
    if (numFixed == 0) {
      // No fixed with items, just lay everything out evenly.
      double w = max / numFills;
      for (int i = 0; i < numFills; i++)
        x = setSize(fills.get(i), w, x);
    } else {
      // Some fixed / percent width items.
      double size = px + percents * max;
      double scale = size / max;
      if (numFills == 0) {
        // No fills, distribute proportionally
        for (int i = 0;i < numFixed; i++) {
          PanelNode child = fixed.get(i);
          x = setSize(child, (child.width < 1 ? size * child.width : child.width) / scale, x);
        }
      } else {
        // Some fixed, some fills; things could get messy...
        double fillSize;
        overflow = scale > 1;
        if (overflow) {
            // We have an effin' mess! Just scroll everything off the end
          fillSize = max * .25; // Fills get 25% screen width
          scale = 1; // Everything else gets what it asked for
          size = max; 
        } else {
          // There's enough to give everyone what they want, and give the rest to fills.
          fillSize = Math.max(350, (max - size) / numFills);
          
        }
        node = head; // We have to iterate all nodes
        while (node.next != null) {
          node = node.next;
          if (node.width < 0) {
            // A fill. Give it our leftover size
            x = setSize(node, fillSize, x);
          } else if (node.width < 1) {
            // A percent.  Give it a ratio of max
            x = setSize(node, node.width * size
//                size / scale
                , x);
          } else {
            // A fixed size. Scale it up / down.
            x = setSize(node, node.width, x);
          }
        }
      }
      enableOverflow(overflow);
    }
  }

  protected Element wrapChild(Element child, PanelNode node) {
    ElementInspector.get().makeInspectable(child, node);
    // Throw in some drag handles.
    Document doc = Browser.getDocument();
    Element wrapper = doc.createDivElement();
    child.addClassName(css.panelContent());
    wrapper.addClassName(css.splitPanelChild());
    wrapper.getStyle().setPosition("absolute");
    wrapper.appendChild(child);
    createSlider(wrapper, child, node, true);
    createSlider(wrapper, child, node, false);
    return wrapper;
  }

  private Css createCss() {
    Css css = DemoResources.GLOBAL.splitPanel();
    css.ensureInjected();
    return css;
  }

  private void createSlider(final Element wrapper, final Element child, 
      final PanelNode node, final boolean first) {
    final Element sliderEl = Browser.getDocument().createSpanElement();
    final String cls;
    if (vertical) {
      if (first) {
        // Do a top-slider
        cls = css.topSplit();
      } else {
        // Do a bottom-slider
        cls = css.bottomSplit();
      }
    } else {
      if (first) {
        // Do a left-slider
        cls = css.leftSplit();
      } else {
        // Do a right-slider
        cls = css.rightSplit();
      }
    }
    sliderEl.addClassName(cls);
    wrapper.appendChild(sliderEl);
    class PanelPosition {
      float posStart;
      float sizeStart;
      float mouseStart;
      PanelNode node;
      PanelPosition next; 
    }
    final PanelPosition self = new PanelPosition();
    self.node = node;
    
    sliderEl.setOnmousedown(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        evt.preventDefault();
        final PanelNode siblingNode = first ? getPreviousSibling(node) : node.next;
        final Element sibling;
        if (siblingNode == null) {
          sibling = null;
        } else {
          self.next = new PanelPosition();
          sibling = siblingNode.el;
          self.next.node = siblingNode;
        }
        PanelPosition affected = self.next;
        MouseEvent e = (MouseEvent) evt;
        if (vertical) {
          self.mouseStart = e.getClientY();
          // We parse from the css values set (which we set before this is called),
          // so we don't have to do any measuring
          // or compensate for weird offsets in the client elements.
          self.posStart = CssUtils.parsePixels(wrapper.getStyle().getTop());
          self.sizeStart = CssUtils.parsePixels(wrapper.getStyle().getHeight());
          if (sibling != null) {
            affected.posStart = CssUtils.parsePixels(sibling.getStyle().getTop());
            affected.sizeStart = CssUtils.parsePixels(sibling.getStyle().getHeight());
          }
        } else {
          self.mouseStart = e.getClientX();
          self.posStart = CssUtils.parsePixels(wrapper.getStyle().getLeft());
          self.sizeStart = CssUtils.parsePixels(wrapper.getStyle().getWidth());
          if (sibling != null) {
            affected.posStart = CssUtils.parsePixels(sibling.getStyle().getLeft());
            affected.sizeStart = CssUtils.parsePixels(sibling.getStyle().getWidth());
          }
        }
        
        final EventRemover[] remover = new EventRemover[2];
        // TODO put these event listeners over an empty iframe, to cover up any iframes on page.
        remover[0] = Browser.getWindow().addEventListener("mouseup", new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            if (remover[0] != null){
              remover[0].remove();
              remover[1].remove();
              remover[0] = null;
            }
          }
        }, true);
        remover[1] = Browser.getWindow().addEventListener("mousemove", new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            evt.preventDefault();
            final MouseEvent e = (MouseEvent) evt;
            final Element el = self.next == null ? null : self.next.node.el;
            assert el != wrapper;
            double delta;
            if (vertical) {
              delta = e.getClientY() - self.mouseStart;
              if (first) {
                // A top drag affects top and height
                wrapper.getStyle().setTop((int)(self.posStart+ delta),"px");
                wrapper.getStyle().setHeight((int)(self.sizeStart - delta),"px");
                if (el != null) {
                  // TODO implement max/min/pref values, and iterate through the 
                  // nodes to push delta off on any neighbors
                  el.getStyle().setHeight((int)(self.next.sizeStart + delta),"px");
                }
              } else {
                wrapper.getStyle().setHeight((int)(self.sizeStart + delta),"px");
                if (el != null) {
                  el.getStyle().setTop((int)(self.next.posStart+ delta),"px");
                  el.getStyle().setHeight((int)(self.next.sizeStart - delta),"px");
                }
              }
            } else {
              delta = e.getClientX() - self.mouseStart;
              if (first) {
                // A left drag affects left and width
                wrapper.getStyle().setLeft((int)(self.posStart + delta),"px");
                wrapper.getStyle().setWidth((int)(self.sizeStart - delta),"px");
                if (el != null) {
                  el.getStyle().setWidth((int)(self.next.sizeStart + delta),"px");
                }
              } else {
                wrapper.getStyle().setWidth((int)(self.sizeStart + delta),"px");
                if (el != null) {
                  el.getStyle().setLeft((int)(self.next.posStart + delta),"px");
                  el.getStyle().setWidth((int)(self.next.sizeStart - delta),"px");
                }
              }
            }
          }
        }, true);
        
        
      }
    });
  }

  private PanelNode getPreviousSibling(PanelNode node) {
    if (node == head.next)
      return null;
    PanelNode search = head;
    while (search.next != null) {
      if (search.next == node) {
        return search;
      }
      search = search.next;
    }
    return null;
  }

  private void enableOverflow(boolean enable) {
    if (enable) {
      if (vertical) {
        head.el.getStyle().setOverflowY("auto");
      } else {
        head.el.getStyle().setOverflowX("auto");
      }
    } else {
      if (vertical) {
        head.el.getStyle().clearOverflowY();
      } else {
        head.el.getStyle().clearOverflowX();
      }
    }
  }

  private double setSize(PanelNode panelNode, double w, double x) {
    if (vertical) {
      panelNode.el.getStyle().setHeight((int)w, "px");
      panelNode.el.getStyle().setTop((int)x, "px");
    } else {
      panelNode.el.getStyle().setWidth((int)w, "px");
      panelNode.el.getStyle().setLeft((int)x, "px");
    }
    return x+w;
  }
  
}
