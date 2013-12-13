package collide.junit;

import static com.google.gwt.reflect.client.GwtReflect.magicClass;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

import collide.junit.cases.ReflectionCaseKeepsEverything;
import collide.junit.cases.ReflectionCaseKeepsNothing;
import collide.junit.cases.ReflectionCaseNoMagic;
import collide.junit.cases.ReflectionCaseNoMagic.Subclass;
import collide.junit.cases.ReflectionCaseSimple;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@ReflectionStrategy(magicSupertypes=false, keepCodeSource=true)
public class FieldTests extends AbstractReflectionTest {
  
  static final Class<ReflectionCaseSimple> c = magicClass(ReflectionCaseSimple.class);
  static final Class<Primitives> PRIMITIVE_CLASS = magicClass(Primitives.class);
  static final Class<Objects> OBJECTS_CLASS = magicClass(Objects.class);
  static final Class<ReflectionCaseKeepsNothing> KEEPS_NONE = magicClass(ReflectionCaseKeepsNothing.class);
  static final Class<ReflectionCaseKeepsEverything> KEEPS_EVERYTHING = magicClass(ReflectionCaseKeepsEverything.class);

  Primitives primitives;
  Objects objects;

  @Before 
  public void initObjects() throws InstantiationException, IllegalAccessException {
    primitives = PRIMITIVE_CLASS.newInstance();
    objects = OBJECTS_CLASS.newInstance();
  }
  
  public static class Primitives {
    public Primitives() {}
    public boolean z;
    public byte b;
    public char c;
    public short s;
    public int i;
    public long j;
    public float f;
    public double d;
  }
  
  public static class Objects {
    Objects() {}
    public Object L;
    public Primitives P;
    
    public final Object FINAL = null;
    
    public Boolean Z;
    public Byte B;
    public Character C;
    public Short S;
    public Integer I;
    public Long J;
    public Float F;
    public Double D;
  }
  
  public FieldTests() {}

  @Test(expected=NullPointerException.class)
  public void testObjectNullAccess() throws Exception {
    Field f = OBJECTS_CLASS.getField("L");
    f.get(null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("P");
    f.set(objects, new Object());
  }
  
  @Test
  public void testObjectLegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("L");
    f.set(objects, primitives);
  }
  

/////////////////////////////////////////////////
/////////////////Booleans////////////////////////
/////////////////////////////////////////////////
  
  @Test
  public void testBooleanPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    f.set(primitives, true);
    assertTrue(f.getBoolean(primitives));
    assertTrue((Boolean)f.get(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBooleanPrimitiveIllegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getDeclaredField("z");
    assertEquals(1, f.getInt(primitives));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBooleanPrimitiveIllegalSet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    
    f.set(primitives, 1);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBooleanPrimitiveNullSet() throws Exception {    
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    
    f.set(primitives, (Boolean)null);
  }

  @Test
  public void testBooleanObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("Z");
    assertNull(f.get(objects));
    objects.Z = true;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBooleanObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("Z");
    assertNull(f.get(objects));
    assertFalse(f.getBoolean(objects));
  }

/////////////////////////////////////////////////
///////////////////Bytes/////////////////////////
/////////////////////////////////////////////////

  @Test
  public void testBytePrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    assertEquals(0, f.getByte(primitives));
    assertEquals(0, ((Byte)f.get(primitives)).byteValue());
    f.set(primitives, (byte)1);
    assertEquals(1, f.getByte(primitives));
    assertEquals(1, ((Byte)f.get(primitives)).byteValue());
  }
  
