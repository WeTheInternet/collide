package collide.server;

import collide.plugin.server.ant.AntServerPlugin;
import collide.plugin.server.gwt.GwtServerPlugin;
import collide.server.codegraph.CodeGraphMonitor;
import com.google.collide.server.documents.EditSessions;
import com.google.collide.server.fe.WebFE;
import com.google.collide.server.filetree.FileTree;
import com.google.collide.server.maven.MavenController;
import com.google.collide.server.participants.Participants;
import com.google.collide.server.workspace.WorkspaceState;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.metrics.MetricsOptions;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/28/16.
 */
public class StartServer {

    public static void main(String[] args) {
        // Create an HTTP server which simply returns "Hello World!" to each request.
        final Vertx vertx = Vertx.vertx(

            new VertxOptions().setMetricsOptions(new MetricsOptions().setEnabled(true))

        );

        String webRoot = "/opt/collide";
        String staticFiles = "/opt/collide/client/build/gwt/out/Collide";

        final JsonObject webConfig = new JsonObject()
            .put("port", 13337)
            .put("host", "0.0.0.0")
            .put("bridge", true)
            .put("webRoot", webRoot)
            .put("staticFiles", staticFiles)
            .put("in_permitted", new JsonArray()
                .add(".*")
            )
            .put("out_permitted", new JsonArray()
                .add(".*")
            )
        ;

        final JsonObject participantsConfig = new JsonObject()
            .put("usernames", new JsonArray().add("James"))
        ;

        final JsonObject workspaceConfig = new JsonObject()
            .put("plugins", new JsonArray()
                .add("gwt").add("ant"))
            .put("webRoot", webRoot)
        ;

        final JsonObject pluginConfig = new JsonObject()
            .put("usernames", webRoot)
            .put("plugins", new JsonArray()
                .add("gwt").add("ant"))
            .put("includes", new JsonArray()
                .add("gwt").add("ant"))
            .put("preserve-cwd", true)
            .put("webRoot", webRoot)
            .put("staticFiles", staticFiles)
        ;

        final JsonObject filetreeConfig = new JsonObject()
            .put("webRoot", webRoot)
            .put("packages", new JsonArray()
                .add("api/src/main/java")
                .add("shared/src/main/java")
                .add("client/src/main/java")
                .add("server/src/main/java")
            )
        ;

        vertx.deployVerticle(WebFE.class.getCanonicalName(), opts(webConfig)
            .setInstances(10));

        deploy(vertx, Participants.class, participantsConfig);
        deploy(vertx, CodeGraphMonitor.class);
        deploy(vertx, EditSessions.class);
        deploy(vertx, FileTree.class, filetreeConfig);
        deploy(vertx, WorkspaceState.class, workspaceConfig);
        deploy(vertx, MavenController.class, workspaceConfig);
        deploy(vertx, GwtServerPlugin.class, pluginConfig);
        deploy(vertx, AntServerPlugin.class, pluginConfig);

    }

    public static <T extends Verticle> void deploy(Vertx vertx, Class<T> type, JsonObject config) {
        vertx.deployVerticle(type.getCanonicalName(), opts(config));
    }

    public static <T extends Verticle> void deploy(Vertx vertx, Class<T> type) {
        vertx.deployVerticle(type.getCanonicalName());
    }

    private static DeploymentOptions opts(JsonObject config) {
        return new DeploymentOptions().setConfig(config);
    }

}
