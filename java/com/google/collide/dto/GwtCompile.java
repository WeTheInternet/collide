package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

@RoutingType(type = RoutingTypes.GWTCOMPILE)
public interface GwtCompile extends GwtRecompile, ClientToServerDto, ServerToClientDto{

  public String getDeployDir();

  public String getExtrasDir();

  public int getFragments();

  public String getGenDir();

  public String getGwtVersion();

  public int getLocalWorkers();

  public int getOptimizationLevel();
  
  public JsonArray<String> getSystemProperties();

  public String getUnitCacheDir();
  
  public String getUrlToOpen();

  public String getWarDir();

  public String getWorkDir();

  public boolean isClosureCompiler();
  
  public boolean isDisableAggressiveOptimize();
  
  public boolean isDisableCastCheck();
  
  public boolean isDisableClassMetadata();

  public boolean isDisableRunAsync();
  
  public boolean isDisableThreadedWorkers();

  public boolean isDisableUnitCache();
  
  public boolean isDraftCompile();
  
  public boolean isEnableAssertions();
  
  public boolean isSoyc();
  
  public boolean isSoycDetailed();
  
  public boolean isStrict();
  
  public boolean isValidateOnly();
  
}
