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

package com.google.collide.client.editor.renderer;

import com.google.collide.client.Resources;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.editor.FocusManager;
import com.google.collide.client.editor.ViewportModel;
import com.google.collide.client.editor.gutter.Gutter;
import com.google.collide.client.editor.renderer.ChangeTracker.ChangeType;
import com.google.collide.client.editor.selection.SelectionModel;
import com.google.collide.client.util.logging.Log;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.util.ListenerManager;
import com.google.collide.shared.util.ListenerRegistrar;

import java.util.EnumSet;

/**
 * A class that is the entry point for the rendering of the editor.
 *
 * The lifecycle of this class is tied to the current document. If the document
 * is replaced, a new instance of this class is created for the new document.
 *
 */
public class Renderer {

  public static Renderer create(Document document, ViewportModel viewport,
      Buffer buffer, Gutter leftGutter, SelectionModel selection, FocusManager focusManager,
      Editor editor, Resources res, RenderTimeExecutor renderTimeExecutor) {
    return new Renderer(document, viewport, buffer, leftGutter, selection, focusManager, 
      editor, res, renderTimeExecutor);
  }

  /**
   * Listener that is notified when the rendering is finished.
   */
  public interface CompletionListener {
    void onRenderCompleted();
  }

  /**
   * Listener that is notified on creation and garbage collection of a
   * rendered line.
   */
  public interface LineLifecycleListener {
    void onRenderedLineCreated(Line line, int lineNumber);
    void onRenderedLineGarbageCollected(Line line);
    void onRenderedLineShifted(Line line, int lineNumber);
  }

  private static final boolean ENABLE_PROFILING = false;
  
  private final ChangeTracker changeTracker;
  private final ListenerManager<CompletionListener> completionListenerManager;

  private final ListenerManager<LineLifecycleListener> lineLifecycleListenerManager;
  private final LineNumberRenderer lineNumberRenderer;

  private final ListenerManager.Dispatcher<CompletionListener> renderCompletedDispatcher =
      new ListenerManager.Dispatcher<CompletionListener>() {
        @Override
        public void dispatch(CompletionListener listener) {
          listener.onRenderCompleted();
        }
      };

  private final ViewportRenderer viewportRenderer;
  private final ViewportModel viewport;
  private final RenderTimeExecutor renderTimeExecutor;
  
  private Renderer(Document document, ViewportModel viewport, Buffer buffer,
      Gutter leftGutter, SelectionModel selection, FocusManager focusManager, 
      Editor editor, Resources res, RenderTimeExecutor renderTimeExecutor) {
    this.viewport = viewport;
    this.renderTimeExecutor = renderTimeExecutor;
    this.completionListenerManager = ListenerManager.create();
    this.lineLifecycleListenerManager = ListenerManager.create();
    this.changeTracker =
        new ChangeTracker(this, buffer, document, viewport, selection, focusManager);
    this.viewportRenderer = new ViewportRenderer(
            document, buffer, viewport, editor.getView(), lineLifecycleListenerManager);
    this.lineNumberRenderer = new LineNumberRenderer(buffer, res, leftGutter, viewport, selection,
        editor);
  }

  public void addLineRenderer(LineRenderer lineRenderer) {
    viewportRenderer.addLineRenderer(lineRenderer);
  }

  public ListenerRegistrar<CompletionListener> getCompletionListenerRegistrar() {
    return completionListenerManager;
  }

  public ListenerRegistrar<LineLifecycleListener> getLineLifecycleListenerRegistrar() {
    return lineLifecycleListenerManager;
  }

  public void removeLineRenderer(LineRenderer lineRenderer) {
    viewportRenderer.removeLineRenderer(lineRenderer);
  }

  public void renderAll() {
    viewportRenderer.render();
    renderTimeExecutor.executeQueuedCommands();
    handleRenderCompleted();
  }

  public void renderChanges() {
    if (ENABLE_PROFILING) {
      Log.markTimeline(getClass(), "Rendering changes...");
    }
    
    EnumSet<ChangeType> changes = changeTracker.getChanges();

    int viewportTopmostContentChangedLine =
        Math.max(viewport.getTopLineNumber(), changeTracker.getTopmostContentChangedLineNumber());

    if (changes.contains(ChangeType.VIEWPORT_LINE_NUMBER)) {
      if (ENABLE_PROFILING) {
        Log.markTimeline(getClass(), " - lineNumberRenderer...");
      }
      
      lineNumberRenderer.render();

      if (ENABLE_PROFILING) {
        Log.markTimeline(getClass(), " - renderViewportLineNumbersChanged...");
      }
      
      viewportRenderer.renderViewportLineNumbersChanged(changeTracker
          .getViewportLineNumberChangedEdges());
    }

    if (changes.contains(ChangeType.VIEWPORT_CONTENT)) {
      if (ENABLE_PROFILING) {
        Log.markTimeline(getClass(), " - renderViewportContentChange...");
      }
      
      viewportRenderer.renderViewportContentChange(viewportTopmostContentChangedLine,
          changeTracker.getViewportRemovedLines());

      if (changeTracker.hadContentChangeThatUpdatesFollowingLines()) {
        if (ENABLE_PROFILING) {
          Log.markTimeline(getClass(), " - renderLineAndFollowing...");
        }
        
        lineNumberRenderer.renderLineAndFollowing(viewportTopmostContentChangedLine);
      }
    }

    if (changes.contains(ChangeType.VIEWPORT_SHIFT)) {
      if (ENABLE_PROFILING) {
        Log.markTimeline(getClass(), " - renderViewportShift...");
      }
      
      viewportRenderer.renderViewportShift(false);
      lineNumberRenderer.render();
    }
    
    if (changes.contains(ChangeType.DIRTY_LINE)) {
      if (ENABLE_PROFILING) {
        Log.markTimeline(getClass(), " - renderDirtyLines...");
      }
      
      viewportRenderer.renderDirtyLines(changeTracker.getDirtyLines());
    }

    renderTimeExecutor.executeQueuedCommands();
    
    handleRenderCompleted();

    if (ENABLE_PROFILING) {
      Log.markTimeline(getClass(), " - Done rendering changes");
    }
  }

  private void handleRenderCompleted() {
    viewportRenderer.handleRenderCompleted();
    completionListenerManager.dispatch(renderCompletedDispatcher);
  }

  public void requestRenderLine(Line line) {
    changeTracker.requestRenderLine(line);
  }

  public void teardown() {
    changeTracker.teardown();
    viewportRenderer.teardown();
    lineNumberRenderer.teardown();
  }
}
