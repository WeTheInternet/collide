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

import com.google.collide.client.util.PathUtil;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Simple event that is fired within a {@link WorkspacePlace} when the user
 * clicks on the "upload" menu item(s).
 *
 */
public class UploadClickedEvent extends GwtEvent<UploadClickedEvent.Handler> {

  public interface Handler extends EventHandler {
    void onUploadClicked(UploadClickedEvent evt);
  }

  public enum UploadType {
    FILE, ZIP, DIRECTORY
  }
  
  public static final Type<Handler> TYPE = new Type<Handler>();

  private final UploadType uploadType;
  private final PathUtil targetPath;

  public UploadClickedEvent(UploadType uploadType, PathUtil targetPath) {
    this.uploadType = uploadType;
    this.targetPath = targetPath;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  public PathUtil getTargetPath() {
    return targetPath;
  }

  public UploadType getUploadType() {
    return uploadType;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onUploadClicked(this);
  }
}
