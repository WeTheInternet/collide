package com.google.gwt.dev.codeserver;

import collide.plugin.server.AbstractCompileThread;
import collide.plugin.server.IsCompileThread;
import collide.plugin.server.ReflectionChannelTreeLogger;
import collide.plugin.server.gwt.CompilerBusyException;
import collide.server.configuration.CollideOpts;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtRecompileImpl;
import com.google.collide.server.shared.launcher.VertxLauncher;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import xapi.collect.X_Collect;
import xapi.dev.gwtc.impl.GwtcManifestImpl;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.time.X_Time;

import java.io.IOException;
import java.util.HashMap;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

public final class GwtCompilerThread extends AbstractCompileThread<GwtRecompile>
implements IsCompileThread<GwtRecompile> {

  private GwtCompilationServer compileServer;

  public GwtCompilerThread() {
  }
  public GwtCompilerThread(String module) {
    messageKey = this.module = module;
  }

  private final HashMap<String, CompiledDirectory> modules = new HashMap<>();
  ReflectionChannelTreeLogger logger;
  private boolean started, recompile;

  @Override
  protected TreeLogger logger() {
    return logger == null ? new PrintWriterTreeLogger() : logger;
  }
  RecompileController controller;
  private String module;
  private String messageKey;

  // these are native objects, created using reflection
  @Override
  public void run() {
    while (!Thread.interrupted())
      try {
        // grab our request from originating thread
        String compileRequest = io.receive();
        if (compileRequest == null) {
          working = false;
          // no request means we should just sleep for a while
          try {
            synchronized (GwtCompilerThread.class) {
              GwtCompilerThread.class.wait(20000);
            }
          } catch (InterruptedException e) {// wake up!
            Thread.interrupted();
          }
          continue;
        }
        working = true;
        GwtRecompile request = GwtRecompileImpl.fromJsonString(compileRequest);
        module = request.getModule();
        messageKey = request.getMessageKey() == null ? module : request.getMessageKey();
        // prepare a response to let the user know we are working
        final CompileResponseImpl response = CompileResponseImpl.make();
        response.setModule(messageKey).setStaticName(module);
        response.setCompilerStatus(CompilerState.RUNNING);

        Type logLevel = request.getLogLevel();
        if (logLevel != null)
          logger.setMaxDetail(logLevel);
        logger.setModule(messageKey);

        io.send(response.toJson());

        server.get();

        controller = SuperDevUtil.getOrMakeController(
            logger, toManifest(request));

        CompiledDirectory dir = controller.recompile();

        modules.put(messageKey, dir);
        // notify user we completed successfully
        response.setCompilerStatus(CompilerState.FINISHED);

        try {
          // also notify our frontend that the compiled output has changed
          // start or update a proxy server to pull source files from this
          // compile.
          synchronized (getClass()) {
            status = response;
            startOrUpdateProxy(dir, controller);
          }
          initialize(server.get(), server.getPort());
        } finally {
          final String status = response.toJson();
          X_Time.runLater(new Runnable() {
            @Override
            public void run() {
              X_Time.trySleep(500, 0);
              io.send(status);
            }
          });
        }


        // This message is routed to WebFE
        io.send("_frontend.symlink_" + dir.toString());

        logger.log(Type.INFO, "Finished gwt compile for "
            + request.getModule());

        // reset interrupted flag so we loop back to the beginning
        Thread.interrupted();

      } catch (Throwable e) {
        System.out.println("Exception caught...");
        logger.log(Type.ERROR, "Error encountered during compile : " + e);

        Throwable cause = e;
        while (cause != null) {
          for (StackTraceElement trace : cause.getStackTrace())
            logger.log(Type.ERROR, trace.toString());
          cause = cause.getCause();
        }
        if (status == null) {
          status = CompileResponseImpl.make();
          status.setModule(messageKey);
          status.setStaticName(module);
        }
        status.setCompilerStatus(CompilerState.FAILED);
        io.send(status.toJson());
        if (isFatal(e))
          try {
            logger.log(Type.INFO, "Destroying thread " + getClass());
            io.destroy();
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        return;
      } finally {
        working = false;
      }
  }

  private GwtManifest toManifest(GwtRecompile request) {
    GwtManifest manifest = new GwtcManifestImpl(request.getModule());
    manifest.setPort(request.getPort());
    manifest.setAutoOpen(request.getAutoOpen());
    manifest.setSources(X_Collect.asList(String.class, request.getSources().asIterable()));
    manifest.setDependencies(X_Collect.asList(String.class, request.getDependencies().asIterable()));
    manifest.setLogLevel(request.getLogLevel());
    manifest.setExtraArgs(X_Collect.asList(String.class, request.getExtraArgs().asIterable()));
    manifest.setObfuscationLevel(request.getObfuscationLevel());
    manifest.setOpenAction(request.getOpenAction());
    manifest.setRecompile(request.isRecompile());
    manifest.setWarDir(CollideOpts.getOpts().getStaticFiles().toString());
    return manifest;
  }

  protected boolean isFatal(Throwable e) {
    return true;
  }

  @Override
  public void setChannel(ClassLoader cl, Object io) {
    ReflectionChannel channel = new ReflectionChannel(cl, io);
    this.io = channel;
    logger = new ReflectionChannelTreeLogger(channel, Type.INFO);
  }

  @Override
  public void setOnDestroy(Object runOnDestroy) {
    assert io != null : "You must call .setChannel() before calling .setOnDestroy()."
        + "  Called from " + DebugUtil.getCaller();
    this.io.setOnDestroy(runOnDestroy);
  }

  @Override
  public void compile(String request) throws CompilerBusyException {
    if (working)
      throw new CompilerBusyException(GwtRecompileImpl.fromJsonString(request).getModule());
    synchronized (GwtCompilerThread.class) {
      working = true;
      try {
        if (!isAlive()) {
          start();
        }
      } catch (Exception e) {
        logger().log(Type.ERROR, "Fatal error trying to start gwt compiler", e);
        try {
          io.destroy();
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      } finally {
        // wake everyone up to check if they have work to do
        getClass().notify();
      }
    }
  }

  @Override
  protected void handleBuffer(NetSocket event, Buffer buffer)
      throws IOException {
    // called when this compile is open for business
    // status.setCompilerStatus(CompileStatus.SERVING);
    // io.send(status.toJson());

    compileServer = new GwtCompilationServer();
    compileServer.handleBuffer(event, buffer, controller, modules::get);
  }

  @Override
  public boolean isRunning() {
    return working;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

  @Override
  public void kill() {
    if (controller != null) {
      controller.cleanup();
    }
  }

  @Override
  public void doRecompile() {
    recompile = true;
  }

}
