package collide.demo.view;

import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.util.api.HasId;
import collide.client.util.Elements;
import collide.demo.view.TabPanel.TabView;
import collide.gwtc.resources.GwtcCss;

import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelContent.AbstractContent;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.mvp.View;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.HasText;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DivElement;

public class TabPanel extends UiComponent<View<?>> implements PanelContent{

  public static final SingletonProvider<TabPanelResources> DEFAULT_RESOURCES =
      new SingletonProvider<TabPanelResources>() {
        protected TabPanelResources initialValue() {
          TabPanelResources res = GWT.create(TabPanelResources.class);
          res.css().ensureInjected();
          return res;
        };
      };

  public static TabPanel create(TabPanelResources res) {
    return new TabPanel(res);
  }
  
  public static class TabPanelView extends CompositeView<Void> {
    public TabPanelView(TabPanelResources res) {
      setElement(Elements.createDivElement(res.css().tabPanel(),res.css().hidden()));
    }
  }
  
  public class TabView extends CompositeView<Void> {
    private final DivElement tabHeader;
    private final DivElement tabBody;
    private final String id;

    public TabView(String id) {
      this.id = id;
      tabHeader = Elements.createDivElement(css.tabHeader());
      header.appendChild(tabHeader);
      tabBody = Elements.createDivElement(css.hidden());
      body.appendChild(tabBody);
    }

    public void setContent(View<?> view, String title) {
      tabBody.setInnerHTML("");
      tabBody.appendChild(view.getElement());
      tabHeader.setInnerHTML("");
      Element head = buildTabHeader(title);
      tabHeader.appendChild(head);
      attachHandler(this, tabHeader);
    }
  }
  
  private final JsoStringMap<TabView> panels;
  private final Element header;
  private final Element body;
  private final TabPanelCss css;
  private TabView selected;

  public TabPanel(TabPanelResources res) {
    super(new TabPanelView(res));
    css = res.css();
    panels = JsoStringMap.create();
    header = Elements.createDivElement(css.tabBar());
    getView().getElement().appendChild(header);
    body = Elements.createDivElement(css.tabBody());
    getView().getElement().appendChild(body);
  }
  
  public void attachHandler(final TabView tabView, final Element head) {
    DivElement el = Elements.createDivElement(css.closeIcon());
    el.setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        remove(tabView);
      }
    });
    head.setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (panels.containsKey(tabView.id)) {
          select(tabView);
        }
      }
    });
    head.appendChild(el);
  }

  protected void remove(TabView tabView) {
    tabView.tabBody.removeFromParent();
    tabView.tabHeader.removeFromParent();
    panels.remove(tabView.id);
    if (panels.isEmpty()) {
      getContentElement().addClassName(css.hidden());
    } else {
      if (selected == tabView) {
        if (selectById(panels.getKeys().get(0))) {
          return;
        }
        selected = null;
      }
    }
  }

  private boolean selectById(String id) {
    TabView panel = panels.get(id);
    if (panel == null) {
      X_Log.warn(getClass(), "Tried to select panel with id",id,"but no panel with that id is attached");
      return false;
    } else {
      select(panel);
      return true;
    }
  }

  public void select(TabView tabView) {
    if (selected != null) {
      selected.tabHeader.removeClassName(css.selected());
      selected.tabBody.addClassName(css.hidden());
    }
    selected = tabView;
    selected.tabHeader.addClassName(css.selected());
    selected.tabBody.removeClassName(css.hidden());
  }

  public Element buildTabHeader(String title) {
    DivElement el = Elements.createDivElement();
    el.setInnerHTML(title);
    return el;
  }


  public <V extends com.google.collide.mvp.View<?> & HasId> TabView addContent(V view) {
    final String title;
    if (view instanceof HasText) {
      title = ((HasText)view).getText();
    } else {
      title = view.getId();
    }
    TabView existing = panels.get(view.getId());
    if (existing == null) {
      existing = createPanel(view, title);
      panels.put(view.getId(), existing);
    }
    if (selected == null) {
      select(existing);
    }
    existing.setContent(view, title);
    getContentElement().removeClassName(css.hidden());
    return existing;
  }
  
  protected <V extends com.google.collide.mvp.View<?> & HasId> TabView createPanel(V view, String title) {
    return new TabView(view.getId());
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
  }

  @Override
  public void onContentDestroyed() {
  }

  public boolean unhide(TabView tabView) {
    if (tabView.tabHeader.getParentElement() == null) {
      tabView.tabHeader.addClassName(css.selected());
      tabView.tabBody.removeClassName(css.hidden());
      getView().getElement().removeClassName(css.hidden());
      panels.put(tabView.id, tabView);
      header.appendChild(tabView.tabHeader);
      body.appendChild(tabView.tabBody);
      return true;
    }
    return false;
  }
  
}
