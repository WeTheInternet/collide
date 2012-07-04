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

import com.google.collide.dtogen.shared.ClientToServerDto;
import com.google.collide.dtogen.shared.RoutableDto;
import com.google.collide.dtogen.shared.SerializationIndex;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Generates the source code for a generated Server DTO impl.
 *
 */
public class DtoImplServerTemplate extends DtoImpl {
  private static final String JSON_ARRAY = JsonArray.class.getCanonicalName();

  private static final String JSON_ARRAY_ADAPTER =
      JsonStringMap.class.getPackage().getName().replace(".shared", ".server.JsonArrayListAdapter");

  private static final String JSON_MAP = JsonStringMap.class.getCanonicalName();

  private static final String JSON_MAP_ADAPTER =
      JsonStringMap.class.getPackage().getName().replace(".shared", ".server.JsonStringMapAdapter");

  private static final String ROUTABLE_DTO_IMPL =
      RoutableDto.class.getPackage().getName().replace("dtogen.shared", "dtogen.server")
          + ".RoutableDtoServerImpl";

  DtoImplServerTemplate(DtoTemplate template, int routingType, Class<?> superInterface) {
    super(template, routingType, superInterface);
  }

  @Override
  String serialize() {
    StringBuilder builder = new StringBuilder();

    Class<?> dtoInterface = getDtoInterface();
    List<Method> methods = getDtoMethods();

    emitPreamble(dtoInterface, builder);

    // Enumerate the getters and emit field names and getters + setters.
    emitFields(methods, builder);
    emitMethods(methods, builder);
    emitEqualsAndHashcode(methods, builder);
    emitSerializer(methods, builder);
    emitDeserializer(methods, builder);
    emitDeserializerShortcut(methods, builder);
    builder.append("  }\n");

    // Emit a testing mock
    emitMockPreamble(dtoInterface, builder);
    builder.append("  }\n");

    return builder.toString();
  }

  private void emitDefaultRoutingTypeConstructor(StringBuilder builder) {
    builder.append("    private ");
    builder.append(getImplClassName());
    builder.append("() {");
    builder.append("\n      super(");
    builder.append("" + getRoutingType());
    builder.append(");\n    ");
    builder.append("}\n\n");
  }

  private void emitEqualsAndHashcode(List<Method> methods, StringBuilder builder) {
    builder.append("\n");
    builder.append("    @Override\n");
    builder.append("    public boolean equals(Object o) {\n");
    // if this class inherits from anything, check that the superclass fields are also equal
    Class<?> superType = getSuperInterface();
    if (superType != null) {
      builder.append("      if (!super.equals(o)) {\n");
      builder.append("        return false;\n");
      builder.append("      }\n");
    }
    builder.append("      if (!(o instanceof " + getImplClassName() + ")) {\n");
    builder.append("        return false;\n");
    builder.append("      }\n");
    builder.append("      " + getImplClassName() + " other = (" + getImplClassName() + ") o;\n");
    for (Method method : methods) {
      if (!ignoreMethod(method)) {
        String fieldName = getFieldName(method.getName());
        String hasFieldName = getHasFieldName(fieldName);
        builder.append("      if (this." + hasFieldName + " != other." + hasFieldName + ") {\n");
        builder.append("        return false;\n");
        builder.append("      }\n");
        builder.append("      if (this." + hasFieldName + ") {\n");
        if (method.getReturnType().isPrimitive()) {
          builder.append("        if (this." + fieldName + " != other." + fieldName + ") {\n");
        } else {
          builder.append("        if (!this." + fieldName + ".equals(other." + fieldName +
              ")) {\n");
        }
        builder.append("          return false;\n");
        builder.append("        }\n");
        builder.append("      }\n");
      }
    }
    builder.append("      return true;\n");
    builder.append("    }\n");

    // this isn't the greatest hash function in the world, but it meets the requirement that for any
    // two objects A and B, A.equals(B) only if A.hashCode() == B.hashCode()
    builder.append("\n");
    builder.append("    @Override\n");
    builder.append("    public int hashCode() {\n");
    // if this class inherits from anything, include the superclass hashcode
    if (superType != null) {
      builder.append("      int hash = super.hashCode();\n");
    } else {
      builder.append("      int hash = 1;\n");
    }
    for (Method method : methods) {
      if (!ignoreMethod(method)) {
        Class<?> type = method.getReturnType();

        String fieldName = getFieldName(method.getName());
        builder.append("      hash = hash * 31 + (" + getHasFieldName(fieldName) + " ? ");
        if (type.isPrimitive()) {
          Class<?> wrappedType = Primitives.wrap(type);
          builder.append(wrappedType.getName() + ".valueOf(" + fieldName + ").hashCode()");
        } else {
          builder.append(fieldName + ".hashCode()");
        }
        builder.append(" : 0);\n");
      }
    }
    builder.append("      return hash;\n");
    builder.append("    }\n");
  }

