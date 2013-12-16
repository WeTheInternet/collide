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

import collide.client.common.CommonResources;
import collide.client.treeview.NodeRenderer;
import collide.client.treeview.Tree;
import collide.client.treeview.TreeNodeElement;
import collide.client.treeview.TreeNodeMutator;
import collide.client.util.CssUtils;
import collide.client.util.Elements;

import com.google.collide.client.workspace.WorkspaceUtils;
import com.google.collide.dto.DirInfo;
import com.google.collide.json.client.JsoStringMap;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MouseEvent;
import elemental.html.AnchorElement;
import elemental.html.SpanElement;

/**
 * Renderer for nodes in the file tree.
 */
public class FileTreeNodeRenderer implements NodeRenderer<FileTreeNode> {

  public static FileTreeNodeRenderer create(Resources res) {
    return new FileTreeNodeRenderer(res);
  }

  public interface Css extends TreeNodeMutator.Css {
    String file();
    
    String cssIcon();
    
    String emptyFolder();
    
    String emptyPackageIcon();

    String folder();
    
    String htmlIcon();
    
    String jarIcon();
    
    String javaIcon();
    
    String javascriptIcon();
    
    String xmlIcon();
    
    String packageIcon();

    String folderOpen();

    String folderLoading();

    String icon();

    String label();

    String root();

    @Override
    String nodeNameInput();
  }

  public interface Resources extends CommonResources.BaseResources, Tree.Resources {
    @Source({"FileTreeNodeRenderer.css", "collide/client/common/constants.css"})
    Css workspaceNavigationFileTreeNodeRendererCss();
  }

  /**
   * Renders the given information as a node.
   * @param fileTypes 
   * @param b 
   *
   * @param mouseDownListener an optional listener to be attached to the anchor. If not given, the
   *        label will not be an anchor.
   */
  public static SpanElement renderNodeContents(
      Css css, String name, boolean isFile, boolean isPackage, JsoStringMap<String> fileTypes, EventListener mouseDownListener, boolean renderIcon) {

    SpanElement root = Elements.createSpanElement(css.root());
    if (renderIcon) {
      SpanElement icon = Elements.createSpanElement(css.icon());
      if (isFile) {
        String clsName = css.file();
        int ind = name.lastIndexOf('.');
        if (ind > -1) {
          String type = name.substring(ind+1);
          if (fileTypes.containsKey(type)) {
            clsName = fileTypes.get(type);
          }
        }
        icon.addClassName(clsName);
      } else {
        if (isPackage) {
          icon.addClassName(css.packageIcon());
        } else {
          icon.addClassName(css.folder());
        }
      }
      root.appendChild(icon);
    }

    final Element label;
    if (mouseDownListener != null) {
      label = Elements.createAnchorElement(css.label());
      ((AnchorElement) label).setHref("javascript:;");
      label.addEventListener(Event.MOUSEDOWN, mouseDownListener, false);
    } else {
      label = Elements.createSpanElement(css.label());
    }

    label.setTextContent(name);

    root.appendChild(label);

    return root;
  }

  private final EventListener mouseDownListener = new EventListener() {
    @Override
    public void handleEvent(Event evt) {
      MouseEvent event = (MouseEvent) evt;
      AnchorElement anchor = (AnchorElement) evt.getTarget();

      if (event.getButton() == MouseEvent.Button.AUXILIARY) {
        Element parent = CssUtils.getAncestorOrSelfWithClassName(anchor, res.treeCss().treeNode());

        if (parent != null) {
          @SuppressWarnings("unchecked")
          TreeNodeElement<FileTreeNode> fileNode = (TreeNodeElement<FileTreeNode>) parent;
          anchor.setHref(
              WorkspaceUtils.createDeepLinkToFile(fileNode.getData().getNodePath()));
        }
      }
    }
  };

  private final Css css;

  private final Resources res;
  
  private final JsoStringMap<String> fileTypes;

  private FileTreeNodeRenderer(Resources resources) {
    this.res = resources;
    this.css = res.workspaceNavigationFileTreeNodeRendererCss();
    fileTypes = createFileTypeMap(css);
  }

  public static JsoStringMap<String> createFileTypeMap(Css css) {
    JsoStringMap<String> fileTypes = JsoStringMap.create();
    fileTypes.put("css", css.cssIcon());
    fileTypes.put("html", css.htmlIcon());
    fileTypes.put("jar", css.jarIcon());
    fileTypes.put("java", css.javaIcon());
    fileTypes.put("js", css.javascriptIcon());
    return fileTypes;
  }

  @Override
  public Element getNodeKeyTextContainer(SpanElement treeNodeLabel) {
    return (Element) treeNodeLabel.getChildNodes().item(1);
  }

  @Override
  public SpanElement renderNodeContents(FileTreeNode data) {
    if (data.isDirectory()) {
      DirInfo dir = (DirInfo) data;
      return renderNodeContents(css, data.getName(), data.isFile(), dir.isPackage(), fileTypes, mouseDownListener, true);
    } else {
      return renderNodeContents(css, data.getName(), data.isFile(), false, fileTypes, mouseDownListener, true);
    }
  }

  @Override
  public void updateNodeContents(TreeNodeElement<FileTreeNode> treeNode) {
    if (treeNode.getData().isDirectory()) {
      // Update folder icon based on icon state.
      Element icon = treeNode.getNodeLabel().getFirstChildElement();
      icon.setClassName(css.icon());
      if (treeNode.getData().isLoading()) {
        icon.addClassName(css.folderLoading());
      } else if (((DirInfo)treeNode.getData()).isPackage()) {
        icon.addClassName(css.packageIcon());
      } else if (treeNode.isOpen()) {
        icon.addClassName(css.folderOpen());
      } else {
        icon.addClassName(css.folder());
      }
    }
  }
}
