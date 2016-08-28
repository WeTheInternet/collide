package com.google.collide.server.shared.launcher;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class VertxLauncher extends LazyPojo<Vertx> {

  private static AtomicInteger unusedPort = new AtomicInteger(13370);
  private int port;

  @Override
  protected Vertx initialValue() {
    Vertx vertx = null;

    while (vertx == null) {
      synchronized (unusedPort) {
        int port = unusedPort.getAndAdd(1 + ((int)Math.random() * 20));
        try {
          vertx = Vertx.vertx();
          initialize(vertx, port);
          return vertx;
        } catch (Exception e) {
          vertx = null;
          e.printStackTrace();
          if (port > 15000) {
            synchronized (unusedPort) {
              unusedPort.set(10002);
            }
            return null;
          }
        }
      }
    }
    return null;
  }

  protected NetServer initialize(Vertx vertx, int port) {
    final NetServer server = vertx.createNetServer(new NetServerOptions()
      .setTcpKeepAlive(true)
      .setReuseAddress(true)
    );

    server
    .connectHandler(event->{
        //called
        event.exceptionHandler(Throwable::printStackTrace);
        event.closeHandler(e->X_Log.debug("Closed"));
        event.handler( buffer -> {
            X_Log.debug("Buffering");
            try{
              handleBuffer(event, buffer);
            }catch (Exception e) {
              e.printStackTrace();
            }
        });
        event.endHandler(e->X_Log.debug("Ending"));
    })
    .listen(port, "0.0.0.0");
    this.port = port;
    return server;

  }
  /**
   * @throws IOException
   */
  protected void handleBuffer(NetSocket event, Buffer buffer) throws IOException {
  }

  public int getPort() {
    return port;
  }

}
