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

package com.google.collide.client.code.autocomplete;

import com.google.collide.client.code.autocomplete.AutocompleteProposals.ProposalWithContext;

/**
 * Interface used to isolate {@link Autocompleter} from UI implementation.
 */
public interface AutocompleteBox {

  /**
   * Interface that allows {@link AutocompleteBox} implementations
   * to fire back (UI event based) notifications.
   */
  interface Events {

    /**
     * Performs autocompletion selected by user.
     */
    void onSelect(ProposalWithContext proposal);

    /**
     * Closes autocompletion box.
     */
    void onCancel();
  }

  /**
   * Tests if box is shown.
   */
  boolean isShowing();

  /**
   * Reacts on keyboard event.
   *
   * @return {@code true} if event should not be processed further.
   */
  boolean consumeKeySignal(SignalEventEssence signal);

  /**
   * Sets delegate instance that is notified on user actions.
   */
  void setDelegate(Events delegate);

  /**
   * Hides component.
   */
  void dismiss();

  /**
   * Shows component (if hidden) and updates proposals list.
   */
  void positionAndShow(AutocompleteProposals items);
}
