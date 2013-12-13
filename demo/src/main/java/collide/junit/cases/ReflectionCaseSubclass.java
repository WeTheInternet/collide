package collide.junit.cases;


@SuppressWarnings("unused")
public class ReflectionCaseSubclass extends ReflectionCaseSuperclass {
  // Exact same fields as super class so we can test behavior
  private boolean privateCall;
  public boolean publicCall;
  
  private void privateCall(long l) {
    privateCall = true;
  }
  
  public void publicCall(Long l) {
    publicCall = true;
  }
  
}