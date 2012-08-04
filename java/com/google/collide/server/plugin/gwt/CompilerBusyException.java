package com.google.collide.server.plugin.gwt;

public class CompilerBusyException extends Throwable{
  private static final long serialVersionUID = -7998731646049544430L;
  private String module;

  public CompilerBusyException() {
  }
  
  public CompilerBusyException(String module) {
    this.module = module;
  }
  
  /**
   * @return the module
   */
  public String getModule() {
    return module;
  }

  /**
   * @param module the module to set
   */
  public void setModule(String module) {
    this.module = module;
  }
}