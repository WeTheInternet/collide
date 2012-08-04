package com.google.gwt.dev.codeserver;

import org.vertx.java.core.json.JsonObject;

import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

public final class ReflectionChannelTreeLogger extends AbstractTreeLogger{

  private static final String spacer = " " + ((char)160)+" " + ((char)160)+" ";
  private ReflectionChannel io;
  private String indent;

  public ReflectionChannelTreeLogger(ReflectionChannel io) {
    this(io ,"",INFO);
  }
  public ReflectionChannelTreeLogger(ReflectionChannel io, Type logLevel) {
    this(io ,"",logLevel);
  }
  public ReflectionChannelTreeLogger(ReflectionChannel io,String indent, Type logLevel) {
    this.io = io;
    this.indent = indent;
    setMaxDetail(logLevel);
  }
  
  
  @Override
  protected AbstractTreeLogger doBranch() {
    return new ReflectionChannelTreeLogger(io,indent+spacer, getMaxDetail());
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught, helpInfo);
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    //TODO: filter on Type to allow debug level setting
    if (getMaxDetail().ordinal()>=type.ordinal()){
      System.out.println(indent+type+": "+msg+(null==caught?"":DebugUtil.getFullStacktrace(caught, "\n ")));
      JsonObject obj = new JsonObject()
      .putString("type", type.name())
      .putString("msg", indent+msg);
      if (caught != null)
        obj.putString("error", DebugUtil.getFullStacktrace(caught, "\n"));
      if (helpInfo != null){
        obj.putString("help",helpInfo.getPrefix()+": "+helpInfo.getAnchorText());
      }
      io.send(obj.encode());
    }
  }

}
