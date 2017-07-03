package collide.vertx;

import collide.server.configuration.CollideOpts;
import io.vertx.core.Vertx;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import xapi.fu.In1;
import xapi.server.vertx.ScopeServiceVertx;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/3/16.
 */
public interface VertxService {

    HazelcastClusterManager clusterManager();

    Vertx vertx();

    ScopeServiceVertx scope();

    void initialize(CollideOpts opts, In1<VertxService> onDone);
}
