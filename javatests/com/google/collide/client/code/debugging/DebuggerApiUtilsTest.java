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

package com.google.collide.client.code.debugging;

import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectId;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.json.client.Jso;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link DebuggerApiUtils}.
 *
 */
public class DebuggerApiUtilsTest extends GWTTestCase {

  private static class RemoteObjectImpl implements RemoteObject {
    private final String description;
    private final RemoteObjectType type;
    private final RemoteObjectSubType subType;

    public RemoteObjectImpl(String description, RemoteObjectType type) {
      this(description, type, null);
    }

    public RemoteObjectImpl(String description, RemoteObjectType type,
        RemoteObjectSubType subType) {
      this.description = description;
      this.type = type;
      this.subType = subType;
    }

    @Override
    public String getDescription() {
      return description;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public RemoteObjectId getObjectId() {
      return null;
    }

    @Override
    public RemoteObjectType getType() {
      return type;
    }

    @Override
    public RemoteObjectSubType getSubType() {
      return subType;
    }
  }

  private static final RemoteObject NAN_REMOTE_OBJECT =
      new RemoteObjectImpl("NaN", RemoteObjectType.NUMBER);

  private static final RemoteObject POSITIVE_INFINITY_REMOTE_OBJECT =
      new RemoteObjectImpl("Infinity", RemoteObjectType.NUMBER);

  private static final RemoteObject NEGATIVE_INFINITY_REMOTE_OBJECT =
      new RemoteObjectImpl("-Infinity", RemoteObjectType.NUMBER);

  private static final RemoteObject NULL_REMOTE_OBJECT =
      new RemoteObjectImpl("null", RemoteObjectType.OBJECT, RemoteObjectSubType.NULL);

  private static final RemoteObject BOOLEAN_TRUE_REMOTE_OBJECT =
      new RemoteObjectImpl("true", RemoteObjectType.BOOLEAN);

  private static final RemoteObject BOOLEAN_FALSE_REMOTE_OBJECT =
      new RemoteObjectImpl("false", RemoteObjectType.BOOLEAN);

  private static final RemoteObject UNDEFINED_REMOTE_OBJECT =
      DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT;

  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }

  public void testAddPrimitiveJsoFieldForNumber() {
    final RemoteObject remoteNumber = new RemoteObjectImpl("123", RemoteObjectType.NUMBER);

    Jso jso = Jso.create();
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", remoteNumber));

    String serializedJso = Jso.serialize(jso);
    assertEquals("{\"key\":123}", serializedJso);
  }

  public void testAddPrimitiveJsoFieldForNonFiniteNumbers() {
    Jso jso = Jso.create();
    assertFalse(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", NAN_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", POSITIVE_INFINITY_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", NEGATIVE_INFINITY_REMOTE_OBJECT));
  }

  public void testAddPrimitiveJsoFieldForNull() {
    Jso jso = Jso.create();
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", NULL_REMOTE_OBJECT));

    String serializedJso = Jso.serialize(jso);
    assertEquals("{\"key\":null}", serializedJso);
  }

  public void testAddPrimitiveJsoFieldForBoolean() {
    Jso jso = Jso.create();
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key1", BOOLEAN_TRUE_REMOTE_OBJECT));
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key2", BOOLEAN_FALSE_REMOTE_OBJECT));

    String serializedJso = Jso.serialize(jso);
    assertEquals("{\"key1\":true,\"key2\":false}", serializedJso);
  }

  public void testAddPrimitiveJsoFieldForString() {
    final RemoteObject empty = new RemoteObjectImpl("", RemoteObjectType.STRING);
    final RemoteObject nonEmpty = new RemoteObjectImpl("abc", RemoteObjectType.STRING);

    Jso jso = Jso.create();
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key1", empty));
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key2", nonEmpty));

    String serializedJso = Jso.serialize(jso);
    assertEquals("{\"key1\":\"\",\"key2\":\"abc\"}", serializedJso);
  }

  public void testAddPrimitiveJsoFieldForUndefined() {
    Jso jso = Jso.create();
    assertTrue(DebuggerApiUtils.addPrimitiveJsoField(jso, "key", UNDEFINED_REMOTE_OBJECT));

    String serializedJso = Jso.serialize(jso);
    assertTrue(jso.hasOwnProperty("key"));
    assertEquals("{}", serializedJso);
  }

  public void testCastToBooleanForNumber() {
    final RemoteObject zero = new RemoteObjectImpl("0", RemoteObjectType.NUMBER);
    final RemoteObject nonZero = new RemoteObjectImpl("123", RemoteObjectType.NUMBER);
    assertFalse("Zero should cast to false", DebuggerApiUtils.castToBoolean(zero));
    assertTrue("Non-zero should cast to true", DebuggerApiUtils.castToBoolean(nonZero));
  }

  public void testCastToBooleanForNonFiniteNumbers() {
    assertFalse("NaN should cast to false", DebuggerApiUtils.castToBoolean(NAN_REMOTE_OBJECT));
    assertTrue("Infinity should cast to true",
        DebuggerApiUtils.castToBoolean(POSITIVE_INFINITY_REMOTE_OBJECT));
    assertTrue("-Infinity should cast to true",
        DebuggerApiUtils.castToBoolean(NEGATIVE_INFINITY_REMOTE_OBJECT));
  }

  public void testCastToBooleanForNull() {
    assertFalse("Null should cast to false", DebuggerApiUtils.castToBoolean(NULL_REMOTE_OBJECT));
  }

  public void testCastToBooleanForBoolean() {
    assertTrue(DebuggerApiUtils.castToBoolean(BOOLEAN_TRUE_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.castToBoolean(BOOLEAN_FALSE_REMOTE_OBJECT));
  }

  public void testCastToBooleanForString() {
    final RemoteObject empty = new RemoteObjectImpl("", RemoteObjectType.STRING);
    final RemoteObject nonEmpty = new RemoteObjectImpl("abc", RemoteObjectType.STRING);
    assertFalse("Empty string should cast to false", DebuggerApiUtils.castToBoolean(empty));
    assertTrue("Non-empty string should cast to true", DebuggerApiUtils.castToBoolean(nonEmpty));
  }

  public void testCastToBooleanForUndefined() {
    assertFalse("Undefined should cast to false",
        DebuggerApiUtils.castToBoolean(UNDEFINED_REMOTE_OBJECT));
  }

  public void testIsNonFiniteNumber() {
    assertTrue(DebuggerApiUtils.isNonFiniteNumber(NAN_REMOTE_OBJECT));
    assertTrue(DebuggerApiUtils.isNonFiniteNumber(POSITIVE_INFINITY_REMOTE_OBJECT));
    assertTrue(DebuggerApiUtils.isNonFiniteNumber(NEGATIVE_INFINITY_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.isNonFiniteNumber(NULL_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.isNonFiniteNumber(UNDEFINED_REMOTE_OBJECT));
    assertFalse(DebuggerApiUtils.isNonFiniteNumber(BOOLEAN_TRUE_REMOTE_OBJECT));

    final RemoteObject zero = new RemoteObjectImpl("0", RemoteObjectType.NUMBER);
    final RemoteObject nonZero = new RemoteObjectImpl("123", RemoteObjectType.NUMBER);
    assertFalse(DebuggerApiUtils.isNonFiniteNumber(zero));
    assertFalse(DebuggerApiUtils.isNonFiniteNumber(nonZero));
  }
}
