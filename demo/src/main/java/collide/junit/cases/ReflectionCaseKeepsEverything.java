package collide.junit.cases;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@RuntimeRetention
@ReflectionStrategy(keepEverything=true)
public class ReflectionCaseKeepsEverything extends ReflectionCaseSuperclass{

  public ReflectionCaseKeepsEverything() {}
  
  @CompileRetention
  private class Subclass extends ReflectionCaseKeepsEverything {
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
