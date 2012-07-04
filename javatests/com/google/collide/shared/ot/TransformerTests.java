package com.google.collide.shared.ot;

import static com.google.collide.shared.ot.DocOpTestUtils.assertDocOpEquals;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.server.ServerDocOpFactory;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.operation.OperationPair;

/**
 * Tests for the document operation transformer.
 */
public class TransformerTests extends TestCase {

  private final class ReversibleTestParameters extends TestParameters {

    ReversibleTestParameters(DocOp clientMutation, DocOp serverMutation,
        DocOp transformedClientMutation, DocOp transformedServerMutation) {
      super(clientMutation, serverMutation, transformedClientMutation, transformedServerMutation);
    }

    @Override
    void run() throws Exception {
      singleTest(clientMutation, serverMutation, transformedClientMutation,
          transformedServerMutation);
      singleTest(serverMutation, clientMutation, transformedServerMutation,
          transformedClientMutation);
    }
  }

  private class TestParameters {
    final DocOp clientMutation;
    final DocOp serverMutation;
    final DocOp transformedClientMutation;
    final DocOp transformedServerMutation;

    TestParameters(DocOp clientMutation, DocOp serverMutation, DocOp transformedClientMutation,
        DocOp transformedServerMutation) {
      this.clientMutation = clientMutation;
      this.serverMutation = serverMutation;
      this.transformedClientMutation = transformedClientMutation;
      this.transformedServerMutation = transformedServerMutation;
    }

    void run() throws Exception {
      singleTest(clientMutation, serverMutation, transformedClientMutation,
          transformedServerMutation);
    }
  }

  private static final String N = "\n";

  private static final String FIVE_N = "12345\n";

  private static final String TEN_N = "abcdefghij\n";

  private static final String TWENTY_N = "12345678901234567890\n";

  private static final String FIVE = "12345";

  private static final String TEN = "abcdefghij";

  private static final String TWENTY = "12345678901234567890";

  private DocOpCreator docOpCreator;

  private ServerDocOpFactory factory;

  private TerseDocOpBuilder dob;

  /**
   * Performs tests for transforming text deletions against text deletions.
   */
  public void testDeleteVsDelete() throws Exception {
    // A's deletion spatially before B's deletion
    new ReversibleTestParameters(docOpCreator.delete(20, 1, "abcde"), docOpCreator.delete(20, 7,
        "fg"), docOpCreator.delete(18, 1, "abcde"), docOpCreator.delete(15, 2, "fg")).run();
    // A's deletion spatially adjacent to and before B's deletion
    new ReversibleTestParameters(docOpCreator.delete(20, 1, "abcde"), docOpCreator.delete(20, 6,
        "fg"), docOpCreator.delete(18, 1, "abcde"), docOpCreator.delete(15, 1, "fg")).run();
    // A's deletion overlaps B's deletion
    new ReversibleTestParameters(docOpCreator.delete(20, 1, "abcde"), docOpCreator.delete(20, 3,
        "cdefghi"), docOpCreator.delete(13, 1, "ab"), docOpCreator.delete(15, 1, "fghi")).run();
    // A's deletion a subset of B's deletion
    new ReversibleTestParameters(docOpCreator.delete(20, 1, "abcdefg"), docOpCreator.delete(20, 3,
        "cd"), docOpCreator.delete(18, 1, "abefg"), dob.rl(1).b()).run();
    // A's deletion identical to B's deletion
    new ReversibleTestParameters(docOpCreator.delete(20, 1, "abcdefg"), docOpCreator.delete(20, 1,
        "abcdefg"), dob.rl(1).b(), dob.rl(1).b()).run();
  }

