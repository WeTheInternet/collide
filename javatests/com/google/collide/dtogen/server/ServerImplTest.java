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

package com.google.collide.dtogen.server;

import com.google.collide.dtogen.definitions.ComplicatedDto;
import com.google.collide.dtogen.definitions.ComplicatedDto.SimpleEnum;
import com.google.collide.dtogen.definitions.NotRoutable;
import com.google.collide.dtogen.definitions.SimpleDto;
import com.google.collide.dtogen.server.TestDtoServerImpls.ComplicatedDtoImpl;
import com.google.collide.dtogen.server.TestDtoServerImpls.NotRoutableImpl;
import com.google.collide.dtogen.server.TestDtoServerImpls.SimpleDtoImpl;
import com.google.collide.dtogen.server.TestDtoServerImpls.SimpleDtoSubTypeImpl;
import com.google.collide.dtogen.shared.ServerToClientDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tests that the interfaces specified in com.google.gwt.dto.definitions have
 * corresponding generated Server impls.
 */
public class ServerImplTest extends TestCase {
  public void testComplicatedDtoImpl() {
    final String fooString = "Something";
    final int intId = 1;

    ComplicatedDtoImpl dtoImpl = ComplicatedDtoImpl.make();
    dtoImpl.setIntId(intId);
    dtoImpl.setSimpleEnum(SimpleEnum.TWO);
    dtoImpl.getFooStrings().add(fooString);
    dtoImpl.getNightmare();
    dtoImpl.getArrayOfArrayOfEnum();

    // Assume that SimpleDto works. Use it to test nested objects
    SimpleDtoImpl simpleDto = SimpleDtoImpl.make();
    simpleDto.setName(fooString);
    simpleDto.setIDontStartWithGet(intId);

    dtoImpl.getSimpleDtos().add(simpleDto);
    dtoImpl.getMap().put(fooString, simpleDto);

    // Check to make sure things are in a sane state.
    ComplicatedDto dto = dtoImpl;
    assertEquals(intId, dto.getIntId());
    assertEquals(fooString, dto.getFooStrings().get(0));
    // Should be reference equal initially.
    assertEquals(dto.getSimpleDtos().get(0), simpleDto);
    assertEquals(dto.getMap().get(fooString), simpleDto);
    assertEquals(ComplicatedDto.TYPE, dto.getType());

    // Make it json and pull it back out.
    Gson gson = new Gson();
    String serialized = dtoToJson(gson, dtoImpl);
    ComplicatedDtoImpl deserialized = ComplicatedDtoImpl.fromJsonString(serialized);

    // Test correctness of JSON serialization.
    assertEquals(serialized, dtoImpl.toJson());

    assertEquals(intId, deserialized.getIntId());
    assertEquals(fooString, deserialized.getFooStrings().get(0));
    assertEquals(ComplicatedDto.TYPE, deserialized.getType());

    // Pull it out using the DTO's deserializer.
    JsonElement jsonElement = new JsonParser().parse(serialized);
    ComplicatedDtoImpl deserialized2 = ComplicatedDtoImpl.fromJsonElement(jsonElement);
    assertEquals(intId, deserialized2.getIntId());
    assertEquals(fooString, deserialized2.getFooStrings().get(0));
    assertEquals(ComplicatedDto.TYPE, deserialized2.getType());

    // Verify that the hasFields() are correct when using the DTO's deserializer.
    assertTrue(deserialized2.hasIntId());

    // Check that the SimpleDto object looks correct.
    checkSimpleDto(dto.getSimpleDtos().get(0), simpleDto.getName(), simpleDto.iDontStartWithGet());
    checkSimpleDto(dto.getMap().get(fooString), simpleDto.getName(), simpleDto.iDontStartWithGet());
  }

  public void testNotRoutableImpl() {
    final String fooString = "Something";

    NotRoutableImpl dtoImpl = new NotRoutableImpl();
    dtoImpl.setSomeField(fooString);

    // Make it json and pull it back out.
    Gson gson = new Gson();
    String serialized = gson.toJson(dtoImpl);
    NotRoutable dto = NotRoutableImpl.fromJsonString(serialized);

    assertEquals(fooString, dto.getSomeField());
  }

  public void testNullEnum() {
    ComplicatedDtoImpl dtoImpl = ComplicatedDtoImpl.make();
    dtoImpl.setIntId(1);
    dtoImpl.setSimpleEnum(null);

    // Make it json and pull it back out.
    Gson gson = new GsonBuilder().serializeNulls().create();
    String serialized = dtoImpl.toJson();
    assertEquals(dtoToJson(gson, dtoImpl), serialized);

    ComplicatedDto dto = ComplicatedDtoImpl.fromJsonString(serialized);
    assertEquals(1, dto.getIntId());
    assertNull(dto.getSimpleEnum());
  }

  public void testSimpleDtoImpl() {
    final String fooString = "Something";
    final int intValue = 1;

    SimpleDtoImpl dtoImpl = SimpleDtoImpl.make();
    dtoImpl.setName("Something");
    dtoImpl.setIDontStartWithGet(intValue);

    checkSimpleDto(dtoImpl, fooString, intValue);

    // Make it json and pull it back out.
    Gson gson = new Gson();
    String serialized = gson.toJson(dtoImpl);
    SimpleDto dto = SimpleDtoImpl.fromJsonString(serialized);

    checkSimpleDto(dto, fooString, intValue);
  }

