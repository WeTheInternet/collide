package collide.plugin.client.terminal;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.FileAssociation;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.Header.Resources;
import com.google.collide.dto.LogMessage;
import com.google.collide.shared.plugin.PublicService;
import com.google.collide.shared.plugin.PublicService.DefaultServiceProvider;
import com.google.gwt.resources.client.ImageResource;

public class TerminalClientPlugin  implements ClientPlugin<TerminalPlace>, TerminalService{

  private Place parentPlace;
  private TerminalNavigationHandler handler;

  @Override
  public TerminalPlace getPlace() {
    return TerminalPlace.PLACE;
  }

  @Override
  public void initializePlugin(
    AppContext appContext, MultiPanel<?,?> masterPanel, Place currentPlace) {
    this.parentPlace = currentPlace;
    handler = new TerminalNavigationHandler(appContext, masterPanel, currentPlace);
    parentPlace.registerChildHandler(getPlace(), handler);
  }

  @Override
  public ImageResource getIcon(Resources res) {
    return res.terminalIcon();
  }

  @Override
  public void onClicked(ImageButton button) {
    //show terminal
    PlaceNavigationEvent<?> action = TerminalPlace.PLACE.createNavigationEvent();
    parentPlace.fireChildPlaceNavigation(action);

  }

  @Override
  public FileAssociation getFileAssociation() {
    return new FileAssociation.FileAssociationSuffix(".*[.]sh"){
    };
  }

  @Override
  public RunConfiguration getRunConfig() {
    return null;
  }

  @Override
  public void addLog(LogMessage log, TerminalLogHeader header) {
    handler.addLog(log, header);
  }

  @Override
  public void setHeader(String module, TerminalLogHeader header) {
    handler.setHeader(module, header);
  }

  @Override
  public PublicService<?>[] getPublicServices() {
    return new PublicService[] {
      new DefaultServiceProvider<TerminalService>(TerminalService.class, this)
    };
  }

  public void setRename(String from, String to) {
    handler.setRename(from, to);
  }

}