  @Test
  public void testBytePrimitiveWideningLegal() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    
    assertEquals(0, f.getByte(primitives));
    assertEquals(0, f.getShort(primitives));
    assertEquals(0, f.getInt(primitives));
    assertEquals(0, f.getLong(primitives));
    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0., f.getDouble(primitives));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBytePrimitiveWidening_IllegalBoolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    f.set(primitives, true);
    assertEquals(1, f.getByte(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBytePrimitiveWidening_IllegalChar() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    f.set(primitives, 'a');
    assertEquals('a', f.getByte(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBytePrimitiveNullSet() throws Exception {    
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    
    assertEquals(0, f.getByte(primitives));
    assertEquals(0, ((Byte)f.get(primitives)).byteValue());
    
    f.set(primitives, (Byte)null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBytePrimitiveIllegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getDeclaredField("b");
    assertTrue(f.getBoolean(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBytePrimitiveIllegalSet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("b");
    assertEquals(0, f.getByte(primitives));
    assertEquals(0, ((Byte)f.get(primitives)).byteValue());
    
    f.set(primitives, false);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testByteObjectIllegalGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("B");
    assertEquals((byte)0, f.getByte(objects));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testByteObjectIllegalSet_Int() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("B");
    f.set(objects, (int)1);
  }
  
  @Test
  public void testByteObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("B");
    assertNull(f.get(objects));
    objects.B = 1;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testByteObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("B");
    assertNull(f.get(objects));
    assertEquals(0, f.getByte(objects));
  }

/////////////////////////////////////////////////
///////////////////Chars/////////////////////////
/////////////////////////////////////////////////

  @Test
  public void testCharPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("c");
    assertEquals(0, f.getChar(primitives));
    assertEquals(0, ((Character)f.get(primitives)).charValue());
    f.set(primitives, (char)1);
    assertEquals(1, f.getChar(primitives));
    assertEquals(1, ((Character)f.get(primitives)).charValue());
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharPrimitiveWidening_IllegalBoolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("c");
    f.set(primitives, true);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharPrimitiveWidening_IllegalNumber() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("c");
    f.set(primitives, 1);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharPrimitiveNullSet() throws Exception {    
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("c");
    
    assertEquals(0, f.getChar(primitives));
    assertEquals(0, ((Character)f.get(primitives)).charValue());
    
    f.set(primitives, (Character)null);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCharPrimitiveIllegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getDeclaredField("c");
    assertTrue(f.getBoolean(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharPrimitiveIllegalSet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("c");
    assertEquals(0, f.getChar(primitives));
    assertEquals(0, ((Character)f.get(primitives)).charValue());
    
    f.set(primitives, false);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharacterObjectIllegalGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("C");
    assertEquals((char)0, f.getChar(objects));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testCharacterObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("C");
    f.set(objects, (int)1);
  }
  
  @Test
  public void testCharacterObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("C");
    assertNull(f.get(objects));
    objects.C = 'a';
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testCharacterObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("C");
    assertNull(f.get(objects));
    assertEquals(0, f.getChar(objects));
  }

/////////////////////////////////////////////////
/////////////////Shorts//////////////////////////
/////////////////////////////////////////////////
  
  @Test
  public void testShortPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("s");
    assertEquals(0, f.getShort(primitives));
    assertEquals(0, ((Short)f.get(primitives)).shortValue());
    f.set(primitives, (short)1);
    assertEquals(1, f.getShort(primitives));
    assertEquals(1, ((Short)f.get(primitives)).shortValue());
    
    f.set(primitives, new Byte((byte)1));
  }
  
  @Test
  public void testShortPrimitiveLegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("s");
    
    assertEquals(0, f.getShort(primitives));
    assertEquals(0, f.getInt(primitives));
    assertEquals(0, f.getLong(primitives));
    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0., f.getDouble(primitives));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testShortPrimitiveIllegalSet_Boolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("s");
    f.set(primitives, true);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testShortPrimitiveIllegalSet_Char() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("s");
    f.set(primitives, 'a');
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testShortPrimitiveIllegalSet_Null() throws Exception {    
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("s");
    f.set(primitives, (Short)null);
  }
  
  @Test
  public void testShortObjectLegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("S");
    f.set(objects, (short)1);
    assertEquals((short)1, f.get(objects));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testShortObjectIllegalSet_Byte() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("S");
    f.set(objects, (byte)1);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testShortObjectIllegalSet_Int() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("S");
    f.set(objects, (int)1);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testShortObjectIllegalGet_Byte() throws Exception {
    assertNotNull(primitives);
    Field f = OBJECTS_CLASS.getField("S");
    f.getByte(primitives);
  }

  @Test
  public void testShortObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("S");
    assertNull(f.get(objects));
    objects.S = 1;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testShortObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("S");
    assertNull(f.get(objects));
    assertEquals(0, f.getShort(objects));
  }

/////////////////////////////////////////////////
////////////////////Ints/////////////////////////
/////////////////////////////////////////////////

  @Test
  public void testIntPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");
    assertEquals(0, f.getInt(primitives));
    assertEquals(0, ((Integer) f.get(primitives)).intValue());
    f.set(primitives, (int) 1);
    assertEquals(1, f.getInt(primitives));
    assertEquals(1, ((Integer) f.get(primitives)).intValue());
    
    f.set(primitives, 'a');
    f.set(primitives, (byte) 1);
  }

  @Test
  public void testIntPrimitiveLegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");

    assertEquals(0, f.getInt(primitives));
    assertEquals(0, f.getLong(primitives));
    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0., f.getDouble(primitives));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIntPrimitiveIllegalGet_Byte() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");
    f.getByte(primitives);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testIntPrimitiveIllegalGet_Short() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");
    f.getShort(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIntPrimitiveIllegalSet_Boolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");
    f.set(primitives, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIntPrimitiveIllegalSet_Null() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("i");
    f.set(primitives, (Short) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIntegerObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("I");
    f.set(objects, (short)1);
  }

  @Test
  public void testIntegerObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("I");
    assertNull(f.get(objects));
    objects.I = 1;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIntegerObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("I");
    assertNull(f.get(objects));
    assertEquals(0, f.getInt(objects));
  }
  
/////////////////////////////////////////////////
///////////////////Longs/////////////////////////
/////////////////////////////////////////////////

  @Test
  public void testLongPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    assertEquals(0, f.getLong(primitives));
    assertEquals(0, ((Long) f.get(primitives)).longValue());
    f.set(primitives, (long) 1);
    assertEquals(1, f.getLong(primitives));
    assertEquals(1, ((Long) f.get(primitives)).longValue());

    f.set(primitives, 'a');
    f.set(primitives, (byte) 1);
  }

  @Test
  public void testLongPrimitiveLegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");

    assertEquals(0, f.getLong(primitives));
    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0., f.getDouble(primitives));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongPrimitiveIllegalGet_Byte() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    f.getByte(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongPrimitiveIllegalGet_Short() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    f.getShort(primitives);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testLongPrimitiveIllegalGet_Int() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    f.getInt(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongPrimitiveIllegalSet_Boolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    f.set(primitives, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongPrimitiveIllegalSet_Null() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("j");
    f.set(primitives, (Short) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("J");
    f.set(objects, (int) 1);
  }

  @Test
  public void testLongObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("J");
    assertNull(f.get(objects));
    objects.J = 1L;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLongObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("L");
    assertNull(f.get(objects));
    assertEquals(0, f.getLong(objects));
  }

/////////////////////////////////////////////////
////////////////////Floats///////////////////////
/////////////////////////////////////////////////

  @Test
  public void testFloatPrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0f, ((Float) f.get(primitives)).floatValue());
    f.set(primitives, (float) 1);
    assertEquals(1f, f.getFloat(primitives));
    assertEquals(1f, ((Float) f.get(primitives)).floatValue());

    f.set(primitives, 'a');
    f.set(primitives, (byte) 1);
    f.set(primitives, (int) 1);
    f.set(primitives, (float) 1);
  }

  @Test
  public void testFloatPrimitiveLegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");

    assertEquals(0f, f.getFloat(primitives));
    assertEquals(0., f.getDouble(primitives));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalGet_Byte() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.getByte(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalGet_Short() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.getShort(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalGet_Int() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.getInt(primitives);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalGet_Long() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.getLong(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalSet_Boolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.set(primitives, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatPrimitiveIllegalSet_Null() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("f");
    f.set(primitives, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("F");
    f.set(objects, (long) 1);
  }

  @Test
  public void testFloatObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("F");
    assertNull(f.get(objects));
    objects.F = 1f;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFloatObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("F");
    assertNull(f.get(objects));
    assertEquals(0f, f.getFloat(objects));
  }
  

/////////////////////////////////////////////////
///////////////////Doubles///////////////////////
/////////////////////////////////////////////////

  @Test
  public void testDoublePrimitiveLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    assertEquals(0., f.getDouble(primitives));
    assertEquals(0., ((Double) f.get(primitives)).doubleValue());
    f.set(primitives, (double) 1);
    assertEquals(1., f.getDouble(primitives));
    assertEquals(1., ((Double) f.get(primitives)).doubleValue());

    f.set(primitives, 'a');
    f.set(primitives, (byte) 1);
    f.set(primitives, (int) 1);
    f.set(primitives, (long) 1);
    f.set(primitives, (float) 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalGet_Byte() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.getByte(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalGet_Short() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.getShort(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalGet_Int() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.getInt(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalGet_Long() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.getLong(primitives);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalGet_Float() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.getFloat(primitives);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalSet_Boolean() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.set(primitives, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoublePrimitiveIllegalSet_Null() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("d");
    f.set(primitives, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoubleObjectIllegalSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("D");
    f.set(objects, (float) 1);
  }

  @Test
  public void testDoubleObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("D");
    assertNull(f.get(objects));
    objects.D = 1.;
    assertNotNull(f.get(objects));
    f.set(objects, null);
    assertNull(f.get(objects));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDoubleObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("D");
    assertNull(f.get(objects));
    assertEquals(0., f.getDouble(objects));
  }
  
  @Test(expected=IllegalAccessException.class)
  public void testSetFinal() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("FINAL");
    f.set(objects, objects);
  }
  
  @Test
  public void testDirectInjection_Declared() throws Exception{ 
    ReflectionCaseNoMagic superClass = new ReflectionCaseNoMagic();
    Field field = NO_MAGIC.getDeclaredField(PRIVATE_MEMBER);
    field.setAccessible(true);
    assertFalse(field.getBoolean(superClass));
    field.setBoolean(superClass, true);
    assertTrue(field.getBoolean(superClass));
  }

  @Test
  public void testDirectInjection_Public() throws Exception{ 
    Field field = NO_MAGIC.getField(PUBLIC_MEMBER);
    field.setAccessible(true);
    assertFieldFalseToTrue(field, new ReflectionCaseNoMagic());
  }
//  
//  @Test(expected=NoSuchFieldException.class)
//  public void testDirectInjection_PublicFail() throws Exception{ 
//    Field field = NO_MAGIC.getField(PRIVATE_MEMBER);
//    field.setAccessible(true);
//    assertFieldFalseToTrue(field, new ReflectionCaseNoMagic());
//  }
//  
//  @Test(expected=NoSuchFieldException.class)
//  public void testDirectInjection_DeclaredFail() throws Exception{ 
//    Field field = NO_MAGIC_SUBCLASS.getDeclaredField(PRIVATE_MEMBER);
//    field.setAccessible(true);
//    assertFieldFalseToTrue(field, new ReflectionCaseNoMagic.Subclass());
//  }
  
  @Test
  public void testDirectInjection_Visibility() throws Exception{ 
    ReflectionCaseNoMagic superCase = new ReflectionCaseNoMagic();
    ReflectionCaseNoMagic.Subclass subCase = new ReflectionCaseNoMagic.Subclass();
    
    Field superField = NO_MAGIC.getField(OVERRIDE_FIELD);
    Field publicField = NO_MAGIC_SUBCLASS.getField(OVERRIDE_FIELD);
    Field declaredField = NO_MAGIC_SUBCLASS.getDeclaredField(OVERRIDE_FIELD);
    declaredField.setAccessible(true);
    
    
    assertFalse(declaredField.getBoolean(subCase));
    assertFalse(publicField.getBoolean(superCase));
    assertFalse(publicField.getBoolean(subCase));
    assertFalse(superField.getBoolean(subCase));
    assertFalse(superField.getBoolean(superCase));

    publicField.setBoolean(superCase, true);
    superField.setBoolean(subCase, true);
    
    assertTrue(superCase.overrideField);
    assertTrue(superCase.overrideField());
    assertTrue(subCase.overrideField());
    assertFalse(Subclass.getOverrideField(subCase));
    
    assertTrue(publicField.getBoolean(superCase));
    assertTrue(publicField.getBoolean(subCase));
    assertTrue(superField.getBoolean(superCase));
    assertTrue(superField.getBoolean(subCase));
    assertFalse(declaredField.getBoolean(subCase));
    
  }

  private void assertFieldFalseToTrue(Field f, Object o) throws Exception {
    assertFalse(f.getBoolean(o));
    f.setBoolean(o, true);
    assertTrue(f.getBoolean(o));
    
  }
  
}
