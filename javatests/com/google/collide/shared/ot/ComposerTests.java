package com.google.collide.shared.ot;

import static com.google.collide.shared.ot.DocOpTestUtils.assertCompose;
import static com.google.collide.shared.ot.DocOpTestUtils.assertComposeFails;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.server.ServerDocOpFactory;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.ot.Composer.ComposeException;
import com.google.collide.shared.util.StringUtils;

import junit.framework.TestCase;

/**
 * Tests for the document operation composer.
 *
 */
public class ComposerTests extends TestCase {

  private static final String[] LINES = {"Hello world\n", "Foo bar\n", "Something else\n"};

  private Document doc;

  private TerseDocOpBuilder builder;

  public void testOneLastRetainLineMatchesOtherLastComponentWithoutNewline() {
    {
      DocOp a = builder.rl(1).b();
      DocOp b = builder.r(14).b();
      assertCompose(b, a, b);
    }

    {
      DocOp a = builder.rl(1).b();
      DocOp b = builder.d("test").b();
      assertCompose(b, a, b);
    }

    {
      DocOp a = builder.r(14).b();
      DocOp b = builder.rl(1).b();
      assertCompose(a, a, b);
    }

    {
      DocOp a = builder.i("test").b();
      DocOp b = builder.rl(1).b();
      assertCompose(a, a, b);
    }
  }

  public void testNonRetainLineComposition() {
    // R(1) o I(i),R(1)
    assertCompose(builder.i("i").r(1).b(),
        builder.r(1).b(), builder.i("i").r(1).b());

    // R(1) o R(1),I(i)
    assertCompose(builder.r(1).i("i").b(),
        builder.r(1).b(), builder.r(1).i("i").b());

    // D(h) o I(i)
    assertCompose(builder.d("h").i("i").b(),
        builder.d("h").b(), builder.i("i").b());

    // I(h) o R(1),I(i)
    assertCompose(builder.i("hi").b(),
        builder.i("h").b(), builder.r(1).i("i").b());

    // R(1) o D(i)
    assertCompose(builder.d("i").b(),
        builder.r(1).b(), builder.d("i").b());

    // D(h),R(1) o D(i)
    assertCompose(builder.d("hi").b(),
        builder.d("h").r(1).b(), builder.d("i").b());

    // I(h),R(1) o R(1),D(i)
    assertCompose(builder.i("h").d("i").b(),
        builder.i("h").r(1).b(), builder.r(1).d("i").b());
  }

  public void testRetainLineIndependent() {
    DocOp a = builder.i("a\n").rl(1).b();
    DocOp b = builder.rl(2).i("b\n").b();
    assertCompose(builder.i("a\n").rl(1).i("b\n").b(), a, b);

    a = builder.d("a\n").rl(1).b();
    b = builder.i("b").eolR(1).b();
    assertCompose(builder.d("a\n").i("b").eolR(1).b(), a, b);

    a = builder.d("a\n").rl(2).b();
    b = builder.rl(1).i("b").eolR(1).b();
    assertCompose(builder.d("a\n").rl(1).i("b").eolR(1).b(), a, b);
  }

  public void testRetainLineOverlapping() {
    DocOp a = builder.rl(1).i("a").eolR(1).rl(1).b();
    DocOp b = builder.rl(1).r(1).i("b").eolR(1).rl(1).b();
    assertCompose(builder.rl(1).i("ab").eolR(1).rl(1).b(), a, b);
  }

  public void testAdjacentLineModifications() {
    {
      DocOp a = builder.rl(1).i("b").eolR(1).b();
      DocOp b = builder.i("b").eolR(1).rl(1).b();
      assertCompose(builder.i("b").eolR(1).i("b").eolR(1).b(),
          a, b);
    }

    {
      DocOp a = builder.rl(15).r(3).i("b").eolR(1).rl(3).b();
      DocOp b = builder.rl(14).r(1).i("b").eolR(1).rl(4).b();
      assertCompose(builder.rl(14).r(1).i("b").eolR(1).r(3).i("b").eolR(1).rl(3).b(),
          a, b);
    }
  }

