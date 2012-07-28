package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;

@RoutingType(type = RoutingTypes.SETMAVENCONFIG)
public interface SetMavenConfig extends ClientToServerDto{


  public String getProjectId();
  public String getPomPath();
  public MavenConfig getConfig();
}
