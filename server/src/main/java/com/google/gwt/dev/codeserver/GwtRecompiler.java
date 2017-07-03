package com.google.gwt.dev.codeserver;

import collide.plugin.server.IsCompileThread;
import collide.plugin.server.ReflectionChannelTreeLogger;
import collide.plugin.server.gwt.CompilerBusyException;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.collide.server.shared.util.ReflectionChannel;
import xapi.gwtc.api.CompiledDirectory;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;

public class GwtRecompiler implements IsCompileThread<GwtRecompile> {

  private final class CompileThread extends Thread {
    String next;
    private RecompileController controller;
    private CompiledDirectory dir;
    private CompileResponseImpl status;

    @Override
    public void run() {
      while (!stopping) {
        try {
          String next = io.receive();
          if (next == null) {
            if (recompile) {
              next = this.next;
              recompile = false;
            }
          }
          if (next == null) {
            working = false;
            synchronized (compileThread) {
              try {
                compileThread.wait(2000);
              } catch (InterruptedException e) {
                Thread.interrupted();
              }
            }
          } else {
            working = true;
            // We have a pending gwt compile
            this.next = next;
            GwtRecompile request = GwtRecompileImpl.fromJsonString(next);
            try {
              compile(request);
            } catch (Exception e) {
              e.printStackTrace();
              working = false;
            }
          }
        } finally {
          working = false;
        }
      }
    }

    private void compile(GwtRecompile request) {
      assert module.equals(request.getModule()) : "GWT Compiler for "+module+
        " received request to compile "+request.getModule()+":\n"+request;
      // prepare a response to let the user know we are working
      CompileResponseImpl response;
      response = CompileResponseImpl.make();
      response.setCompilerStatus(CompilerState.RUNNING);

      Type logLevel = request.getLogLevel();
      if (logLevel != null)
        logger.setMaxDetail(logLevel);
      String key = request.getMessageKey() == null ? request.getModule() : request.getMessageKey();
      logger.setModule(key);
      response.setModule(key);
      response.setStaticName(module);

      io.send(response.toJson());

////      server.get();
//      controller = SuperDevUtil.getOrMakeController(
//          logger, request, request.getPort());
//      dir = controller.recompile();
//      // notify user we completed successfully
//      response.setCompilerStatus(CompilerState.FINISHED);
//      io.send(response.toJson());
//
//      // also notify our frontend that the compiled output has changed
//      // start or update a proxy server to pull source files from this
//      // compile.
//      synchronized (getClass()) {
//        status = response;
////        startOrUpdateProxy(dir, controller);
//      }
////      initialize(server.get(), server.getPort());
//
//      // This message is routed to WebFE
//      io.send("_frontend.symlink_" + dir.toString());
//
//      logger.log(Type.INFO, "Finished gwt compile for "
//          + controller.getModuleName());

      // reset interrupted flag so we loop back to the beginning
      Thread.interrupted();
    }
  }

  private CompileThread compileThread;
  private boolean working, recompile, stopping;
  private final String module;
  private ReflectionChannel io;
  private ReflectionChannelTreeLogger logger;

  public GwtRecompiler(String module) {
    this.module = module;
  }

  @Override
  public boolean isRunning() {
    return working;
  }

  @Override
  public boolean isStarted() {
    return compileThread != null && compileThread.isAlive();
  }

  @SuppressWarnings("deprecation")
  @Override
  public void kill() {
    recompile = false;
    stopping = true;
    if (compileThread != null) {
      compileThread.interrupt();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
      if (working) {
        compileThread.stop(new UnableToCompleteException());
      }
      compileThread = null;
    }
  }

  @Override
  public void doRecompile() {
    recompile = true;
  }

  @Override
  public void compile(String request) throws CompilerBusyException {
    assert compileThread != null : "You must call .setContextClassLoader() before invoking compile() in "
        + getClass().getName();
    if (!compileThread.isAlive()) {
      compileThread.start();
    } else {
      synchronized (compileThread) {
        compileThread.notify();
      }
    }
  }

  @Override
  public void setContextClassLoader(ClassLoader cl) {
    if (compileThread == null) {
      compileThread = new CompileThread();
      compileThread.setDaemon(true);
    }
    compileThread.setContextClassLoader(cl);
  }

  @Override
  public void setChannel(ClassLoader cl, Object io) {
    this.io = new ReflectionChannel(cl, io);
    logger = new ReflectionChannelTreeLogger(this.io, Type.INFO);
  }

}
