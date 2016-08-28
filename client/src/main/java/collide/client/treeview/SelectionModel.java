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

package collide.client.treeview;

import org.waveprotocol.wave.client.common.util.SignalEvent;

import collide.client.treeview.Tree.Css;

import com.google.collide.client.util.logging.Log;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;

/*
 * TODO : Since we have breadcrumbs and will soon have tabs, we don't
 * need the notion of an active node that is separate from selected nodes.
 */
/**
 * Selection model for selected selected nodes in a {@link Tree}.
 *
 *  A Tree allows for multiple selected nodes with modifiers for ctrl and shift
 * click driven selects.
 */
public class SelectionModel<D> {

  private JsoArray<D> selectedNodes;
  private final NodeDataAdapter<D> dataAdapter;
  private final Css css;

  public SelectionModel(NodeDataAdapter<D> dataAdapter, Css css) {
    this.dataAdapter = dataAdapter;
    this.css = css;
    this.selectedNodes = JsoArray.create();
  }

  /**
   * Adds the node to the selection if it isn't already there in response to a
   * context selection.
   */
  public boolean contextSelect(D nodeData) {

    if (selectedNodes.isEmpty()) {

      // There are no selected nodes. So we should select.
      insertAndSelectNode(nodeData, 0, true);
      return true;
    }

    if (!hasSameParent(selectedNodes.get(0), nodeData) || selectedNodes.size() == 1) {
      return selectSingleNode(nodeData);
    }

    if (!selectedNodes.contains(nodeData)) {
      int insertionIndex = getInsertionIndex(nodeData);
      insertAndSelectNode(nodeData, insertionIndex, true);
      return true;
    }

    return false;
  }

  /**
   * Returns the list of selected nodes. Not a copy. So don't play fast and
   * loose mutating the list outside of this API!
   */
  public JsoArray<D> getSelectedNodes() {
    return selectedNodes;
  }

  public void removeNode(D nodeData) {
    selectedNodes.remove(nodeData);
  }

  /**
   * Restores visual selection for all selected nodes tracked by the
   * SelectionModel.
   */
  public JsonArray<JsonArray<String>> computeSelectedPaths() {
    JsoArray<JsonArray<String>> selectedPaths = JsoArray.create();
    for (int i = 0, n = selectedNodes.size(); i < n; i++) {
      D nodeData = selectedNodes.get(i);
      selectedPaths.add(dataAdapter.getNodePath(nodeData));
    }
    return selectedPaths;
  }

  /**
   * Adds the specified node to the list of selected nodes. The list of selected
   * nodes is guaranteed to be sorted in the same direction that the nodes
   * appear in the tree. We only allow nodes to be in the selected list that are
   * peers in the tree (we do not allow selects to span multiple depths in the
   * tree.
   *
   * Behavior: If no modifier key is depressed, the list of selected nodes will
   * be set to contain just the node passed to this method.
   *
   * Shift select: If shift is depressed, then we attempt to do a continuous
   * range select. If there exists one or more nodes in the selected nodes list,
   * we test if the node falls within the list. If it does not fall within, we
   * connect the contiguous range of nodes from the specified node to the
   * nearest selected node. If the node falls within the list, we do a
   * continuous range selection to the LAST node that was selected, not the
   * closest.
   *
   * CTRL select: If CTRL is depressed then we simply search for the insertion
   * point of the specified node in the already sorted select list. If the node
   * is already present, then we remove it and unselect the node. If it was not
   * present, then we insert the node at the appropriate spot in the array and
   * select it.
   *
   * @param nodeData the node to select
   * @param event the DOM event that was associated with the select trigger.
   *        This is needed to detect modifier keys. If {@code null} then we
   *        assume that we are appending to the selection and behave just like a
   *        CTRL-click.
   * @return whether or not the select region changed at all.
   */
  public boolean selectNode(D nodeData, SignalEvent event) {
    if (selectedNodes.isEmpty()) {

      // There are no selected nodes. So we should select.
      insertAndSelectNode(nodeData, 0, true);
      return true;
    }

    // Ensure that the node we are selecting is a child of the same
    // directory of the other nodes.
    if (!hasSameParent(selectedNodes.get(0), nodeData)) {
      return selectSingleNode(nodeData);
    }

    // So we are guaranteed to have a node that is a peer of the current set of
    // nodes. Now we must examine modifier keys.
    if (event == null || event.getCommandKey()) {
      ctrlSelect(nodeData);
      return true;
    } else {
      if (event.getShiftKey()) {
        return shiftSelect(nodeData);
      }
    }

    // Neither a shift nor a ctrl select. So replace the contents of the
    // selected list with this node.
    return selectSingleNode(nodeData);
  }

  /**
   * Clears the the current selection and selects a single node.
   *
   * @return returns whether or not we actually changed the selection.
   */
  public boolean selectSingleNode(D nodeData) {

    // This is the case where we have a single node selected, and it is the same
    // one we are clicking. We do nothing in this case.
    if ((selectedNodes.size() == 1) && (selectedNodes.get(0).equals(nodeData))) {
      return false;
    }

    clearSelections();
    insertAndSelectNode(nodeData, 0, true);
    return true;
  }

  public void clearSelections() {
    visuallySelect(selectedNodes, false);
    selectedNodes.clear();
  }

