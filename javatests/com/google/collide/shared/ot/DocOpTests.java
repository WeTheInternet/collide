package com.google.collide.shared.ot;

import static com.google.collide.shared.ot.DocOpTestUtils.assertDelete;
import static com.google.collide.shared.ot.DocOpTestUtils.assertInsert;
import static com.google.collide.shared.ot.DocOpTestUtils.assertRetain;
import static com.google.collide.shared.ot.DocOpTestUtils.assertRetainLine;
import static com.google.collide.shared.ot.DocOpTestUtils.assertSize;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.document.Document.TextListener;
import com.google.collide.shared.document.Line;
import com.google.collide.shared.document.TextChange;
import com.google.collide.shared.document.util.LineUtils;
import com.google.collide.shared.util.StringUtils;

import junit.framework.TestCase;

/**
 * Tests for general document operation methods.
 *
 */
public class DocOpTests extends TestCase {

  private static final String[] LINES = {"Hello world\n", "Foo bar\n", "Something else\n"};

  private DocOpFactory factory;

  private Document doc;

  private DocOpBuilder builder;
  
  private TerseDocOpBuilder b;

  public void testDocOpCapturerRetainCompacting() {
    DocOpCapturer c = new DocOpCapturer(factory, true);
    c.retain(5, true);
    c.retain(4, false);
    assertSize(2, c.getDocOp());

    c = new DocOpCapturer(factory, true);
    c.retain(5, false);
    c.retain(4, false);
    assertSize(1, c.getDocOp());
  }

  public void testMultilineTextChangeConversions() {
    TextChange textChange;
    DocOp op;

    textChange =
        TextChange.createInsertion(doc.getFirstLine(), 0, 0, doc.getFirstLine()
            .getNextLine(), 1, "Hello world\n");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(3, op);
    assertInsert("Hello world\n", op, 0);
    // TODO: need to doc how we require this line to be retained
    // instead of "retain line"ed
    assertRetain(doc.getFirstLine().getNextLine().getText().length(), true, op, 1);
    assertRetainLine(2, op, 2);

    textChange =
        TextChange.createInsertion(doc.getFirstLine().getNextLine(), 1, 1, doc.getFirstLine()
            .getNextLine().getNextLine(), 2, "oo\nSomething ");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(6, op);
    assertRetainLine(1, op, 0);
    assertRetain(1, false, op, 1);
    assertInsert("oo\n", op, 2);
    assertInsert("Something ", op, 3);
    assertRetain("else\n".length(), true, op, 4);
    assertRetainLine(1, op, 5);

    textChange =
        TextChange.createInsertion(doc.getFirstLine(), 0, 5, doc.getFirstLine().getNextLine()
            .getNextLine(), 2, " world\nFoo bar\n");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(5, op);
    assertRetain(5, false, op, 0);
    assertInsert(" world\n", op, 1);
    assertInsert("Foo bar\n", op, 2);
    assertRetain("Something else\n".length(), true, op, 3);
    assertRetainLine(1, op, 4);

    textChange = TextChange.createDeletion(doc.getFirstLine(), 0, 3, "Imagine this was a line\n");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(4, op);
    assertRetain(3, false, op, 0);
    assertDelete("Imagine this was a line\n", op, 1);
    assertRetain("lo world\n".length(), true, op, 2);
    assertRetainLine(3, op, 3);

    textChange =
        TextChange.createDeletion(doc.getFirstLine().getNextLine(), 1, 3, "A line\nand some ");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(6, op);
    assertRetainLine(1, op, 0);
    assertRetain(3, false, op, 1);
    assertDelete("A line\n", op, 2);
    assertDelete("and some ", op, 3);
    assertRetain(" bar\n".length(), true, op, 4);
    assertRetainLine(2, op, 5);
  }

