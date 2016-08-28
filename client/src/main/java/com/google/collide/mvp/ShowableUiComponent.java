package com.google.collide.mvp;

/**
 * A {@link UiComponent} which includes {@link #hide()} and {@link #show()} methods.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <V>
 */
public abstract class ShowableUiComponent<V extends View<?>> extends UiComponent<V>{

  /**
   * Called to tell your UiComponent to hide or detach
   */
  public abstract void hide();
  /**
   * Called to tell your UiComponent to show or attach
   */
  public abstract void show();

  public ShowableUiComponent() {
  }
  public ShowableUiComponent(V view) {
    super(view);
  }

}
