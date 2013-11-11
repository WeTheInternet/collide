package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

@RoutingType(type = RoutingTypes.GWTSETTINGS)
public interface GwtSettings extends ClientToServerDto, ServerToClientDto{

  JsonArray<GwtCompile> getModules();
  
  
}
