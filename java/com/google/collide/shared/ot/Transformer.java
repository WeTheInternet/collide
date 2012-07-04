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

package com.google.collide.shared.ot;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.common.base.Preconditions;

/*
 * Influenced by Wave's Transformer and Composer classes. Generally, we can't
 * use Wave's because we introduce the RetainLine doc op component, and don't
 * support a few extra components that Wave has. We didn't fork Wave's
 * Transformer because its design wasn't amenable to the changes required by
 * RetainLine. Instead, we wrote this from scratch to be able to handle that
 * component easily.
 *
 * The operations being transformed are A and B. Both are intended to be applied
 * to the same document. The output of the transformation will be A' and B'. For
 * example, A' will be A transformed so it can be cleanly applied after B has
 * been applied.
 *
 * The Processor class maintains the state for a document operation component.
 * Each method in the Processor handles a component from the other document
 * operation. As each method is executed, it outputs to its output document
 * operation and to the other processor's output document operation. It also
 * marks the general state into the ProcessorResult.
 */
/**
 * Transforms document operations for the code editor.
 *
 */
public class Transformer {

  /**
   * Exception that is thrown when there is a problem transforming two document
   * operations.
   */
  public static class TransformException extends RuntimeException {
    public TransformException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static class DeleteProcessor extends Processor {

    static String performDelete(DocOpCapturer output, String text, int deleteLength,
        ProcessorResult result) {

      output.delete(text.substring(0, deleteLength));

      if (text.length() == deleteLength) {
        result.markMyStateFinished();
      }

      return text.substring(deleteLength);
    }

    private String text;

    DeleteProcessor(DocOpCapturer output, String text) {
      super(output);
      this.text = text;
    }

    @Override
    void handleOtherDelete(DeleteProcessor other, ProcessorResult result) {
      /*
       * The transformed op shouldn't know about the deletes, so don't output
       * anything
       */
      if (text.length() == other.text.length()) {
        result.markMyStateFinished();
        result.markOtherStateFinished();
      } else if (text.length() < other.text.length()) {
        other.text = other.text.substring(text.length());
        result.markMyStateFinished();
      } else {
        text = text.substring(other.text.length());
        result.markOtherStateFinished();
      }
    }

    @Override
    void handleOtherFinished(Processor other, ProcessorResult result) {
      throw new IllegalStateException("Cannot delete if the other side is finished");
    }

    @Override
    void handleOtherInsert(InsertProcessor other, ProcessorResult result) {
      /*
       * Look at comments in InsertProcessor on why it needs to always handle
       * these components
       */
      result.flip();
      other.handleOtherDelete(this, result);
    }

    @Override
    void handleOtherRetain(RetainProcessor other, ProcessorResult result) {
      /*
       * The other transformed op won't have anything to retain, so output
       * nothing for it. Our transformed op needs to delete though, since the
       * other original op is just retaining.
       */
      int minCount = Math.min(text.length(), other.count);
      other.count -= minCount;

      text = performDelete(output, text, minCount, result);

      if (other.count == 0) {
        result.markOtherStateFinished();
      }
    }

    @Override
    void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result) {
      // Let RetainLineProcessor handle this
      result.flip();
      other.handleOtherDelete(this, result);
    }
  }

  private static class FinishedProcessor extends Processor {
    FinishedProcessor(DocOpCapturer output) {
      super(output);
    }

    @Override
    void handleOtherDelete(DeleteProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherFinished(this, result);
    }

    @Override
    void handleOtherFinished(Processor other, ProcessorResult result) {
      throw new IllegalStateException("Both should not be finished");
    }

    @Override
    void handleOtherInsert(InsertProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherFinished(this, result);
    }

    @Override
    void handleOtherRetain(RetainProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherFinished(this, result);
    }

