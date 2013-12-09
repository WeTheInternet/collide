package com.google.collide.plugin.server.gwt;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import xapi.bytecode.ClassFile;
import xapi.dev.gwtc.impl.GwtcServiceImpl;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.util.api.ReceivesValue;

import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.server.shared.util.Dto;
import com.google.gwt.reflect.client.GwtReflectJre;

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
    GwtCompiler compiler = gwtServerPlugin.compilers.get(compileRequest.getModule());
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
      compiler.initialize(compileRequest, cp, this.gwtServerPlugin.getEventBus(), this.gwtServerPlugin.getAddressBase() + ".log");
    }
    GwtcServiceImpl impl = new GwtcServiceImpl();
    String module = compileRequest.getModule();
    URLClassLoader loader = new URLClassLoader(cp);
    Class<?> c;
    try {
      c = loader.loadClass(module);
      impl.addJUnitClass(c);
    } catch (Exception e) {
      Package p = GwtReflectJre.getPackage(module, loader);
      ClasspathResourceMap classes = X_Scanner.findClassesInPackage(loader, p.getName());
      for (ClassFile cls : classes.getAllClasses()) {
        try {
          c = loader.loadClass(cls.getQualifiedName());
          impl.addJUnitClass(c);
        } catch (Exception ex) {
          X_Log.warn(getClass(), "Unable to load scanned class", cls, ex);
        }
      }
    }
    GwtManifest manifest = compiler.resolveCompile(compileRequest);
    impl.generateCompile(manifest);
    
    compiler.compile(compileRequest);
    for (String item : logMessages) {
      compiler.log(item);
    }
    // TODO send back the location of the entry point html file;
    // or, at least symlink it somewhere based on the compiled class/package
  }
}