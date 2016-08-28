package collide.plugin.server.gwt;

public interface CompilerRunner extends Runnable{

  void setChannel(ClassLoader cl, Object io);

  void compile(String request) throws CompilerBusyException;

  void setOnDestroy(Object runOnDestroy);
}
