package collide.plugin.server.gwt;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

import xapi.collect.api.InitMap;

import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtKillImpl;
import com.google.collide.server.shared.util.Dto;

class GwtKillHandle implements Handler<Message<JsonObject>> {

  private final InitMap<String, GwtCompiler> compilers;

  GwtKillHandle(InitMap<String, GwtCompiler> compilers) {
    this.compilers = compilers;
  }

  @Override
  public void handle(Message<JsonObject> message) {
    String jsonString = Dto.get(message);
    GwtKillImpl killRequest = GwtKillImpl.fromJsonString(jsonString);
    String module = killRequest.getModule();
    System.err.println("Killing gwt compile " + module);
    if (compilers.containsKey(module)) {
      compilers.get(module).kill();
      compilers.removeValue(killRequest.getModule());
    }
    CompileResponseImpl reply =
        CompileResponseImpl.make().setModule(killRequest.getModule()).setCompilerStatus(
            CompilerState.UNLOADED);
    message.reply(Dto.wrap(reply));
  }
}
