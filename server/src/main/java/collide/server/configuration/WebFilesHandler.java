package collide.server.configuration;

import xapi.args.ArgHandlerPath;
import xapi.fu.In1;

import java.nio.file.Path;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/29/16.
 */
public abstract class WebFilesHandler extends ArgHandlerPath {

    public static WebFilesHandler handle(In1<Path> consumer) {
        return new WebFilesHandler() {
            @Override
            public void setPath(Path path) {
                consumer.in(path);
            }
        };
    }

    @Override
    public String getPurpose() {
        return "The /static/ web files to serve for CollIDE.\n" +
            "Default is $COLLIDE_HOME/client/build/putnami/out/Collide";
    }

    @Override
    public String getTag() {
        return "web";
    }
}
