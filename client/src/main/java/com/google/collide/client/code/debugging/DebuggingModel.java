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

package com.google.collide.client.code.debugging;

import com.google.collide.client.util.logging.Log;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;

/**
 * The model for the debugging info, such as breakpoints and etc.
 */
public class DebuggingModel {

  /**
   * Callback interface for getting notified about changes to the debugging
   * model that have been applied by a controller.
   */
  public interface DebuggingModelChangeListener {

    /**
     * Notification that a breakpoint was added.
     */
    void onBreakpointAdded(Breakpoint newBreakpoint);

    /**
     * Notification that a breakpoint was removed.
     */
    void onBreakpointRemoved(Breakpoint oldBreakpoint);

    /**
     * Notification that a breakpoint was replaced.
     */
    void onBreakpointReplaced(Breakpoint oldBreakpoint, Breakpoint newBreakpoint);

    /**
     * Notification that the Pause-On-Exceptions mode was changed.
     */
    void onPauseOnExceptionsModeUpdated(PauseOnExceptionsMode oldMode,
        PauseOnExceptionsMode newMode);

    /**
     * Notification that the breakpoints-enabled flag was changed.
     */
    void onBreakpointsEnabledUpdated(boolean newValue);
  }

  private interface ChangeDispatcher {
    void dispatch(DebuggingModelChangeListener changeListener);
  }

  /**
   * Pause-On-Exceptions modes. Tells the debugger what to do if an exception is fired.
   */
  public enum PauseOnExceptionsMode {
    NONE, ALL, UNCAUGHT
  }

  private final JsonArray<DebuggingModelChangeListener> modelChangeListeners =
      JsonCollections.createArray();

  private final JsonArray<Breakpoint> breakpoints = JsonCollections.createArray();
  
  private PauseOnExceptionsMode pauseOnExceptionsMode = PauseOnExceptionsMode.NONE;

  private boolean breakpointsEnabled = true;

  public DebuggingModel() {
    Log.debug(getClass(), "Creating DebuggingModel.");    
  }

  /**
   * Adds a {@link DebuggingModelChangeListener} to be notified of mutations applied
   * by a controller to the underlying debugging model.
   *
   * @param modelChangeListener the listener we are adding
   */
  public void addModelChangeListener(DebuggingModelChangeListener modelChangeListener) {
    modelChangeListeners.add(modelChangeListener);
  }

  /**
   * Removes a {@link DebuggingModelChangeListener} from the set of listeners
   * subscribed to model changes.
   *
   * @param modelChangeListener the listener we are removing
   */
  public void removeModelChangeListener(DebuggingModelChangeListener modelChangeListener) {
    modelChangeListeners.remove(modelChangeListener);
  }

  /**
   * Adds a breakpoint to the debugging model.
   */
  public void addBreakpoint(final Breakpoint breakpoint) {
    Log.debug(getClass(), "Adding " + breakpoint);

    if (!breakpoints.contains(breakpoint)) {
      breakpoints.add(breakpoint);

      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(DebuggingModelChangeListener changeListener) {
          changeListener.onBreakpointAdded(breakpoint);
        }
      });
    }
  }

  /**
   * Removes a breakpoint from the debugging model.
   */
  public void removeBreakpoint(final Breakpoint breakpoint) {
    Log.debug(getClass(), "Removing " + breakpoint);

    if (breakpoints.remove(breakpoint)) {
      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(DebuggingModelChangeListener changeListener) {
          changeListener.onBreakpointRemoved(breakpoint);
        }
      });
    }
  }

  /**
   * Updates a breakpoint from the debugging model.
   */
  public void updateBreakpoint(final Breakpoint oldBreakpoint, final Breakpoint newBreakpoint) {
    Log.debug(getClass(), "Updating " + oldBreakpoint + " - to - " + newBreakpoint);

    if (oldBreakpoint.equals(newBreakpoint)) {
      return; // Nothing to do.
    }

    if (breakpoints.contains(oldBreakpoint)) {
      if (breakpoints.contains(newBreakpoint)) {
        removeBreakpoint(oldBreakpoint);
        return;
      }

      breakpoints.remove(oldBreakpoint);
      breakpoints.add(newBreakpoint);

      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(DebuggingModelChangeListener changeListener) {
          changeListener.onBreakpointReplaced(oldBreakpoint, newBreakpoint);
        }
      });
    }
  }

  /**
   * Sets the Pause-On-Exceptions mode.
   */
  public void setPauseOnExceptionsMode(PauseOnExceptionsMode mode) {
    Log.debug(getClass(), "Setting pause-on-exceptions " + mode);

    if (!pauseOnExceptionsMode.equals(mode)) {
      final PauseOnExceptionsMode oldMode = pauseOnExceptionsMode;
      final PauseOnExceptionsMode newMode = mode;

      pauseOnExceptionsMode = mode;

      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(DebuggingModelChangeListener changeListener) {
          changeListener.onPauseOnExceptionsModeUpdated(oldMode, newMode);
        }
      });
    }
  }

  /**
   * Enables or disables all breakpoints.
   */
  public void setBreakpointsEnabled(boolean value) {
    Log.debug(getClass(), "Setting enable-breakpoints to " + value);

    if (breakpointsEnabled != value) {
      breakpointsEnabled = value;

      dispatchModelChange(new ChangeDispatcher() {
        @Override
        public void dispatch(DebuggingModelChangeListener changeListener) {
          changeListener.onBreakpointsEnabledUpdated(breakpointsEnabled);
        }
      });
    }
  }

  /**
   * @return copy of all breakpoints in the given workspace
   */
  public JsonArray<Breakpoint> getBreakpoints() {
    return breakpoints.copy();
  }

  public int getBreakpointCount() {
    return breakpoints.size();
  }

  public PauseOnExceptionsMode getPauseOnExceptionsMode() {
    return pauseOnExceptionsMode;
  }

  public boolean isBreakpointsEnabled() {
    return breakpointsEnabled;
  }

  private void dispatchModelChange(ChangeDispatcher dispatcher) {
    JsonArray<DebuggingModelChangeListener> copy = modelChangeListeners.copy();
    for (int i = 0, n = copy.size(); i < n; i++) {
      dispatcher.dispatch(copy.get(i));
    }
  }
}
