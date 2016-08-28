package com.google.collide.server.shared.launcher;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.impl.DefaultVertxFactory;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import xapi.inject.impl.LazyPojo;
import xapi.log.X_Log;

public class VertxLauncher extends LazyPojo<Vertx> {

  private static AtomicInteger unusedPort = new AtomicInteger(13370);
  private int port;

  @Override
  protected Vertx initialValue() {
    Vertx vertx = null;
    DefaultVertxFactory factory = new DefaultVertxFactory();
    while (vertx == null) {
      synchronized (unusedPort) {
        int port = unusedPort.getAndAdd(1 + ((int)Math.random() * 20));
        try {
          vertx = factory.createVertx();
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
    final NetServer server = vertx.createNetServer();

    server
    .setTCPKeepAlive(true)
    .connectHandler(new Handler<NetSocket>() {
      @Override
      public void handle(final NetSocket event) {
        //called
        event.exceptionHandler(new Handler<Exception>() {
          @Override
          public void handle(Exception event) {
            event.printStackTrace();
          }
        });
        event.closedHandler(new Handler<Void>() {
          @Override
          public void handle(Void event) {
            X_Log.debug("Closed");
          }
        });
        event.dataHandler(new Handler<Buffer>() {
          public void handle(Buffer buffer) {
            X_Log.debug("Buffering");
            try{
              handleBuffer(event, buffer);
            }catch (Exception e) {
              e.printStackTrace();
            }finally{
//              event.close();
            }
          }

        });
        event.endHandler(new Handler<Void>() {
          @Override
          public void handle(Void event) {
            X_Log.debug("Ending");
          }
        });
      }
    })
    .setReuseAddress(true)
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
