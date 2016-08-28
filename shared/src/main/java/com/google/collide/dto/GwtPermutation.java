package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

@RoutingType(type = RoutingTypes.GWTPERMUTATION)
public interface GwtPermutation extends ClientToServerDto, ServerToClientDto{

  String getPermutationName();
  JsonArray<String> getPermutationOptions();
  JsonArray<String> getPermutationsUsed();
  
}
