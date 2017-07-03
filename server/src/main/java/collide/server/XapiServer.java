package collide.server;

import collide.server.handler.WebAppHandler;
import collide.vertx.VertxService;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.http.HttpServerRequest;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.scope.api.RequestScope;
import xapi.server.vertx.VertxRequest;
import xapi.util.X_String;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public class XapiServer {
    private final String staticFiles;
    private final String webRoot;
    private final String workDir;
    private final String warDir;
    private final String collideHome;
    private final VertxService service;
    private final String xapiRoot;

    public XapiServer(
        String bundledStaticFilesPrefix,
        String webRootPrefix,
        String workDir,
        String warDir,
        String collideHome
    ) {
        this.staticFiles = bundledStaticFilesPrefix;
        this.webRoot = webRootPrefix;
        this.workDir = workDir;
        this.warDir = warDir;
        this.collideHome = collideHome;
        this.xapiRoot = collideHome + "server/src/main/xapi/";
        this.service = X_Inject.singleton(VertxService.class);
    }

    public boolean serve(HttpServerRequest req) {
        String path = req.path().replace("/xapi", "");
        if (path.isEmpty() || "/".equals(path)) {
            path = "index";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String file = xapiRoot + path + ".xapi";
        try {
            String contents = X_String.join("\n", Files.readAllLines(Paths.get(file)));
            final UiContainerExpr container = JavaParser.parseUiContainer(contents);
            return serveContainer(req, container);
        } catch (ParseException | IOException e) {
            X_Log.error(getClass(), "Unable to load xapi files; expect bad things to happen", e);
        }
        return false;
    }

    private boolean serveContainer(HttpServerRequest req, UiContainerExpr container) {
        // create request scope and user scope.
        final RequestScope<VertxRequest> scope = service.scope().requestScope(req);
        WebAppHandler visitor = new WebAppHandler(xapiRoot);
        return visitor.handle(scope, container);
    }
}
