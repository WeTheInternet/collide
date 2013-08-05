package com.google.gwt.dev.codeserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.filters.StringInputStream;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import xapi.log.X_Log;
import xapi.util.impl.StringId;

import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.server.DtoServerImpls.CompileResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.plugin.server.gwt.CompilerBusyException;
import com.google.collide.plugin.shared.CompiledDirectory;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class GwtCompilerThread extends AbstractCompileThread<GwtCompile> {

  private final class RunningModule extends StringId {
    GwtCompile compile;
    CompiledDirectory dir;

    public RunningModule(String module) {
      super(module);
    }

  }

  private final HashMap<String, CompiledDirectory> modules = new HashMap<>();

  // these are native objects, created using reflection
  @Override
  public void run() {
    ReflectionChannelTreeLogger logger = new ReflectionChannelTreeLogger(io,
        Type.INFO);
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
        GwtCompile request = GwtCompileImpl.fromJsonString(compileRequest);

        // prepare a response to let the user know we are working
        CompileResponseImpl response;
        response = CompileResponseImpl.make();
        response.setModule(request.getModule());
        response.setCompilerStatus(CompilerState.RUNNING);

        Type logLevel = request.getLogLevel();
        if (logLevel != null)
          logger.setMaxDetail(logLevel);
        logger.setModule(request.getModule());

        io.send(response.toJson());

        server.get();
        RecompileController controller = SuperDevUtil.getOrMakeController(
            logger, request, 8080
        // server.getPort()
            );
        CompiledDirectory dir = controller.recompile();
        modules.put(request.getModule(), dir);
        // notify user we completed successfully
        response.setCompilerStatus(CompilerState.FINISHED);
        io.send(response.toJson());

        // also notify our frontend that the compiled output has changed
        // start or update a proxy server to pull source files from this
        // compile.
        synchronized (getClass()) {
          status = response;
          startOrUpdateProxy(dir, controller);
        }
        initialize(server.get(), server.getPort());

        // This message is routed to WebFE
        io.send("_frontend.symlink_" + dir.toString());

        System.out.println("Finished gwt compile for "
            + controller.getModuleName());

      } catch (Throwable e) {
        System.out.println("Exception caught...");
        logger.log(Type.ERROR, "Error encountered during compile : " + e);
        
        Throwable cause = e;
        while (cause != null) {
          for (StackTraceElement trace : cause.getStackTrace())
            logger.log(Type.ERROR, trace.toString());
          cause = cause.getCause();
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

  protected boolean isFatal(Throwable e) {
    return true;
  }

  @Override
  public void setChannel(ClassLoader cl, Object io) {
    this.io = new ReflectionChannel(cl, io);
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
      throw new CompilerBusyException(GwtCompileImpl.fromJsonString(request)
          .getModule());
    synchronized (GwtCompilerThread.class) {
      if (isAlive()) {
        // if we're already running, we should notify so we can continue
        // working.
        getClass().notify();// wake up!
      }
      working = true;
      if (!isAlive()) {
        try {
          start();
        } catch (Exception e) {
          X_Log.error("Fatal error trying to start gwt compiler", e);
          try {
            io.destroy();
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        }
      }
    }
  }

  @Override
  protected void handleBuffer(NetSocket event, Buffer buffer)
      throws IOException {
    // called when this compile is open for business
    // status.setCompilerStatus(CompileStatus.SERVING);
    // io.send(status.toJson());
    //

    int len = buffer.length();
    String got = buffer.getString(0, len);
    Map<String, String> headers = new HashMap<>();
    for (String line : got.split("\n")) {
      int ind = line.indexOf(' ');
      if (ind > 0)
        headers.put(line.substring(0, ind), line.substring(ind + 1));
    }
    if (got.contains("sourcemaps")) {
      got = got.split("sourcemaps[/]")[1].split("\\s")[0];
    }
    // if requesting gwtSourceMap.json, we must resolve the compiled directory
    if (got.endsWith("gwtSourceMap.json")) {
      String module = got.split("/")[0];
      CompiledDirectory dir = modules.get(module);
      File extras = new File(dir.getSourceMapDir());
      if (!extras.exists()) {
        throw new RuntimeException("Can't find symbolMaps dir for " + module);
      }
      File[] sourceMapFiles = extras.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.matches(".*_sourceMap.*[.]json");
        }
      });
      File winner = sourceMapFiles[0];
      if (sourceMapFiles.length > 1) {
        // more than one permutation; we need to look up permutation map
        // we have to do browser-sniffing using User-Agent header; ick
        String userAgent = headers.get("User-Agent:").toLowerCase();
        String strongName;
        Map<String, String> permutations = dir.getUserAgentMap();
        if (userAgent.contains("safari")) {
          strongName = permutations.get("safari");
        } else if (userAgent.indexOf("gecko") > 0) {
          strongName = permutations.get("gecko1_8");
        } else {
          strongName = permutations.get("ie9");
          if (strongName == null)
            strongName = permutations.get("ie8");
          if (strongName == null)
            strongName = permutations.get("ie6");
        }
        // see if we've found the permutation we're looking for
        if (strongName != null) {
          for (File candidate : sourceMapFiles) {
            if (candidate.getName().startsWith(strongName)) {
              winner = candidate;
              break;
            }
          }
        }
      } else if (sourceMapFiles.length == 0) {
        // no compiled sourcemaps. Fail.
        throw new RuntimeException("No sourcemap files found in "
            + extras.getCanonicalPath());
      }
      stream(event, new FileInputStream(winner));
      // event.sendFile(winner.getCanonicalPath());
    } else {
      // assume non-sourcemap request wants a java / resource file.
      URL res = controller.getResourceLoader().getResource(got);
      if (res == null) {
        System.err.println("Proxy only supports sourcemaps " + got);
        stream(event, new StringInputStream(
            "<pre>Proxy only supports sourcemaps. You sent: " + got + "</pre>"));
      } else {
        stream(event, res.openStream());
      }
    }
  }

  private void stream(final NetSocket event, InputStream in) throws IOException {
    byte[] buff;
    Buffer b = new Buffer();
    try {
      while (true) {
        buff = new byte[in.available() + 4096];
        int bytesRead = in.read(buff);
        if (bytesRead == -1) {
          return;
        }
        if (buff.length == bytesRead)
          b.appendBytes(buff);
        else
          b.appendBytes(Arrays.copyOf(buff, bytesRead));
      }
    } finally {
      in.close();
      buff = null;
      event.write(b, new Handler<Void>() {
        @Override
        public void handle(Void ev) {
          event.close();
        }
      });
    }
  }

}