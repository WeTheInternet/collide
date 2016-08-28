package collide.junit;

import collide.junit.cases.AbstractAnnotation;
import collide.junit.cases.CompileRetention;
import collide.junit.cases.ComplexAnnotation;
import collide.junit.cases.ReflectionCaseHasAllAnnos;
import collide.junit.cases.ReflectionCaseSimple;
import collide.junit.cases.RuntimeRetention;
import collide.junit.cases.SimpleAnnotation;
import org.junit.Test;

import static collide.junit.cases.AbstractAnnotation.MemberType.*;
import static collide.junit.cases.AbstractAnnotation.MemberType.Boolean;
import static collide.junit.cases.AbstractAnnotation.MemberType.Class;
import static collide.junit.cases.AbstractAnnotation.MemberType.Enum;
import static collide.junit.cases.AbstractAnnotation.MemberType.Long;
import static collide.junit.cases.AbstractAnnotation.MemberType.String;
import static xapi.reflect.X_Reflect.magicClass;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ComplexAnnotation
@SuppressWarnings("all")
@ReflectionStrategy(annotationRetention=ReflectionStrategy.COMPILE|ReflectionStrategy.RUNTIME)
public class AnnotationTests extends AbstractReflectionTest{

  static class ValueImpl extends AbstractAnnotation implements SimpleAnnotation {

    public ValueImpl() {
      this("1");
    }
    public ValueImpl(String value) {
      setValue("value", value);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return SimpleAnnotation.class;
    }

    @Override
    public String value() {
      return getValue("value");
    }

    @Override
    protected String[] members() {
      return new String[] {"value"};
    }

    @Override
    protected MemberType[] memberTypes() {
      return new MemberType[] {String};
    }
  }

  static class TestCaseImpl extends AbstractAnnotation implements ComplexAnnotation {
    private static final String[] members = new String[] {
      "singleBool",
      "singleInt",
      "singleLong",
      "singleString",
      "singleEnum",
      "singleAnnotation",
      "singleClass",
      "multiBool",
      "multiInt",
      "multiLong",
      "multiString",
      "multiEnum",
      "multiAnnotation",
      "multiClass"
    };
    private static final MemberType[] types = new MemberType[] {
      Boolean,
      Int,
      Long,
      String,
      Enum,
      Annotation,
      Class,
      Boolean_Array,
      Int_Array,
      Long_Array,
      String_Array,
      Enum_Array,
      Annotation_Array,
      Class_Array
    };

    public TestCaseImpl() {

      set(
        true,
        1,
        2,
        "3",
        ElementType.ANNOTATION_TYPE,
        new ValueImpl(),
        ElementType.class,
        new boolean[]{true, false, true},
        new int[] {1, 3, 2},
        new long[] {2, 4, 3},
        new String[] {"3", "0", "a"},
        new ElementType[] {ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE},
        new SimpleAnnotation[] {new ValueImpl(), new ValueImpl("2")},
        new Class<?>[] {ElementType.class, SimpleAnnotation.class}
        );
    }

    @UnsafeNativeLong
    public native void set(
      boolean singleBool,
      int singleInt,
      long singleLong,
      String singleString,
      Enum<?> singleEnum,
      Annotation singleAnnotation,
      Class<?> singleClass,
      boolean[] multiBool,
      int[] multiInt,
      long[] multiLong,
      String[] multiString,
      Enum<?>[] multiEnum,
      Annotation[] multiAnnotation,
      Class<?>[] multiClass
      )
    /*-{
      var m = this.@collide.junit.cases.AbstractAnnotation::memberMap;
      m['singleBool'] = singleBool;
      m['singleInt'] = singleInt;
      m['singleLong'] = singleLong;
      m['singleString'] = singleString;
      m['singleEnum'] = singleEnum;
      m['singleAnnotation'] = singleAnnotation;
      m['singleClass'] = singleClass;
      m['multiBool'] = multiBool;
      m['multiInt'] = multiInt;
      m['multiLong'] = multiLong;
      m['multiString'] = multiString;
      m['multiEnum'] = multiEnum;
      m['multiAnnotation'] = multiAnnotation;
      m['multiClass'] = multiClass;
    }-*/;

    @Override
    public native boolean singleBool()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleBool'];
    }-*/;

