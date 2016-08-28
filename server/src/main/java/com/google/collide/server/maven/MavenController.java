package com.google.collide.server.maven;

import com.google.collide.dto.server.DtoServerImpls.MavenConfigImpl;
import com.google.collide.server.shared.BusModBase;
import com.google.collide.server.shared.util.Dto;

public class MavenController extends BusModBase{


  private String addressBase;

  @Override
  public void start() {
    super.start();
    this.addressBase = getOptionalStringConfig("address", "maven");
    vertx.eventBus().consumer(addressBase+".save", message -> {
        System.out.println(message.body());
        MavenConfigImpl cfg = MavenConfigImpl.make();

        message.reply(Dto.wrap(cfg));

//        vertx.eventBus().send("gwt.status", Dto.wrap(GwtStatusImpl.make().setModule("grrrawr!")));
    });

  }
}
