package collide.server.configuration;

import xapi.args.ArgHandlerPath;
import xapi.fu.In1;
import xapi.fu.Out1;

import java.nio.file.Path;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/29/16.
 */
public abstract class EditorHomeHandler extends ArgHandlerPath {

    public static EditorHomeHandler handle(In1<Path> consumer) {
        return new EditorHomeHandler() {
            @Override
            public void setPath(Path path) {
                consumer.in(path);
            }
        };
    }

    @Override
    public Out1<String>[] getDefaultArgs() {
        return new Out1[]{
            ()->"-" + getTag(), ()->"/opt/collide"
        };
    }

    @Override
    public String getPurpose() {
        return "The directory to display in the editor\n" +
            "Default is $COLLIDE_HOME (or the directory where CollIDE is compiled)";
    }

    @Override
    public String getTag() {
        return "files";
    }
}