    @Override
    public native int singleInt()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long singleLong()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleLong'];
    }-*/;

    @Override
    public native String singleString()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleString'];
    }-*/;

    @Override
    public native ElementType singleEnum()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation singleAnnotation()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleAnnotation'];
    }-*/;

    @Override
    public native Class<?> singleClass()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['singleClass'];
    }-*/;

    @Override
    public native boolean[] multiBool()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiBool'];
    }-*/;

    @Override
    public native int[] multiInt()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long[] multiLong()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiLong'];
    }-*/;

    @Override
    public native String[] multiString()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiString'];
    }-*/;

    @Override
    public native ElementType[] multiEnum()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation[] multiAnnotation()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiAnnotation'];
    }-*/;

    @Override
    public native Class<?>[] multiClass()
    /*-{
      return this.@collide.junit.cases.AbstractAnnotation::memberMap['multiClass'];
    }-*/;

    @Override
    public Class<? extends Annotation> annotationType() {
      return ComplexAnnotation.class;
    }

    @Override
    protected String[] members() {
      return members;
    }

    @Override
    protected MemberType[] memberTypes() {
      return types;
    }

  }

  @Test
  public void testAnnotationMethods() {
    if (!GWT.isClient())
      return;// Don't let jvms try to load jsni; this @Test is gwt only
    TestCaseImpl impl1 = new TestCaseImpl();
    TestCaseImpl impl2 = new TestCaseImpl();
    assertEquals(impl1, impl2);
    assertEquals(impl1.toString(), impl2.toString());
    assertEquals(impl1.hashCode(), impl2.hashCode());
  }


  @Test
  public void testSimpleReflection() throws Exception {
    final Class<ReflectionCaseSimple> c = ReflectionCaseSimple.class;
    ReflectionCaseSimple inst = testNewInstance(magicClass(c));
    ReflectionCaseSimple anon = new ReflectionCaseSimple() {};
    testAssignable(inst, anon);

    testHasNoArgDeclaredMethods(c, "privatePrimitive", "privateObject", "publicPrimitive", "publicObject");
    testHasNoArgPublicMethods(c, "publicPrimitive", "publicObject", "hashCode", "toString");
    testCantAccessNonPublicMethods(c, "privatePrimitive", "privateObject");
    testCantAccessNonDeclaredMethods(c, "hashCode", "toString");
  }

  @Test
  public void testAnnotationsKeepAll() throws Exception {
    Class<?> testCase = magicClass(ReflectionCaseHasAllAnnos.class);
    Field field = testCase.getDeclaredField("field");
    Method method = testCase.getDeclaredMethod("method", Long.class);
    Constructor<?> ctor = testCase.getDeclaredConstructor(long.class);
    Annotation[] annos = testCase.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }
    annos = field.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

    annos = method.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

    annos = ctor.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

  }

  private void assertHasAnno(Class<?> cls, Annotation[] annos, Class<? extends Annotation> annoClass) {
    for (Annotation anno : annos) {
      if (anno.annotationType() == annoClass)
        return;
    }
    fail(cls.getName()+" did not have required annotation "+annoClass);
  }

  private void testCantAccessNonPublicMethods(Class<?> c, String ... methods) {
    for (String method : methods) {
      try {
        c.getMethod(method);
        fail("Could erroneously access non-public method "+method+" in "+c.getName());
      } catch (NoSuchMethodException e) {}
    }
  }

  private void testCantAccessNonDeclaredMethods(Class<?> c, String ... methods) {
    for (String method : methods) {
      try {
        c.getDeclaredMethod(method);
        fail("Could erroneously access non-declared method "+method+" in "+c.getName());
      } catch (NoSuchMethodException e) {}
    }
  }

  private void testHasNoArgDeclaredMethods(Class<?> c, String ... methods) throws Exception{
    for (String method : methods) {
      assertNotNull(c.getDeclaredMethod(method));
    }
  }
    private void testHasNoArgPublicMethods(Class<?> c, String ... methods) throws Exception{
      for (String method : methods) {
        assertNotNull(c.getMethod(method));
      }
  }

  private void testAssignable(Object inst, Object anon) {
    assertTrue(inst.getClass().isAssignableFrom(anon.getClass()));
    assertFalse(anon.getClass().isAssignableFrom(inst.getClass()));
  }

  private <T> T testNewInstance(Class<T> c) throws Exception {
    T newInst = c.newInstance();
    assertNotNull(c.getName()+" returned null instead of a new instance", newInst);
    assertTrue(c.isAssignableFrom(newInst.getClass()));
    return newInst;
  }


}
