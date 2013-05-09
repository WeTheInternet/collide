package com.google.collide.client.ui.panel;

import elemental.dom.Element;

/**
 * Type for things that can be added to the content area of the CodePerspective.
 */
public interface PanelContent {

  static interface HiddenContent extends PanelContent{}

  /**
   * @return The {@link Element} that we set as the contents of the content area.
   */
  Element getContentElement();

  /**
   * Called when the content is displayed. It's possible that element returned by
   * {@link #getContentElement()} was removed from DOM and re-added, so this callback is a good place to
   * re-initialize any values that may have been cleared.
   * @param content
   */
  void onContentDisplayed();

  /**
   * Called when the content is destroyed. Any time a displayed panel is destroyed, this method will be
   * called. Useful if you have panels with maps and listeners to clear.
   */
  void onContentDestroyed();

  public static class AbstractContent implements PanelContent {
    private final Element contentElement;

    public AbstractContent(Element contentElement) {
      this.contentElement = contentElement;
    }

    /**
     * @return the contentElement
     */
    public Element getContentElement() {
      return contentElement;
    }

    @Override
    public void onContentDisplayed() {

    }

    @Override
    public void onContentDestroyed() {

    }
  }
}