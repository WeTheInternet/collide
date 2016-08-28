package com.google.collide.plugin.server.gwt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import xapi.log.X_Log;
import collide.shared.manifest.CollideManifest;
import collide.shared.manifest.CollideManifest.GwtEntry;

import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtSettingsImpl;
import com.google.collide.server.shared.util.Dto;

import elemental.util.ArrayOf;

public class GwtSettingsHandler implements Handler<Message<JsonObject>>{

  @Override
  public void handle(Message<JsonObject> message) {
    // load up our known gwt modules
    GwtSettingsImpl reply = GwtSettingsImpl.make();
    
    // Check for META-INF/collide.settings
    URL collideSettings = GwtSettingsHandler.class.getResource("/META-INF/collide.settings");
    if (collideSettings != null) {
      CollideManifest manifest = new CollideManifest("");
      try (
      BufferedReader reader = new BufferedReader(new InputStreamReader(collideSettings.openStream()));
          ){
        String line;
        while ((line = reader.readLine()) != null)
          manifest.addEntry(line);
      } catch (IOException e) {
        X_Log.info(getClass(), "IOException reading collide.settings", e);
      }
      ArrayOf<GwtEntry> entries = manifest.getGwtEntries();
      for (int i = 0, m = entries.length(); i < m; i++) {
        GwtEntry entry = entries.get(i);
        List<String> sources = Arrays.asList(entry.sources);
        List<String> classpath = Arrays.asList(entry.dependencies);
        for (String module : entry.modules) {
          X_Log.info(module);
          GwtRecompileImpl build = GwtRecompileImpl.make();
          build.setModule(module);
          build.setSources(sources);
          build.setDependencies(classpath);
          reply.addModules(build);
        }
      }
    }
    // TODO: send message to @FileTree, asking for .gwt.xml files, and search for sibling .gwt.settings files...
    
    
    message.reply(Dto.wrap(reply));
    
  }

}
