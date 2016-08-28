package collide.junit;

import collide.junit.cases.ReflectionCaseNoMagic;

import static xapi.reflect.X_Reflect.magicClass;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@ReflectionStrategy(keepNothing=true)
@SuppressWarnings("rawtypes")
public class AbstractReflectionTest {

  protected static final Class CLASS_OBJECT = magicClass(Object.class);

  protected static final String METHOD_EQUALS = "equals";
  protected static final String METHOD_HASHCODE = "hashCode";
  protected static final String METHOD_TOSTRING = "toString";

  protected static final String PRIVATE_MEMBER = "privateCall";
  protected static final String PUBLIC_MEMBER = "publicCall";
  protected static final String OVERRIDE_FIELD = "overrideField";

  static final Class<ReflectionCaseNoMagic> NO_MAGIC = ReflectionCaseNoMagic.class;
  static final Class<ReflectionCaseNoMagic.Subclass> NO_MAGIC_SUBCLASS = ReflectionCaseNoMagic.Subclass.class;


  static public void fail(String message) {
      if (message == null) {
          throw new AssertionError();
      }
      throw new AssertionError(message);
  }

  /**
   * Asserts that a condition is true. If it isn't it throws
   * an AssertionFailedError with the given message.
   */
  static public void assertTrue(String message, boolean condition) {
      if (!condition) {
          fail(message);
      }
  }

