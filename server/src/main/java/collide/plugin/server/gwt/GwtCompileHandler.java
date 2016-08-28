package collide.plugin.server.gwt;

import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.server.shared.util.Dto;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import xapi.util.api.ReceivesValue;

import java.net.URL;
import java.util.ArrayList;

public class GwtCompileHandler implements Handler<Message<JsonObject>> {
  /**
   *
   */
  private final GwtServerPlugin gwtServerPlugin;

  /**
   * @param gwtServerPlugin
   */
  GwtCompileHandler(GwtServerPlugin gwtServerPlugin) {
    this.gwtServerPlugin = gwtServerPlugin;
  }

  @Override
  public void handle(Message<JsonObject> message) {
    String jsonString = Dto.get(message);
    GwtCompileImpl compileRequest = GwtCompileImpl.fromJsonString(jsonString);
    GwtCompiler compiler = gwtServerPlugin.compilers.get(compileRequest.getModule());
    // This is an initialization request, so we should create a new compile server
    if (compiler.isRunning()) {
      compiler.kill();
    }
    // Initialize new compiler
    final ArrayList<String> logMessages = new ArrayList<>();
    synchronized (this.gwtServerPlugin) {
      URL[] cp = this.gwtServerPlugin.getCompilerClasspath(compileRequest, new ReceivesValue<String>() {
        @Override
        public void set(String log) {
          logMessages.add(log);
        }
      }).toArray(new URL[0]);
      compiler.initialize(compileRequest, cp, this.gwtServerPlugin.getEventBus(), this.gwtServerPlugin.getAddressBase() + ".log");
    }

    compiler.compile(compileRequest);
    for (String item : logMessages) {
      compiler.log(item);
    }
  }
}
