package collide.junit.cases;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@RuntimeRetention
@ReflectionStrategy(keepNothing=true)
public class ReflectionCaseKeepsNothing extends ReflectionCaseSuperclass{

  public ReflectionCaseKeepsNothing() {}
  

  @RuntimeRetention
  private class Subclass extends ReflectionCaseKeepsNothing {
    @RuntimeRetention
    long privateCall;
    
    @CompileRetention
    Long publicCall;
    
    @CompileRetention private void privateCall() {
      privateCall+=2;
    }
    
    @RuntimeRetention public void publicCall() {
      publicCall = 2L;
    }
  }
  
  @RuntimeRetention
  long privateCall;
  
  @CompileRetention
  Long publicCall;
  
  @CompileRetention private void privateCall() {
    privateCall++;
  }
  
  @RuntimeRetention public void publicCall() {
    publicCall = 1L;
  }
  
}
