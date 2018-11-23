package com.google.collide.mvp;

import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;

/**
 * A {@link UiComponent} which includes {@link #hide()} and {@link #show()} methods.
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <V>
 */
public abstract class ShowableUiComponent<V extends View<?>> extends UiComponent<V> implements ShowableComponent {

  private boolean showing;
  private final ListenerRegistrar<ShowStateChangedListener> stateChangeRegistrar;

  @Override public final boolean isShowing() {
    return showing;
  }

  /**
   * Called to tell your UiComponent to hide or detach
   */
  public final void hide() {
    if (showing) {
      this.showing = false;
      doHide();
    }
  }
  public abstract void doHide();

  /**
   * Called to tell your UiComponent to show or attach
   */
  public final void show() {
    if (!showing) {
      this.showing = true;
      doShow();
    }
  }
  public abstract void doShow();

  public ShowableUiComponent() {
    stateChangeRegistrar = ListenerManager.create();
  }
  public ShowableUiComponent(V view) {
    super(view);
    stateChangeRegistrar = ListenerManager.create();
  }

  @Override
  public ListenerRegistrar<ShowStateChangedListener> getShowStateChangedListenerRegistrar() {
    return stateChangeRegistrar;
  }
}
