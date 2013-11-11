package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;

@RoutingType(type = RoutingTypes.GWTKILL)
public interface GwtKill extends ClientToServerDto {

  public String getModule();
}