  private void emitFactoryMethod(StringBuilder builder) {
    builder.append("    public static ");
    builder.append(getImplClassName());
    builder.append(" make() {");
    builder.append("\n      return new ");
    builder.append(getImplClassName());
    builder.append("();\n    }\n\n");
  }

  private void emitFields(List<Method> methods, StringBuilder builder) {
    for (Method method : methods) {
      if (!ignoreMethod(method)) {
        String methodName = method.getName();
        String fieldName = getFieldName(methodName);

        builder.append("    ");
        builder.append(getFieldTypeAndAssignment(method, fieldName));

        // Emit a boolean to track whether the DTO has the field.
        builder.append("    private boolean ");
        builder.append(getHasFieldName(fieldName));
        builder.append(";\n");
      }
    }
  }

  private void emitHasField(String fieldName, StringBuilder builder) {
    String camelCaseFieldName = getCamelCaseName(fieldName);
    builder.append("\n    public boolean has");
    builder.append(camelCaseFieldName);
    builder.append("() {\n");
    builder.append("      return ");
    builder.append(getHasFieldName(fieldName));
    builder.append(";\n    }\n");
  }

  /**
   * Emits a method to get a field. Getting a collection ensures that the collection
   * is created.
   */
  private void emitGetter(Method method, String methodName, String fieldName, String returnType,
      StringBuilder builder) {
    builder.append("\n    @Override\n    public ");
    builder.append(returnType);
    builder.append(" ");
    builder.append(methodName);
    builder.append("() {\n");

    // Initialize the collection.
    Class<?> returnTypeClass = method.getReturnType();
    if (isJsonArray(returnTypeClass) || isJsonStringMap(returnTypeClass)) {
      builder.append("      ");
      builder.append(getEnsureName(fieldName));
      builder.append("();\n");
    }

    builder.append("      return ");
    emitReturn(method, fieldName, builder);
    builder.append(";\n    }\n");
  }

  private void emitMethods(List<Method> methods, StringBuilder builder) {
    for (Method method : methods) {
      if (ignoreMethod(method)) {
        continue;
      }

      String methodName = method.getName();
      String fieldName = getFieldName(methodName);
      Class<?> returnTypeClass = method.getReturnType();
      String returnType = method
          .getGenericReturnType()
          .toString()
          .replace('$', '.')
          .replace("class ", "")
          .replace("interface ", "");

      // HasField.
      emitHasField(fieldName, builder);

      // Getter.
      emitGetter(method, methodName, fieldName, returnType, builder);

      // Setter.
      emitSetter(method, getImplClassName(), fieldName, builder);

      // List-specific methods.
      if (isJsonArray(returnTypeClass)) {
        emitListAdd(method, fieldName, builder);
        emitClear(fieldName, builder);
        emitEnsureCollection(method, fieldName, builder);
      } else if (isJsonStringMap(returnTypeClass)) {
        emitMapPut(method, fieldName, builder);
        emitClear(fieldName, builder);
        emitEnsureCollection(method, fieldName, builder);
      }
    }
  }

