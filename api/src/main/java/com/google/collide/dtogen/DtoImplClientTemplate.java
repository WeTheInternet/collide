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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutableDto;
import com.google.collide.dtogen.shared.SerializationIndex;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.common.base.Preconditions;

/**
 * Generates the source code for a generated Client DTO impl.
 *
 */
public class DtoImplClientTemplate extends DtoImpl {

  private static final String ROUTABLE_DTO_IMPL =
      RoutableDto.class.getPackage().getName().replace("dtogen.shared", "dtogen.client")
          + ".RoutableDtoClientImpl";


  private static final String JSO_TYPE = "com.google.collide.json.client.Jso";

  private static boolean isEnum(Class<?> type) {
    return type != null && (type.equals(Enum.class) || isEnum(type.getSuperclass()));
  }

  DtoImplClientTemplate(DtoTemplate template, int routingType, Class<?> dtoInterface) {
    super(template, routingType, dtoInterface);
  }

  @Override
  String serialize() {
    StringBuilder builder = new StringBuilder();

    Class<?> dtoInterface = getDtoInterface();
    List<Method> methods = getDtoMethods();

    emitPreamble(dtoInterface, builder);
    emitMethods(methods, builder);

    // Only emit a factory method if the supertype is a ClientToServerDto or is
    // non-routable.
    if (DtoTemplate.implementsClientToServerDto(dtoInterface)
        || getRoutingType() == RoutableDto.NON_ROUTABLE_TYPE) {
      emitFactoryMethod(builder);
    } else {
      builder.append("\n  }\n");
      // emit testing mock, with factory, as a separate subclass
      emitMockPreamble(dtoInterface, builder);
      emitFactoryMethod(builder);
    }

    builder.append("  }\n");
    return builder.toString();
  }

  /**
   * Emits a factory method that trivially returns a new Javascript object with
   * the type set.
   */
  private void emitFactoryMethod(StringBuilder builder) {
    builder.append("\n    public static native ");
    builder.append(getImplClassName());
    builder.append(" make() /*-{\n");
    if (isCompactJson()) {
      builder.append("      return [];");
    } else {
      builder.append("      return {\n");
      if (getRoutingType() != RoutableDto.NON_ROUTABLE_TYPE) {
        emitKeyValue(RoutableDto.TYPE_FIELD, Integer.toString(getRoutingType()), builder);
      }
      builder.append("\n      };");
    }
    builder.append("\n    }-*/;");
  }

  private void emitHasMethod(String methodName, String fieldSelector, StringBuilder builder) {
    builder.append("\n    public final native boolean ").append(methodName).append("() /*-{\n");
    builder.append("      return this.hasOwnProperty(").append(fieldSelector).append(");\n");
    builder.append("    }-*/;\n");
  }

  private void emitGetter(Method method,
      String methodName, String fieldSelector, String returnType, StringBuilder builder) {
    builder.append("\n    @Override\n    public final native ");
    builder.append(returnType);
    builder.append(" ");
    builder.append(methodName);
    builder.append("() /*-{\n");
    if (isCompactJson()) {
      // We can omit last members in list it they do not carry any information.
      // Currently we skip only one member, and only if it is an array.
      // We can add more cases and more robust "tail" detection in the future.
      if (isLastMethod(method)) {
        List<Type> expandedTypes = expandType(method.getGenericReturnType());
        if (isJsonArray(getRawClass(expandedTypes.get(0)))) {
          builder.append("      if (!this.hasOwnProperty(").append(fieldSelector).append(")) {\n");
          builder.append("        this[").append(fieldSelector).append("] = [];\n");
          builder.append("      }\n");
        }
      }
    }
    emitReturn(method, fieldSelector, builder);
    builder.append("    }-*/;\n");
  }

  private void emitKeyValue(String fieldName, String value, StringBuilder builder) {
    builder.append("        ");
    builder.append(fieldName);
    builder.append(": ");
    builder.append(value);
  }

