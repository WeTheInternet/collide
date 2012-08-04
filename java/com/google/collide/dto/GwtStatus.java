package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

@RoutingType(type = RoutingTypes.GWTSTATUS)
public interface GwtStatus extends ServerToClientDto{

  public static enum CompileStatus{
    UNLOADED, RUNNING, BLOCKING, FINISHED
  }
  
  public CompileStatus getCompilerStatus();
  public String getModule();
  
}
