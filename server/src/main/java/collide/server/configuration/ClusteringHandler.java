package collide.server.configuration;

import xapi.args.ArgHandlerFlag;
import xapi.fu.In1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/29/16.
 */
public abstract class ClusteringHandler extends ArgHandlerFlag {

    public static ClusteringHandler handle(In1<Boolean> consumer) {
        return new ClusteringHandler() {

            @Override
            public boolean setFlag() {
                consumer.in(true);
                return true;
            }
        };
    }

    @Override
    public String getPurpose() {
        return "Whether to run with or without clustering (slower startup).";
    }

    @Override
    public String getTag() {
        return "clustered";
    }
}
