package com.google.collide.dto;

import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.core.ext.TreeLogger;

@RoutingType(type = RoutingTypes.RECEIVERUNCONFIG)
public interface GetRunConfigResponse
extends ServerToClientDto{

  String getModule();

  JsonArray<String> getSrc();
  JsonArray<String> getLib();
  JsonArray<String> getDeps();
  String getOutput();
  TreeLogger.Type getLogLevel();

}