  public void testSimpleDtoImpl_deserialize() {
    final String fooString = "Something";

    JsonObject json = new JsonObject();
    json.add("name", new JsonPrimitive(fooString));

    SimpleDtoImpl deserialized = SimpleDtoImpl.fromJsonElement(json);
    assertTrue(deserialized.hasName());
    assertFalse(deserialized.hasIDontStartWithGet());
    checkSimpleDto(deserialized, fooString, 0);
  }

  public void testSimpleDtoImpl_nullStringSerialization() {
    final String fooString = null;
    final int intValue = 1;

    SimpleDtoImpl dtoImpl = SimpleDtoImpl.make();
    dtoImpl.setName(fooString);
    dtoImpl.setIDontStartWithGet(intValue);

    checkSimpleDto(dtoImpl, fooString, intValue);

    // Make it json and pull it back out.
    Gson gson = new GsonBuilder().serializeNulls().create();
    String serialized = dtoImpl.toJson();
    assertEquals(dtoToJson(gson, dtoImpl), serialized);
    SimpleDto dto = SimpleDtoImpl.fromJsonString(serialized);

    checkSimpleDto(dto, fooString, intValue);
  }

  private void checkSimpleDto(SimpleDto dto, String expectedName, int expectedNum) {
    assertEquals(expectedName, dto.getName());
    assertEquals(expectedNum, dto.iDontStartWithGet());
    assertEquals(SimpleDto.TYPE, dto.getType());
  }

  public void testSimpleDtoSubtypeImpl() {
    final String valueForFieldOnSupertype = "valueForFieldOnSupertype";
    final String valueForFieldOnSelf = "valueForFieldOnSelf";
    SimpleDtoSubTypeImpl dto = SimpleDtoSubTypeImpl.make();
    dto.setName(valueForFieldOnSupertype);
    dto.setAnotherField(valueForFieldOnSelf);
    String json = dto.toJson();

    assertTrue(json.contains(valueForFieldOnSelf));
    assertTrue(json.contains(valueForFieldOnSupertype));
  }

  public void testEqualsAndHashCode() {
    final String fooString = "something";
    final String barString = "something else";
    final String bazString = "something else, again";
    final String fluxString = "yet something ELSE";
    final int fooNum = 4;
    final int barNum = 5;

    SimpleDtoSubTypeImpl dtoA = SimpleDtoSubTypeImpl.make();
    SimpleDtoSubTypeImpl dtoB = SimpleDtoSubTypeImpl.make();
    checkEqualsAndHashCode(dtoA, dtoB, true);

    // test on an object field
    dtoA.setName(fooString);
    assert(dtoA.hasName());
    assertFalse(dtoB.hasName());
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoB.setName(fooString);
    checkEqualsAndHashCode(dtoA, dtoB, true);

    dtoA.setName(barString);
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoA.setName(fooString);
    checkEqualsAndHashCode(dtoA, dtoB, true);

    // test on a primitive field
    dtoA.setNumber(fooNum);
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoB.setNumber(fooNum);
    checkEqualsAndHashCode(dtoA, dtoB, true);

    dtoA.setNumber(barNum);
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoA.setNumber(fooNum);
    checkEqualsAndHashCode(dtoA, dtoB, true);

    // test on a subclass' field
    dtoA.setAnotherField(bazString);
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoB.setAnotherField(bazString);
    checkEqualsAndHashCode(dtoA, dtoB, true);

    dtoA.setAnotherField(fluxString);
    checkEqualsAndHashCode(dtoA, dtoB, false);

    dtoA.setAnotherField(bazString);
    checkEqualsAndHashCode(dtoA, dtoB, true);

  }

  private void checkEqualsAndHashCode(Object a, Object b, boolean shouldBeEqual) {
    assertTrue(a.equals(a));
    assertTrue(b.equals(b));
    assertTrue(a.equals(b) == shouldBeEqual);
    assertTrue(b.equals(a) == shouldBeEqual);
    if (shouldBeEqual) {
      assertEquals(a, b);
      assertEquals(b, a);
    }

    // if a and b are not equal, their hashcodes could still collide if we are unlucky, but since
    // this test uses static data for all objects, we can ensure this doesn't happen
    assertTrue((a.hashCode() == b.hashCode()) == shouldBeEqual);
  }

  /**
   * Converts the object to JSON, removing all _has fields.
   *
   * @param gson the Gson used to parse the object
   */
  private String dtoToJson(Gson gson, ServerToClientDto dtoImpl) {
    com.google.gson.JsonObject jsonObj = gson.toJsonTree(dtoImpl).getAsJsonObject();
    stripHasFields(jsonObj);
    return gson.toJson(jsonObj);
  }

  /**
   * Recursively strip fields that start with _has from the specified object. Modifies the object.
   */
  private void stripHasFields(JsonElement jsonElem) {
    if (jsonElem.isJsonObject()) {
      com.google.gson.JsonObject jsonObj = jsonElem.getAsJsonObject();

      // Determine which fields should be removed.
      Set<String> hasFields = new HashSet<String>();
      for (Map.Entry<String, JsonElement> field : jsonObj.entrySet()) {
        if (field.getKey().startsWith("_has")) {
          // Remove the _hasAbc field.
          hasFields.add(field.getKey());
        } else {
          // Recursively strip fields on the child.
          stripHasFields(field.getValue());
        }
      }

      // Remove the fields.
      for (String hasField : hasFields) {
        jsonObj.remove(hasField);
      }
    } else if (jsonElem.isJsonArray()) {
      JsonArray array = jsonElem.getAsJsonArray();

      // Recursively call on child objects in an array.
      for (int i = 0; i < array.size(); i++) {
        stripHasFields(array.get(i));
      }
    }
  }
}
