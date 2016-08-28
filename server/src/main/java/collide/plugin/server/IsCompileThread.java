package collide.plugin.server;

import collide.plugin.server.gwt.CompilerBusyException;

public interface IsCompileThread <Model> {

  boolean isRunning();
  boolean isStarted();
  void kill();
  void doRecompile();
  void compile(String request) throws CompilerBusyException;
  void setContextClassLoader(ClassLoader cl);
  void setChannel(ClassLoader cl, Object io);

}
