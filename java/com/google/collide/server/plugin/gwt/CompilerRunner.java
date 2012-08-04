package com.google.collide.server.plugin.gwt;


public interface CompilerRunner extends Runnable{

//  void setHandler(Object message);
//  void setClassLoader(Object cl);

  void setChannel(ClassLoader cl, Object io);
//  void setRequest(GwtCompile request);
//  void setLogger(TreeLogger logger);

  void setOnDestroy(Object runOnDestroy);
}
