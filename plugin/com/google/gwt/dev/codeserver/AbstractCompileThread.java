package com.google.gwt.dev.codeserver;

import java.io.IOException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import com.google.collide.dto.CodeModule;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.plugin.server.gwt.CompilerBusyException;
import com.google.collide.plugin.server.gwt.CompilerRunner;
import com.google.collide.plugin.shared.CompiledDirectory;
import com.google.collide.plugin.shared.IsCompiler;
import com.google.collide.server.shared.launcher.VertxLauncher;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;

public abstract class AbstractCompileThread
<CompileType extends CodeModule>
extends Thread
implements CompilerRunner

{

  protected ReflectionChannel io;
  protected boolean working = false;
  protected int port;

  protected IsCompiler controller;

  protected final VertxLauncher server = new VertxLauncher() {
    @Override
    protected NetServer initialize(Vertx vertx, int port) {
      AbstractCompileThread.this.port = port;
      return super.initialize(vertx, port);
    }
    @Override
    protected void handleBuffer(NetSocket event, Buffer buffer) throws IOException {
      AbstractCompileThread.this.handleBuffer(event, buffer);
    };

  };
  protected CompiledDirectory compileRequest;
  protected CompileResponseImpl status;


  protected boolean isFatal(Exception e) {
    return true;
  }

  protected abstract void handleBuffer(NetSocket event, Buffer buffer) throws IOException;

  protected void initialize(Vertx vertx, int port) {
    //called when this compile is open for business
    if (status == null)
      return;
    status.setCompilerStatus(CompilerState.SERVING);
    status.setPort(port);
    io.send(status.toJson());
  }

  protected synchronized void startOrUpdateProxy(CompiledDirectory impl, IsCompiler compiler) {
    this.compileRequest = impl;
    this.controller = compiler;
    System.out.println("Starting plugin thread "+getClass());
    server.get()//performs actual initialization in the LazyPojo class
    .setTimer(1000, new Handler<Long>() {
      @Override
      public void handle(Long event) {
        //keep heap clean; the generated gwt classes will fill permgen quickly if they survive;
        System.gc();
      }
    });

    System.out.println("Started plugin thread on port "+server.getPort());
    impl.setPort(server.getPort());
  }

  @Override
  public void setChannel(ClassLoader cl, Object io){
    this.io = new ReflectionChannel(cl, io);
  }
  @Override
  public void setOnDestroy(Object runOnDestroy){
    assert io != null : "You must call .setChannel() before calling .setOnDestroy()." +
        "  Called from "+DebugUtil.getCaller();
    this.io.setOnDestroy(runOnDestroy);
  }

  public void compile(String request) throws CompilerBusyException{
    if (working)
      throw new CompilerBusyException(status.getModule());
    synchronized (getClass()) {
      if (isAlive()){
        //if we're already running, we should notify so we can continue working.
          getClass().notify();//wake up!
      }
      working = true;
      if (!isAlive()){
        start();
      }
    }
  }

}