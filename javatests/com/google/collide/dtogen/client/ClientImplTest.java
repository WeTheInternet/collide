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

package com.google.collide.dtogen.client;

import com.google.collide.dtogen.client.TestDtoClientImpls.ComplicatedDtoImpl;
import com.google.collide.dtogen.client.TestDtoClientImpls.NotRoutableImpl;
import com.google.collide.dtogen.client.TestDtoClientImpls.SimpleDtoImpl;
import com.google.collide.dtogen.definitions.ComplicatedDto;
import com.google.collide.dtogen.definitions.ComplicatedDto.SimpleEnum;
import com.google.collide.dtogen.definitions.NotRoutable;
import com.google.collide.dtogen.definitions.SimpleDto;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonArray;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that the interfaces specified in com.google.gwt.dto.definitions have
 * corresponding generated Client impls.
 *
 */
public class ClientImplTest extends GWTTestCase {
  private static native NotRoutableImpl getNotRoutable() /*-{
    return {};
  }-*/;

  @Override
  public String getModuleName() {
    return "com.google.collide.dtogen.ClientImplTestModule";
  }

  public void testComplicatedDtoImpl() {
    final String fooString = "Something";
    final int intId = 1;

    SimpleDto simpleDto = SimpleDtoImpl.make().setName(fooString).setIDontStartWithGet(intId);

    @SuppressWarnings("unchecked")
    JsoArray<JsoArray<SimpleEnum>> arrArrEnum =
        JsoArray.from(JsoArray.from(SimpleEnum.ONE, SimpleEnum.TWO, SimpleEnum.THREE),
            JsoArray.from(SimpleEnum.TWO, SimpleEnum.THREE), JsoArray.from(SimpleEnum.THREE));

    ComplicatedDto dto =
        ComplicatedDtoImpl.make().setIntId(intId).setMap(JsoStringMap.<SimpleDto>create())
            .setSimpleDtos(JsoArray.<SimpleDto>create()).setFooStrings(JsoArray.<String>create())
            .setArrayOfArrayOfEnum(arrArrEnum);

    dto.getFooStrings().add(fooString);
    dto.getSimpleDtos().add(simpleDto);

    assertEquals(intId, dto.getIntId());
    assertEquals(fooString, dto.getFooStrings().get(0));
    assertEquals(simpleDto, dto.getSimpleDtos().get(0));
    assertTrue(areArraysEqual(arrArrEnum, dto.getArrayOfArrayOfEnum()));
    assertEquals(ComplicatedDto.TYPE, dto.getType());
  }

  private static native final boolean areArraysEqual(JsonArray<?> arr1, JsonArray<?> arr2) /*-{
    // Lists can't be tested for equality with "==", but "<" and ">" work.
    // This creates "equality" by making sure that arr1 >= arr2 && arr1 <= arr2
    return !(arr1 < arr2) && !(arr1 > arr2);
  }-*/;

  public void testNotRoutableImpl() {
    final String fooString = "Something";
    NotRoutable dto = getNotRoutable().setSomeField(fooString);
    assertEquals(fooString, dto.getSomeField());
  }

  public void testSimpleDtoImpl() {
    final String nameString = "Something";
    final int intValue = 1;

    SimpleDto dto = SimpleDtoImpl.make().setName(nameString).setIDontStartWithGet(intValue);
    assertEquals(nameString, dto.getName());
    assertEquals(intValue, dto.iDontStartWithGet());
    assertEquals(SimpleDto.TYPE, dto.getType());
  }
}