  private void emitMethods(List<Method> methods, StringBuilder builder) {
    for (Method method : methods) {
      if (ignoreMethod(method)) {
        continue;
      }
      String methodName = method.getName();
      String fieldName = getFieldName(methodName);
      String fieldSelector;
      if (isCompactJson()) {
        SerializationIndex serializationIndex = Preconditions.checkNotNull(
            method.getAnnotation(SerializationIndex.class));
        fieldSelector = String.valueOf(serializationIndex.value() - 1);
      } else {
        fieldSelector = "\"" + getFieldName(methodName) + "\"";
      }
      String returnTypeName =
          method.getGenericReturnType().toString().replace('$', '.').replace("class ", "")
              .replace("interface ", "");

      // Native JSNI Getter.
      emitGetter(method, methodName, fieldSelector, returnTypeName, builder);

      // Native JSNI Setter
      emitSetter(getSetterName(fieldName), fieldName, fieldSelector,
          method.getGenericReturnType(), returnTypeName, getImplClassName(), builder);

      emitHasMethod("has" + getCamelCaseName(fieldName), fieldSelector, builder);
    }
  }

  private void emitMockPreamble(Class<?> dtoInterface, StringBuilder builder) {
    builder.append("\n\n  public static class Mock");
    builder.append(getImplClassName());
    builder.append(" extends ");
    builder.append(getImplClassName());
    builder.append(" {\n    protected Mock");
    builder.append(getImplClassName());
    builder.append("() {}\n");
  }

  private void emitPreamble(Class<?> dtoInterface, StringBuilder builder) {
    builder.append("\n\n  public static class ");
    builder.append(getImplClassName());
    builder.append(" extends ");
    Class<?> superType = getSuperInterface();
    if (superType != null) {
      // We special case ServerToClientDto and ClientToServerDto since their
      // impls are not generated.
      if (superType.equals(ServerToClientDto.class) || superType.equals(ClientToServerDto.class)) {
        builder.append(ROUTABLE_DTO_IMPL);
      } else {
        builder.append(superType.getSimpleName() + "Impl");
      }
    } else {
      // Just a plain Jso.
      builder.append(JSO_TYPE);
    }
    boolean isSerializable = isSerializable();
    builder.append(" implements ");
    builder.append(dtoInterface.getCanonicalName());
    if (isSerializable){
      builder.append(", java.io.Serializable");
    }
    builder.append(" {\n    protected ");
    builder.append(getImplClassName());
    builder.append("() {}\n");
    if (isSerializable){
      builder.append("  private static final long serialVersionUID = " +dtoInterface.getCanonicalName().hashCode()+"L;\n");
    }
  }

  private void emitReturn(Method method, String fieldSelector, StringBuilder builder) {
    Type type = method.getGenericReturnType();
    Class<?> rawClass = getRawClass(type);
    String thisFieldName = "this[" + fieldSelector + "]";

    if (type instanceof ParameterizedType) {
      List<Type> expandedTypes = expandType(type);
      if (hasEnum(expandedTypes)) {
        final String tmpVar = "_tmp";
        emitReturnEnumReplacement(expandedTypes, 0, thisFieldName, tmpVar, "      ", builder);
        builder.append("      return ").append(tmpVar).append(";\n");
        return;
      }
    }
    
    builder.append("      return ");
    if (isEnum(method.getReturnType())) {
      // Gson serializes enums with their toString() representation
      emitEnumValueOf(rawClass, thisFieldName, builder);
    } else {
      builder.append(thisFieldName);
    }
    builder.append(";\n");
  }

  private void emitReturnEnumReplacement(List<Type> types, int i, String inVar, String outVar,
      String indentation, StringBuilder builder) {
    Class<?> rawClass = getRawClass(types.get(i));
    String tmpVar = "tmp" + i;
    String childInVar = "in" + (i + 1);
    String childOutVar = "out" + (i + 1);
    
    if (isJsonArray(rawClass)) {
      // tmpVar is the index
      builder.append(indentation).append(outVar).append(" = [];\n");
      builder.append(indentation).append(inVar).append(".forEach(function(").append(childInVar)
          .append(", ").append(tmpVar).append(") {\n");
    } else if (isEnum(rawClass)) {
      builder.append(indentation).append(outVar).append(" = ");
      emitEnumValueOf(rawClass, inVar, builder);
      builder.append(";\n");
    } else if (isJsonStringMap(rawClass)) {
      // TODO: implement when needed
      throw new IllegalStateException("enums inside JsonStringMaps need to be implemented");
    }
    
    if (i + 1 < types.size()) {
      emitReturnEnumReplacement(types, i + 1, childInVar, childOutVar, indentation + "  ", builder);
    }
    
    if (isJsonArray(rawClass)) {
      builder.append(indentation).append("  ").append(outVar).append("[").append(tmpVar)
          .append("] = ").append(childOutVar).append(";\n");
      builder.append(indentation).append("});\n");
    }
  }