  private void emitSerializer(List<Method> methods, StringBuilder builder) {
    builder.append("\n    @Override\n");
    builder.append("    public JsonElement toJsonElement() {\n");
    if (isCompactJson()) {
      builder.append("      JsonArray result = new JsonArray();\n");
      for (Method method : methods) {
        emitSerializeFieldForMethodCompact(method, builder);
      }
    } else {
      builder.append("      JsonObject result = new JsonObject();\n");
      for (Method method : methods) {
        emitSerializeFieldForMethod(method, builder);
      }
    }
    builder.append("      return result;\n");
    builder.append("    }\n");
    builder.append("\n");
    builder.append("    @Override\n");
    builder.append("    public String toJson() {\n");
    builder.append("      return gson.toJson(toJsonElement());\n");
    builder.append("    }\n");
    builder.append("\n");
    builder.append("    @Override\n");
    builder.append("    public String toString() {\n");
    builder.append("      return toJson();\n");
    builder.append("    }\n");
  }

  private void emitSerializeFieldForMethod(Method method, final StringBuilder builder) {
    if (method.getName().equals("getType")) {
      String typeFieldName = "_type";
      if (getRoutingType() == RoutableDto.NON_ROUTABLE_TYPE) {
        typeFieldName = "type";
      }
      builder.append("      result.add(\"" + typeFieldName
          + "\", new JsonPrimitive(getType()));\n");
      return;
    }

    final String fieldName = getFieldName(method.getName());
    final String fieldNameOut = fieldName + "Out";
    final String baseIndentation = "      ";

    builder.append("\n");
    List<Type> expandedTypes = expandType(method.getGenericReturnType());
    emitSerializerImpl(expandedTypes, 0, builder, fieldName, fieldNameOut, baseIndentation);
    builder.append("      result.add(\"" + fieldName + "\", ").append(fieldNameOut).append(");\n");
  }

  private void emitSerializeFieldForMethodCompact(Method method, StringBuilder builder) {
    if (method == null) {
      builder.append("      result.add(JsonNull.INSTANCE);\n");
      return;
    }
    final String fieldName = getFieldName(method.getName());
    final String fieldNameOut = fieldName + "Out";
    final String baseIndentation = "      ";

    builder.append("\n");
    List<Type> expandedTypes = expandType(method.getGenericReturnType());
    emitSerializerImpl(expandedTypes, 0, builder, fieldName, fieldNameOut, baseIndentation);
    if (isLastMethod(method)) {
      if (isJsonArray(getRawClass(expandedTypes.get(0)))) {
        builder.append("      if (").append(fieldNameOut).append(".size() != 0) {\n");
        builder.append("        result.add(").append(fieldNameOut).append(");\n");
        builder.append("      }\n");
        return;
      }
    }
    builder.append("      result.add(").append(fieldNameOut).append(");\n");
  }

