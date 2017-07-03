package collide.plugin.server.gwt;

import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.dto.server.DtoServerImpls.LogMessageImpl;
import com.google.collide.server.shared.util.Dto;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import xapi.bytecode.ClassFile;
import xapi.dev.gwtc.impl.GwtcServiceImpl;
import xapi.dev.scanner.X_Scanner;
import xapi.file.X_File;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GwtTestRunHandler  implements Handler<Message<JsonObject>> {
  /**
   *
   */
  private final GwtServerPlugin gwtServerPlugin;

  /**
   * @param gwtServerPlugin
   */
  GwtTestRunHandler(GwtServerPlugin gwtServerPlugin) {
    this.gwtServerPlugin = gwtServerPlugin;
  }

  @Override
  public void handle(Message<JsonObject> message) {
    String jsonString = Dto.get(message);
    GwtCompileImpl compileRequest = GwtCompileImpl.fromJsonString(jsonString);
    String module = compileRequest.getModule();
    compileRequest.setModule(module.replace('.', '_'));
    compileRequest.setMessageKey(module);

    log(module, "Searching for tests in "+module);

    try {
      List<String> resolved = new ArrayList<String>();
      for (String source : compileRequest.getSources().asIterable()) {
        File f = new File(source).getCanonicalFile();
        if (!f.exists()) {
          f = new File(gwtServerPlugin.getWebRoot(), source);
        }
        if (!f.exists()) {
          X_Log.warn(getClass(), "Missing source", f);
        }
        resolved.add(f.getCanonicalPath());
      }
      X_Log.info(getClass(), "new sources", resolved);
      compileRequest.setSources(resolved);
      resolved = new ArrayList<String>();
      for (String source : compileRequest.getDependencies().asIterable()) {
        File f = new File(source).getCanonicalFile();
        if (!f.exists()) {
          f = new File(gwtServerPlugin.getLibRoot(), source);
        }
        if (!f.exists()) {
          X_Log.warn(getClass(), "Missing dependency", f);
        }
        resolved.add(f.getCanonicalPath());
      }
      X_Log.info(getClass(), "new dependencies", resolved);
      compileRequest.setDependencies(resolved);
    } catch (Exception e) {
      X_Log.warn(getClass(), "Error resolving canonical dependency paths for ",compileRequest, e);
    }

    GwtCompiler compiler = gwtServerPlugin.compilers.get(compileRequest.getMessageKey());
    // This is an initialization request, so we should create a new compile server
    if (compiler.isRunning()) {
      compiler.kill();
    }
    // Initialize new compiler
    final ArrayList<String> logMessages = new ArrayList<>();
    URL[] cp;
    synchronized (this.gwtServerPlugin) {
      cp = this.gwtServerPlugin.getCompilerClasspath(compileRequest, new ReceivesValue<String>() {
        @Override
        public void set(String log) {
          logMessages.add(log);
        }
      }).toArray(new URL[0]);
    }
    GwtcServiceImpl impl = new GwtcServiceImpl();
    URLClassLoader loader = new URLClassLoader(cp, getClass().getClassLoader());
    Set<URL> paths = new LinkedHashSet<URL>();
    GwtManifest manifest = compiler.resolveCompile(compileRequest);
    Class<?> c;
    try {
      c = loader.loadClass(module);
      impl.addClass(c);
      log(module, "Found test class "+c.getCanonicalName());
    } catch (Exception e) {
      X_Log.info(getClass(), "Searching for tests in ",module);
      for (ClassFile cls : X_Scanner.findClassesInPackage(loader, module)) {
        try {
          c = loader.loadClass(cls.getQualifiedName());
          if (impl.addJUnitClass(c)) {
            log(module, "Found test class "+c.getCanonicalName());
            X_Log.info(getClass(), "Adding JUnit test class", c, cls.getResourceName());
          } else {
            X_Log.info(getClass(), "Skipping non-JUnit test class", c, cls.getResourceName());
          }
        } catch (Exception ex) {
          X_Log.warn(getClass(), "Unable to load scanned class", cls, ex);
          continue;
        }
        String clsName = cls.getResourceName().replace(".java", ".class");
        URL location = loader.getResource(clsName);
        try {
          String loc = location.toExternalForm();
          if (loc.contains("jar!")) {
            loc = loc.split("jar!")[0]+"jar";
          }
          if (loc.startsWith("jar:")) {
            loc = loc.substring(4);
          }
          X_Log.info(getClass(), "Adding source to GwtManifest: ",loc);
          location = new URL(loc);
          paths.add(location);
          manifest.addSource(loc);
        } catch (Exception x) {
          X_Log.warn(getClass(), "Unable to resolve resource location of ",location," from ", clsName, x);
        }
      }
    }
    manifest.addSystemProp("gwt.usearchives=false");
    manifest.setWarDir(impl.getTempDir().getAbsolutePath());
    compileRequest.setWarDir(X_File.createTempDir("Gwtc"+manifest.getModuleName()).getAbsolutePath());
    log(module, "Generating module into "+impl.getTempDir());
    impl.generateCompile(manifest);
    impl.copyModuleTo(module, manifest);
    String genDir = "file:"+impl.getTempDir().getAbsolutePath();
    try {
      paths.add(new URL(genDir));
      X_Log.info(getClass(), "Added gen dir to classpath", genDir);
    } catch (MalformedURLException e) {
      X_Log.error(getClass(),"Malformed temp dir", genDir, e);
    }

    compileRequest.addSources(impl.getTempDir().getAbsolutePath());

    if (paths.size() > 0) {
      X_Log.info(getClass(), "Adding additional classpath elements", paths);
      for (URL url : paths) {
        compileRequest.addSources(url.toExternalForm().replace("file:", ""));
      }
      for (URL url : cp) {
        paths.add(url);
      }
      cp = paths.toArray(new URL[paths.size()]);
    }
    synchronized (this.gwtServerPlugin) {
      compiler.initialize(compileRequest, cp, this.gwtServerPlugin.getEventBus(), this.gwtServerPlugin.getAddressBase() + ".log", ()->{
        log(module, "Compiling test module");
        message.reply(Dto.wrap(compileRequest.toString()));
        X_Log.info(getClass(), "Recompiling test class:",compileRequest);
        compiler.recompile(compileRequest.toString());
        for (String item : logMessages) {
          compiler.log(item);
        }
      });
      X_Log.trace(getClass(), "Classpath: ", cp);
    }
  }

  private void log(String module, String log) {
    LogMessageImpl message = LogMessageImpl.make();
    message.setLogLevel(Type.INFO);
    message.setMessage(log);
    message.setModule(module);
    gwtServerPlugin.getEventBus().send("gwt.log", Dto.wrap(message.toString()));
  }
}