  /**
   * Asserts that a condition is true. If it isn't it throws
   * an AssertionFailedError.
   */
  static public void assertTrue(boolean condition) {
      assertTrue(null, condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws
   * an AssertionFailedError with the given message.
   */
  static public void assertFalse(String message, boolean condition) {
      assertTrue(message, !condition);
  }

  /**
   * Asserts that a condition is false. If it isn't it throws
   * an AssertionFailedError.
   */
  static public void assertFalse(boolean condition) {
      assertFalse(null, condition);
  }

  /**
   * Asserts that an object isn't null. If it is
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertNotNull(String message, Object object) {
      assertTrue(message, object != null);
  }

  /**
   * Asserts that an object isn't null.
   */
  static public void assertNotNull(Object object) {
      assertNotNull(null, object);
  }

  /**
   * Asserts that an object is null. If it isn't an {@link AssertionError} is
   * thrown.
   * Message contains: Expected: <null> but was: object
   *
   * @param object Object to check or <code>null</code>
   */
  static public void assertNull(Object object) {
      if (object != null) {
          assertNull("Expected: <null> but was: " + object.toString(), object);
      }
  }

  /**
   * Asserts that an object is null.  If it is not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertNull(String message, Object object) {
      assertTrue(message, object == null);
  }


  /**
   * Asserts that two objects are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, Object expected, Object actual) {
      if (expected == null && actual == null) {
          return;
      }
      if (expected != null && expected.equals(actual)) {
          return;
      }
      failNotEquals(message, expected, actual);
  }

  /**
   * Asserts that two objects are equal. If they are not
   * an AssertionFailedError is thrown.
   */
  static public void assertEquals(Object expected, Object actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two Strings are equal.
   */
  static public void assertEquals(String message, String expected, String actual) {
      if (expected == null && actual == null) {
          return;
      }
      if (expected != null && expected.equals(actual)) {
          return;
      }
      String cleanMessage = message == null ? "" : message;
      throw new ComparisonFailure(cleanMessage, expected, actual);
  }

  /**
   * Asserts that two Strings are equal.
   */
  static public void assertEquals(String expected, String actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two doubles are equal concerning a delta.  If they are not
   * an AssertionFailedError is thrown with the given message.  If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(String message, double expected, double actual, double delta) {
      if (Double.compare(expected, actual) == 0) {
          return;
      }
      if (!(Math.abs(expected - actual) <= delta)) {
          failNotEquals(message, new Double(expected), new Double(actual));
      }
  }

  /**
   * Asserts that two doubles are equal concerning a delta. If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(double expected, double actual, double delta) {
      assertEquals(null, expected, actual, delta);
  }

  /**
   * Asserts that two floats are equal concerning a positive delta. If they
   * are not an AssertionFailedError is thrown with the given message. If the
   * expected value is infinity then the delta value is ignored.
   */
  static public void assertEquals(String message, float expected, float actual, float delta) {
      if (Float.compare(expected, actual) == 0) {
          return;
      }
      if (!(Math.abs(expected - actual) <= delta)) {
          failNotEquals(message, new Float(expected), new Float(actual));
      }
  }

  /**
   * Asserts that two floats are equal concerning a delta. If the expected
   * value is infinity then the delta value is ignored.
   */
  static public void assertEquals(float expected, float actual, float delta) {
      assertEquals(null, expected, actual, delta);
  }

  /**
   * Asserts that two longs are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, long expected, long actual) {
      assertEquals(message, new Long(expected), new Long(actual));
  }

  /**
   * Asserts that two longs are equal.
   */
  static public void assertEquals(long expected, long actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two booleans are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, boolean expected, boolean actual) {
      assertEquals(message, Boolean.valueOf(expected), Boolean.valueOf(actual));
  }

  /**
   * Asserts that two booleans are equal.
   */
  static public void assertEquals(boolean expected, boolean actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two bytes are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, byte expected, byte actual) {
      assertEquals(message, new Byte(expected), new Byte(actual));
  }

  /**
   * Asserts that two bytes are equal.
   */
  static public void assertEquals(byte expected, byte actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two chars are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, char expected, char actual) {
      assertEquals(message, new Character(expected), new Character(actual));
  }

  /**
   * Asserts that two chars are equal.
   */
  static public void assertEquals(char expected, char actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two shorts are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, short expected, short actual) {
      assertEquals(message, new Short(expected), new Short(actual));
  }

  /**
   * Asserts that two shorts are equal.
   */
  static public void assertEquals(short expected, short actual) {
      assertEquals(null, expected, actual);
  }

  /**
   * Asserts that two ints are equal. If they are not
   * an AssertionFailedError is thrown with the given message.
   */
  static public void assertEquals(String message, int expected, int actual) {
      assertEquals(message, new Integer(expected), new Integer(actual));
  }

  /**
   * Asserts that two ints are equal.
   */
  static public void assertEquals(int expected, int actual) {
      assertEquals(null, expected, actual);
  }

  private static boolean isEquals(Object expected, Object actual) {
    return expected.equals(actual);
  }

  private static boolean equalsRegardingNull(Object expected, Object actual) {
      if (expected == null) {
          return actual == null;
      }

      return isEquals(expected, actual);
  }

  /**
   * Asserts that two objects are <b>not</b> equals. If they are, an
   * {@link AssertionError} is thrown with the given message. If
   * <code>first</code> and <code>second</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(String message, Object first,
          Object second) {
      if (equalsRegardingNull(first, second)) {
          failEquals(message, first);
      }
  }

  /**
   * Asserts that two objects are <b>not</b> equals. If they are, an
   * {@link AssertionError} without a message is thrown. If
   * <code>first</code> and <code>second</code> are <code>null</code>,
   * they are considered equal.
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(Object first, Object second) {
      assertNotEquals(null, first, second);
  }


  private static void failEquals(String message, Object actual) {
      String formatted = "Values should be different. ";
      if (message != null) {
          formatted = message + ". ";
      }

      formatted += "Actual: " + actual;
      fail(formatted);
  }

  /**
   * Asserts that two longs are <b>not</b> equals. If they are, an
   * {@link AssertionError} is thrown with the given message.
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(String message, long first, long second) {
      assertNotEquals(message, (Long) first, (Long) second);
  }

  /**
   * Asserts that two longs are <b>not</b> equals. If they are, an
   * {@link AssertionError} without a message is thrown.
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   */
  static public void assertNotEquals(long first, long second) {
      assertNotEquals(null, first, second);
  }

  /**
   * Asserts that two doubles or floats are <b>not</b> equal to within a positive delta.
   * If they are, an {@link AssertionError} is thrown with the given
   * message. If the expected value is infinity then the delta value is
   * ignored. NaNs are considered equal:
   * <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
   *
   * @param message the identifying message for the {@link AssertionError} (<code>null</code>
   * okay)
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   * @param delta the maximum delta between <code>expected</code> and
   * <code>actual</code> for which both numbers are still
   * considered equal.
   */
  static public void assertNotEquals(String message, double first,
          double second, double delta) {
      if (!doubleIsDifferent(first, second, delta)) {
          failEquals(message, new Double(first));
      }
  }

  /**
   * Asserts that two doubles or floats are <b>not</b> equal to within a positive delta.
   * If they are, an {@link AssertionError} is thrown. If the expected
   * value is infinity then the delta value is ignored.NaNs are considered
   * equal: <code>assertNotEquals(Double.NaN, Double.NaN, *)</code> fails
   *
   * @param first first value to check
   * @param second the value to check against <code>first</code>
   * @param delta the maximum delta between <code>expected</code> and
   * <code>actual</code> for which both numbers are still
   * considered equal.
   */
  static public void assertNotEquals(double first, double second, double delta) {
      assertNotEquals(null, first, second, delta);
  }


  static private boolean doubleIsDifferent(double d1, double d2, double delta) {
      if (Double.compare(d1, d2) == 0) {
          return false;
      }
      if ((Math.abs(d1 - d2) <= delta)) {
          return false;
      }

      return true;
  }

  static public void failNotEquals(String message, Object expected, Object actual) {
    fail(format(message, expected, actual));
  }

  public static String format(String message, Object expected, Object actual) {
      String formatted = "";
      if (message != null && message.length() > 0) {
          formatted = message + " ";
      }
      return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
  }

}