  /**
   * Produces code to serialize the type with the given variable names.
   *
   * @param expandedTypes the type and its generic (and its generic (..))
   *        expanded into a list, @see {@link #expandType(Type)}
   * @param depth the depth (in the generics) for this recursive call. This can
   *        be used to index into {@code expandedTypes}
   * @param inVar the java type that will be the input for serialization
   * @param outVar the JsonElement subtype that will be the output for
   *        serialization
   * @param i indentation string
   */
  private void emitSerializerImpl(List<Type> expandedTypes, int depth,
      StringBuilder builder, String inVar, String outVar, String i) {

    Type type = expandedTypes.get(depth);
    String childInVar = inVar + "_";
    String childOutVar = outVar + "_";
    String entryVar = "entry" + depth;
    Class<?> rawClass = getRawClass(type);

    if (isJsonArray(rawClass)) {
      String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
      builder.append(i).append("JsonArray ").append(outVar).append(" = new JsonArray();\n");
      if (depth == 0) {
        builder.append(i).append(getEnsureName(inVar)).append("();\n");
      }
      builder.append(i).append("for (").append(childInTypeName).append(" ")
          .append(childInVar).append(" : ").append(inVar).append(") {\n");

    } else if (isJsonStringMap(rawClass)) {
      String childInTypeName = getImplName(expandedTypes.get(depth + 1), false);
      builder.append(i).append("JsonObject ").append(outVar).append(" = new JsonObject();\n");
      if (depth == 0) {
        builder.append(i).append(getEnsureName(inVar)).append("();\n");
      }
      builder.append(i).append("for (Map.Entry<String, ").append(childInTypeName).append("> ")
          .append(entryVar).append(" : ").append(inVar).append(".entrySet()) {\n");
      builder.append(i).append("  ").append(childInTypeName).append(" ").append(childInVar)
          .append(" = ").append(entryVar).append(".getValue();\n");

    } else if (rawClass.isEnum()) {
      builder.append(i).append("JsonElement ").append(outVar).append(" = (").append(inVar)
          .append(" == null) ? JsonNull.INSTANCE : new JsonPrimitive(").append(inVar)
          .append(".name());\n");
    } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
      builder.append(i).append("JsonElement ").append(outVar).append(" = ").append(inVar)
          .append(" == null ? JsonNull.INSTANCE : ").append(inVar).append(".toJsonElement();\n");

    } else if (rawClass.equals(String.class)) {
      builder.append(i).append("JsonElement ").append(outVar)
          .append(" = (").append(inVar).append(" == null) ? JsonNull.INSTANCE : new JsonPrimitive(")
          .append(inVar).append(");\n");
    } else {
      builder.append(i).append("JsonPrimitive ").append(outVar).append(" = new JsonPrimitive(")
          .append(inVar).append(");\n");
    }