  public void testRetainTrailingNewLineBehavior() {
    // Add the to-be-inserted "A" and the "r" that is to be retained
    doc.insertText(doc.getLastLine(), 0, "Ar");

    TextChange textChange;
    DocOp op;

    textChange =
        TextChange.createInsertion(doc.getLastLine(), doc.getLastLineNumber(), 0,
            doc.getLastLine(), doc.getLastLineNumber(), "A");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(3, op);
    assertRetainLine(3, op, 0);
    assertInsert("A", op, 1);
    assertRetain(1, false, op, 2);

    Line line = doc.getLastLine().getPreviousLine();
    textChange =
        TextChange.createInsertion(line, doc.getLastLineNumber() - 1, 0, line,
            doc.getLastLineNumber() - 1, "S");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(4, op);
    assertRetainLine(2, op, 0);
    assertInsert("S", op, 1);
    assertRetain(line.getText().length() - 1, true, op, 2);
    assertRetainLine(1, op, 3);
  }

  public void testSimpleTextChangeConversions() {
    Document doc = Document.createFromString("\nThis is\na test\n");
    TextChange textChange =
        TextChange.createInsertion(doc.getFirstLine(), 0, 0, doc.getFirstLine(), 0, "\n");
    DocOp op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(3, op);
    assertInsert("\n", op, 0);
    assertRetain(8, true, op, 1);
    // There's an empty line at the end
    assertRetainLine(2, op, 2);
  }

  public void testSingleLineTextChangeConversions() {
    TextChange textChange;
    DocOp op;

    textChange =
        TextChange.createInsertion(doc.getFirstLine(), 0, 1, doc.getFirstLine(), 0, "ello");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(4, op);
    assertRetain(1, false, op, 0);
    assertInsert("ello", op, 1);
    assertRetain(7, true, op, 2);
    assertRetainLine(3, op, 3);

    textChange = TextChange.createDeletion(doc.getFirstLine(), 0, 2, "WOOT");
    op = DocOpUtils.createFromTextChange(factory, textChange);
    assertSize(4, op);
    assertRetain(2, false, op, 0);
    assertDelete("WOOT", op, 1);
    assertRetain(10, true, op, 2);
    assertRetainLine(3, op, 3);
  }
  
  /**
   * Tests the scenario where the document is:
   * 
   * <pre>
   * \n
   * \n
   * aa
   * </pre>
   * 
   * and we insert a newline at line 0, position 0
   */
  public void testBuggyScenario1() {
    doc = Document.createFromString("\n\na");
    
    final DocOp[] opFromListener = new DocOp[1];
    doc.getTextListenerRegistrar().add(new TextListener() {
      @Override
      public void onTextChange(Document document, JsonArray<TextChange> textChanges) {
        opFromListener[0] = DocOpUtils.createFromTextChange(factory, textChanges.get(0));
      }
    });
    
    TextChange textChange = doc.insertText(doc.getFirstLine(), 0, "\n");
    assertEquals(TextChange.createInsertion(doc.getFirstLine(), 0, 0, doc.getFirstLine()
        .getNextLine(), 1, "\n"), textChange);
    
    DocOp opPostListener = DocOpUtils.createFromTextChange(factory, textChange);
    DocOpTestUtils.assertDocOpEquals(opPostListener, opFromListener[0]);
  }

  /**
   * Tests the scenario where the document is:
   *
   * <pre>
   * a\n
   * b\n
   * c\n
   * d\n
   * e
   * </pre>
   *
   * and we delete a\nb\nc\nd.
   */
  public void testBuggyDueToNoRetainWithTrailingNewLine() {
    doc = Document.createFromString("a\nb\nc\nd\ne");
    TextChange textChange = doc.deleteText(doc.getFirstLine(), 0, 0, 7);
    DocOp op = DocOpUtils.createFromTextChange(factory, textChange);

    assertSize(6, op);
    assertDelete("a\n", op, 0);
    assertDelete("b\n", op, 1);
    assertDelete("c\n", op, 2);
    assertDelete("d", op, 3);
    assertRetain(1, true, op, 4);
    assertRetainLine(1, op, 5);
  }

