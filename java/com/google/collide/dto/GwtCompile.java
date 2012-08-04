package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.json.shared.JsonArray;

@RoutingType(type = RoutingTypes.GWTCOMPILE)
public interface GwtCompile extends ClientToServerDto{

  String getModule();
  JsonArray<String> getSrc();
  JsonArray<String> getLib();
  JsonArray<String> getDeps();
  
}
