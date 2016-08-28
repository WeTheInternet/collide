package com.google.collide.plugin.server;

import java.io.Serializable;

import com.google.collide.dto.server.DtoServerImpls.LogMessageImpl;
import com.google.collide.server.shared.util.ReflectionChannel;
import com.google.collide.shared.util.DebugUtil;
import com.google.gwt.dev.util.log.AbstractTreeLogger;

public final class ReflectionChannelTreeLogger extends AbstractTreeLogger implements Serializable{

  private static final long serialVersionUID = -8661296485728859162L;

  private static final String spacer = " " + ((char)160)+" " + ((char)160)+" ";
  private ReflectionChannel io;
  private String indent;
  private String module;

  public ReflectionChannelTreeLogger(ReflectionChannel io) {
    this(io ,"",INFO);
  }
  public ReflectionChannelTreeLogger(ReflectionChannel io, Type logLevel) {
    this(io ,"",logLevel);
  }
  public ReflectionChannelTreeLogger(ReflectionChannel io, String indent, Type logLevel) {
    this.io = io;
    this.indent = indent;
    setMaxDetail(logLevel);
  }
  
  
  @Override
  protected AbstractTreeLogger doBranch() {
    ReflectionChannelTreeLogger branch = new ReflectionChannelTreeLogger(io,indent+spacer, getMaxDetail());
    branch.setModule(module);
    return branch;
  }

  @Override
  protected void doCommitBranch(AbstractTreeLogger childBeingCommitted, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    doLog(childBeingCommitted.getBranchedIndex(), type, msg, caught, helpInfo);
  }

  @Override
  protected void doLog(int indexOfLogEntryWithinParentLogger, Type type, String msg,
      Throwable caught, HelpInfo helpInfo) {
    if (getMaxDetail().ordinal()>=type.ordinal()){
      System.out.println(indent+type+": "+msg+(null==caught?"":DebugUtil.getFullStacktrace(caught, "\n ")));
      LogMessageImpl message = LogMessageImpl.make();
      message.setLogLevel(type);
      message.setMessage(msg);
      message.setModule(module);
      if (caught != null)
        message.setError(DebugUtil.getFullStacktrace(caught, "\n"));
      if (helpInfo != null){
        message.setHelpInfo(helpInfo.getPrefix()+": "+helpInfo.getAnchorText());
      }
      io.send(message.toJson());
    }
  }
  public void setModule(String module) {
    this.module = module;
  }

}