  public void testRetainLineCanStartMidline() {
    {
      /*
       * Retain line will retain an empty string (this composition was
       * discovered in the real world)
       */
      DocOp a = builder.r(1).i("a").b();
      DocOp b = builder.r(1).d("a").rl(1).b();

      assertCompose(builder.r(1).rl(1).b(), a, b);
    }
  }

  public void testRetainLineAndInsertPlayNice() {
    {
      DocOp a = builder.rl(2).r(5).i("en").eolR(1).rl(1).b();
      DocOp b = builder.rl(3).r(1).i("tu").b();

      DocOp expected = builder.rl(2).r(5).i("en").eolR(1).r(1).i("tu").b();
      assertCompose(expected, a, b);
    }
    {
      DocOp a = builder.rl(2).r(1).i("ip").eolR(1).rl(3).b();
      DocOp b = builder.rl(5).r(1).i("is").b();

      DocOp expected = builder.rl(2).r(1).i("ip").eolR(1).rl(2).r(1).i("is").b();
      assertCompose(expected, a, b);
    }
  }

  public void testThatIncorrectCompositionFails() {
    {
      DocOp a = builder.i("test").b();
      DocOp b = builder.rl(50).rl(1).b();
      assertComposeFails(a, b);
    }
    {
      DocOp a = builder.i("test").b();
      DocOp b = builder.rl(1).rl(1).b();
      assertComposeFails(a, b);
    }
  }
  
  public void testADeletesAndBInserts() {
    {
      DocOp a = builder.d("hello").d("world").b();
      DocOp b = builder.i("foo").i("bar").b();

      DocOp expected = builder.d("helloworld").i("foobar").b();
      assertCompose(expected, a, b);
    }
  }

  public void testPositionMigratorCompositionFailure() throws ComposeException {
    {
      // Simplified test case
      // This tickles the path through ProcessingBForAInsert
      DocOp a = builder.i("\n").b();
      DocOp b = builder.rl(2).b();

      DocOp expected = builder.i("\n").b();
      assertCompose(expected, a, b);
    }
    
    {
      // Simplified test case
      // This tickles the path through ProcessingBForAInsert
      DocOp a = builder.i("\n").b();
      DocOp b = builder.rl(1).b();

      DocOp expected = builder.i("\n").b();
      assertCompose(expected, a, b);
    }
    
    {
      // Simplified test case
      // This tickles the path through ProcessingBForAInsert
      DocOp a = builder.i("abc\n").i("def\n").b();
      DocOp b = builder.d("a").eolR(3).rl(2).b();

      DocOp expected = builder.i("bc\n").i("def\n").b();
      assertCompose(expected, a, b);
    }
    
    {
      // Simplified test case
      // This tickles the path through ProcessingAForBRetainLine
      DocOp a = builder.i("abc\n").i("def\n").b();
      DocOp b = builder.rl(3).b();

      DocOp expected = builder.i("abc\n").i("def\n").b();
      assertCompose(expected, a, b);
    }

    {
      // This should fail
      DocOp a = builder.i("\n").b();
      DocOp b = builder.rl(3).b();
      assertComposeFails(a, b);
    }

    {
      // This should fail
      DocOp a = builder.i("\n").i("\n").b();
      DocOp b = builder.rl(4).b();
      assertComposeFails(a, b);
    }

    {
      // Related test case
      DocOp a = builder.i("abc\n").r(5).b();
      DocOp b = builder.rl(2).b();

      DocOp expected = builder.i("abc\n").r(5).b();
      assertCompose(expected, a, b);
    }

    {
      // Full test case
      DocOp a = builder.d("var f = function() {\n").
          d("  alert(\"foo!\");\n").
          d("}\n").
          d("\n").
          d("f();\n").
          i("d3.svg.diagonal = function() {\n").
          i("  var source = d3_svg_chordSource,\n").
          i("      target = d3_svg_chordTarget,\n").
          i("      projection = d3_svg_diagonalProjection;\n").
          i("\n").
          i("  function diagonal(d, i) {\n").
          i("    var p0 = source.call(this, d, i),\n").
          i("        p3 = target.call(this, d, i),\n").
          i("        m = (p0.y + p3.y) / 2,\n").
          i("        p = [p0, {x: p0.x, y: m}, {x: p3.x, y: m}, p3];\n").
          i("    p = p.map(projection);\n").
          i("    return \"M\" + p[0] + \"C\" + p[1] + \" \" + p[2] + \" \" + p[3];\n").
          i("  }\n").
          i("\n").
          i("  diagonal.source = function(x) {\n").
          i("    if (!arguments.length) return source;\n").
          i("    source = d3.functor(x);\n").
          i("    return diagonal;\n").
          i("  };\n").
          i("\n").
          i("  diagonal.target = function(x) {\n").
          i("    if (!arguments.length) return target;\n").
          i("    target = d3.functor(x);\n").
          i("    return diagonal;\n").
          i("  };\n").
          i("\n").
          i("  diagonal.projection = function(x) {\n").
          i("    if (!arguments.length) return projection;\n").
          i("    projection = x;\n").
          i("    return diagonal;\n").
          i("  };\n").
          i("\n").
          i("  return diagonal;\n").
          i("};\n").b();

      DocOp b = builder.d("d3.svg.").eolR(24).rl(34).b();
      
      DocOpTestUtils.compose(a, b);
    }
  }
  
