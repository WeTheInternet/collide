package collide.junit.cases;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ComplexAnnotation {
  boolean singleBool() default true;
  int singleInt() default 1;
  long singleLong() default 2;
  String singleString() default "3";
  ElementType singleEnum() default ElementType.ANNOTATION_TYPE;
  SimpleAnnotation singleAnnotation() default @SimpleAnnotation;
  Class<?> singleClass() default ElementType.class;
  boolean[] multiBool() default {true, false, true};
  int[] multiInt() default {1, 3, 2};
  long[] multiLong() default {2, 4, 3};
  String[] multiString() default {"3", "0", "a"};
  ElementType[] multiEnum() default {ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE};
  SimpleAnnotation[] multiAnnotation() default {@SimpleAnnotation, @SimpleAnnotation(value="2")};
  Class<?>[] multiClass() default {ElementType.class, SimpleAnnotation.class};
}