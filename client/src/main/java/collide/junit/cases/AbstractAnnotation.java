package collide.junit.cases;

import xapi.reflect.X_Reflect;

import com.google.gwt.core.client.JavaScriptObject;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 * A somewhat ugly, but functional implementation of an annotation;
 * it should never be used in production, but it exposes a relatively simple
 * and correct api for "how an annotation should behave".
 * <p>
 *
 * @author "james@wetheinter.net"
 *
 */
public abstract class AbstractAnnotation extends SourceVisitor{

  public static enum MemberType {
    Boolean, Byte, Short, Int, Long, Float, Double, Class, Enum, String, Annotation,
    Boolean_Array, Byte_Array, Short_Array, Int_Array, Long_Array, Float_Array,
    Double_Array, Class_Array, Enum_Array, String_Array, Annotation_Array
  }

  private final JavaScriptObject memberMap;

  public AbstractAnnotation() {
    this(JavaScriptObject.createObject());
  }

  public AbstractAnnotation(JavaScriptObject memberMap) {
    this.memberMap = memberMap;
  }

  public abstract Class<? extends Annotation> annotationType();

  protected abstract String[] members();

  protected abstract MemberType[] memberTypes();

  protected final native <T> T getValue(String member)
  /*-{
    return this.@collide.junit.cases.AbstractAnnotation::memberMap[member];
  }-*/;

  protected final native void setValue(String member, Object value)
  /*-{
    this.@collide.junit.cases.AbstractAnnotation::memberMap[member] = value;
  }-*/;

  @Override
  public final int hashCode() {
    String[] members = members();
    MemberType[] types = memberTypes();
    int hash = 0, i = members.length;
    for (; i-- > 0;) {
      String member = members[i];
      switch (types[i]) {
      case Annotation:
      case Class:
      case Enum:
      case String:
        hash += ((127) * member.hashCode()) ^ getObjectHash(member);
        break;
      case Boolean:
        hash += ((127) * member.hashCode()) ^ getBoolInt(member);
        break;
      case Byte:
      case Int:
      case Short:
      case Float:
      case Double:
        hash += ((127) * member.hashCode()) ^ getInt(member);
        break;
      case Long:
        hash += ((127) * member.hashCode()) ^ (int)getLong(memberMap, member);
        break;
      case Annotation_Array:
      case Class_Array:
      case Enum_Array:
      case String_Array:
        hash += ((127) * member.hashCode()) ^ Arrays.hashCode(this.<Object[]>getValue(member));
        break;
      case Boolean_Array:
      case Byte_Array:
      case Int_Array:
      case Short_Array:
      case Float_Array:
      case Double_Array:
        hash += ((127) * member.hashCode()) ^ getPrimitiveArrayHash(member);
        break;
      case Long_Array:
        hash += ((127) * member.hashCode()) ^ Arrays.hashCode(this.<long[]>getValue(member));
      }
    }
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder("@");
    b.append(annotationType().getName());
    b.append(" (");
    String[] members = members();
    if (members.length == 0) {
      b.append(")");
      return b.toString();
    }
    MemberType[] types = memberTypes();
    b.append(members[0]);
    b.append(" = ");
    b.append(toString(members[0], types[0]));
    for (int i = 1, m = members.length; i < m; i++) {
      b.append(", ");
      b.append(members[i]);
      b.append(" = ");
      b.append(toString(members[i], types[i]));
    }
    b.append(")");
    return b.toString();
  }