  /**
   * Performs tests for transforming text insertions against text deletions.
   */
  public void testInsertVsDelete() throws Exception {
    // A's insertion spatially before B's deletion
    new ReversibleTestParameters(docOpCreator.insert(20, 1, "abc"),
        docOpCreator.delete(20, 2, "de"), docOpCreator.insert(18, 1, "abc"), docOpCreator.delete(
            23, 5, "de")).run();
    // A's insertion spatially inside B's deletion
    new ReversibleTestParameters(docOpCreator.insert(20, 2, "abc"),
        docOpCreator.delete(20, 1, "ce"), docOpCreator.insert(18, 1, "abc"), new DocOpBuilder(
            factory, false).retain(1, false).delete("c").retain(3, false).delete("e")
            .retain(17, true).build()).run();
    // A's insertion spatially at the start of B's deletion
    new ReversibleTestParameters(docOpCreator.insert(20, 1, "abc"),
        docOpCreator.delete(20, 1, "de"), docOpCreator.insert(18, 1, "abc"), docOpCreator.delete(
            23, 4, "de")).run();
    // A's insertion spatially at the end of B's deletion
    new ReversibleTestParameters(docOpCreator.insert(20, 3, "abc"),
        docOpCreator.delete(20, 1, "de"), docOpCreator.insert(18, 1, "abc"), docOpCreator.delete(
            23, 1, "de")).run();
    // A's insertion spatially after B's deletion
    new ReversibleTestParameters(docOpCreator.insert(20, 4, "abc"),
        docOpCreator.delete(20, 1, "de"), docOpCreator.insert(18, 2, "abc"), docOpCreator.delete(
            23, 1, "de")).run();
  }

  /**
   * Performs tests for transforming text insertions against text insertions.
   */
  public void testInsertVsInsert() throws Exception {
    // A's insertion spatially before B's insertion
    new ReversibleTestParameters(docOpCreator.insert(20, 1, "a"), docOpCreator.insert(20, 2, "1"),
        docOpCreator.insert(21, 1, "a"), docOpCreator.insert(21, 3, "1")).run();
    // client's insertion spatially at the same location as server's insertion
    new TestParameters(docOpCreator.insert(20, 2, "abc"), docOpCreator.insert(20, 2, "123"),
        docOpCreator.insert(23, 2, "abc"), docOpCreator.insert(23, 5, "123")).run();
  }

