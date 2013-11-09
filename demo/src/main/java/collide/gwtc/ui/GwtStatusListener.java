package collide.gwtc.ui;

import com.google.collide.dto.CompileResponse;
import com.google.gwt.core.ext.TreeLogger.Type;

public interface GwtStatusListener {

  void onGwtStatusUpdate(CompileResponse status);

  void onLogLevelChange(String id, Type level);
  
}
