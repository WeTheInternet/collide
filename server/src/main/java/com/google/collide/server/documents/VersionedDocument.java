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

package com.google.collide.server.documents;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.vertx.java.core.logging.Logger;

import com.google.collide.dto.ClientToServerDocOp;
import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.server.DtoServerImpls.DocumentSelectionImpl;
import com.google.collide.dto.server.DtoServerImpls.FilePositionImpl;
import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.LineInfo;
import com.google.collide.shared.document.anchor.Anchor;
import com.google.collide.shared.document.anchor.AnchorManager;
import com.google.collide.shared.document.anchor.AnchorType;
import com.google.collide.shared.ot.Composer;
import com.google.collide.shared.ot.Composer.ComposeException;
import com.google.collide.shared.ot.DocOpApplier;
import com.google.collide.shared.ot.DocOpUtils;
import com.google.collide.shared.ot.OperationPair;
import com.google.collide.shared.ot.PositionTransformer;
import com.google.collide.shared.ot.Transformer;
import com.google.collide.shared.ot.Transformer.TransformException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


/**
 * A document at a specific revision. It can be converted to and from raw text, and can be mutated
 * via the application of document operations passed to
 * {@link #consume(List, String, int, DocumentSelection)}.
 *
 * <p>
 * This class is thread-safe.
 *
 */
public class VersionedDocument {

  /**
   * A simple class for the result of the {@link VersionedDocument#consume} method.
   */
  public static class ConsumeResult {
    /**
     * The set of transformed DocOps, where the value is the DocOp and the key is the revision of
     * the document resulting from the application of that DocOp. The size of the returned Map will
     * equal that of the List of input DocOps.
     */
    public final SortedMap<Integer, AppliedDocOp> appliedDocOps;

    /**
     * The transformed selection of the user, or null if one was not given.
     *
     * @see ClientToServerDocOp#getSelection()
     */
    public final DocumentSelection transformedDocumentSelection;

    private ConsumeResult(SortedMap<Integer, AppliedDocOp> appliedDocOps,
        DocumentSelection transformedDocumentSelection) {
      this.appliedDocOps = appliedDocOps;
      this.transformedDocumentSelection = transformedDocumentSelection;
    }
  }

  /**
   * Doc op that was applied to the document, tagged with its author.
   */
  public static class AppliedDocOp {
    public final DocOp docOp;
    public final String authorClientId;

    private AppliedDocOp(DocOp docOp, String authorClientId) {
      this.docOp = docOp;
      this.authorClientId = authorClientId;
    }

    @Override
    public String toString() {
      return "[" + authorClientId + ", " + DocOpUtils.toString(docOp, true) + "]";
    }
  }

  /**
   * Serialized form of the document at a particular revision.
   */
  public static class VersionedText {
    public final int ccRevision;
    public final String text;

    private VersionedText(int ccRevision, String text) {
      this.ccRevision = ccRevision;
      this.text = text;
    }
  }

  /**
   * Thrown when there was a problem with document operations transformation or composition.
   */
  public static class DocumentOperationException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = -7596186181810388015L;

    public DocumentOperationException(String text, Throwable cause) {
      super(text, cause);
    }

