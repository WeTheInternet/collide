// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.dtogen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutableDto;
import com.google.collide.dtogen.shared.RoutingType;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.common.base.Preconditions;

/**
 * Base template for the generated output file that contains all the DTOs.
 *
 *  Note that we generate client and server DTOs in separate runs of the
 * generator.
 *
 *  The directionality of the DTOs only affects whether or not we expose methods
 * to construct an instance of the DTO. We need both client and server versions
 * of all DTOs (irrespective of direction), but you aren't allowed to construct
 * a new {@link ServerToClientDto} on the client. And similarly, you aren't
 * allowed to construct a {@link ClientToServerDto} on the server.
 *
 */
public class DtoTemplate {
  @SuppressWarnings("serial")
  public static class MalformedDtoInterfaceException extends RuntimeException {
    public MalformedDtoInterfaceException(String msg) {
      super(msg);
    }
  }

  // We keep a whitelist of allowed non-DTO generic types.
  static final Set<Class<?>> jreWhitelist =
      new HashSet<Class<?>>(Arrays.asList(
          new Class<?>[] {String.class, Integer.class, Double.class, Float.class, Boolean.class}));

  private final List<DtoImpl> dtoInterfaces = new ArrayList<DtoImpl>();
  private final String packageName;
  private final String className;
  private final boolean isServerType;
  private final String apiHash;

  /**
   * @return whether or not the specified interface implements
   *         {@link ClientToServerDto}.
   */
  static boolean implementsClientToServerDto(Class<?> i) {
    return implementsInterface(i, ClientToServerDto.class);
  }

  /**
   * @return whether or not the specified interface implements
   *         {@link ServerToClientDto}.
   */
  static boolean implementsServerToClientDto(Class<?> i) {
    return implementsInterface(i, ServerToClientDto.class);
  }

  /**
   * Walks the superinterface hierarchy to determine if a Class implements some
   * target interface transitively.
   */
  static boolean implementsInterface(Class<?> i, Class<?> target) {
    if (i.equals(target)) {
      return true;
    }

    boolean rtn = false;
    Class<?>[] superInterfaces = i.getInterfaces();
    for (Class<?> superInterface : superInterfaces) {
      rtn = rtn || implementsInterface(superInterface, target);
    }
    return rtn;
  }

  /**
   * @return whether or not the specified interface implements
   *         {@link RoutableDto}.
   */
  private static boolean implementsRoutableDto(Class<?> i) {
    return implementsInterface(i, RoutableDto.class);
  }

  /**
   * Constructor.
   *
   * @param packageName The name of the package for the outer DTO class.
   * @param className The name of the outer DTO class.
   * @param isServerType Whether or not the DTO impls are client or server.
   */
  DtoTemplate(String packageName, String className, String apiHash, boolean isServerType) {
    this.packageName = packageName;
    this.className = className;
    this.apiHash = apiHash;
    this.isServerType = isServerType;
  }

  /**
   * Adds an interface to the DtoTemplate for code generation.
   *
   * @param i
   */
  public void addInterface(Class<?> i) {
    getDtoInterfaces().add(createDtoImplTemplate(i));
  }

  /**
   * @return the dtoInterfaces
   */
  public List<DtoImpl> getDtoInterfaces() {
    return dtoInterfaces;
  }

  /**
   * Returns the source code for a class that contains all the DTO impls for any
   * intefaces that were added via the {@link #addInterface(Class)} method.
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    emitPreamble(builder);
    emitClientFrontendApiVersion(builder);
    emitDtos(builder);
    emitPostamble(builder);
    return builder.toString();
  }

  /**
   * Tests whether or not a given class is a part of our dto jar, and thus will
   * eventually have a generated Impl that is serializable (thus allowing it to
   * be a generic type).
   */
  boolean isDtoInterface(Class<?> potentialDto) {
    for (DtoImpl dto : dtoInterfaces) {
      if (dto.getDtoInterface().equals(potentialDto)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Will initialize the routing ID to be RoutableDto.INVALID_TYPE if it is not
   * routable. This is a small abuse of the intent of that value, but it allows
   * us to simply omit it from the routing type enumeration later.
   *
   * @param i the super interface type
   * @return a new DtoServerTemplate or a new DtoClientTemplate depending on
   *         isServerImpl.
   */
  private DtoImpl createDtoImplTemplate(Class<?> i) {
    int routingId = implementsRoutableDto(i) ? getRoutingId(i) : RoutableDto.NON_ROUTABLE_TYPE;
    return isServerType ? new DtoImplServerTemplate(this, routingId, i) : new DtoImplClientTemplate(
        this, routingId, i);
  }

  private void emitDtos(StringBuilder builder) {
    for (DtoImpl dto : getDtoInterfaces()) {
      builder.append(dto.serialize());
    }
  }

  private void emitPostamble(StringBuilder builder) {
    builder.append("\n}");
  }

  private void emitPreamble(StringBuilder builder) {
    builder.append("// GENERATED SOURCE. DO NOT EDIT.\npackage ");
    builder.append(packageName);
    builder.append(";\n\n");
    if (isServerType) {
      builder.append("import com.google.collide.dtogen.server.JsonSerializable;\n");
      builder.append("\n");
      builder.append("import com.google.gson.Gson;\n");
      builder.append("import com.google.gson.GsonBuilder;\n");
      builder.append("import com.google.gson.JsonArray;\n");
      builder.append("import com.google.gson.JsonElement;\n");
      builder.append("import com.google.gson.JsonNull;\n");
      builder.append("import com.google.gson.JsonObject;\n");
      builder.append("import com.google.gson.JsonParser;\n");
      builder.append("import com.google.gson.JsonPrimitive;\n");
      builder.append("\n");
//      builder.append("import java.util.List;\n"); //unused
      builder.append("import java.util.Map;\n");
    }
    builder.append("\n\n@SuppressWarnings({\"unchecked\", \"cast\", \"rawtypes\"})\n");
    
    // Note that we always use fully qualified path names when referencing Types
    // so we need not add any import statements for anything.
    builder.append("public class ");
    builder.append(className);
    builder.append(" {\n\n");
    if (isServerType) {
      builder.append("  private static final Gson gson = "
          + "new GsonBuilder().serializeNulls().create();\n\n");
    }
    builder.append("  private  ");
    builder.append(className);
    builder.append("() {}\n");
  }
  
  /**
   * Emits a static variable that is the hash of all the classnames, methodnames, and return types
   * to be used as a version hash between client and server.
   */
  private void emitClientFrontendApiVersion(StringBuilder builder) {
    builder.append("\n  public static final String CLIENT_SERVER_PROTOCOL_HASH = \"");
    builder.append(getApiHash());
    builder.append("\";\n");
  }

  private String getApiHash() {
    return apiHash;
  }

  /**
   * Extracts the {@link RoutingType} annotation to derive the stable
   * routing type.
   */
  private int getRoutingId(Class<?> i) {
    RoutingType routingTypeAnnotation = i.getAnnotation(RoutingType.class);

    Preconditions.checkNotNull(routingTypeAnnotation,
        "RoutingType annotation must be specified for all subclasses of RoutableDto. " + 
        i.getName());

    return routingTypeAnnotation.type();
  }
}
