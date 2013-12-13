package collide.junit.cases;

public class ReflectionCaseNoMagic {
  
  public static class Subclass extends ReflectionCaseNoMagic {
    protected boolean overrideField;// shadows the superclass field
    
    public static boolean getOverrideField(Subclass s) {
      return s.overrideField;
    }
    public Subclass() {}
    
    protected Subclass(String s) {
      super(s+"1");
    }

    public Subclass(long l) {
      super(l+1);
    }
  }
  
  public ReflectionCaseNoMagic() {}
  protected ReflectionCaseNoMagic(String s) {
    _String = s;
  }
  private ReflectionCaseNoMagic(long l) {
    this._long = l;
  }

  @RuntimeRetention
  private boolean privateCall;
  @RuntimeRetention
  public boolean publicCall;
  public boolean overrideField;
  boolean _boolean;
  byte _byte;
  short _short;
  char _char;
  int _int;
  public long _long;
  float _float;
  double _double;
  public String _String;
  
  @SuppressWarnings("unused")
  private void privateCall() { privateCall = true; }
  public void publicCall() { publicCall = true; }

  public boolean wasPrivateCalled(){return privateCall;}
  
  public boolean overrideField() {
    return this.overrideField;
  }
  
}