    public DocumentOperationException(String text) {
      super(text);
    }
  }

  /** Revision number of the document */  
  private int ccRevision;

  /** Backing document */
  private final Document contents;

  /**
   * Stores the doc ops used to build the document, where the doc op at index i was applied to form
   * the document at revision i. There is a null value at index 0 since there was no doc op that
   * gave birth to the document.
   */
  private final List<AppliedDocOp> docOpHistory;

  /**
   * Intended revision of the last doc op from each client. If we see the same revision twice, we
   * consider the second a duplicate and discard it. One such scenario would be when the client
   * re-sends an unacked doc op after being momentarily disconnected, but the original doc op
   * actually did make it to the server.
   */
  private final Map<String, Integer> lastIntendedCcRevisionPerClient = Maps.newHashMap();

  private final Logger logger;

  /**
   * Constructs a new {@link VersionedDocument} with the given contents and revision number
   */
  public VersionedDocument(Document contents, int ccRevision, Logger logger) {
    this.ccRevision = ccRevision;
    this.contents = contents;
    this.logger = logger;

    // See javadoc for docOpHistory to understand the null element
    this.docOpHistory = Lists.newArrayList((AppliedDocOp) null);
  }

  /**
   * Constructs a new {@link VersionedDocument} with the given initial contents. The revision number
   * starts at zero.
   */
  public VersionedDocument(String initialContents, Logger logger) {
    this(Document.createFromString(initialContents), 0, logger);
  }

  public int getCcRevision() {
    return ccRevision;
  }

  /**
   * Applies the given list of {@link DocOp DocOps} to the backing document.
   *
   * @param docOps the list of {@code DocOp}s being applied
   * @param authorClientId clientId who sent the doc ops
   * @param intendedCcRevision the revision of the document that the DocOps are intended to be
   *        applied to
   * @param selection see {@link ClientToServerDocOp#getSelection()}
   * @return the transformed doc ops, or <code>null</code> if we discarded them as duplicates
   */
  public ConsumeResult consume(List<? extends DocOp> docOps, String authorClientId,
      int intendedCcRevision, DocumentSelection selection) throws DocumentOperationException {
    return consumeWithoutLocking(docOps, authorClientId, intendedCcRevision, selection);
  }

  /**
   * Private helper method that does the actual work of consuming doc ops. The publicly-visible
   * {@link #consume(List, String, int, DocumentSelection)} takes care of acquiring/releasing the
   * write lock around calls to this method.
   */
  private ConsumeResult consumeWithoutLocking(List<? extends DocOp> docOps, String authorClientId,
      int intendedCcRevision, DocumentSelection selection) throws DocumentOperationException {
    // Check the incoming intended revision against what we last got from this
    // client
    Integer lastIntendedCcRevision = lastIntendedCcRevisionPerClient.get(authorClientId);
    if (lastIntendedCcRevision != null) {
      if (intendedCcRevision == lastIntendedCcRevision.intValue()) {
        // We've already seen a doc op from this client intended for this
        // revision, assume this is a retry and ignore
        logger.debug(String.format(
            "clientId [%s] already sent a doc op intended for revision [%d]; "
            + "ignoring this one ", authorClientId, intendedCcRevision));
        return null;
      }

      // Sanity check that the client is not sending an obsolete doc op
      if (intendedCcRevision < lastIntendedCcRevision.intValue()) {
        logger.error(String.format(
            "clientId [%s] is sending a doc op intended for revision [%d] older than "
            + "the last one [%d] we saw from that client", authorClientId, intendedCcRevision,
            lastIntendedCcRevision.intValue()));
        return null;
      }
    }

    /*
     * First step, build the bridge from the intended revision to the latest revision by composing
     * all of the doc ops between these ranges. This bridge will be used to update a client doc op
     * that's intended to be applied to a document in the past.
     */
    DocOp bridgeDocOp = null;
    int bridgeBeginIndex = intendedCcRevision + 1;
    int bridgeEndIndexInclusive = ccRevision;
    for (int i = bridgeBeginIndex; i <= bridgeEndIndexInclusive; i++) {
      DocOp curDocOp = docOpHistory.get(i).docOp;
      try {
        bridgeDocOp = bridgeDocOp == null ? curDocOp : Composer.compose(
            ServerDocOpFactory.INSTANCE, bridgeDocOp, curDocOp);
      } catch (ComposeException e) {
        throw newExceptionForConsumeWithoutLocking("Could not build bridge",
            e,
            intendedCcRevision,
            bridgeBeginIndex,
            bridgeEndIndexInclusive,
            docOps);
      }
    }

    /*
     * Second step, iterate through doc ops from the client and transform each against the bridge.
     * Take the server op result of the transformation and make that the new bridge. Record each
     * into our map that will be returned to the caller of this method.
     */
    SortedMap<Integer, AppliedDocOp> appliedDocOps = new TreeMap<Integer, AppliedDocOp>();
    for (int i = 0, n = docOps.size(); i < n; i++) {
      DocOp clientDocOp = docOps.get(i);

      if (bridgeDocOp != null) {
        try {
          OperationPair transformedPair =
              Transformer.transform(ServerDocOpFactory.INSTANCE, clientDocOp, bridgeDocOp);
          clientDocOp = transformedPair.clientOp();
          bridgeDocOp = transformedPair.serverOp();
        } catch (TransformException e) {
          throw newExceptionForConsumeWithoutLocking("Could not transform doc op\ni: " + i + "\n",
              e,
              intendedCcRevision,
              bridgeBeginIndex,
              bridgeEndIndexInclusive,
              docOps);
        }
      }

      try {
        DocOpApplier.apply(clientDocOp, contents);
      } catch (Throwable t) {
        throw newExceptionForConsumeWithoutLocking("Could not apply doc op\nDoc op being applied: "
            + DocOpUtils.toString(clientDocOp, true) + "\n",
            t,
            intendedCcRevision,
            bridgeBeginIndex,
            bridgeEndIndexInclusive,
            docOps);
      }

      AppliedDocOp appliedDocOp = new AppliedDocOp(clientDocOp, authorClientId);
      docOpHistory.add(appliedDocOp);
      ccRevision++;

      appliedDocOps.put(ccRevision, appliedDocOp);
      lastIntendedCcRevisionPerClient.put(authorClientId, intendedCcRevision);
    }

    if (bridgeDocOp != null && selection != null) {
      PositionTransformer cursorTransformer = new PositionTransformer(
          selection.getCursorPosition().getLineNumber(), selection.getCursorPosition().getColumn());
      cursorTransformer.transform(bridgeDocOp);

      PositionTransformer baseTransformer = new PositionTransformer(
          selection.getBasePosition().getLineNumber(), selection.getBasePosition().getColumn());
      baseTransformer.transform(bridgeDocOp);

      FilePositionImpl basePosition = FilePositionImpl.make().setLineNumber(
          baseTransformer.getLineNumber()).setColumn(baseTransformer.getColumn());
      FilePositionImpl cursorPosition = FilePositionImpl.make().setLineNumber(
          cursorTransformer.getLineNumber()).setColumn(cursorTransformer.getColumn());

      DocumentSelectionImpl transformedSelection = DocumentSelectionImpl.make()
          .setBasePosition(basePosition).setCursorPosition(cursorPosition)
          .setUserId(selection.getUserId());

      selection = transformedSelection;
    }

    return new ConsumeResult(appliedDocOps, selection);
  }

  private DocumentOperationException newExceptionForConsumeWithoutLocking(String customMessage,
      Throwable e,
      int intendedCcRevision,
      int bridgeBeginIndex,
      int bridgeEndIndexInclusive,
      List<? extends DocOp> clientDocOps) {
    
    StringBuilder msg = new StringBuilder(customMessage).append('\n');

    msg.append("ccRevision: ").append(ccRevision).append('\n');
    msg.append("intendedCcRevision: ").append(intendedCcRevision).append('\n');
    msg.append("Bridge from ")
        .append(bridgeBeginIndex)
        .append(" to ")
        .append(bridgeEndIndexInclusive)
        .append(" doc ops:\n")
        .append(docOpHistory.subList(bridgeBeginIndex, bridgeEndIndexInclusive + 1))
        .append("\n");
    msg.append("Document (hyphens are line separators):\n").append(contents.asDebugString());
    msg.append("Client doc ops:\n")
        .append(DocOpUtils.toString(clientDocOps, 0, clientDocOps.size() - 1, true)).append("\n");
    msg.append("Recent doc ops from server history:\n").append(docOpHistoryToString());
    
    return new DocumentOperationException(msg.toString(), e);
  }

  public SortedMap<Integer, AppliedDocOp> getAppliedDocOps(int startingCcRevision) {
    SortedMap<Integer, AppliedDocOp> appliedDocOps = new TreeMap<Integer, AppliedDocOp>();
    if (startingCcRevision > (docOpHistory.size() - 1)) {
      logger.error(String.format(
          "startingCcRevision [%d] is larger than last revision in docOpHistory [%d]",
          startingCcRevision, docOpHistory.size()));
      return appliedDocOps;
    }

    for (int i = startingCcRevision; i < docOpHistory.size(); i++) {
      appliedDocOps.put(i, docOpHistory.get(i));
    }
    return appliedDocOps;
  }

  public VersionedText asText() {
    return new VersionedText(ccRevision, contents.asText());    
  }

  /**
   * @param column the column of the anchor, or {@link AnchorManager#IGNORE_COLUMN} for a line
   *        anchor
   */
  public Anchor addAnchor(AnchorType type, int lineNumber, int column) {
    LineInfo lineInfo = contents.getLineFinder().findLine(lineNumber);
    return contents.getAnchorManager()
        .createAnchor(type, lineInfo.line(), lineInfo.number(), column);    
  }

  /**
   * @param column the column of the anchor, or {@link AnchorManager#IGNORE_COLUMN} for a line
   *        anchor
   */
  public void moveAnchor(Anchor anchor, int lineNumber, int column) {
    LineInfo lineInfo = contents.getLineFinder().findLine(lineNumber);
    contents.getAnchorManager().moveAnchor(anchor, lineInfo.line(), lineInfo.number(), column);    
  }
  
  public void removeAnchor(Anchor anchor) {
    contents.getAnchorManager().removeAnchor(anchor);    
  }

  private String docOpHistoryToString() {
    List<DocOp> docOps = Lists.newArrayListWithExpectedSize(docOpHistory.size());
    for (AppliedDocOp appliedDocOp : docOpHistory) {
      docOps.add(appliedDocOp == null ? null : appliedDocOp.docOp);
    }

    return DocOpUtils.toString(
        docOps, Math.max(0, docOps.size() - 10), Math.max(0, docOps.size() - 1), false);
  }
}