  public void testUnicodeCompositionFailure() {
    {
      /*
       * Simplified test case so we don't deal with the long unicode escape
       * sequences
       */
      DocOp a =
          builder
              .rl(1)
              .d("a").i("abcde").eolR(5)
              .d("abcde").i("abcde").eolR(5)
              .rl(1)
              .b();

      DocOp b =
          builder
              .d("abcde\n")
              .d("abcdeabcd\n")
              .d("abcdeabcd\n")
              .b();

      DocOp expected = builder
          .d("abcde\n")
          .d("aabcd\n")
          .d("abcdeabcd\n")
          .b();
      assertCompose(expected, a, b);
    }

    {
      DocOp a = builder.rl(1).b();
      DocOp b = builder.b();

      DocOp expected = builder.b();
      assertCompose(expected, a, b);
    }
  }
  
  public void testProcessingBForAFinishedMightBeTooLeniant() {
    {
      // This makes sense
      DocOp a = builder.i("a").b();
      DocOp b = builder.rl(1).b();
      assertCompose(a, a, b);
    }

    {
      // This doesn't
      DocOp a = builder.i("a").b();
      DocOp b = builder.rl(2).b();
      assertComposeFails(a, b);
    }

    {
      // This makes sense
      DocOp a = builder.i("\n").b();
      DocOp b = builder.rl(2).b();
      assertCompose(a, a, b);
    }

    {
      // This doesn't
      DocOp a = builder.i("\n").b();
      DocOp b = builder.rl(3).b();
      assertComposeFails(a, b);
    }

    {
      // This makes sense
      DocOp a = builder.r(1).b();
      DocOp b = builder.rl(1).b();
      assertCompose(a, a, b);
    }

    {
      // This doesn't
      DocOp a = builder.r(1).b();
      DocOp b = builder.rl(2).b();
      assertComposeFails(a, b);
    }

    {
      // This makes sense
      DocOp a = builder.eolR(1).b();
      DocOp b = builder.rl(2).b();
      
      // It will compact the r("\n") to rl(1)
      DocOp expected = builder.rl(1).b();
      assertCompose(expected, a, b);
    }

    {
      // This doesn't
      DocOp a = builder.eolR(1).b();
      DocOp b = builder.rl(3).b();
      assertComposeFails(a, b);
    }
  }
  
  public void testRetainLastLineNotCancelledStillSucceeds() {
    {
      DocOp a = builder.i("a").d("b").b();
      DocOp b = builder.rl(1).b();
      
      DocOp expected = builder.i("a").d("b").b();
      assertCompose(expected, a, b);
    }
    {
      DocOp a = builder.rl(1).i("hh\n").i("ii").d("hh\n").d("ii").b();
      DocOp b = builder.rl(1).i("hh\n").i("ii\n").eolR(3).rl(1).b();

      DocOp expected = builder.rl(1).i("hh\n").i("ii\n").i("hh\n").i("ii").d("hh\n").d("ii").b();
      DocOpTestUtils.assertCompose(expected, a, b);
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    builder = new TerseDocOpBuilder(ServerDocOpFactory.INSTANCE, false);
    doc = Document.createEmpty();
    doc.insertText(doc.getFirstLine(), 0, StringUtils.join(LINES, ""));
  }
  
}
