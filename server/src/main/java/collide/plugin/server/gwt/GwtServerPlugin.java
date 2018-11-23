package collide.plugin.server.gwt;

import collide.plugin.server.AbstractPluginServer;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.log.X_Log;
import xapi.util.X_Namespace;

import java.util.HashMap;
import java.util.Map;

public class GwtServerPlugin extends AbstractPluginServer //<GwtCompilerThread>
{

  public GwtServerPlugin() {
    if (null == System.getProperty(X_Namespace.PROPERTY_MULTITHREADED)) {
      int max = Runtime.getRuntime().availableProcessors()*2+2;
      X_Log.trace(getClass(), "Setting max threads to "+max);
      System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, Integer.toString(max));
    }
  }

  @Override
  public String getAddressBase() {
    return "gwt";
  }

  final InitMap<String, GwtCompiler> compilers = new InitMapDefault<>(
      In1Out1.identity(),
      GwtCompiler::new);

  private final Lazy<Map<String, Handler<Message<JsonObject>>>> allModules =
      Lazy.deferred1(() -> {
          Map<String, Handler<Message<JsonObject>>> map = new HashMap<>();

          map.put("recompile", new GwtRecompileHandler(GwtServerPlugin.this));
          map.put("compile", new GwtCompileHandler(GwtServerPlugin.this));
          map.put("test", new GwtTestRunHandler(GwtServerPlugin.this));
          map.put("settings", new GwtSettingsHandler());
          map.put("kill", new GwtKillHandle(compilers));
          map.put("save", new GwtSaveHandler());

          return map;
      });

  @Override
  public Map<String, Handler<Message<JsonObject>>> getHandlers() {
    return ImmutableMap.copyOf(allModules.out1());
  }

}
