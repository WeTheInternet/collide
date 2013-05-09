package com.google.collide.plugin.client.terminal;

import com.google.collide.dto.LogMessage;

public interface TerminalService {
  
  void setHeader(String module, TerminalLogHeader header);

  void addLog(LogMessage log, TerminalLogHeader header);

}