  /**
   * <pre>
   * ??????????
   * dAsSafasD
   * ASasd
   * DaASdf
   * DASas
   * DASs
   * DdAS
   * fD
   * ASD
   * ASD
   * ASD
   * ASD
   * ASD
   * AS
   * DAS
   * DAS
   * D
   * ASD
   * ASD
   * ASD
   * AS?
   * ? (this maps to a RL(1), so not sure if it was empty or had text)
   * (this is an empty line)
   * </pre>
   */
  public void testWhetherDeleteMultilineSelectionInThisCaseCreatesRetainLineToMatchEmptyLastLine() {
    doc = Document.createFromString(
        "??????????\ndAsSafasD\nASasd\nDaASdf\nDASas\nDASs\nDdAS\nfD\nASD\nASD\nASD\nASD\n"
        + "ASD\nAS\nDAS\nDAS\nD\nASD\nASD\nASD\nAS?\n?\n");
    assertEquals(23, doc.getLineCount());
    
    int textCount = LineUtils.getTextCount(
        doc.getFirstLine(), 10, doc.getLastLine().getPreviousLine().getPreviousLine(), 1);
    TextChange textChange = doc.deleteText(doc.getFirstLine(), 10, textCount);
    DocOp docOp = DocOpUtils.createFromTextChange(factory, textChange);
    docOp.toString();
  }
  
  public void testMissingRetainLineAfterDelete() {
    {
      doc = Document.createFromString("\n");

      TextChange textChange = doc.deleteText(doc.getFirstLine(), 0, 1);
      DocOp docOp = DocOpUtils.createFromTextChange(factory, textChange);

      DocOp expected = b.d("\n").rl(1).b();
      DocOpTestUtils.assertDocOpEquals(expected, docOp);
    }
    {
      doc = Document.createFromString("alex\n");

      TextChange textChange = doc.deleteText(doc.getFirstLine(), 4, 1);
      DocOp docOp = DocOpUtils.createFromTextChange(factory, textChange);

      DocOp expected = b.r(4).d("\n").rl(1).b();
      DocOpTestUtils.assertDocOpEquals(expected, docOp);
    }
    {
      doc = Document.createFromString("alex");

      TextChange textChange = doc.insertText(doc.getFirstLine(), 4, "\n");
      DocOp docOp = DocOpUtils.createFromTextChange(factory, textChange);

      DocOp expected = b.r(4).i("\n").rl(1).b();
      DocOpTestUtils.assertDocOpEquals(expected, docOp);
    }
    {
      doc = Document.createFromString("alex\ntest");

      TextChange textChange = doc.deleteText(doc.getFirstLine(), 0, 9);
      DocOp docOp = DocOpUtils.createFromTextChange(factory, textChange);

      DocOp expected = b.d("alex\n").d("test").rl(1).b();
      DocOpTestUtils.assertDocOpEquals(expected, docOp);
    }
  }

  public void testSimpleConversionWorks() {
    doc = Document.createFromString("aa\nhh\nii");
    
    TextChange textChange = doc.insertText(doc.getFirstLine().getNextLine(), 0, "hh\nii\n");
    DocOp docOpA = DocOpUtils.createFromTextChange(factory, textChange);
    DocOp expected = b.rl(1).i("hh\n").i("ii\n").eolR(3).rl(1).b();
    DocOpTestUtils.assertDocOpEquals(expected, docOpA);
    
    textChange = doc.deleteText(doc.getFirstLine().getNextLine().getNextLine(), 2, 6);
    DocOp docOpB = DocOpUtils.createFromTextChange(factory, textChange);
    expected = b.rl(2).r(2).d("\n").d("hh\n").d("ii").b();
    DocOpTestUtils.assertDocOpEquals(expected, docOpB);

  }
  
  @Override
  protected void setUp() throws Exception {
    factory = ServerDocOpFactory.INSTANCE;
    builder = new DocOpBuilder(factory, false);
    b = new TerseDocOpBuilder(factory, false);
    doc = Document.createEmpty();
    doc.insertText(doc.getFirstLine(), 0, StringUtils.join(LINES, ""));
  }
}
