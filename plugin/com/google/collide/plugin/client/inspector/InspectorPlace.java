package com.google.collide.plugin.client.inspector;

import xapi.inject.impl.LazyPojo;
import xapi.util.X_String;

import com.google.collide.client.CollideSettings;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceConstants;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.client.DtoClientImpls.GwtCompileImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;
import elemental.client.Browser;
import elemental.dom.Node;
import elemental.dom.NodeList;

public class InspectorPlace extends Place{


  public class NavigationEvent extends PlaceNavigationEvent<InspectorPlace> {
    public static final String MODULE_KEY = "m";
    public static final String SRC_KEY = "s";
    public static final String DEPS_KEY = "d";

    private final String module;
    private final JsoArray<String> srcDir;
    private final JsoArray<String> depsDir;

    private NavigationEvent(GwtCompile module) {
      super(InspectorPlace.this);
      this.module = module.getModule();
      this.srcDir = JsoArray.from(module.getSrc());
      this.depsDir = JsoArray.from(module.getDeps());
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

    public JsoArray<String> getSourceDirectory() {
      return srcDir;
    }

    public JsoArray<String> getLibsDirectory() {
      return depsDir;
    }
  }

  public static final InspectorPlace PLACE = new InspectorPlace();

  private InspectorPlace() {
    super(PlaceConstants.INSPECTOR_PLACE_NAME);
  }

  private final LazyPojo<String> guessModuleFromHostPage
    = new LazyPojo<String>(){
      @Override
      protected String initialValue() {

        // first try guessing from values embedded in page
        String module = CollideSettings.get().getModule();
        if (!X_String.isEmptyTrimmed(module))return module.trim();
        // else, look for a script tage
        NodeList kids = Browser.getDocument().getHead().getChildNodes();
        int limit = kids.getLength();
        // search backwards, as gwt .nocache.js generally comes after other js imports
        while(limit-->0){
          Node n = kids.item(limit);
          if (n.getNodeName().equalsIgnoreCase("script")){
            Node srcAttr = n.getAttributes().getNamedItem("src");
            if (srcAttr == null) continue;
            String src = srcAttr.getNodeName();
            if (src.contains(".nocache.js")){
              // we have a winner!
              src = src.substring(src.lastIndexOf('/')+1);
              // TODO if nocache name is shortened, ask server to find correct gwt.xml.
              return src.substring(0, src.length() - 11);
            }
          }
        }

        return null;
      };
    };

  @Override
  public PlaceNavigationEvent<? extends Place> createNavigationEvent(
      JsonStringMap<String> decodedState) {
    String srcDir = decodedState.get(NavigationEvent.SRC_KEY);
    if (srcDir == null) {
      srcDir = "";
    }
    String libDir = decodedState.get(NavigationEvent.DEPS_KEY);
    if (libDir == null) {
      libDir = "";
    }
    String module = decodedState.get(NavigationEvent.MODULE_KEY);
    if (module == null){
      //guess our own module source
      module = guessModuleFromHostPage.get();
    }
    GwtCompileImpl compile = GwtCompileImpl.make();
    compile.setModule(module);
    JsoArray<String>
    array = JsoArray.splitString(srcDir, "::");
    compile.setSrc(array);
    array = JsoArray.splitString(libDir, "::");
    compile.setDeps(array);
    return new NavigationEvent(compile);
  }



  /**
   * @param module the gwt module to compile
   * @return a new navigation event
   */
  public PlaceNavigationEvent<InspectorPlace> createNavigationEvent(GwtCompile compile) {
    return new NavigationEvent(compile);
  }


}
