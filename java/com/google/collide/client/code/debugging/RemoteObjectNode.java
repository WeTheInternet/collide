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

import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.client.ui.tree.TreeNodeElement;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.SortedList;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * Represents a {@link RemoteObject} node in the tree UI.
 *
 */
class RemoteObjectNode implements Comparable<RemoteObjectNode> {

  private static final String GETTER_PROPERTY_PREFIX = "get ";
  private static final String SETTER_PROPERTY_PREFIX = "set ";
  private static final String PROTO_PROPERTY_NAME = "__proto__";
  private static final RegExp CHUNK_FROM_BEGINNING = RegExp.compile("(^\\d+)|(^\\D+)");

  private static final SortedList.Comparator<RemoteObjectNode> SORTING_FUNCTION =
      new SortedList.Comparator<RemoteObjectNode>() {
        @Override
        public int compare(RemoteObjectNode a, RemoteObjectNode b) {
          return a.compareTo(b);
        }
      };

  private String name;
  private final int orderIndex;
  private final SortedList<RemoteObjectNode> children;
  private final RemoteObject remoteObject;
  private final boolean wasThrown;
  private final boolean isDeletable;
  private final boolean isWritable;
  private final boolean isEnumerable;
  private final String getterOrSetterName;
  private final boolean isTransient;
  private boolean shouldRequestChildren;

  public static RemoteObjectNode createRoot() {
    return new Builder("/")
        .setHasChildren(true)
        .setDeletable(false)
        .build();
  }

  public static RemoteObjectNode createGetterProperty(String name, RemoteObject getterFunction) {
    return new Builder(GETTER_PROPERTY_PREFIX + name, getterFunction)
        .setGetterOrSetterName(name)
        .build();
  }

  public static RemoteObjectNode createSetterProperty(String name, RemoteObject setterFunction) {
    return new Builder(SETTER_PROPERTY_PREFIX + name, setterFunction)
        .setGetterOrSetterName(name)
        .build();
  }

  public static RemoteObjectNode createBeingEdited() {
    return new Builder("")
        .setOrderIndex(Integer.MAX_VALUE)
        .build();
  }

  private static RemoteObjectNode createNoPropertiesPlaceholder() {
    // TODO: i18n?
    return new Builder("No Properties")
        .setDeletable(false)
        .setWritable(false)
        .build();
  }

  private RemoteObjectNode(Builder builder) {
    this.name = builder.name;
    this.orderIndex = builder.orderIndex;
    this.children = (builder.hasChildren && !builder.wasThrown) ?
        new SortedList<RemoteObjectNode>(SORTING_FUNCTION) : null;
    this.remoteObject = builder.remoteObject;
    this.wasThrown = builder.wasThrown;
    this.getterOrSetterName = builder.getterOrSetterName;
    this.isTransient = builder.isTransient;

    boolean isDeletable = builder.isDeletable;
    boolean isWritable = builder.isWritable;
    boolean isEnumerable = builder.isEnumerable;

    if (PROTO_PROPERTY_NAME.equals(name)) {
      // The __proto__ property can not be deleted, although can be changed.
      isDeletable = false;
      isEnumerable = false;
    }
    if (getterOrSetterName != null) {
      // TODO: Maybe allow editing and/or deleting getters and setters?
      isDeletable = false;
      isWritable = false;
    }

    this.isDeletable = isDeletable;
    this.isWritable = isWritable;
    this.isEnumerable = isEnumerable;

    this.shouldRequestChildren = (!wasThrown && remoteObject != null && remoteObject.hasChildren());
  }

