package collide.server.handler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.log.X_Log;
import xapi.scope.api.RequestScope;
import xapi.server.gen.WebAppGenerator;
import xapi.server.vertx.VertxRequest;
import xapi.time.X_Time;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public class WebAppHandler {

    private final String xapiRoot;

    public WebAppHandler(String xapiRoot) {
        this.xapiRoot = xapiRoot;
    }

    public boolean handle(RequestScope<VertxRequest> scope, UiContainerExpr app) {
        X_Time.runLater(()->{
            WebAppGenerator generator = new WebAppGenerator();
            generator.generateWebApp("root", app);
            X_Log.warn(getClass(), scope, app);
        });
        return true;
    }
}
