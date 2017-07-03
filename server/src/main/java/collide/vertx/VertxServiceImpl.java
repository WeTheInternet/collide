package collide.vertx;

import collide.server.StartServer;
import collide.server.configuration.CollideOpts;
import com.hazelcast.config.Config;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import xapi.annotation.inject.SingletonDefault;
import xapi.fu.In1;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.scope.service.ScopeService;
import xapi.server.vertx.ScopeServiceVertx;
import xapi.time.X_Time;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/3/16.
 */
@SingletonDefault(implFor = VertxService.class)
public class VertxServiceImpl implements VertxService {

    private static final VertxService IMPL = X_Inject.singleton(VertxService.class);
    private final ScopeServiceVertx scope;
    private Vertx vertx;
    private HazelcastClusterManager manager;
    private CollideOpts opts;

    public VertxServiceImpl() {
        scope = (ScopeServiceVertx)X_Inject.instance(ScopeService.class);
    }

    public static void service(CollideOpts opts, In1<VertxService> service) {
        IMPL.initialize(opts, service);
    }

    @Override
    public void initialize(CollideOpts opts, In1<VertxService> onDone) {
        this.opts = opts;
        final Config custerOpts = new Config("collide");
        manager = new HazelcastClusterManager(custerOpts);

        final VertxOptions vertxOpts = new VertxOptions();
        initializeOpts(opts, vertxOpts);
        if (opts.isClustered()) {
            vertxOpts.setClusterManager(manager);

            Vertx.clusteredVertx(vertxOpts, async->{
                if (async.succeeded()) {
                    vertx = async.result();
                    onDone.in(VertxServiceImpl.this);
                } else {
                    X_Log.error(StartServer.class, "Failed to start node", async.cause());
                    throw new IllegalStateException("Vertx Unable to start", async.cause());
                }
            });
        } else {
            X_Time.runLater(()->{
                vertx = Vertx.vertx(vertxOpts);
                onDone.in(this);
            });
        }
    }

    protected void initializeOpts(CollideOpts opts, VertxOptions vertxOpts) {
        vertxOpts.setMetricsOptions(new MetricsOptions().setEnabled(true));
    }

    @Override
    public HazelcastClusterManager clusterManager() {
        return manager;
    }

    @Override
    public Vertx vertx() {
        return vertx;
    }

    @Override
    public ScopeServiceVertx scope() {
        return scope;
    }
}
