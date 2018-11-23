package com.google.collide.client.ui.panel;

import java.util.Comparator;
import java.util.Iterator;

import xapi.fu.itr.MappedIterable;
import xapi.util.api.ConvertsValue;
import xapi.util.api.RemovalHandler;
import collide.client.util.Elements;

import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;

import elemental.dom.Element;

/**
 * A default implementation of the {@link PanelPositioner} interface.
 *
 * This positioner will attempt to fill horizontal space with new panels,
 * and will close the oldest visible panel (that is closable) when there is not enough space.
 *
 * This positioner will be using the document viewport for maximum sizes;
 * override {@link #getMaxHeight()} and {@link #getMaxWidth()} to change this behaviour.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class AbstractPanelPositioner implements PanelPositioner{

  public static class PanelNode {
    float x, y, w, h;
    public final Panel<?, ?> panel;
    public PanelNode(Panel<?, ?> panel) {
      this.panel = panel;
    }
    PanelNode[] onLeft;
    PanelNode[] onRight;
    PanelNode[] onTop;
    PanelNode[] onBottom;

    void setPosition(float x, float y) {
      this.x = x;
      this.y = y;
      panel.setPosition(x, y);
    }

    void setSize(float w, float h) {
      this.w = w;
      this.h = h;
      panel.setSize(w, h);
    }

    public MappedIterable<PanelNode> toTheLeft() {
      return () -> new SiblingIterator(this, IterateLeft);
    }

    public MappedIterable<PanelNode> toTheRight() {
      return () -> new SiblingIterator(this, IterateRight);
    }

    public float offerWidth(float deltaW) {
      ResizeBounds bounds = panel.getBounds();
      if (deltaW > 0) {
        // We are being offered width
        float maxWidth = bounds.getMaxWidth();
        Log.info(getClass(), panel.getId()+" Offered width "+deltaW+"; maxWidth: "+maxWidth+"; w: "+w);
        if (w < maxWidth) {
          float consume = w + deltaW - maxWidth;
          if (consume <= 0) {
            w += deltaW;
            panel.setSize(w, h);
            Log.info(getClass(), "Consuming "+deltaW);
            return 0;
          }
          Log.info(getClass(), "Consume "+consume+" ; "+(deltaW - consume));
          w += consume;
          panel.setSize(w, h);
          return deltaW - consume;
        }
      } else {
        // We are being asked for width
        float minWidth = bounds.getMinWidth();
        Log.info(getClass(), panel.getId()+" Asked for width "+deltaW+"; minWidth: "+minWidth+"; w: "+w);
        if (w > minWidth) {
          float available = w - minWidth;
          if (available + deltaW > 0) {
            w += deltaW;
            panel.setSize(w, h);
            return 0;
          }
          w -= available;
          panel.setSize(w, h);
          return deltaW + available;
        }
      }
      return deltaW;
    }
  }

  private static final PanelNode[] empty = new PanelNode[0];

  private static final ConvertsValue<PanelNode, PanelNode[]> IterateLeft
    = new ConvertsValue<PanelNode, PanelNode[]>() {
      @Override
      public PanelNode[] convert(PanelNode node) {
        PanelNode[] onLeft = node.onLeft;
        if (onLeft == null) {
          return empty;
        } else {
          int len = onLeft.length;
          if (len > 1) {
            // make sure the first node is the most left node of all
            float mostLeft = onLeft[0].x;
            int mostLeftIndex = 0;
            while (len --> 1) {
              if (onLeft[len].x < mostLeft) {
                mostLeft = onLeft[len].x;
                mostLeftIndex = len;
              }
            }
            if (mostLeftIndex != 0) {
              PanelNode swap = onLeft[mostLeftIndex];
              onLeft[mostLeftIndex] = onLeft[0];
              onLeft[0] = swap;
            }
          }
        }
        return onLeft;
      }
    };

    private static final ConvertsValue<PanelNode, PanelNode[]> IterateRight
    = new ConvertsValue<PanelNode, PanelNode[]>() {
      @Override
      public PanelNode[] convert(PanelNode node) {
        PanelNode[] onRight = node.onRight;
        if (onRight == null) {
          return empty;
        } else {
          int len = onRight.length;
          if (len > 1) {
            // make sure the first node is the most right node of all
            float mostRight = onRight[0].x + onRight[0].w;
            int mostRightIndex = 0;
            while (len --> 1) {
              float right = onRight[len].x + onRight[len].w;
              if (right > mostRight) {
                mostRight = right;
                mostRightIndex = len;
              }
            }
            if (mostRightIndex != 0) {
              PanelNode swap = onRight[mostRightIndex];
              onRight[mostRightIndex] = onRight[0];
              onRight[0] = swap;
            }
          }
        }
        return onRight;
      }
    };

    private static final Comparator<? super PanelNode> sorter = new Comparator<PanelNode>() {
      @Override
      public int compare(PanelNode o1, PanelNode o2) {
        float delta = o1.x - o2.x;
        if (delta < 0)
          return -1;
        if (delta > 0)
          return 1;
        delta = o1.y - o2.y;
        if (delta < 0)
          return -1;
        if (delta > 0)
          return 1;
        return o1.hashCode() - o2.hashCode();
      }
    };

  protected static class SiblingIterator implements Iterator<PanelNode> {

    private final JsoArray<PanelNode> flattened;
    int pos = 0;

    public SiblingIterator(PanelNode node, ConvertsValue<PanelNode, PanelNode[]> provider) {
      flattened = JsoArray.create();
      JsoStringMap<PanelNode> seen = JsoStringMap.create();
      recurse(node, provider, seen);
    }

    private void recurse(PanelNode node, ConvertsValue<PanelNode, PanelNode[]> provider,
        JsoStringMap<PanelNode> seen) {
      for (PanelNode sibling : provider.convert(node)) {
        if (sibling.panel.isHidden())continue;
        if (!seen.containsKey(sibling.panel.getId())) {
          seen.put(node.panel.getId(), node);
          recurse(sibling, provider, seen);
          flattened.add(sibling);
        }
      }
    }

    @Override
    public boolean hasNext() {
      return pos < flattened.size();
    }

    @Override
    public PanelNode next() {
      Log.info(getClass(), "Recursing into "+flattened.get(pos).panel.getId());
      return flattened.get(pos++);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  PanelNode leftMost, rightMost;
  JsoStringMap<PanelNode> panels = JsoStringMap.create();

  public RemovalHandler addPanel(final Panel<?, ?> panel) {
    if (panels.size() == 0) {
      positionFirstPanel((
          leftMost = rightMost = newNode(panel)
          ));
    } else {
      PanelNode existing = panels.get(panel.getId());
      if (existing == null) {
        addNewPanel(panel);
      } else {
        if (existing.panel != panel) {
          removePanel(existing.panel);
          addNewPanel(panel);
        } else {
//          focusPanel(existing);
        }
      }
    }
    return new RemovalHandler() {
      @Override
      public void remove() {
        removePanel(panel);
      }
    };
  }

  protected void addNewPanel(Panel<?, ?> panel) {
    PanelNode node = newNode(panel);
    panels.put(panel.getId(), node);
    if (!panel.isHidden())
      focusPanel(node);
  }

  protected float makeRoomOnRight(float prefWidth, float maxRight) {
    Log.info(getClass(), "Making room on right", maxRight);
    return 0;
  }

  protected float makeRoomOnLeft(float prefWidth, float maxRight) {
    // Starting at the right-most panel,
    Log.info(getClass(), "Making room on left", maxRight);
    return 0;
  }


  protected PanelNode newNode(Panel<?, ?> panel) {
    return new PanelNode(panel);
  }


  public void focusPanel(PanelNode toFocus) {
    assert panels.containsKey(toFocus.panel.getId());
    Panel<?, ?> panel = toFocus.panel;
    final Element el = panel.getView().getElement();
    Log.info(getClass(), "Focusing panel "+toFocus.panel.getId());
    // First, try to get off easy.
    // If nothing else is visible, we can just open centered on screen.
    JsoArray<String> keys = panels.getKeys();
    JsoArray<PanelNode> visible = JsoArray.create();
    for (int i = 0, m = keys.size(); i < m; i++) {
      PanelNode node = panels.get(keys.get(i));
      if (node != toFocus && !node.panel.isHidden())
        visible.add(node);
    }
    if (visible.isEmpty()) {
      // Nothing else is showing.  Let's fill the screen.
      centerOnScreen(toFocus);
    }

    ResizeBounds bounds = panel.getBounds();
    float leftest = leftMost.x;
    float minLeft = getMinLeft();
    float minTop = getMinTop();
    float maxHeight = getMaxHeight();
    float maxWidth = getMaxWidth();
    float prefWidth = bounds.getPreferredWidth();
    float prefHeight = bounds.getPreferredHeight();
    if (prefWidth < 0)
      prefWidth = maxWidth + prefWidth;
    if (prefHeight < 0)
      prefHeight = maxHeight + prefHeight;
    float maxRight = maxWidth + minLeft;
    // Try to get off easy:
    float deltaLeft = leftest - prefWidth;
    if (deltaLeft > minLeft) {
      Log.info(getClass(), "Leftest: "+leftest+"; deltaLeft: "+deltaLeft+"; minLeft: "+minLeft);
      // simplest case; there's room on the left, just dump in a new panel.
      toFocus.setPosition(deltaLeft, minTop);
      toFocus.setSize(prefWidth, Math.min(maxHeight, minTop + prefHeight));
      leftMost.onLeft = new PanelNode[]{toFocus};
      leftMost = toFocus;
      return;
    }
    // No room on the left; maybe room on the right?
    float rightest = rightMost.x + rightMost.w;
    float deltaRight = rightest + prefWidth;
    if (deltaRight < maxRight) {
      Log.info(getClass(), "Rightest: "+rightest+"; deltaRight: "+deltaRight+"; maxRight: "+maxRight);
      toFocus.setPosition(deltaLeft, minTop);
      toFocus.setSize(prefWidth, Math.min(maxHeight, minTop + prefHeight));
      rightMost.onRight = new PanelNode[]{toFocus};
      rightMost = toFocus;
      return;
    }
    // No free space on either side.  Start squishing to make room
    boolean squishRight = ((maxRight - deltaRight) > (minLeft - deltaLeft));
    if (squishRight) {
      // First, slide everything to the right to make more room.
      float roomMade = makeRoomOnLeft(prefWidth, minLeft);
    } else {
      // Slide everything to the left to make more roomt
      float roomMade = makeRoomOnRight(prefWidth, maxRight);

    }
//
//    ClientRect bounds = fileNav.getBoundingClientRect();
//    // First, chose whether to add to the left or the right based on
//    // where there is more room.
//    float middle = parent.getLeft() + parent.getWidth()/2;
//    // rather than duplicate the moving code, we're just going to collect
//    // up movable elements, as well as the amount of left they must move
//    float delta;
//    ArrayOf<Element> toMove = Collections.arrayOf();
//    boolean goingLeft = bounds.getLeft() + bounds.getWidth() / 2 > middle;
//    if (goingLeft) {
//      // place new panel to the left
//      // everything currently to the left must slide left (-delta)
//      delta = -el.getClientWidth()-20;
//
//    } else {
//      // place new panel to the right; all other panels slide right (+delta)
//      delta = el.getClientWidth()+20;
//    }
//    el.getStyle().setLeft(bounds.getLeft()+delta, "px");
//    float minBound = goingLeft ? parent.getLeft() + parent.getWidth() : parent.getLeft();
//    JsoArray<String> keys = positioner.keys();
//    Panel<?, ?> edge = fileNavPanel;
//    for (int i = 0; i < keys.size(); i++ ) {
//      String key = keys.get(i);
//      Panel<?, ?> other = positioner.getPanel(key).panel;
//      if (other == panel)
//        continue;
//      ClientRect otherBounds = other.getView().getElement().getBoundingClientRect();
//      if (goingLeft) {
//        if (otherBounds.getLeft() < minBound) {
//          edge = other;
//          minBound = otherBounds.getLeft();
//          X_Log.info(edge, minBound);
//        }
//      } else {
//        if (otherBounds.getRight() > minBound) {
//          edge = other;
//          minBound = otherBounds.getRight();
//          X_Log.info(minBound, edge);
//        }
//      }
//    }
//    ResizeBounds desired = panel.getBounds();
//    double w = desired.getPreferredWidth()+40;//throw in padding
//    if (goingLeft) {
//      if (minBound - w < 0) {
//        el.getStyle().setLeft(0, "px");
//        if (w - minBound > desired.getMinWidth()) {
//          el.getStyle().setWidth((w-minBound-20)+"px");
//        } else {
//          el.getStyle().setWidth(desired.getMinWidth()+"px");
//        }
//      } else {
//        el.getStyle().setWidth((w-20)+"px");
//        el.getStyle().setLeft((minBound - w)+"px");
//      }
//    } else {
//      if (minBound + w > parent.getRight()) {
//
//      }
//    }
//    middle = parent.getTop() + parent.getHeight() / 2;
//    float top = bounds.getTop(), bottom = bounds.getBottom();
//    el.getStyle().setTop(5,"px");
//    el.getStyle().setHeight(bounds.getBottom()-15,"px");


  }

  protected void centerOnScreen(PanelNode toFocus) {
    ResizeBounds bounds = toFocus.panel.getBounds();
    float screenWidth = getMaxWidth();
    float screenHeight = getMaxHeight();
    float maxWidth = bounds.getMaxWidth();
    float maxHeight = bounds.getMaxWidth();
    // Normalize negative anchors
    if (maxWidth < 0) {
      maxWidth = screenWidth + maxWidth;
    }
    if (maxHeight < 0) {
      maxHeight = screenHeight + maxHeight;
    }
    // Set constrained bounds
    toFocus.setSize(
        Math.min(maxWidth, screenWidth*7/8),
        Math.min(maxHeight, screenHeight*7/8)
    );
    // Apply position
    toFocus.setPosition(
        getMinLeft() + (maxWidth-toFocus.w)/2f,
        getMinTop() + (maxHeight-toFocus.h)/2f
    );
    if (toFocus.panel.isHidden())
      toFocus.panel.show();
  }

  protected void positionFirstPanel(PanelNode panelNode) {
    // Our default implementation starts at top right and expands left.
    panels.put(panelNode.panel.getId(), panelNode);
    float end = getMaxWidth();
    ResizeBounds bounds = panelNode.panel.getBounds();
    // first panel always gets its preferred size
    panelNode.setSize(
        bounds.getPreferredWidth(),
        bounds.getPreferredHeight()
    );
    panelNode.setPosition(
        end - panelNode.w - getMinLeft(),
        panelNode.y = 0 - getMinTop()
    );

  }

  @Override
  public boolean adjustHorizontal(Panel<?, ?> panel, float deltaX, float deltaW) {
    PanelNode node = panels.get(panel.getId());
    if (node == null) return false;
    boolean newWidth = Math.abs(deltaW) > 0.001;
    deltaW = -deltaW;// when our node grows, we offer siblings the negative value of our change
    if (deltaX > 0.001f) {
      // moving right, pull anyone to the left with us, squish anyone to the right
      node.x += deltaX;
      float left = deltaX;
      for (PanelNode onLeft : node.toTheLeft()) {
        Log.info(getClass(), "Moving right "+deltaX+ " : "+onLeft.panel.getId());
        float consumes = onLeft.offerWidth(left);
        if (consumes != left) {
          deltaX -= (left - consumes);
          left = consumes;
        }
        onLeft.setPosition(onLeft.x + deltaX, onLeft.y);
      }
      if (newWidth) {
        for (PanelNode onRight : node.toTheRight()) {
          Log.info(getClass(), "Go right "+deltaX+ " : "+onRight.panel.getId());
          deltaW = onRight.offerWidth(deltaW);
          if (deltaW >= 0)
            return true;
        }
      }

    }
    else if (deltaX < -0.001f) {
      // moving left; bring anyone to the right with us, squish anyone on the left
      Log.info(getClass(), "Moving left "+deltaX);
      for (PanelNode onLeft : node.toTheLeft()) {
        onLeft.setPosition(onLeft.x + deltaX, onLeft.y);
        if (newWidth) {
          deltaW = onLeft.offerWidth(deltaW);
          if (deltaW == 0) {
            newWidth = false;
          }
        }
      }
      return newWidth == false;

    } else if (newWidth) {
      // No delta in position, this is a drag resize event (on right or bottom axis).
      for (PanelNode onRight : node.toTheRight()) {
        deltaW = onRight.offerWidth(deltaW);
        if (Math.abs(deltaW) < 0.001)
          return true;
      }
      for (PanelNode onLeft : node.toTheLeft()) {
        deltaW = onLeft.offerWidth(deltaW);
        if (Math.abs(deltaW) < 0.001)
          return true;
      }
    }
    return false;
  }

  @Override
  public boolean adjustVertical(Panel<?, ?> panel, float deltaY, float deltaH) {
    // TODO Auto-generated method stub
    return false;
  }

  public void removePanel(Panel<?, ?> panel) {

  }

  protected float getMinLeft() {
    return 0;
  }

  protected float getMinTop() {
    return 0;
  }

  protected float getMaxWidth() {
    return Elements.getWindow().getInnerWidth();
  }

  protected float getMaxHeight() {
    return Elements.getWindow().getInnerHeight();
  }


  public boolean hasPanel(String namespace) {
    return panels.containsKey(namespace);
  }

  public PanelNode getPanel(String namespace) {
    return panels.get(namespace);
  }


  public JsoArray<String> keys() {
    return panels.getKeys();
  }


  public void recalculate() {
    // Iterate through all panels, and make sure they are all sorted correctly.
    // If there are any changes, we should redraw (but avoid unneccessary layout())
    JsoArray<String> keys =  panels.getKeys();
    JsoArray<PanelNode> visible = JsoArray.create();
    for (String key : keys.asIterable()) {
      PanelNode node = panels.get(key);
      if (!node.panel.isHidden())
        visible.add(node);
    }
    if (visible.size() < 2)
      return;
    visible.sort(sorter);
    PanelNode onLeft = visible.remove(0);
    onLeft.onLeft = empty;
    PanelNode next = null;
    while (visible.size() > 0) {
      next = visible.remove(0);
      onLeft.onRight = new PanelNode[]{next};
      next.onLeft = new PanelNode[]{onLeft};
      onLeft = next;
    }
    next.onRight = empty;
  }

}
