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

import com.google.collide.client.workspace.WorkspacePlace;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Simple event that is fired within a {@link WorkspacePlace} that toggles the
 * right sidebar on the workspace.
 *
 */
public class RightSidebarToggleEvent extends GwtEvent<RightSidebarToggleEvent.Handler> {

  /**
   * Handler interface for getting notified when the right sidebar is toggled.
   */
  public interface Handler extends EventHandler {
    void onRightSidebarToggled(RightSidebarToggleEvent evt);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRightSidebarToggled(this);
  }
}