  private void emitEnumValueOf(Class<?> rawClass, String var, StringBuilder builder) {
    builder.append(var);
    builder.append("? @");
    builder.append(rawClass.getCanonicalName());
    builder.append("::valueOf(Ljava/lang/String;)(");
    builder.append(var);
    builder.append(")");
    builder.append(": null");
  }

  private void emitSetter(String methodName, String fieldName, String fieldSelector, Type type,
      String paramTypeName, String returnType,
      StringBuilder builder) {
    builder.append("\n    public final native ");
    builder.append(returnType);
    builder.append(" ");
    builder.append(methodName);
    builder.append("(");
    emitSetterParameterTypeName(getRawClass(type), paramTypeName, builder);
    builder.append(" ");
    builder.append(fieldName);
    builder.append(") /*-{\n");
    emitSetterPropertyAssignment(fieldName, fieldSelector, type, builder);
    builder.append("      return this;\n    }-*/;\n");
  }

  private void emitSetterParameterTypeName(Class<?> paramType, String paramTypeName,
      StringBuilder builder) {
    /*
     * For our Json collections, require the concrete client-side type since we
     * call JSON.stringify on this DTO.
     */
    if (paramType == JsonArray.class) {
      paramTypeName = paramTypeName.replace("com.google.collide.json.shared.JsonArray",
          "com.google.collide.json.client.JsoArray");
    } else if (paramType == JsonStringMap.class) {
      paramTypeName =
          paramTypeName.replace("com.google.collide.json.shared.JsonStringMap",
              "com.google.collide.json.client.JsoStringMap");
    }

    builder.append(paramTypeName);
  }

  private void emitSetterPropertyAssignment(
      String fieldName, String fieldSelector, Type type, StringBuilder builder) {
    Class<?> rawClass = getRawClass(type);

    if (type instanceof ParameterizedType) {
      List<Type> expandedTypes = expandType(type);
      if (hasEnum(expandedTypes)) {
        final String tmpVar = "_tmp";
        builder.append("      ").append(tmpVar).append(" = ").append(fieldName).append(";\n");
        emitSetterEnumReplacement(expandedTypes, 0, tmpVar, fieldName, "      ", builder);
      }
    } else if (isEnum(rawClass)) {
      /*-
       * codeBlockType =
       *     codeBlockType.@com.google.collide.dto.CodeBlock.Type::
       *     toString()()
       */
      builder.append("      ").append(fieldName).append(" = ");
      emitEnumToString(rawClass, fieldName, builder);
      builder.append(";\n");
    }

    builder.append("      this[");
    builder.append(fieldSelector);
    builder.append("] = ");
    builder.append(fieldName);
    builder.append(";\n");
  }

  private boolean hasEnum(List<Type> types) {
    for (Type type : types) {
      if (isEnum(getRawClass(type))) {
        return true;
      }
    }
    
    return false;
  }
  
  private void emitSetterEnumReplacement(List<Type> types, int i, String inVar, String outVar,
      String indentation, StringBuilder builder) {
    Class<?> rawClass = getRawClass(types.get(i));
    String tmpVar = "tmp" + i;
    String childInVar = "in" + (i + 1);
    String childOutVar = "out" + (i + 1);
    
    if (isJsonArray(rawClass)) {
      builder.append(indentation).append(tmpVar).append(" = [];\n");
      builder.append(indentation).append(inVar).append(".forEach(function(").append(childInVar)
          .append(") {\n");
    } else if (isEnum(rawClass)) {
      builder.append(indentation).append(outVar).append(" = ");
      emitEnumToString(rawClass, inVar, builder);
      builder.append(";\n");
    } else if (isJsonStringMap(rawClass)) {
      // TODO: implement when needed
      throw new IllegalStateException("enums inside JsonStringMaps need to be implemented");
    }
    
    if (i + 1 < types.size()) {
      emitSetterEnumReplacement(types, i + 1, childInVar, childOutVar, indentation + "  ", builder);
    }
    
    if (isJsonArray(rawClass)) {
      builder.append(indentation).append("  ").append(tmpVar).append(".push(").append(childOutVar)
          .append(");\n");
      builder.append(indentation).append("});\n");
      builder.append(indentation).append(outVar).append(" = ").append(tmpVar).append(";\n");
    }
  }

  private void emitEnumToString(Class<?> enumClass, String fieldName, StringBuilder builder) {
    builder.append(fieldName);
    builder.append(".@");
    builder.append(enumClass.getCanonicalName());
    builder.append("::toString()()");
  }
}
