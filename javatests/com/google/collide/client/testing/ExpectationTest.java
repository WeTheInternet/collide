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

package com.google.collide.client.testing;

import com.google.collide.client.testing.CollideDtoClientTestingImpls.ComplexDtoImpl;
import com.google.collide.client.testing.CollideDtoClientTestingImpls.MockNestedDtoResponseImpl;
import com.google.collide.client.testing.CollideDtoClientTestingImpls.NestedDtoResponseImpl;
import com.google.collide.client.testing.CollideDtoClientTestingImpls.SimpleDtoImpl;
import com.google.collide.client.testing.dto.ComplexDto;
import com.google.collide.client.testing.dto.SimpleDto;
import com.google.collide.dto.client.DtoClientImpls.CreateProjectImpl;
import com.google.collide.dto.client.DtoClientImpls.CreateWorkspaceImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.gwt.junit.client.GWTTestCase;

/**
 *
 */
public class ExpectationTest extends GWTTestCase {

  private static final String NAME = "SomeName";

  private static final String OTHER = "Other";

  /**
   * Some syntactic sugar for asserting a happy {@link FrontendExpectation#checkMatch}.
   *
   * @param checkResult
   */
  private void assertCheckOkay(String checkResult) {
    assertEquals("", checkResult);
  }

  /**
   * Checks that a given {@link FrontendExpectation#checkMatch} result contains the
   * expected failure as a substring.
   */
  private void assertCheckFails(String substring, String checkResult) {
    assertTrue("expected \"" + substring + "\" does not occur in \"" + checkResult + "\"",
        checkResult.contains(substring));
  }

  @Override
  public String getModuleName() {
    return "com.google.collide.client.Collide";
  }

  public void testCheckMatchSimple() {
    // simple cases: same object, same type different fields, different types
    assertCheckOkay(FrontendExpectation.checkMatch(
        CreateProjectImpl.make().setName(NAME), CreateProjectImpl.make().setName(NAME)));
    assertCheckFails("field name does not match", FrontendExpectation.checkMatch(
        CreateProjectImpl.make().setName(NAME), CreateProjectImpl.make().setName(OTHER)));
    assertCheckFails("field _type does not match", FrontendExpectation.checkMatch(
        CreateProjectImpl.make().setName(NAME), CreateWorkspaceImpl.make().setName(NAME)));
  }

  public void testCheckMatchPartial() {
    // partial pattern matching: ignore the extra project id
    assertCheckOkay(FrontendExpectation.checkMatch(CreateWorkspaceImpl.make().setName(NAME),
        CreateWorkspaceImpl.make().setName(NAME).setProjectId(OTHER)));
    // but not the reverse:
    assertCheckFails("field projectId does not match",
        FrontendExpectation.checkMatch(CreateWorkspaceImpl.make().setName(NAME).setProjectId(OTHER),
            CreateWorkspaceImpl.make().setName(NAME)));
  }

  public void testCheckMatchCompound() {
    // validating deep recursion

    // Different parents, same children.
    JsoArray<ComplexDto> parents = JsoArray.create();
    ComplexDto parent = ComplexDtoImpl.make().setId(NAME);
    parents.add(parent);
    JsoStringMap<SimpleDto> childMap = JsoStringMap.create();
    childMap.put("child1", SimpleDtoImpl.make().setName("c1-name").setValue("c1-value"));
    childMap.put("child2", SimpleDtoImpl.make().setName("c2-name").setValue("c2-value"));
    parent = ComplexDtoImpl.make().setId(OTHER).setMap(childMap);
    parents.add(parent);
    NestedDtoResponseImpl two = MockNestedDtoResponseImpl.make().setArray(parents);

    // Different parents, different children.
    parents = JsoArray.create();
    childMap = JsoStringMap.create();
    parent = ComplexDtoImpl.make().setId(NAME).setMap(childMap);
    parents.add(parent);
    childMap = JsoStringMap.create();
    childMap.put("child3", SimpleDtoImpl.make().setName("c3-name").setValue("c3-value"));
    childMap.put("child4", SimpleDtoImpl.make().setName("c4-name").setValue("c4-value"));
    parent = ComplexDtoImpl.make().setId(OTHER).setMap(childMap);
    parents.add(parent);
    NestedDtoResponseImpl twoDifferent = MockNestedDtoResponseImpl.make().setArray(parents);

    // One parent, no children.
    parents = JsoArray.create();
    childMap = JsoStringMap.create();
    parent = ComplexDtoImpl.make().setId(OTHER).setMap(childMap);
    parents.add(parent);
    NestedDtoResponseImpl oneOfZero = MockNestedDtoResponseImpl.make().setArray(parents);

    // One parent, two children.
    parents = JsoArray.create();
    childMap = JsoStringMap.create();
    childMap.put("child1", SimpleDtoImpl.make().setName("c1-name").setValue("c1-value"));
    childMap.put("child2", SimpleDtoImpl.make().setName("c2-name").setValue("c2-value"));
    parent = ComplexDtoImpl.make().setId(NAME).setMap(childMap);
    parents.add(parent);
    NestedDtoResponseImpl oneOfSome = MockNestedDtoResponseImpl.make().setArray(parents);

    assertCheckOkay(FrontendExpectation.checkMatch(two, two));
    assertCheckFails("", FrontendExpectation.checkMatch(oneOfZero, oneOfSome));
    assertCheckFails("field child1 is not an object, but undefined, "
        + "field child2 is not an object, but undefined",
        FrontendExpectation.checkMatch(oneOfSome, oneOfZero));
    assertCheckFails("field child1 is not an object, but undefined, "
        + "field child2 is not an object, but undefined",
        FrontendExpectation.checkMatch(two, twoDifferent));
  }
}
