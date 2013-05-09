package com.google.collide.shared.util;

public class DebugUtil {

  public static String getFullStacktrace(Throwable t, String separator){
    StringBuilder b = new StringBuilder();
    
    for (StackTraceElement el : t.getStackTrace()){
      b.append(toString(el));
      b.append(separator);
    }
    Throwable cause = t.getCause();
    if (cause != null&&t.getCause()!=t)
      b.append(getFullStacktrace(t.getCause(), separator+"  "));
    return b.toString();
  }
  public static String toString(StackTraceElement el) {
    return el.getClassName()+"."+el.getMethodName()+"("+el.getLineNumber()+"): "+el.getFileName();
  }
  public static String getCaller(){
    return getCaller(10, "\n");
  }
  public static String getCaller(int limit){
    return getCaller(limit, "\n");
  }
  public static String getCaller(int limit,String separator){
    Throwable t = new Throwable();
    t.fillInStackTrace();
    StringBuilder b = new StringBuilder();
    StackTraceElement[] trace = t.getStackTrace();
    limit = Math.min(limit, trace.length);
    for (int i = 0;++i<limit;){
      b.append(toString(trace[i]));
      b.append(separator);
    }
    
    return b.toString();
  }
  
}
