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

package com.google.collide.client.workspace.outline;

import static com.google.collide.shared.grok.GrokUtils.findFileCodeBlock;
import collide.client.treeview.Tree;
import collide.client.treeview.TreeNodeElement;

import com.google.collide.client.codeunderstanding.CubeClient;
import com.google.collide.client.codeunderstanding.CubeData;
import com.google.collide.client.codeunderstanding.CubeDataUpdates;
import com.google.collide.client.codeunderstanding.CubeUpdateListener;
import com.google.collide.client.documentparser.DocumentParser;
import com.google.collide.client.editor.Editor;
import com.google.collide.codemirror2.SyntaxType;
import com.google.collide.dto.CodeBlock;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.LineFinder;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.common.base.Preconditions;

import elemental.js.html.JsDragEvent;

/**
 * Controller object that is directly or indirectly notified about events
 * and performs corresponding over outline model.
 */
public class OutlineController implements CubeUpdateListener, Tree.Listener<OutlineNode>,
    OutlineConsumer {

  private final OutlineModel model;
  private final CubeClient cubeClient;
  private final Editor editor;
  private boolean acceptCubeUpdates;
  private OutlineParser currentOutlineParser;
  private OutlineBuilder currentOutlineBuilder;
  private final OutlineNode emptyRoot;

  public OutlineController(OutlineModel model, CubeClient cubeClient, Editor editor) {
    this.editor = editor;
    Preconditions.checkNotNull(model);
    Preconditions.checkNotNull(cubeClient);
    this.model = model;
    this.cubeClient = cubeClient;
    emptyRoot = new OutlineNode("empty-root", OutlineNode.OutlineNodeType.ROOT, null, 0, 0);
  }

  @Override
  public void onCubeResponse(CubeData data, CubeDataUpdates updates) {
    if (!acceptCubeUpdates) {
      return;
    }
    if (model.getRoot() != null && !updates.isFileTree()) {
      return;
    }
    updateFileTree(data);
  }

  private void updateFileTree(CubeData data) {
    Preconditions.checkNotNull(currentOutlineBuilder);
    CodeBlock fileTree = data.getFileTree();
    if (fileTree == null) {
      fileTree = findFileCodeBlock(data.getFullGraph(), data.getFilePath());
    }
    if (fileTree == null) {
      return;
    }
    Document document = editor.getDocument();
    model.updateRoot(currentOutlineBuilder.build(fileTree, document), document);
  }


  public void cleanup() {
    if (acceptCubeUpdates) {
      cubeClient.removeListener(this);
      acceptCubeUpdates = false;
    }
    if (currentOutlineParser != null) {
      currentOutlineParser.cleanup();
      currentOutlineParser = null;
    }
    model.setListener(null);
    model.cleanup();
  }

  public void onDocumentChanged(DocumentParser parser) {
    model.cleanup();
    SyntaxType syntax = (parser != null) ? parser.getSyntaxType() : SyntaxType.NONE;

    boolean oldAcceptCubeUpdates = acceptCubeUpdates;
    if (currentOutlineParser != null) {
      currentOutlineParser.cleanup();
      currentOutlineParser = null;
    }

    switch (syntax) {
      case JS:
        currentOutlineBuilder = new JsOutlineBuilder();
        acceptCubeUpdates = true;
        break;

      case PY:
        currentOutlineBuilder = new PyOutlineBuilder();
        acceptCubeUpdates = true;
        break;

      case CSS:
        acceptCubeUpdates = false;
        currentOutlineParser = new CssOutlineParser(parser.getListenerRegistrar(), this);
        model.updateRoot(currentOutlineParser.getRoot(), editor.getDocument());
        break;

      default:
        acceptCubeUpdates = false;
        model.updateRoot(emptyRoot, editor.getDocument());
    }

    if (acceptCubeUpdates != oldAcceptCubeUpdates) {
      if (acceptCubeUpdates) {
        cubeClient.addListener(this);
      } else {
        cubeClient.removeListener(this);
      }
    }
    if (acceptCubeUpdates) {
      updateFileTree(cubeClient.getData());
    }
  }
  
  @Override
  public void onNodeAction(TreeNodeElement<OutlineNode> node) {
    OutlineNode data = node.getData();
    Anchor anchor = data.getAnchor();
    if (anchor == null) {
      return;
    }
    if (anchor.isAttached()) {
      // TODO: check that item is still there,
      //               see comments in OutlineNodeBuilder.
      Line line = anchor.getLine();
      if (line.getText().contains(data.getName())) {
        editor.getFocusManager().focus();
        LineFinder lineFinder = editor.getDocument().getLineFinder();
        editor.scrollTo(lineFinder.findLine(line).number(), anchor.getColumn());
        return;
      }
    }
    // If we didn't find what we were looking for, then:
    // 1) render node as disabled
    model.setDisabled(data);
    // TODO: 2) schedule navigation for next data update
  }

  @Override
  public void onNodeClosed(TreeNodeElement<OutlineNode> node) {
    // do nothing
  }

  @Override
  public void onNodeContextMenu(int mouseX, int mouseY, TreeNodeElement<OutlineNode> node) {
    // do nothing
  }

  @Override
  public void onNodeDragDrop(TreeNodeElement<OutlineNode> node, JsDragEvent event) {
    // do nothing
  }

  @Override
  public void onRootDragDrop(JsDragEvent event) {
    // do nothing
  }

  @Override
  public void onNodeExpanded(TreeNodeElement<OutlineNode> node) {
    // do nothing
  }

  @Override
  public void onRootContextMenu(int mouseX, int mouseY) {
    // do nothing
  }

  @Override
  public void onOutlineParsed(JsonArray<OutlineNode> nodes) {
    if (acceptCubeUpdates) {
      return;
    }

    model.setRootChildren(nodes);
  }

  @Override
  public void onNodeDragStart(TreeNodeElement<OutlineNode> node, JsDragEvent event) {
    // do nothing
  }
}
