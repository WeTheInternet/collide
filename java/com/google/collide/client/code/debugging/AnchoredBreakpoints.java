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

import com.google.collide.client.util.ScheduledCommandExecutor;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.document.anchor.AnchorUtils;
import com.google.collide.shared.util.JsonCollections;

import java.util.Comparator;

/**
 * Handles an array of breakpoints anchored into a {@link Document}.
 */
class AnchoredBreakpoints {

  private static final AnchorType BREAKPOINT_ANCHOR_TYPE = AnchorType.create(
      AnchoredBreakpoints.class, "breakpoint");

  /**
   * A listener that is called when a line that has a breakpoint set on it
   * changes.
   */
  public interface BreakpointDescriptionListener {
    void onBreakpointDescriptionChange(Breakpoint breakpoint, String newText);
  }

  private static final Comparator<Anchor> anchorComparator = new Comparator<Anchor>() {
    @Override
    public int compare(Anchor o1, Anchor o2) {
      Breakpoint b1 = o1.getValue();
      Breakpoint b2 = o2.getValue();
      return b1.getLineNumber() - b2.getLineNumber();
    }
  };

  private abstract static class AnchorBatchCommandExecutor extends ScheduledCommandExecutor {
    private JsonArray<Anchor> updatedAnchors = JsonCollections.createArray();

    @Override
    protected final void execute() {
      if (updatedAnchors.isEmpty()) {
        return;
      }

      JsonArray<Anchor> anchors = updatedAnchors;
      updatedAnchors = JsonCollections.createArray();

      executeBatchCommand(anchors);
    }

    protected abstract void executeBatchCommand(JsonArray<Anchor> anchors);

    public void addAnchor(Anchor anchor) {
      if (BREAKPOINT_ANCHOR_TYPE.equals(anchor.getType()) && !updatedAnchors.contains(anchor)) {
        updatedAnchors.add(anchor);
        scheduleDeferred();
      }
    }

    public void removeAnchor(Anchor anchor) {
      updatedAnchors.remove(anchor);
    }

    public void teardown() {
      updatedAnchors.clear();
      cancel();
    }
  }

  private final AnchorBatchCommandExecutor anchorsLineShiftedCommand =
      new AnchorBatchCommandExecutor() {
        @Override
        protected void executeBatchCommand(JsonArray<Anchor> anchors) {
          applyMovedAnchors(anchors);
        }
      };

  private final Anchor.ShiftListener anchorShiftListener = new Anchor.ShiftListener() {
    @Override
    public void onAnchorShifted(Anchor anchor) {
      anchorsLineShiftedCommand.addAnchor(anchor);
    }
  };

  private final AnchorBatchCommandExecutor anchorChangedCommand = new AnchorBatchCommandExecutor() {
    @Override
    protected void executeBatchCommand(JsonArray<Anchor> anchors) {
      applyUpdatedAnchors(anchors);
    }
  };

  /**
   * Listener of the document text changes to track breakpoint descriptions.
   */
  private class TextListenerImpl implements Document.TextListener, AnchorManager.AnchorVisitor {
    @Override
    public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
      for (int i = 0, n = textChanges.size(); i < n; ++i) {
        TextChange textChange = textChanges.get(i);
        Line line = textChange.getLine();
        Line stopAtLine = textChange.getEndLine().getNextLine();
        while (line != stopAtLine) {
          AnchorUtils.visitAnchorsOnLine(line, this);
          line = line.getNextLine();
        }
      }
    }

