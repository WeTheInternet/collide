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

package com.google.collide.client.code;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * An event which can be fired to indicate that a snapshot is requested and the
 * dialog should be opened if possible.
 */
public class SnapshotRequestedEvent extends GwtEvent<SnapshotRequestedEvent.Handler> {

  /**
   * Handler interface for getting notified when the right sidebar is expanded
   * or collapsed.
   */
  public interface Handler extends EventHandler {
    void onSnapshotRequested(SnapshotRequestedEvent evt);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();
  private final String suggestedLabel;

  public SnapshotRequestedEvent() {
    this(null);
  }
  
  public SnapshotRequestedEvent(String suggestedLabel) {
    this.suggestedLabel = suggestedLabel;
  }
  
  public String getSuggestedLabel() {
    return suggestedLabel;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onSnapshotRequested(this);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType() {
    return TYPE;
  }
}
