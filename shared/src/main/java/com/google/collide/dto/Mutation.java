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

package com.google.collide.dto;

/**
 * Dual use. Used by clients to request tree mutations on the server, and broadcast by the server to
 * inform clients about mutations that happened.
 */
public interface Mutation {
  /**
   * The mutation to perform. Note that the server does not currently report copies, it simply sends
   * an {@link #ADD} mutation for the (maybe recursively) copied node.
   */
  public enum Type {
    // TODO: This smells an awful lot like {@link NodeMutationDto}.
    // Explore combining them using inheritance or something.
    ADD, DELETE, MOVE, COPY;
  }

  /**
   * The String value of the {@link Type}.
   */
  Mutation.Type getMutationType();

  /**
   * During a request, this is (@code null} except for {@link Type#ADD} mutations. For an
   * {@link Type#ADD} mutation, this field's {@link TreeNodeInfo#getNodeType()} should be set to
   * specify whether to add a new file or directory.
   *
   * <p>
   * After the mutation is applied, this field is set to the mutated node and broadcast.
   * <ul>
   * <li>For an {@link Type#ADD} mutation, this node will be a complete tree (for example, in the
   * case of a recursive {@link Type#COPY}.</li>
   * <li>For a {@link Type#MOVE} mutation, this is the moved node. If the moved node is a directory
   * with children, it will be an incomplete node, since the client is expected to already know what
   * children original node had.</li>
   * <li>For {@link Type#DELETE} mutations, this will be {@code null}.</li>
   */
  TreeNodeInfo getNewNodeInfo();

  /**
   * The path to the changed node. This will be {@code null} for {@value Type#DELETE} mutations.
   */
  String getNewPath();

  /**
   * The old path of the node. This will be {@code null} for {@link Type#ADD} mutations.
   */
  String getOldPath();
}
