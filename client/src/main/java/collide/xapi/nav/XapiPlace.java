package collide.xapi.nav;

import collide.gwtc.ui.GwtCompilePlace;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.client.DtoClientImpls.GwtRecompileImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/26/16.
 */
public class XapiPlace extends Place {

    public static final XapiPlace PLACE = new XapiPlace();

    public class NavigationEvent extends PlaceNavigationEvent<XapiPlace> {
        public static final String MODULE_KEY = "m";
        public static final String SRC_KEY = "s";
        public static final String DEPS_KEY = "d";

        private final String module;
        private final JsoArray<String> srcDir;
        private final JsoArray<String> depsDir;
        private final boolean recompile;

        private NavigationEvent(GwtRecompile module) {
            super(XapiPlace.this);
            this.module = module.getModule();
            this.recompile = module.isRecompile();
            if (module.getSources() == null) {
                this.srcDir = JsoArray.create();
                this.depsDir = JsoArray.create();
            } else {
                this.srcDir = JsoArray.from(module.getSources());
                this.depsDir = JsoArray.from(module.getDependencies());
            }
        }

        @Override
        public JsonStringMap<String> getBookmarkableState() {
            JsoStringMap<String> map = JsoStringMap.create();
            map.put(MODULE_KEY, module);
            map.put(SRC_KEY, srcDir.join("::"));
            map.put(DEPS_KEY, depsDir.join("::"));
            return map;
        }

        public String getModule() {
            return module;
        }

        public boolean isRecompile() {
            return recompile;
        }

        public JsoArray<String> getSourceDirectory() {
            return srcDir;
        }

        public JsoArray<String> getLibsDirectory() {
            return depsDir;
        }
    }

    protected XapiPlace() {
        super(PlaceConstants.XAPI_PLACE_NAME);
    }

    @Override
    public PlaceNavigationEvent<? extends Place> createNavigationEvent(JsonStringMap<String> decodedState) {
        String srcDir = decodedState.get(XapiPlace.NavigationEvent.SRC_KEY);
        if (srcDir == null) {
            srcDir = "";
        }
        String libDir = decodedState.get(XapiPlace.NavigationEvent.DEPS_KEY);
        if (libDir == null) {
            libDir = "";
        }
        String module = decodedState.get(XapiPlace.NavigationEvent.MODULE_KEY);

        GwtRecompileImpl compile = GwtRecompileImpl.make();
        compile.setModule(module);
        JsoArray<String>
            array = JsoArray.splitString(srcDir, "::");
        compile.setSources(array);
        array = JsoArray.splitString(libDir, "::");
        compile.setDependencies(array);
        return new NavigationEvent(compile);
    }
}
