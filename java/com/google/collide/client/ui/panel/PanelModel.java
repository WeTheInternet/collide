package com.google.collide.client.ui.panel;

public class PanelModel {

  private final boolean historyIcon;
  private final boolean closeIcon;
  private final boolean settingsIcon;
  private final boolean collapseIcon;
  private final boolean clearNavigator;

  protected PanelModel(boolean showHistory, boolean showClose, boolean showSettings, boolean showCollapse,
    boolean showClear) {
    this.historyIcon = showHistory;
    this.closeIcon = showClose;
    this.settingsIcon = showSettings;
    this.collapseIcon = showCollapse;
    this.clearNavigator = showClear;
  }

  public final boolean showHistoryIcon() {
    return historyIcon;
  }

  public final boolean showCloseIcon() {
    return closeIcon;
  }

  public final boolean showSettingsIcon() {
    return settingsIcon;
  }

  public final boolean showCollapseIcon() {
    return collapseIcon;
  }

  public static class Builder <M extends PanelModel> {
    protected boolean historyIcon;
    protected boolean closeIcon;
    protected boolean settingsIcon;
    protected boolean collapseIcon;
    protected boolean clearNavigator;

    /**
     * @return the closeIcon
     */
    public boolean isCloseIcon() {
      return closeIcon;
    }

    /**
     * @param closeIcon the closeIcon to set
     */
    public PanelModel.Builder<M> setCloseIcon(boolean closeIcon) {
      this.closeIcon = closeIcon;
      return this;
    }

    /**
     * @return the collapseIcon
     */
    public boolean isCollapseIcon() {
      return collapseIcon;
    }

    /**
     * @param collapseIcon the collapseIcon to set
     * @return
     */
    public PanelModel.Builder<M> setCollapseIcon(boolean collapseIcon) {
      this.collapseIcon = collapseIcon;
      return this;
    }
    /**
     * @return the clearFiles
     */
    public boolean isClearNavigator() {
      return clearNavigator;
    }

    /**
     * @param clearFiles whether or not to clear file navigator
     * @return this for chaining
     */
    public PanelModel.Builder<M> setClearNavigator(boolean clearFiles) {
      this.clearNavigator = clearFiles;
      return this;
    }

    /**
     * @return the historyIcon
     */
    public boolean isHistoryIcon() {
      return historyIcon;
    }

    /**
     * @param historyIcon the historyIcon to set
     * @return
     */
    public PanelModel.Builder<M> setHistoryIcon(boolean historyIcon) {
      this.historyIcon = historyIcon;
      return this;
    }

    /**
     * @return the settingsIcon
     */
    public boolean isSettingsIcon() {
      return settingsIcon;
    }

    /**
     * @param settingsIcon the settingsIcon to set
     * @return
     */
    public PanelModel.Builder<M> setSettingsIcon(boolean settingsIcon) {
      this.settingsIcon = settingsIcon;
      return this;
    }

    @SuppressWarnings("unchecked")
    public M build() {
      return (M)new PanelModel(historyIcon, closeIcon, settingsIcon, collapseIcon, clearNavigator);
    }

  }
  public static PanelModel.Builder<PanelModel> newBasicModel(){
    return new PanelModel.Builder<PanelModel>();
  }

  /**
   * @return the clearNavigator
   */
  public boolean isClearNavigator() {
    return clearNavigator;
  }

}