    if (depth + 1 < expandedTypes.size()) {
      emitSerializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i
          + "  ");
    }

    if (isJsonArray(rawClass)) {
      builder.append(i).append("  ").append(outVar).append(".add(").append(childOutVar)
          .append(");\n");
      builder.append(i).append("}\n");

    } else if (isJsonStringMap(rawClass)) {
      builder.append(i).append("  ").append(outVar).append(".add(").append(entryVar)
          .append(".getKey(), ").append(childOutVar).append(");\n");
      builder.append(i).append("}\n");
    }
  }

  /**
   * Generates a static factory method that creates a new instance based
   * on a JsonElement.
   */
  private void emitDeserializer(List<Method> methods, StringBuilder builder) {
    builder.append("\n    public static ");
    builder.append(getImplClassName());
    builder.append(" fromJsonElement(JsonElement jsonElem) {\n");
    builder.append("      if (jsonElem == null || jsonElem.isJsonNull()) {\n");
    builder.append("        return null;\n");
    builder.append("      }\n\n");
    builder.append("      ").append(getImplClassName()).append(" dto = new ")
        .append(getImplClassName()).append("();\n");
    if (isCompactJson()) {
      builder.append("      JsonArray json = jsonElem.getAsJsonArray();\n");
      for (Method method : methods) {
        if (method == null) {
          continue;
        }
        emitDeserializeFieldForMethodCompact(method, builder);
      }
    } else {
      builder.append("      JsonObject json = jsonElem.getAsJsonObject();\n");
      for (Method method : methods) {
        emitDeserializeFieldForMethod(method, builder);
      }
    }
    builder.append("\n      return dto;\n");
    builder.append("    }");
  }

  private void emitDeserializerShortcut(List<Method> methods, StringBuilder builder) {
    builder.append("\n");
    builder.append("    public static ");
    builder.append(getImplClassName());
    builder.append(" fromJsonString(String jsonString) {\n");
    builder.append("      if (jsonString == null) {\n");
    builder.append("        return null;\n");
    builder.append("      }\n\n");
    builder.append("      return fromJsonElement(new JsonParser().parse(jsonString));\n");
    builder.append("    }\n");
  }

  private void emitDeserializeFieldForMethod(Method method, final StringBuilder builder) {
    if (method.getName().equals("getType")) {
      // The type is set in the constructor.
      return;
    }

    final String fieldName = getFieldName(method.getName());
    final String fieldNameIn = fieldName + "In";
    final String fieldNameOut = fieldName + "Out";
    final String baseIndentation = "        ";

    builder.append("\n");
    builder.append("      if (json.has(\"").append(fieldName).append("\")) {\n");
    List<Type> expandedTypes = expandType(method.getGenericReturnType());
    builder.append("        JsonElement ").append(fieldNameIn).append(" = json.get(\"")
        .append(fieldName).append("\");\n");
    emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
    builder.append("        dto.").append(getSetterName(fieldName)).append("(")
        .append(fieldNameOut).append(");\n");
    builder.append("      }\n");
  }

  private void emitDeserializeFieldForMethodCompact(
      Method method, final StringBuilder builder) {
    final String fieldName = getFieldName(method.getName());
    final String fieldNameIn = fieldName + "In";
    final String fieldNameOut = fieldName + "Out";
    final String baseIndentation = "        ";
    SerializationIndex serializationIndex = Preconditions.checkNotNull(
        method.getAnnotation(SerializationIndex.class));
    int index = serializationIndex.value() - 1;

    builder.append("\n");
    builder.append("      if (").append(index).append(" < json.size()) {\n");
    List<Type> expandedTypes = expandType(method.getGenericReturnType());
    builder.append("        JsonElement ").append(fieldNameIn).append(" = json.get(")
        .append(index).append(");\n");
    emitDeserializerImpl(expandedTypes, 0, builder, fieldNameIn, fieldNameOut, baseIndentation);
    builder.append("        dto.").append(getSetterName(fieldName)).append("(")
        .append(fieldNameOut).append(");\n");
    builder.append("      }\n");
  }

  /**
   * Produces code to deserialize the type with the given variable names.
   *
   * @param expandedTypes the type and its generic (and its generic (..))
   *        expanded into a list, @see {@link #expandType(Type)}
   * @param depth the depth (in the generics) for this recursive call. This can
   *        be used to index into {@code expandedTypes}
   * @param inVar the java type that will be the input for serialization
   * @param outVar the JsonElement subtype that will be the output for
   *        serialization
   * @param i indentation string
   */
  private void emitDeserializerImpl(List<Type> expandedTypes, int depth, StringBuilder builder,
      String inVar, String outVar, String i) {

    Type type = expandedTypes.get(depth);
    String childInVar = inVar + "_";
    String childOutVar = outVar + "_";
    Class<?> rawClass = getRawClass(type);

    if (isJsonArray(rawClass)) {
      String inVarIterator = inVar + "Iterator";
      builder.append(i).append(getImplName(type, false)).append(" ").append(outVar)
          .append(" = null;\n");
      builder.append(i).append("if (").append(inVar).append(" != null && !").append(inVar)
          .append(".isJsonNull()) {\n");
      builder.append(i).append("  ").append(outVar).append(" = new ")
          .append(getImplName(type, false)).append("();\n");
      builder.append(i).append("  ").append(getImplName(Iterator.class, true))
          .append("<JsonElement> ").append(inVarIterator).append(" = ").append(inVar)
          .append(".getAsJsonArray().iterator();\n");
      builder.append(i).append("  while (").append(inVarIterator).append(".hasNext()) {\n");
      builder.append(i).append("    JsonElement ").append(childInVar).append(" = ")
          .append(inVarIterator).append(".next();\n");

      emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

      builder.append(i).append("    ").append(outVar).append(".add(").append(childOutVar)
          .append(");\n");
      builder.append(i).append("  }\n");
      builder.append(i).append("}\n");
    } else if (isJsonStringMap(rawClass)) {
      // TODO: Handle type
      String entryVar = "entry" + depth;
      String entriesVar = "entries" + depth;
      builder.append(i).append(getImplName(type, false)).append(" ").append(outVar)
          .append(" = null;\n");
      builder.append(i).append("if (").append(inVar).append(" != null && !").append(inVar)
          .append(".isJsonNull()) {\n");
      builder.append(i).append("  ").append(outVar).append(" = new ")
          .append(getImplName(type, false)).append("();\n");
      builder.append(i).append("  java.util.Set<Map.Entry<String, JsonElement>> ")
          .append(entriesVar).append(" = ").append(inVar)
          .append(".getAsJsonObject().entrySet();\n");
      builder.append(i).append("  for (Map.Entry<String, JsonElement> ").append(entryVar)
          .append(" : ").append(entriesVar).append(") {\n");
      builder.append(i).append("    JsonElement ").append(childInVar).append(" = ")
          .append(entryVar).append(".getValue();\n");

      emitDeserializerImpl(expandedTypes, depth + 1, builder, childInVar, childOutVar, i + "    ");

      builder.append(i).append("    ").append(outVar).append(".put(").append(entryVar)
          .append(".getKey(), ").append(childOutVar).append(");\n");
      builder.append(i).append("  }\n");
      builder.append(i).append("}\n");
    } else if (getEnclosingTemplate().isDtoInterface(rawClass)) {
      String implClassName = getImplName(rawClass, false);
      builder.append(i).append(implClassName).append(" ").append(outVar).append(" = ")
          .append(implClassName).append(".fromJsonElement(").append(inVar).append(");\n");
    } else if (rawClass.isPrimitive()) {
      String primitiveName = rawClass.getSimpleName();
      String primitiveNameCap =
          primitiveName.substring(0, 1).toUpperCase() + primitiveName.substring(1);
      builder.append(i).append(primitiveName).append(" ").append(outVar).append(" = ")
          .append(inVar).append(".getAs").append(primitiveNameCap).append("();\n");
    } else {
      // Use gson to handle all other types.
      String rawClassName = rawClass.getName().replace('$', '.');
      builder.append(i).append(rawClassName).append(" ").append(outVar).append(" = gson.fromJson(")
          .append(inVar).append(", ").append(rawClassName).append(".class);\n");
    }
  }

  private void emitMockPreamble(Class<?> dtoInterface, StringBuilder builder) {
    builder.append("\n  public static class ");
    builder.append("Mock" + getImplClassName());
    builder.append(" extends ");
    builder.append(getImplClassName());
    builder.append(" {\n");
    builder.append("    protected Mock");
    builder.append(getImplClassName());
    builder.append("() {}\n\n");

    emitFactoryMethod(builder);
  }

  private void emitPreamble(Class<?> dtoInterface, StringBuilder builder) {
    builder.append("\n  public static class ");
    builder.append(getImplClassName());

    Class<?> superType = getSuperInterface();
    if (superType != null) {
      // We need to extend something.
      builder.append(" extends ");
      if (superType.equals(ServerToClientDto.class) || superType.equals(ClientToServerDto.class)) {
        // We special case RoutableDto's impl since it isnt generated.
        builder.append(ROUTABLE_DTO_IMPL);
      } else {
        builder.append(superType.getSimpleName() + "Impl");
      }
    }
    builder.append(" implements ");
    builder.append(dtoInterface.getCanonicalName());
    builder.append(", JsonSerializable");
    builder.append(" {\n\n");

    // If this guy is Routable, we make two constructors. One is a private
    // default constructor that hard codes the routing type, the other is a
    // protected constructor for any subclasses of this impl to pass up its
    // routing type.
    if (getRoutingType() != RoutableDto.NON_ROUTABLE_TYPE) {
      emitDefaultRoutingTypeConstructor(builder);
      emitProtectedConstructor(builder);
    }

    // If this DTO is allowed to be constructed on the server, we expose a
    // static factory method. A DTO is allowed to be constructed if it is a
    // ServerToClientDto, or if it is not a top level type (non-routable).
    if (DtoTemplate.implementsServerToClientDto(dtoInterface)
        || getRoutingType() == RoutableDto.NON_ROUTABLE_TYPE) {
      emitFactoryMethod(builder);
    }
  }

  private void emitProtectedConstructor(StringBuilder builder) {
    builder.append("    protected ");
    builder.append(getImplClassName());
    builder.append("(int type) {\n      super(type);\n");
    builder.append("    }\n\n");
  }

  private void emitReturn(Method method, String fieldName, StringBuilder builder) {
    if (isJsonArray(method.getReturnType())) {
      // Wrap the returned List in the server adapter.
      builder.append("(");
      builder.append(JSON_ARRAY);
      builder.append(") new ");
      builder.append(JSON_ARRAY_ADAPTER);
      builder.append("(");
      builder.append(fieldName);
      builder.append(")");
    } else if (isJsonStringMap(method.getReturnType())) {
      // Wrap the JsonArray.
      builder.append("(");
      builder.append(JSON_MAP);
      builder.append(") new ");
      builder.append(JSON_MAP_ADAPTER);
      builder.append("(");
      builder.append(fieldName);
      builder.append(")");
    } else {
      builder.append(fieldName);
    }
  }

  private void emitSetter(Method method, String implName, String fieldName, StringBuilder builder) {
    builder.append("\n    public ");
    builder.append(implName);
    builder.append(" ");
    builder.append(getSetterName(fieldName));
    builder.append("(");
    appendType(method.getGenericReturnType(), builder);
    builder.append(" v) {\n");
    builder.append("      ");
    builder.append(getHasFieldName(fieldName));
    builder.append(" = true;\n");
    builder.append("      ");
    builder.append(fieldName);
    builder.append(" = ");
    builder.append("v;\n      return this;\n    }\n");
  }

  /**
   * Emits an add method to add to a list. If the list is null, it is created.
   *
   * @param method a method with a list return type
   */
  private void emitListAdd(Method method, String fieldName, StringBuilder builder) {
    builder.append("\n    public void ");
    builder.append(getListAdderName(fieldName));
    builder.append("(");
    builder.append(getTypeArgumentImplName((ParameterizedType) method.getGenericReturnType()));
    builder.append(" v) {\n      ");
    builder.append(getEnsureName(fieldName));
    builder.append("();\n      ");
    builder.append(fieldName);
    builder.append(".add(v);\n");
    builder.append("    }\n");
  }

  /**
   * Emits a put method to put a value into a map. If the map is null, it is created.
   *
   * @param method a method with a map return value
   */
  private void emitMapPut(Method method, String fieldName, StringBuilder builder) {
    builder.append("\n    public void ");
    builder.append(getMapPutterName(fieldName));
    builder.append("(String k, ");
    builder.append(getTypeArgumentImplName((ParameterizedType) method.getGenericReturnType()));
    builder.append(" v) {\n      ");
    builder.append(getEnsureName(fieldName));
    builder.append("();\n      ");
    builder.append(fieldName);
    builder.append(".put(k, v);\n");
    builder.append("    }\n");
  }

  /**
   * Emits a method to clear a list or map. Clearing the collections ensures
   * that the collection is created.
   */
  private void emitClear(String fieldName, StringBuilder builder) {
    builder.append("\n    public void ");
    builder.append(getClearName(fieldName));
    builder.append("() {\n      ");
    builder.append(getEnsureName(fieldName));
    builder.append("();\n      ");
    builder.append(fieldName);
    builder.append(".clear();\n");
    builder.append("    }\n");
  }

  /**
   * Emit a method that ensures a collection is initialized.
   */
  private void emitEnsureCollection(Method method, String fieldName, StringBuilder builder) {
    builder.append("\n    private void ");
    builder.append(getEnsureName(fieldName));
    builder.append("() {\n");
    builder.append("      if (!");
    builder.append(getHasFieldName(fieldName));
    builder.append(") {\n        ");
    builder.append(getSetterName(fieldName));
    builder.append("(");
    builder.append(fieldName);
    builder.append(" != null ? ");
    builder.append(fieldName);
    builder.append(" : new ");
    builder.append(getImplName(method.getGenericReturnType(), false));
    builder.append("());\n");
    builder.append("      }\n");
    builder.append("    }\n");
  }

  /**
   * Appends a suitable type for the given type. For example, at minimum, this
   * will replace DTO interfaces with their implementation classes and JSON
   * collections with corresponding Java types. If a suitable type cannot be
   * determined, this will throw an exception.
   *
   * @param genericType the type as returned by e.g.
   *        method.getGenericReturnType()
   */
  private void appendType(Type genericType, final StringBuilder builder) {
    builder.append(getImplName(genericType, true));
  }

  /**
   * In most cases we simply echo the return type and field name, except for
   * JsonArray<T>, which is special in the server impl case, since it must be
   * represented by a List<T> for Gson to correctly serialize/deserialize it.
   *
   * @param method The getter method.
   * @return String representation of what the field type should be, as well as
   *         the assignment (initial value) to said field type, if any.
   */
  private String getFieldTypeAndAssignment(Method method, String fieldName) {
    StringBuilder builder = new StringBuilder();
    builder.append("protected ");
    appendType(method.getGenericReturnType(), builder);
    builder.append(" ");
    builder.append(fieldName);
    builder.append(";\n");
    return builder.toString();
  }

  /**
   * Returns the fully-qualified type name using Java concrete implementation
   * classes.
   *
   * For example, for JsonArray&lt;JsonStringMap&lt;Dto&gt;&gt;, this would
   * return "ArrayList&lt;Map&lt;String, DtoImpl&gt;&gt;".
   */
  private String getImplName(Type type, boolean allowJreCollectionInterface) {
    Class<?> rawClass = getRawClass(type);
    String fqName = getFqParameterizedName(type);
    fqName = fqName.replaceAll(JsonArray.class.getCanonicalName(),
        ArrayList.class.getCanonicalName());
    fqName = fqName.replaceAll(JsonStringMap.class.getCanonicalName() + "<",
        HashMap.class.getCanonicalName() + "<String, ");

    if (allowJreCollectionInterface) {
      if (isJsonArray(rawClass)) {
        fqName =
            fqName.replaceFirst(ArrayList.class.getCanonicalName(), List.class.getCanonicalName());
      } else if (isJsonStringMap(rawClass)) {
        fqName =
            fqName.replaceFirst(HashMap.class.getCanonicalName(), Map.class.getCanonicalName());
      }
    }

    return fqName;
  }

  /**
   * Returns the fully-qualified type name including parameters.
   */
  private String getFqParameterizedName(Type type) {
    if (type instanceof Class<?>) {
      return getImplNameForDto((Class<?>) type);

    } else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;

      StringBuilder sb = new StringBuilder(getRawClass(pType).getCanonicalName());
      sb.append('<');
      for (int i = 0; i < pType.getActualTypeArguments().length; i++) {
        sb.append(getFqParameterizedName(pType.getActualTypeArguments()[i]));
      }
      sb.append('>');

      return sb.toString();

    } else {
      throw new IllegalArgumentException("We do not handle this type");
    }
  }

  /**
   * Returns the fully-qualified type name using Java concrete implementation
   * classes of the first type argument for a parameterized type. If one is
   * not specified, returns "Object".
   *
   * @param type the parameterized type
   * @return the first type argument
   */
  private String getTypeArgumentImplName(ParameterizedType type) {
    Type[] typeArgs = type.getActualTypeArguments();
    if (typeArgs.length == 0) {
      return "Object";
    }
    return getImplName(typeArgs[0], false);
  }

  /**
   * Returns the name of the field that indicates the specified field was set.
   */
  private String getHasFieldName(String fieldName) {
    return "_has" + getCamelCaseName(fieldName);
  }

  private String getImplNameForDto(Class<?> dtoInterface) {
    if (getEnclosingTemplate().isDtoInterface(dtoInterface)) {
      // This will eventually get a generated impl type.
      return dtoInterface.getSimpleName() + "Impl";
    }

    return dtoInterface.getCanonicalName();
  }
}