  /**
   * From java.lang.annotation:
   * Returns true if the specified object represents an annotation that is
   * logically equivalent to this one. In other words, returns true if the specified object is an instance of
   * the same annotation type as this instance, all of whose members are equal to the corresponding member of
   * this annotation, as defined below:
   * <ul>
   * <li>Two corresponding primitive typed members whose values are <tt>x</tt> and <tt>y</tt> are considered
   * equal if <tt>x == y</tt>, unless their type is <tt>float</tt> or <tt>double</tt>.
   * <li>Two corresponding <tt>float</tt> members whose values are <tt>x</tt> and <tt>y</tt> are considered
   * equal if <tt>Float.valueOf(x).equals(Float.valueOf(y))</tt>. (Unlike the <tt>==</tt> operator, NaN is
   * considered equal to itself, and <tt>0.0f</tt> unequal to <tt>-0.0f</tt>.)
   * <li>Two corresponding <tt>double</tt> members whose values are <tt>x</tt> and <tt>y</tt> are considered
   * equal if <tt>Double.valueOf(x).equals(Double.valueOf(y))</tt>. (Unlike the <tt>==</tt> operator, NaN is
   * considered equal to itself, and <tt>0.0</tt> unequal to <tt>-0.0</tt>.)
   * <li>Two corresponding <tt>String</tt>, <tt>Class</tt>, enum, or annotation typed members whose values are
   * <tt>x</tt> and <tt>y</tt> are considered equal if <tt>x.equals(y)</tt>. (Note that this definition is
   * recursive for annotation typed members.)
   * <li>Two corresponding array typed members <tt>x</tt> and <tt>y</tt> are considered equal if
   * <tt>Arrays.equals(x, y)</tt>, for the appropriate overloading of {@link java.util.Arrays#equals}.
   * </ul>
   *
   * @return true if the specified object represents an annotation that is logically equivalent to this one,
   * otherwise false
   */
  @Override
  public final boolean equals(Object o) {
    if (o == this) return true;
    if (o instanceof AbstractAnnotation) {
      AbstractAnnotation you = (AbstractAnnotation)o;
      MemberType[] myTypes = memberTypes();
      if (!Arrays.equals(myTypes, you.memberTypes())) return false;

      String[] myMembers = members();
      String[] yourMembers = you.members();
      // These member lists are generated, so they will always be in the same order for the same type
      if (myMembers.length == yourMembers.length) {
        for (int i = myMembers.length; i-- > 0;) {
          String key = myMembers[i];
          if (!key.equals(yourMembers[i])) return false;
          MemberType type = myTypes[i];

          assert !isNull(key);
          assert !you.isNull(key);

          switch (type) {
          case Annotation:
            if (getValue(key).equals(you.getValue(key))) continue;
            return false;
          case Class:
          case Enum:
          case String:
          case Boolean:
          case Byte:
          case Int:
          case Short:
          case Float:
          case Double:
            if (quickEquals(key, you.memberMap))
              continue;
            return false;
          case Long:
            if (getLong(memberMap, key) == getLong(you.memberMap, key))
              continue;
            return false;
          case Annotation_Array:
          case Class_Array:
          case Enum_Array:
          case String_Array:
            if (arraysEqualObject(getValue(key), you.getValue(key)))
              continue;
            return false;
          case Boolean_Array:
          case Byte_Array:
          case Int_Array:
          case Short_Array:
          case Float_Array:
          case Double_Array:
            if (arraysEqualPrimitive(getValue(key), you.getValue(key)))
              continue;
            return false;
          case Long_Array:
            if (arraysEqualLong(getValue(key), you.getValue(key)))
              continue;
            return false;
          }
        }// end for loop
        return true;
      }
    }
    return false;
  }

