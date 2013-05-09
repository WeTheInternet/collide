package com.google.collide.plugin.client.gwt;

import xapi.util.api.SuccessHandler;

import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.GwtCompile;

public interface GwtCompilerService {

  void compile(GwtCompile module, SuccessHandler<CompileResponse> response);
  
  void recompile(String module, SuccessHandler<CompileResponse> response);
  
  void kill(String module);

  GwtCompilerShell getShell();
  
}
