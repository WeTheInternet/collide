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

import static com.google.collide.dto.DocOpComponent.Type.DELETE;
import static com.google.collide.dto.DocOpComponent.Type.INSERT;
import static com.google.collide.dto.DocOpComponent.Type.RETAIN;
import static com.google.collide.dto.DocOpComponent.Type.RETAIN_LINE;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.DocOpComponent.Delete;
import com.google.collide.dto.DocOpComponent.Insert;
import com.google.collide.dto.DocOpComponent.Retain;
import com.google.collide.dto.DocOpComponent.RetainLine;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Utility methods for document operation manipulation.
 *
 */
public final class DocOpUtils {

  public static void accept(DocOp docOp, DocOpCursor visitor) {
    JsonArray<DocOpComponent> components = docOp.getComponents();

    for (int i = 0, n = components.size(); i < n; i++) {
      acceptComponent(components.get(i), visitor);
    }
  }

  public static void acceptComponent(DocOpComponent component, DocOpCursor visitor) {
    switch (component.getType()) {
      case DELETE:
        visitor.delete(((Delete) component).getText());
        break;

      case INSERT:
        visitor.insert(((Insert) component).getText());
        break;

      case RETAIN:
        Retain retain = (Retain) component;
        visitor.retain(retain.getCount(), retain.hasTrailingNewline());
        break;

      case RETAIN_LINE:
        visitor.retainLine(((RetainLine) component).getLineCount());
        break;

      default:
        throw new IllegalArgumentException(
            "Unknown doc op component with ordinal " + component.getType());
    }
  }

  public static DocOp createFromTextChange(DocOpFactory factory, TextChange textChange) {

    DocOp docOp = factory.createDocOp();
    JsonArray<DocOpComponent> components = docOp.getComponents();

    int lineNumber = textChange.getLineNumber();
    if (lineNumber > 0) {
      components.add(factory.createRetainLine(lineNumber));
    }

    int column = textChange.getColumn();
    if (column > 0) {
      components.add(factory.createRetain(column, false));
    }

    String text = textChange.getText();

    /*
     * Split the potentially multiline text into a component per line
     */
    JsonArray<String> lineTexts = StringUtils.split(text, "\n");

    // Create components for all but the last line
    int nMinusOne = lineTexts.size() - 1;
    for (int i = 0; i < nMinusOne; i++) {
      components.add(createComponentFromTextChange(factory, textChange, lineTexts.get(i) + "\n"));
    }

    String lastLineText = lineTexts.get(nMinusOne);
    if (!lastLineText.isEmpty()) {
      // Create a component for the last line
      components.add(createComponentFromTextChange(factory, textChange, lastLineText));
    }

    // Create a retain, if required
    int remainingRetainCount;
    int numNewlines = lineTexts.size() - 1;
    if (textChange.getType() == TextChange.Type.INSERT) {
      Line lastModifiedLine = LineUtils.getLine(textChange.getLine(), numNewlines);

      remainingRetainCount = lastModifiedLine.getText().length() - lastLineText.length();

      if (numNewlines == 0) {
        remainingRetainCount -= column;
      }
    } else { // DELETE
      remainingRetainCount = textChange.getLine().getText().length() - column;
    }

    // Create a retain line, if required
    int docLineCount = textChange.getLine().getDocument().getLineCount();
    int numNewlinesFromTextChangeInCurDoc =
        textChange.getType() == TextChange.Type.DELETE ? 0 : numNewlines;
    int remainingLineCount = docLineCount - (lineNumber + numNewlinesFromTextChangeInCurDoc + 1);

    // Add the retain and retain line components
    if (remainingRetainCount > 0) {
      // This retain has a trailing new line if it is NOT on the last line
      components.add(factory.createRetain(remainingRetainCount, remainingLineCount > 0));
    }

    if (remainingLineCount > 0) {
      components.add(factory.createRetainLine(remainingLineCount));
    } else {
      Preconditions.checkState(remainingLineCount == 0, "How is it negative?");
      /*
       * If the retainingLineCount calculation resulted in 0, there's still a
       * chance that there is a empty last line that needs to be retained. Our
       * contract says if the resulting document (that is, the document in its
       * state right now) has an empty last line, we should have a RetainLine
       * that accounts for it. In addition if the document contained an empty
       * last line before the delete we should also emit a retain line.
       *
       * Since we didn't emit the RetainLine above (since remainingLineCount ==
       * 0), we can check and emit one here.
       */
      // to check if the document ended in a new line before the change, we check if the change
      // endsWith \n and remainingRetainCount = 0;
      boolean didDocumentEndInEmptyLineBeforeDelete = textChange.getType() == TextChange.Type.DELETE
          && remainingRetainCount == 0 && text.endsWith("\n");
             
      boolean isLastLineEmptyAfterTextChange =
          textChange.getLine().getDocument().getLastLine().getText().length() == 0;
      if (isLastLineEmptyAfterTextChange || didDocumentEndInEmptyLineBeforeDelete) {
        components.add(factory.createRetainLine(1));
      }
    }

    return docOp;
  }

