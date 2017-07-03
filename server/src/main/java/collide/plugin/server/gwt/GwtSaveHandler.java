package collide.plugin.server.gwt;

import collide.shared.manifest.CollideManifest;
import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.exception.NotFoundException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.collide.server.shared.util.DtoManifestUtil;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import xapi.gwtc.api.GwtManifest;
import xapi.io.X_IO;
import xapi.log.X_Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

public class GwtSaveHandler implements Handler<Message<JsonObject>>{

  @Override
  public void handle(Message<JsonObject> message) {
    // extract the saved compiler settings
    GwtRecompileImpl impl = GwtRecompileImpl.fromJsonString(message.body().toString());
    GwtManifest gwtc = DtoManifestUtil.newGwtManifest(impl);

    // Check for META-INF/collide.settings
    URL collideSettings = GwtSettingsHandler.class.getResource("/META-INF/collide.settings");
    JsonContainerExpr settings;
    final JsonContainerExpr expr;
    if (collideSettings != null) {
      CollideManifest manifest = new CollideManifest("");
      boolean found = false;
      try (
          InputStream in = collideSettings.openStream();
          ) {
        String source = X_IO.toStringUtf8(in);
        expr = JavaParser.parseJsonContainer(source);
        assert expr.isArray();
        for (Iterator<JsonPairExpr> itr = expr.getPairs().iterator(); itr.hasNext(); ) {
          final JsonPairExpr pair = itr.next();
          settings = (JsonContainerExpr) pair.getValueExpr();
          Expression module = settings.getNode("module");
          if (module instanceof JsonContainerExpr) {
            for (JsonPairExpr mod : ((JsonContainerExpr)module).getPairs()) {
              if (impl.getModule().equals(ASTHelper.extractStringValue(mod.getValueExpr()))) {
                itr.remove();
                found = true;
                break;
              }
            }
          } else {
            if (impl.getModule().equals(ASTHelper.extractStringValue(module))) {
              itr.remove();
              found = true;
              break;
            }
          }
        }
        if (!found) {
          throw new NotFoundException(impl.getModule());
        }
      } catch (ParseException | IOException e) {
        X_Log.info(getClass(), "Exception reading/parsing collide.settings", e);
        throw new RuntimeException(e);
      }

      // TODO: save the manifest into the settings file.

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
