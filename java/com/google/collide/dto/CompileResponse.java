package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

@RoutingType(type = RoutingTypes.COMPILERESPONSE)
public interface CompileResponse extends ServerToClientDto{

  public static enum CompilerState{
    UNLOADED, RUNNING, BLOCKING, FINISHED, SERVING, FAILED
  }

  public CompilerState getCompilerStatus();
  public String getModule();
  public int getPort();
  public boolean isAuthorized();

}
