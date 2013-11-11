package com.google.collide.plugin.server.gwt;

import java.net.URL;
import java.net.URLClassLoader;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public class UrlAndSystemClassLoader extends URLClassLoader{
  boolean allowSystem = true;
  boolean useParent = false;
  private TreeLogger log;
  public UrlAndSystemClassLoader(URL[] urls, TreeLogger log){
    super(urls,null);
    this.log = log;
  }
  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (allowSystem)
      try{
        //TODO: handle hot-swapping...
        return ClassLoader.getSystemClassLoader().loadClass(name);
      }catch (Exception e) {
        log.log(Type.TRACE, "Couldn't load "+name+" from system classloader");
      }
    try{
      if (useParent) {
        return getClass().getClassLoader().loadClass(name);
      }
      return super.loadClass(name);
    }catch (Exception e) {
      //last resort, use our context classloader.
      //this is required to do stuff like launch a working vertx server in compiler thread.
      if (useParent) {
        return super.loadClass(name);
      }
      return getClass().getClassLoader().loadClass(name);
    }
  }
  public boolean isAllowSystem() {
    return allowSystem;
  }
  public void setAllowSystem(boolean allow) {
    allowSystem = allow;
  }
}