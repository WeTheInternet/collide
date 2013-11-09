package collide.gwtc.ui;

import xapi.util.impl.StringId;
import collide.gwtc.ui.GwtCompilerShell.View;

import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.LogMessage;
import com.google.collide.plugin.client.terminal.TerminalLogHeader;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.user.client.Window;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.html.DivElement;

public class RunningGwtModule extends StringId implements TerminalLogHeader {

  DivElement el;
  Type type = Type.ALL;

  public RunningGwtModule(String module) {
    super(module);
    el = Browser.getDocument().createDivElement();
    el.setInnerHTML("Compiling "+module);
  }
  
  @Override
  public void viewLog(LogMessage log) {
    
  }

  @Override
  public Element getElement() {
    return el;
  }

  @Override
  public String getName() {
    return getId();
  }

  @SuppressWarnings("incomplete-switch")
  public void processResponse(CompileResponse status, View view) {
    switch (status.getCompilerStatus()) {
    case BLOCKING:
      //confirm if the user wants to kill previous compile
      if (Window.confirm("There is already a compile in progress for " +
          "the module "+status.getModule()+". "
          +(status.isAuthorized()?
            "Do you wish to kill the running task?":
            "Press ok to wait for the running task to complete.")
        )) {
        return;
      }
      // kill the compiler
      view.getDelegate().onKillButtonClicked();
      break;
    case RUNNING:
      view.updateStatus("Compiling "+status.getModule());
      break;
    case SERVING:
      view.updateStatus("Serving module "+status.getModule());
      if (view.gwtSettings.radioIframe.isChecked()){
        view.getDelegate().openIframe(status.getModule(), status.getPort());
      }else if (view.gwtSettings.radioSelf.isChecked()){
        Window.Location.reload();
      }else if (view.gwtSettings.radioWindow.isChecked()){
        view.getDelegate().openWindow(status.getModule(), status.getPort());
      }
      break;
    case UNLOADED:
    case FINISHED:
      view.updateStatus("Finished compiling "+status.getModule());
  }
  }
  
}