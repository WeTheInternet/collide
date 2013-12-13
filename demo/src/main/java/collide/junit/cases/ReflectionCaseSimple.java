package collide.junit.cases;



/**
 * A simple reflection case;
 * a class with no annotations, and one of everything to reflect upon.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
public class ReflectionCaseSimple {

  static @interface Anno{}

  public ReflectionCaseSimple() {
    this(1, "1", 1, 1.0);
  }

  @Anno
  public ReflectionCaseSimple(int privatePrimitive, String privateObject, long publicPrimitive, Object ... publicObjects) {
    this.privatePrimitive = privatePrimitive;
    this.privateObject = privateObject;
    this.publicPrimitive = publicPrimitive;
    this.publicObject = publicObjects;
  }

  private int privatePrimitive;
  private String privateObject;
  @Anno
  public long publicPrimitive;
  public Object[] publicObject;

  @Anno
  private int privatePrimitive() {
    return privatePrimitive;
  }
  @SuppressWarnings("unused")
  private String privateObject() {
    return privateObject;
  }
  public long publicPrimitive() {
    return publicPrimitive;
  }
  public Object[] publicObject() {
    return publicObject;
  }

}
