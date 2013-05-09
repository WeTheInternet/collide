package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.core.ext.TreeLogger;

@RoutingType(type=RoutingTypes.CODEMODULE)
public interface CodeModule extends ClientToServerDto, ServerToClientDto {

  String getModule();
  boolean isRecompile();
  JsonArray<String> getSrc();
//  JsonArray<String> getLib();
  JsonArray<String> getDeps();
  TreeLogger.Type getLogLevel();

}
