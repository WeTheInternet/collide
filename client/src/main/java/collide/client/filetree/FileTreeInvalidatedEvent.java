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

package collide.client.filetree;

import com.google.collide.client.util.PathUtil;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * An event that describes an invalidation to the entire or part of the file tree. If the
 * invalidation is at the root of the file tree ({@link #getInvalidatedPath()} is {@code /}), then
 * conflicts are invalidated also.
 */
public class FileTreeInvalidatedEvent extends GwtEvent<FileTreeInvalidatedEvent.Handler> {

  public interface Handler extends EventHandler {
    void onFileTreeInvalidated(PathUtil invalidatedPath);
  }

  public static final Type<Handler> TYPE = new Type<Handler>();

  /**
   * The subtree that was invalidated; '/' means the entire file tree (including conflicts)
   */
  private final PathUtil invalidatedPath;

  public FileTreeInvalidatedEvent(PathUtil invalidatedPath) {
    this.invalidatedPath = invalidatedPath;
  }

  @Override
  public Type<Handler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(Handler handler) {
    handler.onFileTreeInvalidated(invalidatedPath);
  }

  public PathUtil getInvalidatedPath() {
    return invalidatedPath;
  }
}
