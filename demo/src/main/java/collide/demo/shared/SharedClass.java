package collide.demo.shared;

import java.io.File;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepConstructor;
import xapi.annotation.reflect.KeepField;
import xapi.annotation.reflect.KeepMethod;
import xapi.log.X_Log;
import xapi.util.X_Runtime;

/**
 * This is a test class for our reflection api;
 * it is annotated for gwt to pull out reflection data,
 * plus it includes different methods for 
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@ReflectionStrategy(keepCodeSource=true)
public class SharedClass {

  public int sharedInt = 10;
  public String sharedString = "stuff";
  
  
  public void doStuff() throws Exception {
    if (X_Runtime.isJava())
      doJavaStuff();
    else
      doJavascriptStuff();
  }
  
  private native void doJavascriptStuff()
  /*-{
    $doc.body.contentEditable=true;
  }-*/;
  
  private void doJavaStuff() throws Exception{
    // java.io.File is emulated in gwt, but w/ a private constructor.
    // It is only present to allow source-level compatibility (being seen in files),
    // so if we want to create a new file, we just do it through reflection.
    X_Log.info("Running in "+
      File.class.getMethod("getCanonicalPath").invoke(
        File.class.getConstructor(String.class).newInstance(".")
      )
    );
  }
  
}