  /**
   * Tears down the object in order to prevent leaks. Do not use the object once
   * this method is called.
   */
  public void teardown() {
    if (children != null) {
      children.clear();
    }
    setParent(null);
    setRenderedTreeNode(null);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    RemoteObjectNode parent = getParent();
    if (parent != null) {
      parent.removeChild(this);
    }
    this.name = name;
    if (parent != null) {
      parent.addChild(this);
    }
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public String getNodeId() {
    return name + "#" + orderIndex;
  }

  public native final RemoteObjectNode getParent() /*-{
    return this.__parentRef;
  }-*/;

  private native void setParent(RemoteObjectNode parent) /*-{
    this.__parentRef = parent;
  }-*/;

  public boolean hasChildren() {
    return children != null;
  }

  public boolean wasThrown() {
    return wasThrown;
  }

  public boolean canAddRemoteObjectProperty() {
    if (isTransient || remoteObject == null || !hasChildren()) {
      return false;
    }

    RemoteObjectType type = remoteObject.getType();
    RemoteObjectSubType subType = remoteObject.getSubType();

    return type == RemoteObjectType.FUNCTION
        || (type == RemoteObjectType.OBJECT && subType != RemoteObjectSubType.NULL);
  }

  /**
   * @return true if this property can be deleted from the parent object
   */
  public boolean isDeletable() {
    return isDeletable;
  }

  /**
   * @return true if the value of this property can be changed
   */
  public boolean isWritable() {
    return isWritable;
  }

  public boolean isEnumerable() {
    return isEnumerable;
  }

  /**
   * @return true if the object represented by this node refers to an artificial
   *         transient remote object
   * @see DebuggerApiTypes.Scope#isTransient
   */
  public boolean isTransient() {
    return isTransient;
  }

  public boolean shouldRequestChildren() {
    return shouldRequestChildren && hasChildren();
  }

  public void setAllChildrenRequested() {
    shouldRequestChildren = false;
  }

  public JsonArray<RemoteObjectNode> getChildren() {
    if (children == null) {
      return JsonCollections.createArray();
    }
    // Some remote objects may have no children. In this case we return a
    // special "placeholder" child to display this fact in the UI.
    if (children.size() == 0 && remoteObject != null && !shouldRequestChildren) {
      addChild(createNoPropertiesPlaceholder());
    }
    return children.toArray(); // Returns copy.
  }

  public RemoteObject getRemoteObject() {
    return remoteObject;
  }

  /**
   * @return the associated rendered {@link TreeNodeElement}. If there is no
   *         tree node element rendered yet, then {@code null} is returned
   */
  public final native TreeNodeElement<RemoteObjectNode> getRenderedTreeNode() /*-{
    return this.__renderedNode;
  }-*/;

  /**
   * Associates this RemoteObjectNode with the supplied {@link TreeNodeElement}
   * as the rendered node in the tree. This allows us to go from model ->
   * rendered tree element in order to reflect model mutations in the tree.
   */
  public final native void setRenderedTreeNode(TreeNodeElement<RemoteObjectNode> renderedNode) /*-{
    this.__renderedNode = renderedNode;
  }-*/;

  public boolean isRootChild() {
    return getParent() != null && getParent().getParent() == null;
  }

  @Override
  public int compareTo(RemoteObjectNode that) {
    if (this == that) {
      return 0;
    }

    int orderIndexDiff = this.orderIndex - that.orderIndex;
    if (orderIndexDiff != 0) {
      return orderIndexDiff;
    }

    String a = this.getName();
    String b = that.getName();

    if (PROTO_PROPERTY_NAME.equals(a)) {
      return 1;
    }
    if (PROTO_PROPERTY_NAME.equals(b)) {
      return -1;
    }

    // Sort by digits/non-digits chunks.
    while (true) {
      boolean emptyA = StringUtils.isNullOrEmpty(a);
      boolean emptyB = StringUtils.isNullOrEmpty(b);

      if (emptyA && emptyB) {
        return 0;
      }
      if (emptyA && !emptyB) {
        return -1;
      }
      if (emptyB && !emptyA) {
        return 1;
      }

      MatchResult resultA = CHUNK_FROM_BEGINNING.exec(a);
      MatchResult resultB = CHUNK_FROM_BEGINNING.exec(b);

      String chunkA = resultA.getGroup(0);
      String chunkB = resultB.getGroup(0);

      boolean isNumA = !StringUtils.isNullOrEmpty(resultA.getGroup(1));
      boolean isNumB = !StringUtils.isNullOrEmpty(resultB.getGroup(1));
      if (isNumA && !isNumB) {
        return -1;
      }
      if (isNumB && !isNumA) {
        return 1;
      }
      if (isNumA && isNumB) {
        // Must be parseDouble to handle big integers.
        double valueA = Double.parseDouble(chunkA);
        double valueB = Double.parseDouble(chunkB);

        if (valueA != valueB) {
          return valueA < valueB ? -1 : 1;
        }

        int diff = chunkA.length() - chunkB.length();
        if (diff != 0) {
          if (valueA == 0) {
            // "file_0" should precede "file_00".
            return diff;
          } else {
            // "file_015" should precede "file_15".
            return -diff;
          }
        }
      } else {
        int diff = chunkA.compareTo(chunkB);
        if (diff != 0) {
          return diff;
        }
      }

      a = a.substring(chunkA.length());
      b = b.substring(chunkB.length());
    }
  }

  public void addChild(RemoteObjectNode remoteObjectNode) {
    assert (hasChildren()) : "Adding children to a leaf node is not allowed!";

    remoteObjectNode.setParent(this);
    children.add(remoteObjectNode);
  }

  public void removeChild(RemoteObjectNode remoteObjectNode) {
    assert (hasChildren()) : "Removing a child from a leaf node?!";

    children.remove(remoteObjectNode);
  }

  public RemoteObjectNode getFirstChildByName(String name) {
    if (children == null) {
      return null;
    }

    for (int i = 0, n = children.size(); i < n; ++i) {
      RemoteObjectNode child = children.get(i);
      if (name.equals(child.getName())) {
        return child;
      }
    }

    return null;
  }

  public RemoteObjectNode getLastChild() {
    if (children == null || children.size() == 0) {
      return null;
    }
    return children.get(children.size() - 1);
  }

  /**
   * Builder class for the {@link RemoteObjectNode}.
   */
  public static class Builder {
    private final String name;
    private final RemoteObject remoteObject;
    private int orderIndex;
    private boolean hasChildren;
    private boolean wasThrown;
    private boolean isDeletable = true;
    private boolean isWritable = true;
    private boolean isEnumerable = true;
    private boolean isTransient;
    private String getterOrSetterName;

    public Builder(String name) {
      this(name, null);
    }

    public Builder(String name, RemoteObject remoteObject) {
      this(name, remoteObject, null);
    }

    public Builder(String name, RemoteObject remoteObject, RemoteObjectNode proto) {
      this.name = name;
      this.remoteObject = remoteObject;
      this.hasChildren = (remoteObject != null && remoteObject.hasChildren());

      if (proto != null) {
        this.orderIndex = proto.orderIndex;
        this.wasThrown = proto.wasThrown;
        this.isDeletable = proto.isDeletable;
        this.isWritable = proto.isWritable;
        this.isEnumerable = proto.isEnumerable;
        this.isTransient = proto.isTransient;
        this.getterOrSetterName = proto.getterOrSetterName;
      }
    }

    public Builder setOrderIndex(int orderIndex) {
      this.orderIndex = orderIndex;
      return this;
    }

    public Builder setHasChildren(boolean hasChildren) {
      this.hasChildren = hasChildren;
      return this;
    }

    public Builder setWasThrown(boolean wasThrown) {
      this.wasThrown = wasThrown;
      return this;
    }

    public Builder setDeletable(boolean deletable) {
      isDeletable = deletable;
      return this;
    }

    public Builder setWritable(boolean writable) {
      isWritable = writable;
      return this;
    }

    public Builder setEnumerable(boolean enumerable) {
      isEnumerable = enumerable;
      return this;
    }

    public Builder setTransient(boolean aTransient) {
      this.isTransient = aTransient;
      return this;
    }

    private Builder setGetterOrSetterName(String name) {
      getterOrSetterName = name;
      return this;
    }

    public RemoteObjectNode build() {
      return new RemoteObjectNode(this);
    }
  }
}
