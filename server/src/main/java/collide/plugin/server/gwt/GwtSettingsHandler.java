package collide.plugin.server.gwt;

import collide.shared.manifest.CollideManifest;
import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtSettingsImpl;
import com.google.collide.server.shared.util.Dto;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.mvn.X_Maven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GwtSettingsHandler implements Handler<Message<JsonObject>> {

  @Override
  public void handle(Message<JsonObject> message) {
    // load up our known gwt modules
    GwtSettingsImpl reply = GwtSettingsImpl.make();

    // Check for META-INF/collide.settings
    URL collideSettings = GwtSettingsHandler.class.getResource("/META-INF/collide.settings");
    if (collideSettings != null) {
      CollideManifest manifest = new CollideManifest("");
      try (
          InputStream in = collideSettings.openStream();
      ) {
        String source = X_IO.toStringUtf8(in);
        final JsonContainerExpr expr = JavaParser.parseJsonContainer(source);
        assert expr.isArray();
        for (Iterator<JsonPairExpr> itr = expr.getPairs().iterator(); itr.hasNext(); ) {
          final JsonPairExpr pair = itr.next();
          final JsonContainerExpr settings = (JsonContainerExpr) pair.getValueExpr();
          List<String> modules = ASTHelper.extractStrings(settings.getNode("module"));
          List<String> sources = ASTHelper.extractStrings(settings.getNode("sources"));
          List<String> classpath = extractClasspath((JsonContainerExpr)settings.getNode("classpath"));
          for (String module : modules) {
            GwtRecompileImpl build = GwtRecompileImpl.make();
            build.setModule(module);
            build.setSources(sources);
            build.setDependencies(classpath);
            reply.addModules(build);
          }
        }
      } catch (ParseException | IOException e) {
        X_Log.info(getClass(), "Exception reading/parsing collide.settings", e);
        throw new RuntimeException(e);
      }
    }
    // TODO: send message to @FileTree, asking for .gwt.xml files, and search for sibling .gwt.settings files...

    message.reply(Dto.wrap(reply));

  }

  private List<String> extractClasspath(JsonContainerExpr classpath) {
    final Set<String> cp = new LinkedHashSet<>();
    assert classpath.isArray();
    for (JsonPairExpr pair : classpath.getPairs()) {
      if (pair.getValueExpr() instanceof MethodCallExpr) {
        MethodCallExpr method = (MethodCallExpr) pair.getValueExpr();
        switch (method.getName().toLowerCase()) {
          case "maven":
            assert method.getArgs().size() == 1 : "Expect exactly 1 string argument to maven() method";
            final String artifactString = ASTHelper.extractStringValue(method.getArgs().get(0));
            String[] artifact = artifactString.split(":");
            ArtifactResult result;
            switch (artifact.length) {
              case 3:
                result = X_Maven.loadArtifact(artifact[0], artifact[1], artifact[2]);
                break;
              case 4:
                result = X_Maven.loadArtifact(artifact[0], artifact[1], artifact[2], artifact[3]);
                break;
              case 5:
                result = X_Maven.loadArtifact(artifact[0], artifact[1], artifact[2], artifact[3], artifact[4]);
                break;
              default:
                throw new IllegalArgumentException("Malformed maven artifact; " + artifactString + " must have only 3, 4 or 5 segments");
            }
            final Artifact a = result.getArtifact();
            final List<String> dependencies = X_Maven.loadCompileDependencies(a);
            cp.addAll(dependencies);
            break;
          default:
            throw new IllegalArgumentException("Unhandled classpath method: " + method);
        }
      } else {
        cp.add(ASTHelper.extractStringValue(pair.getValueExpr()));
      }
    }

    return new ArrayList<>(cp);
  }

  public static void main(String ... a) throws ParseException {
    final JsonContainerExpr expr = JavaParser
        .parseJsonContainer("[maven(\"net.wetheinter:xapi-gwt:0.5.1-SNAPSHOT\")]");
    new GwtSettingsHandler().extractClasspath(expr);
  }

}
