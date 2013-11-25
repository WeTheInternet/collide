package collide.shared.manifest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import collide.shared.api.ObfuscationLevel;
import collide.shared.api.OpenAction;

import com.google.gwt.core.ext.TreeLogger.Type;

public class CollideManifestTest {

  private static final String gwtFormat = "gwt\tcollide.demo.Parent\tdemo/src/main/java;demo/src/main/resources;java;bin/gen;plugin;deps/guava-12.0/guava-gwt-12.0.jar;deps/guava-12.0/guava-12.0.jar\tgson-2.2.1.jar;waveinabox-import-0.3.jar;collide-source.jar;elemental.jar;gwt-dev.jar;gwt-user.jar;elemental.jar;client-src.jar;client-common-src.jar;client-scheduler-src.jar;common-src.jar;concurrencycontrol-src.jar;model-src.jar;media-src.jar;jsr305.jar;validation-api-1.0.0.GA-sources.jar;validation-api-1.0.0.GA.jar";
  private static final String gwtcFormat = "\ngwtc:\n"
      + " module: collide.demo.Parent\n"
      + " src:\n"
      + " - demo/src/main/java\n"
      + " - demo/src/main/resources\n"
      + " - java\n"
      + " - bin/gen\n"
      + " - plugin\n"
      + " - deps/guava-12.0/guava-gwt-12.0.jar\n"
      + " - deps/guava-12.0/guava-12.0.jar\n"
      + " dependencies:\n"
      + " - gson-2.2.1.jar\n"
      + " - waveinabox-import-0.3.jar\n"
      + " - collide-source.jar\n"
      + " - elemental.jar\n"
      + " - gwt-dev.jar\n"
      + " - gwt-user.jar\n"
      + " - elemental.jar\n"
      + " - client-src.jar\n"
      + " - client-common-src.jar\n"
      + " - client-scheduler-src.jar\n"
      + " - common-src.jar\n"
      + " - concurrencycontrol-src.jar\n"
      + " - model-src.jar\n"
      + " - media-src.jar\n"
      + " - jsr305.jar\n"
      + " - validation-api-1.0.0.GA-sources.jar\n"
      + " - validation-api-1.0.0.GA.jar";
  
  @Test
  public void testGwt() {
    CollideManifest manifest = new CollideManifest(gwtFormat);
    assertEquals(1, manifest.getGwtEntries().length());
  }
  
  @Test
  public void testGwtc() {
    YamlLineEater eater = new YamlLineEater();
    for (String line : gwtcFormat.split("\n")) {
      eater.eat(line);
    }
    System.out.println(eater);
  }
  
  @Test
  public void testGwtManifest() {
    GwtManifest gwtc = new GwtManifest("module")
      .setAutoOpen(true)
      .setClosureCompiler(true)
      .setDeployDir("deployDir")
      .setDisableAggressiveOptimize(true)
      .setDisableCastCheck(true)
      .setDisableClassMetadata(true)
      .setDisableRunAsync(true)
      .setDisableThreadedWorkers(true)
      .setDraftCompile(true)
      .setEnableAssertions(true)
      .setExtrasDir("extraDir")
      .setFragments(20)
      .setGenDir("genDir")
      .setLocalWorkers(8)
      .setLogLevel(Type.WARN)
      .setObfuscationLevel(ObfuscationLevel.OBFUSCATED)
      .setOpenAction(OpenAction.NO_ACTION)
      .setOptimizationLevel(5)
      .setSoyc(true)
      .setSoycDetailed(true)
      .setStrict(true)
      .setUrlToOpen("urlToOpen")
      .setValidateOnly(true)
      .setWorkDir("workDir")
      .addDependency("dep1")
      .addDependency("dep2")
      .addExtraArg("extra")
      .addJvmArg("jvm")
      .addSource("src1")
      .addSource("src2")
      .addSystemProp("sysProp")
    ;
    YamlLineEater eater = new YamlLineEater();
    eater.eatAll(gwtc.toString());
    System.out.println(gwtc.toString());
    System.out.println(eater.toString());
  }
  
}
