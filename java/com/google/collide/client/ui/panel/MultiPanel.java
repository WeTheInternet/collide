package com.google.collide.client.ui.panel;

import com.google.collide.client.ui.panel.PanelModel.Builder;
import com.google.collide.client.util.CssUtils;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.ShowableUiComponent;
import com.google.collide.mvp.UiComponent;

import elemental.dom.Element;

public abstract class MultiPanel
<M extends PanelModel, V extends MultiPanel.View<M>>
extends UiComponent<V> {

  public MultiPanel(V view) {
    super(view);
  }

  public abstract static class View<M extends PanelModel> extends CompositeView<M> {

    public View(Element element, boolean detached) {
      setElement(element);
    }

    public abstract Element getContentElement();

    public abstract Element getHeaderElement();

  }

  protected PanelContent currentContent;

  public PanelContent getCurrentContent() {
    return currentContent;
  }

  public abstract PanelModel.Builder<M> newBuilder();

  /**
   * Sets the contents of the content area under the header.
   *
   * @param panelContent the content to display, or null to clear content
   * @param settings - our immutable settings pojo.
   */
  public void setContent(PanelContent panelContent, PanelModel settings) {
    if (currentContent == panelContent) {
      return;
    }

    if (currentContent != null) {
      currentContent.onContentDestroyed();
      currentContent.getContentElement().removeFromParent();
    }

    getContentElement().setInnerHTML("");
    if (panelContent != null) {
      getContentElement().appendChild(panelContent.getContentElement());
    }
    currentContent = panelContent;
    currentContent.onContentDisplayed();
    if (settings.isClearNavigator()) {
      clearNavigator();
    }
    setHeaderVisibility(true);
  }

  protected Element getContentElement() {
    return getView().getContentElement();
  }

  public void clearNavigator() {

  }

  /**
   * Sets the contents of the content area under the header. Assumes content can display history and shows
   * history icon. (Use {@link #setContent(PanelContent, PanelModel)} to customize).
   *
   * @param panelContent the content to display, or null to clear content
   */
  public void setContent(PanelContent panelContent) {
    setContent(panelContent, PanelModel.newBasicModel().setHistoryIcon(true).build());
  }

  public void setHeaderVisibility(boolean visible) {
    Element el = getView().getHeaderElement();
    if (el == null) return;
    el = el.getFirstElementChild();
    if (el == null) return;
    CssUtils.setDisplayVisibility2(el, visible);
  }

  public void destroy() {
    if (currentContent != null) {
      currentContent.onContentDestroyed();
      currentContent.getContentElement().removeFromParent();
      currentContent = null;
    }
    getView().getContentElement().setInnerHTML("");
  }

  public abstract ShowableUiComponent<?> getToolBar();

  public Builder<PanelModel> defaultBuilder() {
    return PanelModel.newBasicModel();
  }

}