  private String toString(String key, MemberType memberType) {
    switch (memberType) {
    case Class:
      Class<?> c = getValue(key);
      return c.getName() + ".class";
    case Enum:
      Enum<?> e = getValue(key);
      return e.getDeclaringClass().getName() + "." + e.name();
    case Boolean:
    case Byte:
    case Int:
    case Short:
    case Float:
    case Double:
      return getNativeString(key);
    case Long:
      return String.valueOf(getLong(memberMap, key));
    case Class_Array:
      StringBuilder b = new StringBuilder("{ ");
      Class<?>[] classes = this.<Class<?>[]>getValue(key);
      if (classes.length > 0) {
        b.append(classes[0].getName()).append(".class");
        for (int i = 1, m = classes.length; i < m; i++) {
          b.append(", ").append(classes[i].getName()).append(".class");
        }
      }
      b.append(" }");
      return b.toString();
    case Enum_Array:
      b = new StringBuilder("{ ");
      Enum<?>[] enums = this.<Enum<?>[]>getValue(key);
      if (enums.length > 0) {
        b.append(enums[0].getDeclaringClass().getName()).append(".").append(enums[0].name());
        for (int i = 1, m = enums.length; i < m; i++) {
          b.append(", ").append(enums[i].getDeclaringClass().getName()).append(".").append(enums[i].name());
        }
      }
      b.append(" }");
      return b.toString();
    case String_Array:
      b = new StringBuilder("{ ");
      String[] strings = this.<String[]>getValue(key);
      if (strings.length > 0) {
        b.append('"').append(strings[0]).append('"');
        for (int i = 1, m = strings.length; i < m; i++) {
          b.append(", \"").append(strings[i]).append('"');
        }
      }
      b.append(" }");
      return b.toString();
    case Long_Array:
      b = new StringBuilder("{ ");
      long[] longs = this.<long[]>getValue(key);
      if (longs.length > 0) {
        b.append(longs[0]);
        if (longs[0] > Integer.MAX_VALUE)
          b.append('L');
        for (int i = 1, m = longs.length; i < m; i++) {
          b.append(", ").append(longs[i]);
          if (longs[i] > Integer.MAX_VALUE)
            b.append('L');
        }
      }
      b.append(" }");
      return b.toString();
    case Annotation_Array:
    case Boolean_Array:
    case Byte_Array:
    case Int_Array:
    case Short_Array:
    case Float_Array:
    case Double_Array:
      b = new StringBuilder("{ ");
      arrayToString(b, key);
      b.append(" }");
      return b.toString();
    case String:
      return "\"" + X_Reflect.escape(this.<String>getValue(key)) + "\"";
    case Annotation:
    default:
      return String.valueOf(getValue(key));
    }
  }

  private native void arrayToString(StringBuilder b, String key)
  /*-{
    var i = 0, o = this.@collide.junit.cases.AbstractAnnotation::memberMap[key], m = o.length;
    if (m > 0) {
      b.@java.lang.StringBuilder::append(Ljava/lang/String;)(""+o[i++]);
      for (;i < m; i++) {
        b.@java.lang.StringBuilder::append(Ljava/lang/String;)(", ");
        b.@java.lang.StringBuilder::append(Ljava/lang/String;)(""+o[i]);
      }
    }
  }-*/;

  private final native String getNativeString(String key)
  /*-{
    return ''+this.@collide.junit.cases.AbstractAnnotation::memberMap[key];
  }-*/;

  private native int getPrimitiveArrayHash(String member)
  /*-{
    var hash = 1;
    var arr = this.@collide.junit.cases.AbstractAnnotation::memberMap[member];
    for ( var i in arr) {
      hash += 31*(~~arr[i]);// GWT's number hash codes just cast to int anyway
    }
    return hash;
  }-*/;

  private native int getInt(String member)
  /*-{
    return this.@collide.junit.cases.AbstractAnnotation::memberMap[member];
  }-*/;

  private native int getBoolInt(String member)
  /*-{
    return this.@collide.junit.cases.AbstractAnnotation::memberMap[member] ? 1 : 0;
  }-*/;

  private final int getObjectHash(String member) {
    Object value = getValue(member);
    assert value != null : "Annotations can never have null values.  No member " + member + " in " + this;
    return value.hashCode();
  }

  private final native boolean isNull(String key)
  /*-{
    return this.@collide.junit.cases.AbstractAnnotation::memberMap[key] == null;
  }-*/;

  private final native boolean quickEquals(String key, JavaScriptObject you)
  /*-{
    return you[key] === this.@collide.junit.cases.AbstractAnnotation::memberMap[key];
  }-*/;


}