    @Override
    void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherFinished(this, result);
    }
  }

  private static class InsertProcessor extends Processor {

    private static void performInsert(DocOpCapturer output, DocOpCapturer otherOutput,
        String text, ProcessorResult result) {
      output.insert(text);
      boolean endsWithNewline = text.endsWith(NEWLINE);
      otherOutput.retain(text.length(), endsWithNewline);
      
      if (endsWithNewline) {
        result.markMyCurrentComponentInsertOfNewline();
      }
      
      result.markMyStateFinished();
    }

    private String text;

    InsertProcessor(DocOpCapturer output, String text) {
      super(output);
      this.text = text;
    }

    @Override
    void handleOtherDelete(DeleteProcessor other, ProcessorResult result) {
      // Handle insertion
      performInsert(output, other.output, text, result);

      // The delete will be handled by the successor for this processor
    }

    @Override
    void handleOtherFinished(Processor other, ProcessorResult result) {
      performInsert(output, other.output, text, result);
    }

    @Override
    void handleOtherInsert(InsertProcessor other, ProcessorResult result) {
      /*
       * Instead of inserting both, we only insert one so contiguous insertions
       * by one side will end up being contiguous in the transformed op.
       * (Otherwise, you get interleaved I, RL, I, RL, ...)
       */
      performInsert(output, other.output, text, result);
    }

    @Override
    void handleOtherRetain(RetainProcessor other, ProcessorResult result) {
      performInsert(output, other.output, text, result);

      // The retain will be handled by the successor for this processor
    }

    @Override
    void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherInsert(this, result);
    }
  }

  private abstract static class Processor {
    final DocOpCapturer output;

    Processor(DocOpCapturer output) {
      this.output = output;
    }

    abstract void handleOtherDelete(DeleteProcessor other, ProcessorResult result);

    abstract void handleOtherFinished(Processor other, ProcessorResult result);

    abstract void handleOtherInsert(InsertProcessor other, ProcessorResult result);

    abstract void handleOtherRetain(RetainProcessor other, ProcessorResult result);

    abstract void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result);
  }

  private static class ProcessorFactory implements DocOpCursor {

    private DocOpCapturer curOutput;

    private Processor returnProcessor;

    @Override
    public void delete(String text) {
      returnProcessor = new DeleteProcessor(curOutput, text);
    }

    @Override
    public void insert(String text) {
      returnProcessor = new InsertProcessor(curOutput, text);
    }

    @Override
    public void retain(int count, boolean hasTrailingNewline) {
      returnProcessor = new RetainProcessor(curOutput, count, hasTrailingNewline);
    }

    @Override
    public void retainLine(int lineCount) {
      returnProcessor = new RetainLineProcessor(curOutput, lineCount);
    }

    Processor create(DocOpCapturer output, DocOpComponent component) {
      curOutput = output;
      DocOpUtils.acceptComponent(component, this);
      return returnProcessor;
    }
  }

  private static class ProcessorResult {
    private boolean isMyStateFinished;
    private boolean isOtherStateFinished;
    
    /*
     * We need to know the value of the previous component, but if we only tracked that then reset
     * would clear it.
     */
    private boolean isMyCurrentComponentInsertOfNewline;
    private boolean isOtherCurrentComponentInsertOfNewline;
    private boolean isMyPreviousComponentInsertOfNewline;
    private boolean isOtherPreviousComponentInsertOfNewline;


    private boolean isFlipped;

    private ProcessorResult() {
    }

    /**
     * Flips the "my" and "other" states. This should be called before one
     * processor is handing over execution to the other processor, including
     * passing this instance to the other processor.
     */
    void flip() {
      boolean origMyStateFinished = isMyStateFinished;
      isMyStateFinished = isOtherStateFinished;
      isOtherStateFinished = origMyStateFinished;

      boolean origMyPreviousComponentInsertOfNewline = isMyPreviousComponentInsertOfNewline;
      isMyPreviousComponentInsertOfNewline = isOtherPreviousComponentInsertOfNewline;
      isOtherPreviousComponentInsertOfNewline = origMyPreviousComponentInsertOfNewline;
      
      boolean origMyCurrentComponentInsertOfNewline = isMyCurrentComponentInsertOfNewline;
      isMyCurrentComponentInsertOfNewline = isOtherCurrentComponentInsertOfNewline;
      isOtherCurrentComponentInsertOfNewline = origMyCurrentComponentInsertOfNewline;
      
      isFlipped = !isFlipped;
    }

    void markMyStateFinished() {
      if (!isFlipped) {
        isMyStateFinished = true;
      } else {
        isOtherStateFinished = true;
      }
    }

    void markOtherStateFinished() {
      if (!isFlipped) {
        isOtherStateFinished = true;
      } else {
        isMyStateFinished = true;
      }
    }
    
    void markMyCurrentComponentInsertOfNewline() {
      if (!isFlipped) {
        isMyCurrentComponentInsertOfNewline = true;
      } else {
        isOtherCurrentComponentInsertOfNewline = true;
      }
    }

    void markOtherCurrentComponentInsertOfNewline() {
      if (!isFlipped) {
        isOtherCurrentComponentInsertOfNewline = true;
      } else {
        isMyCurrentComponentInsertOfNewline = true;
      }
    }

    void reset() {
      isMyPreviousComponentInsertOfNewline = isMyCurrentComponentInsertOfNewline;
      isOtherPreviousComponentInsertOfNewline = isOtherCurrentComponentInsertOfNewline;
      
      isFlipped = isMyStateFinished = isOtherStateFinished =
          isMyCurrentComponentInsertOfNewline = isOtherCurrentComponentInsertOfNewline = false;
    }
  }

  private static class RetainLineProcessor extends Processor {
    private int lineCount;

    /**
     * In the event that we need to expand the retain line, we need to know
     * exactly how many retains it should be expanded to. This tracks that
     * number.
     */
    private int substituteRetainCount;

    RetainLineProcessor(DocOpCapturer output, int lineCount) {
      super(output);
      this.lineCount = lineCount;
    }

    @Override
    void handleOtherDelete(DeleteProcessor other, ProcessorResult result) {
      other.output.delete(other.text);

      if (other.text.endsWith(NEWLINE)) {
        handleOtherLineEnd(false, result);
      } else {
        // My transformed op won't see the delete, so do nothing
      }

      result.markOtherStateFinished();
    }

    @Override
    void handleOtherFinished(Processor other, ProcessorResult result) {
      Preconditions.checkState(
          lineCount == 1, "Cannot retain more than one line if other side is finished");

      if (result.isMyPreviousComponentInsertOfNewline) {
        other.output.retainLine(1);
      }
      
      lineCount = 0;
      output.retainLine(1);
      result.markMyStateFinished();
    }

    @Override
    void handleOtherInsert(InsertProcessor other, ProcessorResult result) {
      other.output.insert(other.text);

      if (other.text.endsWith(NEWLINE)) {
        // Retain the line just inserted by other
        lineCount++;
        handleOtherLineEnd(true, result);
        
        result.markOtherCurrentComponentInsertOfNewline();
        
      } else {
        substituteRetainCount += other.text.length();
      }

      result.markOtherStateFinished();
    }

    void handleOtherLineEnd(boolean canUseRetainLine, ProcessorResult result) {
      if (canUseRetainLine) {
        output.retainLine(1);
      } else {
        if (substituteRetainCount > 0) {
          output.retain(substituteRetainCount, false);
        }
      }

      lineCount--;
      substituteRetainCount = 0;

      if (lineCount == 0) {
        result.markMyStateFinished();
      }
    }

    @Override
    void handleOtherRetain(RetainProcessor other, ProcessorResult result) {
      other.output.retain(other.count, other.hasTrailingNewline);
      substituteRetainCount += other.count;

      if (other.hasTrailingNewline) {
        handleOtherLineEnd(true, result);
      }

      result.markOtherStateFinished();
    }

    @Override
    void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result) {
      int minLineCount = Math.min(lineCount, other.lineCount);

      output.retainLine(minLineCount);
      lineCount -= minLineCount;

      other.output.retainLine(minLineCount);
      other.lineCount -= minLineCount;

      if (lineCount == 0) {
        result.markMyStateFinished();
      }

      if (other.lineCount == 0) {
        result.markOtherStateFinished();
      }
    }
  }

  private static class RetainProcessor extends Processor {

    static int performRetain(DocOpCapturer output, int fullCount, int retainCount,
        boolean hasTrailingNewline, ProcessorResult result, boolean useOtherInResult) {
      output.retain(retainCount, fullCount == retainCount ? hasTrailingNewline : false);

      if (retainCount == fullCount) {
        if (useOtherInResult) {
          result.markOtherStateFinished();
        } else {
          result.markMyStateFinished();
        }
      }

      return fullCount - retainCount;
    }

    private int count;
    private final boolean hasTrailingNewline;

    RetainProcessor(DocOpCapturer output, int count, boolean hasTrailingNewline) {
      super(output);
      this.count = count;
      this.hasTrailingNewline = hasTrailingNewline;
    }

    @Override
    void handleOtherDelete(DeleteProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherRetain(this, result);
    }

    @Override
    void handleOtherFinished(Processor other, ProcessorResult result) {
      throw new IllegalStateException("Cannot retain if other side is finished");
    }

    @Override
    void handleOtherInsert(InsertProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherRetain(this, result);
    }

    @Override
    void handleOtherRetain(RetainProcessor other, ProcessorResult result) {
      int minCount = Math.min(count, other.count);

      count = performRetain(output, count, minCount, hasTrailingNewline, result, false);
      other.count =
          performRetain(other.output, other.count, minCount, other.hasTrailingNewline, result,
              true);
    }

    @Override
    void handleOtherRetainLine(RetainLineProcessor other, ProcessorResult result) {
      result.flip();
      other.handleOtherRetain(this, result);
    }
  }

  private static final String NEWLINE = "\n";

  private static final ProcessorFactory PROCESSOR_FACTORY = new ProcessorFactory();

  public static OperationPair transform(DocOpFactory factory, DocOp clientOp, DocOp serverOp)
      throws TransformException {
    try {
      return new Transformer(factory).transformImpl(clientOp, serverOp);
    } catch (Throwable t) {
      throw new TransformException("Could not transform doc ops:\nClient: "
          + DocOpUtils.toString(clientOp, false) + "\nServer: "
          + DocOpUtils.toString(serverOp, false) + "\n", t);
    }
  }

  private static void dispatchProcessor(Processor a, Processor b, ProcessorResult result) {
    if (b instanceof DeleteProcessor) {
      a.handleOtherDelete((DeleteProcessor) b, result);
    } else if (b instanceof InsertProcessor) {
      a.handleOtherInsert((InsertProcessor) b, result);
    } else if (b instanceof RetainProcessor) {
      a.handleOtherRetain((RetainProcessor) b, result);
    } else if (b instanceof RetainLineProcessor) {
      a.handleOtherRetainLine((RetainLineProcessor) b, result);
    } else if (b instanceof FinishedProcessor) {
      a.handleOtherFinished(b, result);
    }
  }

  private final DocOpFactory factory;

  private Transformer(DocOpFactory factory) {
    this.factory = factory;
  }

  private OperationPair transformImpl(DocOp clientOp, DocOp serverOp) {
    /*
     * These capturers will create the respective side's doc op which will be
     * transformed from the respective side's original doc op to apply to the
     * document *after* the other side's original doc op.
     */
    DocOpCapturer clientOutput = new DocOpCapturer(factory, true);
    DocOpCapturer serverOutput = new DocOpCapturer(factory, true);

    JsonArray<DocOpComponent> clientComponents = clientOp.getComponents();
    JsonArray<DocOpComponent> serverComponents = serverOp.getComponents();

    int clientIndex = 0;
    int serverIndex = 0;

    boolean clientComponentsFinished = false;
    boolean serverComponentsFinished = false;

    Processor client = null;
    Processor server = null;

    ProcessorResult result = new ProcessorResult();

    while (!clientComponentsFinished || !serverComponentsFinished) {

      if (client == null) {
        if (clientIndex < clientComponents.size()) {
          client = PROCESSOR_FACTORY.create(clientOutput, clientComponents.get(clientIndex++));
        } else {
          client = new FinishedProcessor(clientOutput);
          clientComponentsFinished = true;
        }
      }

      if (server == null) {
        if (serverIndex < serverComponents.size()) {
          server = PROCESSOR_FACTORY.create(serverOutput, serverComponents.get(serverIndex++));
        } else {
          server = new FinishedProcessor(serverOutput);
          serverComponentsFinished = true;
        }
      }

      if (!clientComponentsFinished || !serverComponentsFinished) {
        dispatchProcessor(client, server, result);
      }

      if (result.isMyStateFinished) {
        client = null;
      }

      if (result.isOtherStateFinished) {
        server = null;
      }

      result.reset();
    }

    return new OperationPair(clientOutput.getDocOp(), serverOutput.getDocOp());
  }
}