  public void testMultiLineDeleteVsDelete() throws Exception {
    {
      // Deletion above another deletion
      DocOp c = dob.d(TEN_N).d(TEN_N).d(TEN).eolR(5).rl(10).b();
      DocOp s = dob.rl(5).d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(5).b();
      DocOp cPrime = dob.d(TEN_N).d(TEN_N).d(TEN).eolR(5).rl(7).b();
      DocOp sPrime = dob.rl(3).d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(5).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Deletion above another deletion (lines adjacent)
      DocOp c = dob.d(TEN_N).d(TEN_N).d(TEN_N).rl(10).b();
      DocOp s = dob.rl(3).d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(7).b();
      DocOp cPrime = dob.d(TEN_N).d(TEN_N).d(TEN_N).rl(7).b();
      DocOp sPrime = dob.d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(7).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // TODO: the delete brought a RL onto a previously modified
      // line, is this legal?
      // Deletion above another deletion (characters adjacent)
      DocOp c = dob.d(TEN_N).d(TEN_N).d(TEN).eolR(6).rl(10).b();
      DocOp s = dob.rl(2).r(10).d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(8).b();
      DocOp cPrime = dob.d(TEN_N).d(TEN_N).d(TEN).rl(8).b();
      DocOp sPrime = dob.d(FIVE_N).d(FIVE_N).d(FIVE_N).rl(8).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Overlapping deletions (line granularity)
      DocOp c = dob.d(TEN_N).d(TEN_N).d(TEN_N).rl(10).b();
      DocOp s = dob.rl(2).d(TEN_N).d(FIVE_N).d(FIVE_N).rl(8).b();
      DocOp cPrime = dob.d(TEN_N).d(TEN_N).rl(8).b();
      DocOp sPrime = dob.d(FIVE_N).d(FIVE_N).rl(8).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Overlapping deletions (character granularity)
      DocOp c = dob.d(TEN_N).d(TEN_N).d(TEN).eolR(1).rl(10).b();
      DocOp s = dob.rl(2).d(TEN_N).d(FIVE_N).d(FIVE_N).rl(8).b();
      DocOp cPrime = dob.d(TEN_N).d(TEN_N).rl(8).b();
      DocOp sPrime = dob.d(N).d(FIVE_N).d(FIVE_N).rl(8).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Subset deletions (character granularity)
      DocOp c = dob.d(TEN_N).d(TEN_N).b();
      DocOp s = dob.rl(1).d(TEN).eolR(1).b();
      DocOp cPrime = dob.d(TEN_N).d(N).b();
      DocOp sPrime = dob.b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Identical deletions
      DocOp c = dob.d(TEN_N).d(TEN_N).b();
      DocOp s = c;
      DocOp cPrime = dob.b();
      DocOp sPrime = cPrime;
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
  }

  public void testMultiLineInsertVsDelete() throws Exception {
    {
      // Insertion above deletion
      DocOp c = dob.rl(2).i(FIVE_N).rl(2).b();
      DocOp s = dob.rl(3).d(TEN_N).b();
      DocOp cPrime = dob.rl(2).i(FIVE_N).rl(1).b();
      DocOp sPrime = dob.rl(4).d(TEN_N).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion above deletion, but adjacent lines
      DocOp c = dob.rl(2).i(FIVE_N).rl(1).b();
      DocOp s = dob.rl(2).d(TEN_N).b();
      DocOp cPrime = dob.rl(2).i(FIVE_N).b();
      DocOp sPrime = dob.rl(3).d(TEN_N).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion above/adjacent deletion (the last character of insertion is
      // adjacent to first character of deletion)
      DocOp c = dob.rl(2).i(FIVE_N).i(FIVE).eolR(11).rl(1).b();
      DocOp s = dob.rl(2).d(TEN_N).d(TEN_N).b();
      DocOp cPrime = dob.rl(2).i(FIVE_N).i(FIVE).b();
      DocOp sPrime = dob.rl(3).r(5).d(TEN_N).d(TEN_N).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion above/adjacent deletion (the last character of insertion is
      // adjacent to first character of deletion)
      DocOp c = dob.rl(2).i(FIVE_N).i(FIVE).eolR(11).rl(1).b();
      DocOp s = dob.rl(2).d(TEN_N).d(TEN_N).b();
      DocOp cPrime = dob.rl(2).i(FIVE_N).i(FIVE).b();
      DocOp sPrime = dob.rl(3).r(5).d(TEN_N).d(TEN_N).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion inside deletion
      DocOp c = dob.rl(2).i(FIVE_N).i(FIVE_N).rl(2).b();
      DocOp s = dob.d(TEN_N).d(TEN_N).d(TEN_N).d(TEN_N).b();
      DocOp cPrime = dob.i(FIVE_N).i(FIVE_N).b();
      DocOp sPrime = dob.d(TEN_N).d(TEN_N).rl(2).d(TEN_N).d(TEN_N).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion starts right after the last char deleted
      DocOp c = dob.rl(1).r(10).i(FIVE_N).i(FIVE).eolR(2).rl(3).b();
      DocOp s = dob.d(TEN_N).d(TEN).eolR(2).rl(3).b();
      DocOp cPrime = dob.i(FIVE_N).i(FIVE).eolR(2).rl(3).b();
      DocOp sPrime = dob.d(TEN_N).d(TEN).eolR(6).rl(4).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion starts on the line after the last line deleted
      DocOp c = dob.rl(4).r(10).i(FIVE_N).i(FIVE).eolR(2).rl(3).b();
      DocOp s = dob.d(TEN_N).d(TEN).eolR(2).rl(6).b();
      DocOp cPrime = dob.rl(3).r(10).i(FIVE_N).i(FIVE).eolR(2).rl(3).b();
      DocOp sPrime = dob.d(TEN_N).d(TEN).eolR(2).rl(7).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Insertion is later in the document
      DocOp c = dob.rl(5).i(FIVE_N).i(FIVE_N).b();
      DocOp s = dob.d(TEN_N).d(TEN_N).rl(3).b();
      DocOp cPrime = dob.rl(3).i(FIVE_N).i(FIVE_N).b();
      DocOp sPrime = dob.d(TEN_N).d(TEN_N).rl(5).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }
  }

  public void testMultiLineInsertVsInsert() throws Exception {
    {
      // Simple test of a insertion with newline
      DocOp c = dob.rl(3).b();
      DocOp s = dob.rl(1).i(TEN_N).eolR(1).rl(1).b();
      DocOp cPrime = dob.rl(4).b();
      DocOp sPrime = dob.rl(1).i(TEN_N).eolR(1).rl(1).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion spatially above B's insertion (simpler to trap bug)
      DocOp c = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(1).rl(4).b();
      DocOp s = dob.rl(8).i(TEN_N).i(TWENTY_N).rl(2).b();
      DocOp cPrime = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(1).rl(6).b();
      DocOp sPrime = dob.rl(10).i(TEN_N).i(TWENTY_N).rl(2).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion spatially above B's insertion
      DocOp c = dob.rl(5).r(3).i(FIVE_N).i(FIVE_N).i(TEN).eolR(5).rl(4).b();
      DocOp s = dob.rl(8).i(TEN_N).i(TWENTY_N).rl(2).b();
      DocOp cPrime = dob.rl(5).r(3).i(FIVE_N).i(FIVE_N).i(TEN).eolR(5).rl(6).b();
      DocOp sPrime = dob.rl(10).i(TEN_N).i(TWENTY_N).rl(2).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's retain gets cut short by B's newline insertion
      DocOp c = dob.eolR(5).b();
      DocOp s = dob.r(2).i(FIVE_N).eolR(3).b();
      DocOp cPrime = dob.eolR(8).eolR(3).b();
      DocOp sPrime = dob.r(2).i(FIVE_N).eolR(3).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion's last line touched is the same as B's insertion's first
      // line touched
      DocOp c = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(5).rl(5).b();
      DocOp s = dob.rl(5).r(2).i(TEN_N).i(TWENTY_N).eolR(3).rl(5).b();
      DocOp cPrime = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(13).eolR(21).eolR(3).rl(5).b();
      DocOp sPrime = dob.rl(5).eolR(6).eolR(6).r(12).i(TEN_N).i(TWENTY_N).eolR(3).rl(5).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion's last character is adjacent to B's insertion's first
      // character (simplified)
      DocOp c = dob.i(FIVE_N).i(FIVE_N).b();
      DocOp s = dob.i(TEN_N).i(TEN_N).b();
      DocOp cPrime = dob.i(FIVE_N).i(FIVE_N).rl(2).b();
      DocOp sPrime = dob.rl(2).i(TEN_N).i(TEN_N).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion's last character is adjacent to B's insertion's first
      // character
      DocOp c = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(1).rl(5).b();
      DocOp s = dob.rl(5).i(TEN_N).i(TWENTY_N).rl(6).b();
      DocOp cPrime = dob.rl(5).i(FIVE_N).i(FIVE_N).i(TEN).eolR(11).rl(7).b();
      DocOp sPrime = dob.rl(7).r(10).i(TEN_N).i(TWENTY_N).rl(6).b();
      new TestParameters(c, s, cPrime, sPrime).run();
    }
  }

  public void testRetainLineMatchingOtherNonEmptyLastLine() throws Exception {
    {
      // Tests that a final retain line matches the other's non-empty last line
      DocOp c = dob.rl(25).r(58).i("a").eolR(35).rl(1).b();
      DocOp s = dob.rl(26).r(58).i("b").b();
      DocOp cPrime = c;
      DocOp sPrime = s;
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // Tests that a retain line for more lines than available throws exception
      DocOp c = dob.rl(25).r(58).i("a").eolR(35).rl(2).b();
      DocOp s = dob.rl(26).r(58).i("b").b();
      DocOp cPrime = c;
      DocOp sPrime = s;
      try {
        new ReversibleTestParameters(c, s, cPrime, sPrime).run();
        fail();
      } catch (Throwable t) {
      }
    }
  }

  public void testSingleLineInsertVsInsertInMultiLineDocument() throws Exception {
    {
      // Simple test of a insertion
      DocOp c = dob.rl(3).b();
      DocOp s = dob.rl(1).i(TEN).eolR(1).rl(1).b();
      DocOp cPrime = dob.rl(3).b();
      DocOp sPrime = dob.rl(1).i(TEN).eolR(1).rl(1).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion spatially above B's insertion
      DocOp c = dob.r(5).i(FIVE).eolR(5).rl(2).b();
      DocOp s = dob.rl(2).i(TEN).eolR(10).b();
      DocOp cPrime = c;
      DocOp sPrime = s;
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion spatially directly above B's insertion
      DocOp c = dob.r(5).i(FIVE).eolR(5).rl(1).b();
      DocOp s = dob.rl(1).i(TEN).eolR(10).b();
      DocOp cPrime = c;
      DocOp sPrime = s;
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion spatially intertwined with B's insertion
      DocOp c = dob.r(5).i(FIVE).eolR(5).r(2).d(FIVE).r(4).b();
      DocOp s = dob.rl(1).i(TEN).eolR(11).b();
      DocOp cPrime = dob.r(5).i(FIVE).eolR(5).r(12).d(FIVE).r(4).b();
      DocOp sPrime = dob.rl(1).i(TEN).eolR(6).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }

    {
      // A's insertion would be a subset of B's insertion if this were text
      // replacement
      DocOp c = dob.rl(2).r(5).i(FIVE).eolR(5).rl(2).b();
      DocOp s = dob.rl(2).r(2).i(TWENTY).eolR(8).rl(2).b();
      DocOp cPrime = dob.rl(2).r(25).i(FIVE).eolR(5).rl(2).b();
      DocOp sPrime = dob.rl(2).r(2).i(TWENTY).eolR(13).rl(2).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
  }

  public void testSubstituteRetainCountForRetainLineProcessor() throws Exception {
    {
      DocOp c = dob.rl(1).d("b").b();
      DocOp s = dob.r(1).d("\n").r(1).b();

      DocOp cPrime = dob.r(1).d("b").b();
      DocOp sPrime = dob.r(1).d("\n").b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
    {
      DocOp c = dob.rl(1).i("a").r(1).b();
      DocOp s = dob.d("aaaa").r(3).d("\n").r(1).b();

      DocOp cPrime = dob.r(3).i("a").r(1).b();
      DocOp sPrime = dob.d("aaaa").r(3).d("\n").r(2).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
  }
  
  public void testEmptyLineVsRetainLine() throws Exception {
    {
      // rl will match empty and nothing will be transformed
      DocOp c = dob.b();
      DocOp s = dob.rl(1).b();

      DocOp cPrime = dob.b();
      DocOp sPrime = dob.rl(1).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
    {
      // rl will match the empty string after the retain here as well
      DocOp c = dob.d("\n").b();
      DocOp s = dob.eolR(1).rl(1).b();

      DocOp cPrime = dob.d("\n").b();
      DocOp sPrime = dob.rl(1).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
    {
      // rl cannot suffix here since it can only span an entire line
      DocOp c = dob.r(2).d("\n").b();
      DocOp s = dob.r(1).d("a").eolR(1).rl(1).b();

      DocOp cPrime = dob.r(1).d("\n").b();
      DocOp sPrime = dob.r(1).d("a").rl(1).b();
      new ReversibleTestParameters(c, s, cPrime, sPrime).run();
    }
    {
      DocOp c = dob.r(5).b();
      DocOp s = dob.rl(1).b();

      new ReversibleTestParameters(c, s, c, s).run();
    }
  }

  /**
   * Real world:
   * A: RL(1)
   * A: I(a)
   * A: R(1)I(s)
   * A: R(2)I(d)
   * A: R(3)I(f)
   * A: R(4)I(A)
   * A: R(5)I(S)
   * A: R(6)I(D)
   * A: R(7)I(\n)RL(1)
   * B: R(6)I(f)R(1\n) 
   * @throws Exception 
   */
  public void testThatTransformationDoesNotRemoveEmptyLineBeingRetainLine1() throws Exception {
    {
      DocOp s = dob.r(7).i("\n").rl(1).b(); 
      DocOp c = dob.r(6).i("f").r(1).b();
      
      DocOp sPrime = dob.r(8).i("\n").rl(1).b();
      DocOp cPrime = dob.r(6).i("f").eolR(2).rl(1).b();
      
      singleTest(c, s, cPrime, sPrime);
    }
    
    {
      DocOp s = dob.i("\n").rl(1).b();
      DocOp c = dob.i("f").b();
      DocOp cPrime = dob.i("f").eolR(1).rl(1).b();
      DocOp sPrime = dob.r(1).i("\n").rl(1).b();
      singleTest(c, s, cPrime, sPrime);
    }
    
    {
      DocOp s = dob.i("f").b();
      DocOp c = dob.i("\n").rl(1).b();
      DocOp sPrime = dob.eolR(1).i("f").b();
      singleTest(c, s, c, sPrime);
    }
      
    {
      DocOp s = dob.i("\n").rl(1).b();
      DocOp c = dob.i("f").r(1).b();
      DocOp cPrime = dob.i("f").eolR(1).r(1).b();
      DocOp sPrime = dob.r(1).i("\n").rl(1).b();
      singleTest(c, s, cPrime, sPrime);
    }
  }
  
  public void testSimpleRetainLineOfEmptyLastLine() throws Exception {
    DocOp c = dob.i("alex\n").rl(2).b();
    DocOp s = dob.i("b").eolR(1).rl(1).b();
    
    DocOp sPrime = dob.eolR(5).i("b").eolR(1).rl(1).b();
    
    singleTest(c, s, c, sPrime);
  }
  
  public void testDeleteOfEmptyLastLine() throws Exception {
    // alex\n
    // 
    
    DocOp c = dob.r(4).d("\n").rl(1).b();
    DocOp s = dob.i("a").eolR(5).rl(1).b();
    
    DocOp cPrime = dob.r(5).d("\n").rl(1).b();
    DocOp sPrime = dob.i("a").r(4).rl(1).b();
    
    new ReversibleTestParameters(s, c, sPrime, cPrime).run();
  }
  
  @Override
  protected void setUp() throws Exception {
    factory = ServerDocOpFactory.INSTANCE;
    docOpCreator = new DocOpCreator(factory);
    dob = new TerseDocOpBuilder(factory, true);
  }

  private void singleTest(DocOp clientMutation, DocOp serverMutation,
      DocOp transformedClientMutation, DocOp transformedServerMutation) throws Exception {
    com.google.collide.shared.ot.OperationPair pair =
        Transformer.transform(factory, clientMutation, serverMutation);
    OperationPair<DocOp> mutationPair = new OperationPair<DocOp>(pair.clientOp(), pair.serverOp());
    assertDocOpEquals(transformedClientMutation, mutationPair.clientOp());
    assertDocOpEquals(transformedServerMutation, mutationPair.serverOp());
  }
}
