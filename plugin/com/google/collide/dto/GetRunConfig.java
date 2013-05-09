package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;

@RoutingType(type = RoutingTypes.REQUESTRUNCONFIG)
public interface GetRunConfig
extends ClientToServerDto{

  String getModule();

}
