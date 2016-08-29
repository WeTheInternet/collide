package collide.server.codegraph;

import com.google.collide.dto.server.DtoServerImpls.CodeGraphResponseImpl;
import com.google.collide.server.shared.util.Dto;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/29/16.
 */
public class CodeGraphMonitor extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.eventBus().<JsonObject>consumer("codegraph.get",
            request->{
               request.reply(Dto.wrap(CodeGraphResponseImpl
                   .make())
               );
            });
        super.start();
    }
}
