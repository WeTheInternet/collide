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

package com.google.collide.client.diff;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.NodeConflictDto;
import com.google.collide.dto.NodeMutationDto;
import com.google.collide.dto.NodeMutationDto.MutationType;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.html.DivElement;

/**
 * Common, static functionality shared by the various diff-related presenters.
 *
 */
public class DiffCommon {

  public interface Css extends CssResource {
    String addedIcon();

    String conflictIcon();

    String deltaIcon();

    String iconLabel();

    String removedIcon();

    String resolvedIcon();

    String commonIcon();
  }

  public interface Resources extends BaseResources.Resources {
    @Source("added.png")
    ImageResource addedIcon();

    @Source("delta.png")
    ImageResource deltaIcon();

    @Source("DiffCommon.css")
    DiffCommon.Css diffCommonCss();

    @Source("removed.png")
    ImageResource removedIcon();

    @Source("resolved.png")
    ImageResource resolvedIcon();
  }

  /**
   * Sets the appropriate icon based on the type of changed file.
   *
   * @param changedNode
   */
  public static DivElement makeConflictIconDiv(Css css, NodeConflictDto changedNode) {
    DivElement icon = Elements.createDivElement();
    if (changedNode.getSimplifiedConflictType() !=
        NodeConflictDto.SimplifiedConflictType.RESOLVED) {
      icon.setClassName(css.conflictIcon());
    } else {
      icon.setClassName(css.resolvedIcon());
    }
    icon.addClassName(css.commonIcon());
    return icon;
  }

  /**
   * Sets the appropriate icon based on the type of changed file.
   */
  public static DivElement makeModifiedIconDiv(Css css, NodeMutationDto changedNode) {
    return makeModifiedIconDiv(css, changedNode.getMutationType());
  }

  /**
   * Sets the appropriate icon based on the type of changed file.
   */
  public static DivElement makeModifiedIconDiv(Css css, MutationType mutationType) {
    DivElement icon = Elements.createDivElement();
    switch (mutationType) {
      case ADDED:
      case COPIED:
      case COPIED_AND_EDITED:
        icon.setClassName(css.addedIcon());
        break;
      case DELETED:
        icon.setClassName(css.removedIcon());
        break;
      case EDITED:
      case MOVED:
      case MOVED_AND_EDITED:
        icon.setClassName(css.deltaIcon());
        break;
      default:
        Log.error(DiffCommon.class, "Unknown modification type " + mutationType);
        icon.setClassName(css.deltaIcon());
    }
    icon.addClassName(css.commonIcon());
    return icon;
  }

  private DiffCommon() {
    // Disallow instantiation.
  }
}
