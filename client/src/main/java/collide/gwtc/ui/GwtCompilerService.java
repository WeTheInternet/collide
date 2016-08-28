package collide.gwtc.ui;

import xapi.util.api.SuccessHandler;

import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.GwtRecompile;

public interface GwtCompilerService {

  void compile(GwtRecompile module, SuccessHandler<CompileResponse> response);
  
  void recompile(String module, SuccessHandler<CompileResponse> response);
  
  void kill(String module);

  GwtCompilerShell getShell();
  
}
