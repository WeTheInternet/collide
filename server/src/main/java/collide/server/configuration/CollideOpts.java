package collide.server.configuration;

import xapi.args.ArgProcessorBase;

import java.nio.file.Path;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/29/16.
 */
public class CollideOpts extends ArgProcessorBase {

    private static CollideOpts SINGLETON = new CollideOpts();

    Path editorHome;
    Path staticFiles;
    boolean clustered;

    private CollideOpts() {
        registerHandler(EditorHomeHandler.handle(p->editorHome=p));
        registerHandler(WebFilesHandler.handle(p->staticFiles=p));
        registerHandler(ClusteringHandler.handle(p->clustered=p));
    }

    public static CollideOpts getOpts() {
        return SINGLETON;
    }

    public Path getEditorHome() {
        return editorHome;
    }

    public boolean isClustered() {
        return clustered;
    }

    public Path getStaticFiles() {
        if (staticFiles == null) {
            staticFiles = getEditorHome().resolve("client/build/putnami/out/Demo");
            System.out.println(staticFiles);
        }
        return staticFiles;
    }

}