  /**
   * Collects all nodes in a continuous range from the start node to the end
   * node (assuming that the nodes are peers) obeying the inclusion boolean
   * params for the boundaries. These nodes are not allowed to be in the
   * selected list.
   */
  private JsoArray<D> collectRangeToSelect(
      D startNode, D endNode, boolean includeStart, boolean includeEnd) {
    D parentNode = dataAdapter.getParent(startNode);

    // Do some debug compile sanity checking.
    assert (parentNode != null) : "Null parent nmode when doing range select!";
    assert (parentNode.equals(
        dataAdapter.getParent(endNode))) : "Different parent nodes when doing range highlgiht!";
    assert (dataAdapter.compare(startNode, endNode) <= 0) :
        "Nodes are in reverse order for range select! " + dataAdapter.getNodeName(startNode) + " - "
        + dataAdapter.getNodeName(endNode);

    JsoArray<D> range = JsoArray.create();

    // Do a linear scan until we find the startNode.
    JsonArray<D> children = dataAdapter.getChildren(parentNode);
    int i = 0;
    boolean adding = false;
    for (int n = children.size(); i < n; i++) {
      D child = children.get(i);
      if (child.equals(startNode)) {
        adding = true;

        if (includeStart) {
          range.add(child);
        }

        continue;
      }

      if (adding) {
        if (child.equals(endNode)) {
          if (!includeEnd) {
            break;
          }

          range.add(child);
          break;
        }

        range.add(child);
      }
    }

    // Sanity check
    if (i == children.size()) {
      Log.error(getClass(), "Failed to find the start when doing a range selection. Start:",
          startNode, " End:", endNode);
    }

    return range;
  }

  /**
   * If CTRL is depressed then we simply search for the insertion point of the
   * specified node in the already sorted select list. If the node is already
   * present, then we remove it and unselect the node. If it was not present,
   * then we insert the node at the appropriate spot in the array and select it.
   */
  private void ctrlSelect(D nodeData) {
    // Find the relevant spot in the list of selected nodes.
    int insertionIndex = getInsertionIndex(nodeData);

    // Either select or not select depending on whether or not it was
    // already present in the list.
    insertAndSelectNode(
        nodeData, insertionIndex, !nodeData.equals(selectedNodes.get(insertionIndex)));
  }

  private int getInsertionIndex(D nodeData) {
    int insertionIndex = 0;
    while (insertionIndex < selectedNodes.size()
        && dataAdapter.compare(nodeData, selectedNodes.get(insertionIndex)) > 0) {
      insertionIndex++;
    }
    return insertionIndex;
  }

  private boolean hasSameParent(D a, D b) {
    D parent1 = dataAdapter.getParent(a);
    D parent2 = dataAdapter.getParent(b);
    return parent1 == parent2 || (parent1 != null && parent1.equals(parent2));
  }

  private void insertAndSelectNode(D nodeData, int insertionIndex, boolean selectingNewNode) {

    // Visually represent it.
    visuallySelect(nodeData, selectingNewNode);

    // Update the model.
    if (selectingNewNode) {

      // The node was not in the list. Add it.
      selectedNodes.splice(insertionIndex, 0, nodeData);
    } else {

      // The node was already in the list. Take it out.
      selectedNodes.splice(insertionIndex, 1);
    }
  }

  /**
   * If shift is depressed, then we attempt to do a continuous range select. If
   * there exists one or more nodes in the selected nodes list, we test if the
   * node falls within the list. If it does not fall within, we connect the
   * contiguous range of nodes from the specified node to the nearest selected
   * node. If the node falls within the list, we do a continuous range selection
   * to the LAST node that was selected, not the closest.
   */
  private boolean shiftSelect(D nodeData) {

    // We are guaranteed to have at least one node in the list.
    D firstNode = selectedNodes.get(0);
    D lastNode = selectedNodes.get(selectedNodes.size() - 1);

    int comparisonToFirst = dataAdapter.compare(nodeData, firstNode);
    int comparisonToLast = dataAdapter.compare(nodeData, lastNode);

    // If it is to the left.
    if (comparisonToFirst < 0) {
      JsoArray<D> range = collectRangeToSelect(nodeData, firstNode, true, false);
      visuallySelect(range, true);
      selectedNodes = JsoArray.concat(range, selectedNodes);
      return true;
    }

    // If it is to the right.
    if (comparisonToLast > 0) {
      JsoArray<D> range = collectRangeToSelect(lastNode, nodeData, false, true);
      visuallySelect(range, true);
      selectedNodes = JsoArray.concat(selectedNodes, range);
      return true;
    }

    // If it is somewhere in between, or on the boundary.
    if (comparisonToFirst >= 0 && comparisonToLast <= 0) {

      // Clear the set of selected nodes.
      clearSelections();
      selectedNodes = collectRangeToSelect(nodeData, lastNode, true, true);
      visuallySelect(selectedNodes, true);
      return true;
    }

    assert false : "SelectionModel#shiftSelect(D): This should be unreachable!";

    return false;
  }

  private void visuallySelect(D nodeData, boolean isSelected) {
    TreeNodeElement<D> renderedNode = dataAdapter.getRenderedTreeNode(nodeData);
    if (renderedNode != null) {
      renderedNode.setSelected(isSelected, css);
    }
  }

  private void visuallySelect(JsonArray<D> nodeDatas, boolean isSelected) {
    for (int i = 0, n = nodeDatas.size(); i < n; i++) {
      visuallySelect(nodeDatas.get(i), isSelected);
    }
  }
}
