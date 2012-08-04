package com.google.collide.server.maven;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.collide.dto.server.DtoServerImpls.GwtStatusImpl;
import com.google.collide.dto.server.DtoServerImpls.MavenConfigImpl;
import com.google.collide.server.shared.util.Dto;

public class MavenController extends BusModBase{

  
  private String addressBase;

  @Override
  public void start() {
    super.start();
    this.addressBase = getOptionalStringConfig("address", "maven");
    vertx.eventBus().registerHandler(addressBase+".save", new Handler<Message<JsonObject>>() {
      @Override
      public void handle(Message<JsonObject> message) {
        System.out.println(message.body);
        MavenConfigImpl cfg = MavenConfigImpl.make();
        
        message.reply(Dto.wrap(cfg));
        
        vertx.eventBus().send("gwt.status", Dto.wrap(GwtStatusImpl.make().setModule("grrrawr!")));
      };
    });
    
  }
}
