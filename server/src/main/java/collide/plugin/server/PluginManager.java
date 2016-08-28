package collide.plugin.server;

import com.google.collide.server.shared.BusModBase;
import io.vertx.core.json.JsonArray;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginManager extends BusModBase{

  Logger log = Logger.getLogger(getClass().getSimpleName());

  private JsonArray plugins;

  @Override
  public void start() {
    super.start();

    //now register all requested plugins
    this.plugins = getOptionalArrayConfig("plugins", new JsonArray());
    Iterator<Object> iter = plugins.iterator();
    while(iter.hasNext()){
      Object next = iter.next();
      String name = String.valueOf(next);
      //create the requested plugin through magic naming + reflection.
      //convention: package.of.PluginManager.pluginname.PluginnamePlugin.java
      String qualifiedName = getClass().getName().split(getClass().getSimpleName())[0];
      qualifiedName = qualifiedName + name.toLowerCase()+"."+Character.toUpperCase(name.charAt(0))+name.substring(1);
      ServerPlugin plugin;
      //we're doing magic-naming lookup for plugins,
      //so we need to just eat & log exceptions
      try{
        Class<?> cls;
        //first, try without Plugin suffix, in case packagename = classname
        try{
          cls = Class.forName(qualifiedName, true, getClass().getClassLoader());
        }catch (Exception e) {
          //okay, try with Plugin suffix...
          cls = Class.forName(qualifiedName+"Plugin", true, getClass().getClassLoader());
        }
        Object create = cls.newInstance();
        plugin = (ServerPlugin) create;
      }catch (Exception e) {
        log.log(Level.SEVERE, "Error installing plugin for "+next+".  " +
        		"Please ensure that class "+qualifiedName+" exists on classpath, " +
        				"and that this class implements "+ServerPlugin.class.getName());
        continue;
      }
      install(plugin);
    }
  }

  private void install(ServerPlugin plugin) {
    plugin.initialize(vertx);
  }
}
