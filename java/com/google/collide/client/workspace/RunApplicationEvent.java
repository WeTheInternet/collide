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

package com.google.collide.client.workspace;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event dispatched when the application should be run, for example when the run
 * button is clicked on a given workspace.
 */
public class RunApplicationEvent extends GwtEvent<RunApplicationEvent.Handler> {

  /**
   * Handler interface for getting notified when the run button in the Workspace
   * Header is clicked.
   */
  public interface Handler extends EventHandler {
    void onRunButtonClicked(RunApplicationEvent evt);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  private final String url;

  public RunApplicationEvent(String url) {
    this.url = url;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public String getUrl() {
    return url;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onRunButtonClicked(this);
  }
}
