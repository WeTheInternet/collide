package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.gwt.core.ext.TreeLogger;


@RoutingType(type = RoutingTypes.LOGMESSAGE)
public interface LogMessage extends ServerToClientDto{

  public String getModule();
  public String getMessage();
  public String getHelpInfo();
  public String getError();
  public int getCode();
  public TreeLogger.Type getLogLevel();
  
}
