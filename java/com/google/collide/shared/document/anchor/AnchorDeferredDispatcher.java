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

package com.google.collide.shared.document.anchor;

import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * Helper class to defer the dispatching of anchor callbacks until state has
 * stabilized.
 */
class AnchorDeferredDispatcher {

  private JsonArray<Anchor> shiftedAnchors;
  private JsonArray<Anchor> removedAnchors;
  
  void deferDispatchShifted(Anchor anchor) {
    if (removedAnchors != null && removedAnchors.contains(anchor)) {
      // Anchor has been removed, don't dispatch shift
      return;
    }
    
    if (shiftedAnchors == null) {
      shiftedAnchors = JsonCollections.createArray();
    }
    
    shiftedAnchors.add(anchor);
  }
  
  void deferDispatchRemoved(Anchor anchor) {
    if (removedAnchors == null) {
      removedAnchors = JsonCollections.createArray();
    }
   
    removedAnchors.add(anchor);
    
    // Anchor has been removed, don't dispatch these
    if (shiftedAnchors != null) {
      shiftedAnchors.remove(anchor);
    }
  }
  
  JsonArray<Anchor> getShiftedAnchors() {
    return shiftedAnchors;
  }
  
  void dispatch() {
    if (shiftedAnchors != null) {
      for (int i = 0, n = shiftedAnchors.size(); i < n; i++) {
        shiftedAnchors.get(i).dispatchShifted();
      }
    }
    
    if (removedAnchors != null) {
      for (int i = 0, n = removedAnchors.size(); i < n; i++) {
        removedAnchors.get(i).dispatchRemoved();
      }
    }
  }
}
