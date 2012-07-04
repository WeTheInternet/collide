// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.status;

import com.google.collide.client.util.Elements;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

import elemental.client.Browser;
import elemental.html.AnchorElement;
import elemental.html.SpanElement;

/**
 * This is the base of all message types.
 *
 *  All common message properties are set through this base class. Message
 * events are routed through double-dispatch in the protected do*() methods
 * while their public facing counterparts ensure that the base message state is
 * consistent (e.g. setting canceled).
 */
public class StatusMessage {
  /**
   * A loading message indicates that the application is waiting for a slow
   * operation.
   *
   *  A confirmation message is an informational message to the user.
   *
   *  An error message indicates that there has been a recoverable or transient
   * error.
   *
   *  A fatal message indicates that the application has entered into an
   * unrecoverable state (for example, an uncaught exception). Once a fatal
   * message fires, no other subsequent status messages will fire.
   *
   * NOTE: These message types are in order of priority.
   */
  public enum MessageType {
    LOADING, //
    CONFIRMATION, //
    ERROR, //
    FATAL
  }

  // The default delay to use for avoiding message flicker.
  public static final int DEFAULT_DELAY = 200;

  public static final StatusAction RELOAD_ACTION = new StatusAction() {
    @Override
    public void renderAction(SpanElement actionContainer) {
      actionContainer.setTextContent("Reload Collide");
    }

    @Override
    public void onAction() {
      Browser.getWindow().getLocation().reload();
    }
  };

  public static final StatusAction FEEDBACK_ACTION = new StatusAction() {
    @Override
    public void renderAction(SpanElement actionContainer) {
      AnchorElement a = Elements.createAnchorElement();
      a.setHref(
          "https://groups.google.com/forum/?domain=google.com#!newtopic/collide-discussions");
      a.setTarget("_blank");
      a.setTextContent("Tell us what happened!");   
      a.getStyle().setColor("yellow");
      actionContainer.appendChild(a);
    }

    @Override
    public void onAction() {
      // Nothing. Let the native anchor do its thing.
    }
  };

  private final JsoArray<StatusAction> actions = JsoArray.create();
  private boolean canceled = false;
  private boolean dismissable = false;
  private long expiryTime = 0;
  private String longText;
  private final StatusManager statusManager;
  private String text;

  private final MessageType type;

  public StatusMessage(StatusManager statusManager, MessageType type, String text) {
    this.statusManager = statusManager;
    this.type = type;
    this.text = text;
  }

  /**
   * Cancel a message. Once a message is canceled, all subsequent fires are
   * no-ops.
   */
  public final void cancel() {
    canceled = true;
    statusManager.cancel(this);
  }

  /**
   * Cancel an event in the future.
   *
   * @param milliseconds time to expiry.
   */
  public final void expire(int milliseconds) {
    expiryTime = System.currentTimeMillis() + milliseconds;
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      @Override
      public boolean execute() {
        cancel();
        return false;
      }
    }, milliseconds);
  }

  /**
   * Fires a message to the status manager. If the message has been canceled,
   * this is a no-op. If the message has already been fired, then update that
   * message.
   */
  public final void fire() {
    if (!canceled) {
      statusManager.fire(this);
    }
  }

  /**
   * Fire a message with a delay. If the message is canceled before the delay,
   * this is a no-op.
   *
   * @param milliseconds time to delay firing this message.
   */
  public final void fireDelayed(int milliseconds) {
    Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
      @Override
      public boolean execute() {
        fire();
        return false;
      }
    }, milliseconds);
  }

  /**
   * @return the actions associated with this message or null
   */
  public JsoArray<StatusAction> getActions() {
    return actions;
  }

  public String getLongText() {
    return longText == null ? "" : longText;
  }

  /**
   * Concrete implementations can reference the {@link StatusManager} through
   * this getter.
   *
   * @return the @{link StatusManager} for this message.
   */
  protected final StatusManager getStatusManager() {
    return statusManager;
  }

  public String getText() {
    return text == null ? "" : text;
  }

  public void setText(String text) {
    this.text = text;
  }

  /**
   * @return return the time in milliseconds left until expiration or 0 if there
   *         is no pending expiration.
   */
  public int getTimeToExpiry() {
    return (int) (expiryTime == 0 ? 0 : Math.max(0, expiryTime - System.currentTimeMillis()));
  }

  public MessageType getType() {
    return type;
  }

  /**
   * @return Can the user manually dismiss this message?
   */
  public boolean isDismissable() {
    return dismissable;
  }

  public void addAction(StatusAction action) {
    this.actions.add(action);
  }

  /**
   * @param dismissable whether or not this message can be dismissed by the
   *        user.
   */
  public void setDismissable(boolean dismissable) {
    this.dismissable = dismissable;
  }

  /**
   * The status message must fit on one line, but the message can be expanded to
   * show more detailed information like a stack trace using long text.
   *
   * @param longText
   */
  public void setLongText(String longText) {
    this.longText = longText;
  }
}
