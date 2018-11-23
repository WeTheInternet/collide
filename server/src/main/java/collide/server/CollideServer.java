package collide.server;

import collide.server.handler.WebAppHandler;
import collide.vertx.VertxService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.scope.request.RequestScope;
import xapi.server.api.Route;
import xapi.server.api.Route.RouteType;
import xapi.server.api.WebApp;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;
import xapi.server.vertx.XapiVertxServer;
import xapi.util.X_String;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import static xapi.model.X_Model.create;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public class CollideServer extends XapiVertxServer {

    private final class CachedResponse {
        String path;
        UiContainerExpr source;
        FileTime timestamp;
        volatile boolean result;

        public CachedResponse(String path, UiContainerExpr source, FileTime timestamp) {
            this.path = path;
            this.source = source;
            this.timestamp = timestamp;
        }
    }

    private final String staticFiles;
    private final String webRoot;
    private final String workDir;
    private final String warDir;
    private final String collideHome;
    private final VertxService service;
    private final String xapiRoot;
    private final StringTo<CachedResponse> xapiCache;

    public CollideServer(
        String bundledStaticFilesPrefix,
        String webRootPrefix,
        String workDir,
        String warDir,
        String collideHome
    ) {
        this(defaultApp(), bundledStaticFilesPrefix, webRootPrefix, workDir, warDir, collideHome);
    }

    private static WebApp defaultApp() {
        WebApp app = create(WebApp.class);

        // for our default app, we'll start with just
        // auth and a hello world page:
        final Route login = create(Route.class);
        login.setPath("/login");
        login.setRouteType(RouteType.Template);
        login.setPayload("<>");
        app.getRoute().add(login);


        return app;
    }

    public CollideServer(
        WebApp app,
        String bundledStaticFilesPrefix,
        String webRootPrefix,
        String workDir,
        String warDir,
        String collideHome
    ) {
        super(app);
        this.staticFiles = bundledStaticFilesPrefix;
        this.webRoot = webRootPrefix;
        this.workDir = workDir;
        this.warDir = warDir;
        this.collideHome = collideHome;
        this.xapiRoot = collideHome + "server/src/main/xapi/";
        this.service = X_Inject.singleton(VertxService.class);
        xapiCache = X_Collect.newStringMap(CachedResponse.class);
    }

    @Override
    protected void handleError(HttpServerRequest req, VertxResponse resp, Throwable error) {
        String path = req.path().replace("/xapi", "");
        if (path.isEmpty() || "/".equals(path)) {
            path = "index";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        Path file = Paths.get(xapiRoot + path + ".xapi");
        if (xapiCache.containsKey(path)) {
            // Use the cache, but check freshness on the file
            final CachedResponse cached = xapiCache.get(path);
            try {
                if (Files.getLastModifiedTime(file).compareTo(cached.timestamp) <= 0) {
                    // we can just serve the cached response
                    cached.result = serveContainer(req, cached.source);
                    if (cached.result) {
                        return;
                    }
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                X_Log.warn(CollideServer.class, "Unexpected IOException", e);
            }
        }
        try {
            String contents = X_String.join("\n", Files.readAllLines(file));
            final UiContainerExpr container = JavaParser.parseUiContainer(contents);
            final CachedResponse cached = new CachedResponse(path, container, Files.getLastModifiedTime(file));
            xapiCache.put(path, cached);
            cached.result = serveContainer(req, container);
            if (cached.result) {
                return;
            }
        } catch (ParseException | IOException e) {
            X_Log.error(getClass(), "Unable to load xapi files; expect bad things to happen", e);
        }

        // Nope, no xapi files to serve... let's 404
        super.handleError(req, resp, error);
    }


    private boolean serveContainer(HttpServerRequest req, UiContainerExpr container) {
        // create request scope and user scope.
        final RequestScope<VertxRequest, VertxResponse> scope = service.scope().requestScope(req);
        WebAppHandler visitor = new WebAppHandler(xapiRoot);
        return visitor.handle(scope, container);
    }
}
