package com.google.collide.dto;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;

@RoutingType(type = RoutingTypes.MAVENCONFIG)
public interface MavenConfig extends ClientToServerDto, ServerToClientDto{

  /**
   * @return - The id of the project;
   */
  public String getProjectId();
  /**
   * @return - The relative or root url of the target pom
   */
  public String getPomPath();
  /**
   * @return - The source root directory from which all relative uris derive.
   * Defaults to the collide work directory
   */
  public String getSourceRoot();
  /**
   * @return - The clean war directory (no generated files)
   */
  public String getWarSource();
  /**
   * @return - The target directory to which we copy a generated production war
   */
  public String getWarTarget();
  /**
   * @return - All source folders used by maven (and gwt recompiler)
   */
  public JsonArray<String> getSourceFolders();
  /**
   * @return - The relative of all poms from source root
   */
  public JsonArray<String> getPoms();
  
}
