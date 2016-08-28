package collide.junit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.gwtc.api.Gwtc;
import xapi.test.Assert;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@ReflectionStrategy
@Gwtc(includeSource="")
public class JUnitTest {

  private static boolean beforeClass;
  private static boolean afterClass;
  private boolean before;
  private boolean after;

  public JUnitTest() {
    Assert.assertTrue(beforeClass);
    Assert.assertFalse(before);
    Assert.assertFalse(after);
    Assert.assertFalse(afterClass);
  }
  
  @BeforeClass
  public static void beforeClass() {
    beforeClass = true;
    afterClass = false;
  }
  
  @AfterClass
  public static void afterClass() {
    afterClass = true;
  }

  @Before
  public void before() {
    before = true;
  }
  
  @After
  public void after() {
    after = true;
  }

  @Test
  public void testAfter() {
    Assert.assertFalse(after);
  }

  @Test
  public void testAfterAgain() {
    // Still false; instances must not be reused.
    Assert.assertFalse(after);
  }
  
  @Test
  public void testAfter_SecondMethod() {
    Assert.assertFalse(after);
  }

  @Test
  public void testBeforeClass() {
    Assert.assertTrue(beforeClass);
  }
  
  @Test
  public void testBefore() {
    Assert.assertTrue(before);
  }

  
  @Test
  public void testAfterClass() {
    Assert.assertFalse("AfterClass has already been called",afterClass);
  }
  
}
