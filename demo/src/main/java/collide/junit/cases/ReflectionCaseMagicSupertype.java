package collide.junit.cases;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@ReflectionStrategy(
  keepEverything=true
)
public class ReflectionCaseMagicSupertype {

  ReflectionCaseMagicSupertype() {
    this(0);
  }
  
  public ReflectionCaseMagicSupertype(long field) {
    publicField = field;
  }
  
  private int privateField;
  public final long publicField;

  @SuppressWarnings("unused")
  private int privateMethod() {
    return privateField;
  }
  
  public long publicMethod() {
    return publicField;
  }
  
  
}
