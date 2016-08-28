package com.google.collide.client.ui.panel;

public class PanelState {

  private boolean maximizable, closable = true, refreshable, hotswappable;
  private String namespace, title;
  private boolean allHeader;
  /**
   * @return the closable
   */
  public boolean isClosable() {
    return closable;
  }
  /**
   * @param closable the closable to set
   */
  public PanelState setClosable(boolean closable) {
    this.closable = closable;
    return this;
  }
  /**
   * @return the hotswappable
   */
  public boolean isHotswappable() {
    return hotswappable;
  }
  /**
   * @param hotswappable the hotswappable to set
   * @return
   */
  public PanelState setHotswappable(boolean hotswappable) {
    this.hotswappable = hotswappable;
    return this;
  }
  /**
   * @return the maximizable
   */
  public boolean isMaximizable() {
    return maximizable;
  }
  /**
   * @param maximizable the maximizable to set
   * @return
   */
  public PanelState setMaximizable(boolean maximizable) {
    this.maximizable = maximizable;
    return this;
  }
  /**
   * @return the refreshable
   */
  public boolean isRefreshable() {
    return refreshable;
  }
  /**
   * @param refreshable the refreshable to set
   * @return
   */
  public PanelState setRefreshable(boolean refreshable) {
    this.refreshable = refreshable;
    return this;
  }
  /**
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }
  /**
   * @param namespace the namespace to set
   * @return
   */
  public PanelState setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }
  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }
  /**
   * @param title the title to set
   * @return
   */
  public PanelState setTitle(String title) {
    this.title = title;
    return this;
  }

  @Override
  public String toString() {
    return super.toString();
  }

  public static PanelState fromString(String serialized) {
    PanelState panel = new PanelState();
    return panel;
  }

  public boolean isAllHeader() {
    return allHeader;
  }

  public PanelState setAllHeader(boolean isHeader) {
    allHeader = isHeader;
    return this;
  }

}
