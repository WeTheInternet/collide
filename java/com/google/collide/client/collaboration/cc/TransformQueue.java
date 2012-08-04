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

package com.google.collide.client.collaboration.cc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.waveprotocol.wave.model.operation.OperationPair;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/*
 * Forked from Wave. Currently, the only changes are exposing some otherwise
 * internal state (such as queuedClientOps).
 */
/**
 * Simple implementation of main concurrency control logic, independent of
 * transport concerns.
 *
 * <p>
 * For efficiency, client ops are also compacted before transforming and before
 * sending.
 */
public class TransformQueue<M> {

  public interface Transformer<M> {
    OperationPair<M> transform(M clientOp, M serverOp);
    List<M> compact(List<M> clientOps);
  }

  private final Transformer<M> transformer;

  private int revision = -1;

  @VisibleForTesting int expectedAckedClientOps = 0;
  @VisibleForTesting List<M> serverOps = new LinkedList<M>();
  @VisibleForTesting List<M> unackedClientOps = Collections.emptyList();
  @VisibleForTesting List<M> queuedClientOps = new LinkedList<M>();
  boolean newClientOpSinceTransform = false;

  public TransformQueue(Transformer<M> transformer) {
    this.transformer = transformer;
  }

  public void init(int revision) {
    Preconditions.checkState(this.revision == -1, "Already at a revision (%s), can't init at %s)",
        this.revision, revision);
    Preconditions.checkArgument(revision >= 0, "Initial revision must be >= 0, not %s", revision);
    this.revision = revision;
  }

  public void serverOp(int resultingRevision, M serverOp) {
    checkRevision(resultingRevision);

    Preconditions.checkState(expectedAckedClientOps == 0,
        "server op arrived @%s while expecting %s client ops",
        resultingRevision, expectedAckedClientOps);

    this.revision = resultingRevision;

    if (!unackedClientOps.isEmpty()) {
      List<M> newUnackedClientOps = new LinkedList<M>();
      for (M clientOp : unackedClientOps) {
        OperationPair<M> pair = transformer.transform(clientOp, serverOp);
        newUnackedClientOps.add(pair.clientOp());
        serverOp = pair.serverOp();
      }
      unackedClientOps = newUnackedClientOps;
    }

    if (!queuedClientOps.isEmpty()) {
      if (newClientOpSinceTransform) {
        queuedClientOps = transformer.compact(queuedClientOps);
      }
      newClientOpSinceTransform = false;

      List<M> newQueuedClientOps = new LinkedList<M>();
      for (M clientOp : queuedClientOps) {
        OperationPair<M> pair = transformer.transform(clientOp, serverOp);
        newQueuedClientOps.add(pair.clientOp());
        serverOp = pair.serverOp();
      }
      queuedClientOps = newQueuedClientOps;
    }

    serverOps.add(serverOp);
  }

  public void clientOp(M clientOp) {
    if (!serverOps.isEmpty()) {
      List<M> newServerOps = new LinkedList<M>();
      for (M serverOp : serverOps) {
        OperationPair<M> pair = transformer.transform(clientOp, serverOp);
        newServerOps.add(pair.serverOp());
        clientOp = pair.clientOp();
      }
      serverOps = newServerOps;
    }

    queuedClientOps.add(clientOp);
    newClientOpSinceTransform = true;
  }

  public boolean expectedAck(int resultingRevision) {
    if (expectedAckedClientOps == 0) {
      return false;
    }

    Preconditions.checkArgument(resultingRevision == revision - expectedAckedClientOps + 1,
        "bad rev %s, current rev %s, expected remaining %s",
        resultingRevision, revision, expectedAckedClientOps);

    expectedAckedClientOps--;

    return true;
  }

  /**
   * @param resultingRevision
   * @return true if all unacked ops are now acked
   */
  public boolean ackClientOp(int resultingRevision) {
    checkRevision(resultingRevision);

    Preconditions.checkState(expectedAckedClientOps == 0,
        "must call expectedAck, there are %s expectedAckedClientOps", expectedAckedClientOps);
    Preconditions.checkState(!unackedClientOps.isEmpty(), "unackedClientOps is empty");

    this.revision = resultingRevision;

    unackedClientOps.remove(0);

    return unackedClientOps.isEmpty();
  }

  /**
   * Pushes the queued client ops into the unacked ops, clearing the queued ops.
   * @return see {@link #unackedClientOps()}
   */
  public List<M> pushQueuedOpsToUnacked() {
    Preconditions.checkState(unackedClientOps.isEmpty(),
        "Queue contains unacknowledged operations: %s", unackedClientOps);

    unackedClientOps = new LinkedList<M>(transformer.compact(queuedClientOps));
    queuedClientOps = new LinkedList<M>();

    return unackedClientOps();
  }

  public boolean hasServerOp() {
    return !serverOps.isEmpty();
  }

  public boolean hasUnacknowledgedClientOps() {
    return !unackedClientOps.isEmpty();
  }
  
  public int getUnacknowledgedClientOpCount() {
    return unackedClientOps.size();
  }

  public boolean hasQueuedClientOps() {
    return !queuedClientOps.isEmpty();
  }
  
  public int getQueuedClientOpCount() {
    return queuedClientOps.size();
  }

  public M peekServerOp() {
    Preconditions.checkState(hasServerOp(), "No server ops");
    return serverOps.get(0);
  }

  public M removeServerOp() {
    Preconditions.checkState(hasServerOp(), "No server ops");
    return serverOps.remove(0);
  }

  public int revision() {
    return revision;
  }

  private void checkRevision(int resultingRevision) {
    Preconditions.checkArgument(resultingRevision >= 1, "New revision %s must be >= 1",
        resultingRevision);
    Preconditions.checkState(this.revision == resultingRevision - 1,
        "Revision mismatch: at %s, received %s", this.revision, resultingRevision);
  }

  @Override
  public String toString() {
    return "TQ{ " + revision + "\n  s:" + serverOps +
        "\n  exp: " + expectedAckedClientOps +
        "\n  u:" + unackedClientOps + "\n  q:" + queuedClientOps + "\n}";
  }

  /**
   * @return the current queued client ops. Note: the behavior of this list
   *         after calling mutating methods on the transform queue is undefined.
   *         This method should be called each time immediately before use.
   */
  List<M> queuedClientOps() {
    return Collections.unmodifiableList(queuedClientOps);
  }

  public List<M> ackOpsIfVersionMatches(int newRevision) {
    if (newRevision == revision + unackedClientOps.size()) {
      List<M> expectedAckingClientOps = unackedClientOps;
      expectedAckedClientOps += expectedAckingClientOps.size();
      unackedClientOps = new LinkedList<M>();
      revision = newRevision;
      return expectedAckingClientOps;
    }

    return null;
  }

  /**
   * @return the current unacked client ops. Note: the behavior of this list
   *         after calling mutating methods on the transform queue is undefined.
   *         This method should be called each time immediately before use.
   */
  List<M> unackedClientOps() {
    return Collections.unmodifiableList(unackedClientOps);
  }
}
