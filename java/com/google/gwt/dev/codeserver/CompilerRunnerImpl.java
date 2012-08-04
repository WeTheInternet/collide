package com.google.gwt.dev.codeserver;

import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtStatus.CompileStatus;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;
import com.google.collide.dto.server.DtoServerImpls.GwtStatusImpl;
import com.google.collide.server.plugin.gwt.CompilerBusyException;
import com.google.collide.server.plugin.gwt.CompilerRunner;
import com.google.collide.server.plugin.gwt.CrossThreadVertxChannel;
import com.google.collide.server.plugin.gwt.GwtCompiledDirectory;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;

public final class CompilerRunnerImpl extends Thread implements CompilerRunner{
  
  private ReflectionChannel io;
  private boolean working = false;
  //these are native objects, created using reflection
  @Override
  public void run() {
    System.out.println("run");
    while(!Thread.interrupted())
    try {
      //grab our request from originating thread
      String compileRequest = io.receive();
      if (compileRequest == null){
        working = false;
        //no request means we should just sleep for a while
        try{
          Thread.sleep(20000);
        }catch (InterruptedException e) {//wake up!
        }
        continue;
      }
      working = true;
      System.out.println(compileRequest);
      GwtCompile request = GwtCompileImpl.fromJsonString(compileRequest);
      
      //prepare a response to let the user know we are working
      GwtStatusImpl response = GwtStatusImpl.make();
      response.setModule(request.getModule());
      response.setCompilerStatus(CompileStatus.RUNNING);
      io.send(response.toJson());
      
      //compile the gwt module
      TreeLogger logger = new ReflectionChannelTreeLogger(io, Type.INFO);
      RecompileController controller = SuperDevUtil.getOrMakeController(logger,request);
      CompileDir dir = controller.recompile();
      
      //notify user we completed successfully
      response.setCompilerStatus(CompileStatus.FINISHED);
      io.send(response.toJson());
      
      //also notify our frontend that the compiled output has changed
      GwtCompiledDirectory impl = new GwtCompiledDirectory();
      impl.setDeployDir(dir.getDeployDir().getAbsolutePath());
      impl.setExtraDir(dir.getExtraDir().getAbsolutePath());
      impl.setGenDir(dir.getGenDir().getAbsolutePath());
      impl.setLogFile(dir.getLogFile().getAbsolutePath());
      impl.setWarDir(dir.getWarDir().getAbsolutePath());
      impl.setWorkDir(dir.getWorkDir().getAbsolutePath());
      impl.setSourceMapDir(dir.findSymbolMapDir(request.getModule()).getAbsolutePath());
      impl.setUri(request.getModule());
      //This message is routed to @WebFE
      io.send("_frontend.symlink_"+impl.toString());
    } catch (Exception e) {
      //TODO serialize a proper error message to send to client
      e.printStackTrace();
      try{
        io.destroy();
      }catch (Exception ex) {
        ex.printStackTrace();
      }
      return;
    }
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
  
  public synchronized void compile(String request) throws CompilerBusyException{
    if (isAlive()){
      //if we're already running, we must check if we are sleeping or working.
      if (working)
        throw new CompilerBusyException(GwtCompileImpl.fromJsonString(request).getModule());
      interrupt();//wake up!
    }
    working = true;
    if (!isAlive()){
      start();
    }
  }
  
}