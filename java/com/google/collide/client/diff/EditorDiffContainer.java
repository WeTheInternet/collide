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

import com.google.collide.client.AppContext;
import com.google.collide.client.code.FileContent;
import com.google.collide.client.editor.Buffer;
import com.google.collide.client.editor.Buffer.ScrollListener;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.DiffChunkResponse;
import com.google.collide.dto.Revision;
import com.google.collide.dto.client.DtoClientImpls.RevisionImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.document.Document;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.CssResource;

import elemental.dom.Element;
import elemental.html.DivElement;

/**
 * Container for the diff editor which contains an editor for the before
 * document (on the left) and the after document (on the right).
 *
 *  TODO: In the future, we will support diffing across workspaces.
 * When we do so, we need to revisit this class and consider making its
 * terminology more general than "before" and "after".
 *
 */
public class EditorDiffContainer extends UiComponent<EditorDiffContainer.View>
    implements FileContent {

  /**
   * Static factory method for obtaining an instance of the EditorDiffContainer.
   */
  public static EditorDiffContainer create(AppContext appContext) {
    EditorDiffBundle editorBefore = new EditorDiffBundle(appContext);
    editorBefore.getEditor().setReadOnly(true);

    EditorDiffBundle editorAfter = new EditorDiffBundle(appContext);
    editorAfter.getEditor().setReadOnly(true);
    View view =
        new View(editorBefore.getEditor(), editorAfter.getEditor(), appContext.getResources());
    return new EditorDiffContainer(view, appContext, editorBefore, editorAfter);
  }

  public interface Css extends CssResource {
    String root();

    String diffColumn();

    String editorLeftContainer();

    String editorRightContainer();
  }

  public interface Resources extends DiffRenderer.Resources {
    @Source("EditorDiffContainer.css")
    Css editorDiffContainerCss();
  }

  /**
   * The view for the container for two editors.
   */
  public static class View extends CompositeView<Void> {

    DivElement editorLeftContainer;
    DivElement editorRightContainer;
    DivElement diffLeft;
    DivElement diffRight;
    DivElement root;

    public View(final Editor editorLeft, final Editor editorRight,
        EditorDiffContainer.Resources resources) {
      Css css = resources.editorDiffContainerCss();

      editorLeftContainer = Elements.createDivElement(css.editorLeftContainer());
      editorLeftContainer.appendChild(editorLeft.getElement());
      diffLeft = Elements.createDivElement(css.diffColumn());
      diffLeft.appendChild(editorLeftContainer);
      editorRightContainer = Elements.createDivElement(css.editorRightContainer());
      editorRightContainer.appendChild(editorRight.getElement());
      diffRight = Elements.createDivElement(css.diffColumn());
      diffRight.appendChild(editorRightContainer);

      root = Elements.createDivElement(css.root());
      root.appendChild(diffLeft);
      root.appendChild(diffRight);
      setElement(root);
    }
  }

  private final EditorDiffBundle editorBefore;
  private final EditorDiffBundle editorAfter;
  private final AppContext appContext;
  private PathUtil path;

  public static final Revision UNKNOWN_REVISION = RevisionImpl.make().setRootId("").setNodeId("");
  private Revision revisionBefore = UNKNOWN_REVISION;
  private Revision revisionAfter = UNKNOWN_REVISION;
  private Revision expectedRevisionBefore = UNKNOWN_REVISION;
  private Revision expectedRevisionAfter = UNKNOWN_REVISION;

  private EditorDiffContainer(View view, AppContext appContext, final EditorDiffBundle editorBefore,
      final EditorDiffBundle editorAfter) {
    super(view);
    this.appContext = appContext;
    this.editorBefore = editorBefore;
    this.editorAfter = editorAfter;

    this.editorBefore.getEditor().getBuffer().setVerticalScrollbarVisibility(false);

    editorBefore.getEditor().getBuffer().getScrollListenerRegistrar().add(new ScrollListener() {
      @Override
      public void onScroll(Buffer buffer, int scrollTop) {
        editorAfter.getEditor().getBuffer().setScrollTop(scrollTop);
      }
    });
    editorAfter.getEditor().getBuffer().getScrollListenerRegistrar().add(new ScrollListener() {
      @Override
      public void onScroll(Buffer buffer, int scrollTop) {
        editorBefore.getEditor().getBuffer().setScrollTop(scrollTop);
      }
    });
  }

  private static boolean isSameRevision(Revision revision, Revision expectedRevision) {
    return revision.getRootId().equals(expectedRevision.getRootId())
        && revision.getNodeId().equals(expectedRevision.getNodeId());
  }

  private boolean areRevisionsExpected(Revision revisionBefore, Revision revisionAfter) {
    return isSameRevision(revisionBefore, expectedRevisionBefore)
        && isSameRevision(revisionAfter, expectedRevisionAfter);
  }

  @Override
  public PathUtil filePath() {
    return path;
  }
  
  public void setExpectedRevisions(Revision revisionBefore, Revision revisionAfter) {
    expectedRevisionBefore = Preconditions.checkNotNull(revisionBefore);
    expectedRevisionAfter = Preconditions.checkNotNull(revisionAfter);
  }

  public boolean hasRevisions(Revision revisionBefore, Revision revisionAfter) {
    Preconditions.checkNotNull(revisionBefore);
    Preconditions.checkNotNull(revisionAfter);
    if (this.revisionBefore == UNKNOWN_REVISION || this.revisionAfter == UNKNOWN_REVISION) {
      return false;
    }
    return isSameRevision(revisionBefore, this.revisionBefore)
        && isSameRevision(revisionAfter, this.revisionAfter);
  }

  /**
   * Replace the before and after documents with the new documents formed by the
   * given diffChunks.
   *
   *  TODO: Handle line numbers properly.
   *
   *  TODO: Make the before editor read-only.
   *
   *  TODO: collaboration. Because we don't currently have an origin ID,
   * the diff editors will not be set up for collaboration, but this will only
   * be meaningful once we do diff merging, so we will implement it then.
   */
  public void setDiffChunks(PathUtil path, JsonArray<DiffChunkResponse> diffChunks,
      Revision revisionBefore, Revision revisionAfter) {
    if (!areRevisionsExpected(
        Preconditions.checkNotNull(revisionBefore), Preconditions.checkNotNull(revisionAfter))) {
      return;
    }
    this.revisionBefore = revisionBefore;
    this.revisionAfter = revisionAfter;

    this.path = path;

    Document beforeDoc = Document.createEmpty();
    Document afterDoc = Document.createEmpty();

    DiffRenderer beforeRenderer = new DiffRenderer(beforeDoc, appContext.getResources(), true);
    DiffRenderer afterRenderer = new DiffRenderer(afterDoc, appContext.getResources(), false);

    for (int i = 0, n = diffChunks.size(); i < n; i++) {
      DiffChunkResponse diffChunk = diffChunks.get(i);

      String beforeText = diffChunk.getBeforeData();
      String afterText = diffChunk.getAfterData();

      beforeRenderer.addDiffChunk(diffChunk.getDiffType(), beforeText);
      afterRenderer.addDiffChunk(diffChunk.getDiffType(), afterText);
    }

    // TODO: This setup is a bit awkward so that we can defer setting
    // the editor's document in order to workaround some editor bugs. Clean this
    // up once those bugs are resolved.
    editorBefore.setDocument(beforeDoc, path, beforeRenderer);
    editorAfter.setDocument(afterDoc, path, afterRenderer);
  }

  public int getScrollTop() {
    return editorBefore.getEditor().getBuffer().getScrollTop();
  }

  public void setScrollTop(int scrollTop) {
    editorBefore.getEditor().getBuffer().setScrollTop(scrollTop);
    editorAfter.getEditor().getBuffer().setScrollTop(scrollTop);
  }

  public void clearDiffEditors() {
    // TODO: update when setDocument(null) works
    editorBefore.getEditor().setDocument(Document.createEmpty());
    editorAfter.getEditor().setDocument(Document.createEmpty());

    revisionAfter = UNKNOWN_REVISION;
    revisionBefore = UNKNOWN_REVISION;
  }

  public PathUtil getPath() {
    return path;
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
  }

  @Override
  public void onContentDestroyed() {

  }
}
