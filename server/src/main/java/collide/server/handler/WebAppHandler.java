package collide.server.handler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.log.X_Log;
import xapi.scope.request.RequestScope;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;
import xapi.time.X_Time;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public class WebAppHandler {

    private final String xapiRoot;

    public WebAppHandler(String xapiRoot) {
        this.xapiRoot = xapiRoot;
    }

    public boolean handle(RequestScope<VertxRequest, VertxResponse> scope, UiContainerExpr app) {
        X_Time.runLater(()->{
            X_Log.warn(getClass(), scope, app);

        });
        return true;
    }
}
