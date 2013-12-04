package com.google.collide.dto;

import xapi.gwtc.api.ObfuscationLevel;
import xapi.gwtc.api.OpenAction;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.core.ext.TreeLogger;

@RoutingType(type=RoutingTypes.CODEMODULE)
public interface CodeModule extends HasModule, ClientToServerDto, ServerToClientDto {

  String getManifestFile();
  JsonArray<String> getExtraArgs();
  boolean isRecompile();
  TreeLogger.Type getLogLevel();
  JsonArray<String> getSources();
  JsonArray<String> getDependencies();
  ObfuscationLevel getObfuscationLevel();
  OpenAction getOpenAction();
  
}
