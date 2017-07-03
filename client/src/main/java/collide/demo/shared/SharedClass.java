package collide.demo.shared;

import xapi.annotation.compile.MagicMethod;
import xapi.log.X_Log;
import xapi.util.X_Runtime;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

import java.io.File;

/**
 * This is a test class for our reflection api;
 * it is annotated for gwt to pull out reflection data,
 * plus it includes different methods for
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@ReflectionStrategy(keepEverything=true, keepCodeSource=true, annotationRetention=ReflectionStrategy.ALL_ANNOTATIONS, debug=ReflectionStrategy.ALL_ANNOTATIONS)
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

  @MagicMethod(doNotVisit = true)
  private void doJavaStuff() throws Exception{
    X_Log.info("Running in "+
      Class.forName("java.io.File").getMethod("getCanonicalPath").invoke(
          Class.forName("java.io.File").getConstructor(String.class).newInstance(".")
      )
    );
  }

}
