package collide.gwtc.ui;

import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.GwtRecompile;
import com.google.gwt.core.ext.TreeLogger.Type;

public interface GwtController {
  void onCompileButtonClicked();
  void onStatusMessageReceived(CompileResponse status);
  void setLogLevel(Type type);
  void onDraftButtonClicked();
  void onKillButtonClicked();
  void openIframe(String module, int port);
  void openWindow(String module, int port);
  void recompile(GwtRecompile existing);
  void setAutoOpen(boolean auto);
  void onSaveButtonClicked();
  void onLoadButtonClicked();
  void onTestButtonClicked();
}