  /**
   * Creates a single doc op composed of docops converted from a collection
   * of text changes. For a single text change use
   * {@link #createFromTextChange(DocOpFactory, TextChange)}.
   *
   * @param factory doc ops factory
   * @param textChanges list of changes to convert to doc op
   * @return composed doc op, or {@code null} if text changes array is empty
   * @throws Composer.ComposeException if error happens during composal
   */
  public static DocOp createFromTextChanges(DocOpFactory factory,
      JsonArray<TextChange> textChanges) throws Composer.ComposeException {
    DocOp result = null;
    for (int i = 0, n = textChanges.size(); i < n; i++) {
      TextChange textChange = textChanges.get(i);
      DocOp curOp = DocOpUtils.createFromTextChange(factory, textChange);
      result = result != null ? Composer.compose(factory, result, curOp) : curOp;
    }
    return result;
  }

  public static boolean containsMutation(Iterable<DocOp> docOps) {
    for (DocOp docOp : docOps) {
      if (containsMutation(docOp)) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsMutation(DocOp docOp) {
    for (int i = 0; i < docOp.getComponents().size(); i++) {
      DocOpComponent component = docOp.getComponents().get(i);
      switch (component.getType()) {
        case DocOpComponent.Type.DELETE:
        case DocOpComponent.Type.INSERT:
          return true;
        case DocOpComponent.Type.RETAIN:
        case DocOpComponent.Type.RETAIN_LINE:
          // Retains do not dirty the contents of a file
          break;
        default:
          throw new IllegalArgumentException("Got an unknown doc op type " + component.getType());
      }
    }
    return false;
  }

  public static String toString(DocOp docOp, boolean verbose) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, n = docOp.getComponents().size(); i < n; i++) {
      sb.append(toString(docOp.getComponents().get(i), verbose));
    }

    return sb.toString();
  }

  public static String toString(DocOpComponent component, boolean verbose) {
    switch (component.getType()) {
      case DELETE:
        String deleteText = ((Delete) component).getText();
        return "D(" + toStringForComponentText(deleteText, verbose) + ")";

      case INSERT:
        String insertText = ((Insert) component).getText();
        return "I(" + toStringForComponentText(insertText, verbose) + ")";

      case RETAIN:
        Retain retain = (Retain) component;
        return "R(" + (retain.hasTrailingNewline() ? (retain.getCount() - 1) + "\\n" : ""
            + retain.getCount()) + ")";

      case RETAIN_LINE:
        return "RL(" + ((RetainLine) component).getLineCount() + ")";

      default:
        return "?(???)";
    }
  }

  public static String toString(
      List<? extends DocOp> docOps, int firstIndex, int lastIndex, boolean verbose) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = firstIndex; i <= lastIndex; i++) {
      DocOp docOp = docOps.get(i);
      if (docOp == null) {
        sb.append("<null doc op>,");
      } else {
        sb.append(toString(docOp, verbose)).append(',');
      }
    }
    sb.setLength(sb.length() - 1);
    sb.append(']');

    return sb.toString();
  }

  private static DocOpComponent createComponentFromTextChange(
      DocOpFactory factory, TextChange textChange, String text) {
    switch (textChange.getType()) {
      case INSERT:
        return factory.createInsert(text);

      case DELETE:
        return factory.createDelete(text);

      default:
        throw new IllegalArgumentException(
            "Unknown text change type with ordinal " + textChange.getType().ordinal());
    }
  }

  private static String toStringForComponentText(String componentText, boolean verbose) {
    if (verbose) {
      return componentText.endsWith("\n") ? componentText.substring(0, componentText.length() - 1)
          + "\\n" : componentText;
    } else {
      return componentText.endsWith("\n") ? (componentText.length() - 1) + "\\n" : ""
          + componentText.length();
    }
  }
}
