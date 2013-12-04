package com.google.collide.plugin.server.gwt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import collide.shared.manifest.CollideManifest;

import com.google.collide.dto.client.DtoManifestUtil;
import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class GwtSaveHandler implements Handler<Message<JsonObject>>{

  @Override
  public void handle(Message<JsonObject> message) {
    // extract the saved compiler settings
    GwtRecompileImpl impl = GwtRecompileImpl.fromJsonString(message.body.toString());
    GwtManifest gwtc = DtoManifestUtil.newGwtManifest(impl);
    
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
      // Now put the settings back
      manifest.addGwtc(gwtc);
      try {
        String path = collideSettings.getFile();
        File f = new File(path);
        if (f.exists()) {
          Files.write(manifest.toString(), f, Charsets.UTF_8);
        } else {
          int jarInd = f.getName().indexOf("!");
          if (jarInd == -1) {
            // really shouldn't happen, since our classloader had to get the file from somewhere
            X_Log.error("File ",f.getCanonicalPath()," does not exist");
          } else {
            X_Log.error("File ",f.getCanonicalPath()," is a jar file, which we cannot write into");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
}