    @Override
    public void visitAnchor(Anchor anchor) {
      anchorChangedCommand.addAnchor(anchor);
    }
  }

  private final Document document;
  private final DebuggingModel debuggingModel;
  private final JsonArray<Breakpoint> breakpoints = JsonCollections.createArray();
  private final JsonArray<Anchor> anchors = JsonCollections.createArray();
  private final Document.TextListener documentTextListener = new TextListenerImpl();
  private BreakpointDescriptionListener breakpointDescriptionListener;

  AnchoredBreakpoints(DebuggingModel debuggingModel, Document document) {
    this.debuggingModel = debuggingModel;
    this.document = document;
  }

  void teardown() {
    document.getTextListenerRegistrar().remove(documentTextListener);
    for (int i = 0, n = anchors.size(); i < n; ++i) {
      detachAnchor(anchors.get(i));
    }
    breakpoints.clear();
    anchors.clear();
    anchorsLineShiftedCommand.teardown();
    anchorChangedCommand.teardown();
    breakpointDescriptionListener = null;
  }

  public void setBreakpointDescriptionListener(BreakpointDescriptionListener listener) {
    this.breakpointDescriptionListener = listener;
  }

  public Anchor anchorBreakpoint(Breakpoint breakpoint) {
    LineInfo lineInfo = document.getLineFinder().findLine(breakpoint.getLineNumber());

    Anchor anchor = document.getAnchorManager().createAnchor(BREAKPOINT_ANCHOR_TYPE,
        lineInfo.line(), lineInfo.number(), AnchorManager.IGNORE_COLUMN);
    anchor.setRemovalStrategy(Anchor.RemovalStrategy.SHIFT);
    anchor.setValue(breakpoint);
    anchor.getShiftListenerRegistrar().add(anchorShiftListener);

    if (anchors.isEmpty()) {
      document.getTextListenerRegistrar().add(documentTextListener);
    }

    breakpoints.add(breakpoint);
    anchors.add(anchor);

    return anchor;
  }

  public boolean removeBreakpoint(Breakpoint breakpoint) {
    for (int i = 0, n = breakpoints.size(); i < n; ++i) {
      if (breakpoint.equals(breakpoints.get(i))) {
        detachAnchor(anchors.get(i));
        breakpoints.remove(i);
        anchors.remove(i);
        if (anchors.isEmpty()) {
          document.getTextListenerRegistrar().remove(documentTextListener);
        }
        return true;
      }
    }
    return false;
  }

  public boolean contains(Breakpoint breakpoint) {
    return breakpoints.contains(breakpoint);
  }

  public Breakpoint get(int index) {
    return breakpoints.get(index);
  }

  public int size() {
    return breakpoints.size();
  }

  private void detachAnchor(Anchor anchor) {
    document.getAnchorManager().removeAnchor(anchor);
    anchorsLineShiftedCommand.removeAnchor(anchor);
    anchorChangedCommand.removeAnchor(anchor);
    anchor.setValue(null);
    anchor.getShiftListenerRegistrar().remove(anchorShiftListener);
  }

  private void applyMovedAnchors(JsonArray<Anchor> anchors) {
    // We have to determine what breakpoint to move first in case if we have a
    // few consecutive breakpoints set. For example, if we have breakpoints at
    // lines 3,4,5, and we insert a new line at the beginning of the document,
    // then we should start updating the breakpoints from the end:
    // 5->6, 4->5, 3->4, so that not to mess up with the original breakpoints.
    // Below is a simple strategy to ensure the correct iteration order in most
    // cases (when all breakpoints move in the same direction). In those rare
    // cases when we might loose some breakpoints (for example, due to
    // collaborative editing), we consider this tolerable.
    anchors.sort(anchorComparator);

    int deltaSum = 0;
    for (int i = 0, n = anchors.size(); i < n; ++i) {
      Anchor anchor = anchors.get(i);
      Breakpoint breakpoint = anchor.getValue();
      deltaSum += anchor.getLineNumber() - breakpoint.getLineNumber();
    }

    for (int i = 0, n = anchors.size(); i < n; ++i) {
      Anchor anchor = anchors.get(deltaSum < 0 ? i : n - 1 - i);
      Breakpoint oldBreakpoint = anchor.getValue();
      Breakpoint newBreakpoint = new Breakpoint.Builder(oldBreakpoint)
          .setLineNumber(anchor.getLineNumber())
          .build();
      debuggingModel.updateBreakpoint(oldBreakpoint, newBreakpoint);
    }
  }

  private void applyUpdatedAnchors(JsonArray<Anchor> anchors) {
    if (breakpointDescriptionListener == null) {
      return;
    }

    for (int i = 0, n = anchors.size(); i < n; ++i) {
      Anchor anchor = anchors.get(i);
      Breakpoint breakpoint = anchor.getValue();
      String newText = anchor.getLine().getText();
      breakpointDescriptionListener.onBreakpointDescriptionChange(breakpoint, newText);
    }
  }
}
