package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;

@RoutingType(type=RoutingTypes.HASMODULE)
public interface HasModule extends ServerToClientDto, ClientToServerDto {

  String getModule();
  
}
