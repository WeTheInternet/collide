package collide.gwtc.ui;

import xapi.log.X_Log;

import com.google.collide.client.AppContext;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.FrontendApi.RequestResponseApi;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.FileAssociation;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.Header.Resources;
import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.GwtCompileImpl;
import com.google.collide.dto.client.DtoClientImpls.GwtRecompileImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.plugin.PublicService;
import com.google.collide.shared.plugin.PublicServices;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;

import elemental.dom.Element;

public class GwtClientPlugin 
implements ClientPlugin<GwtCompilePlace>, RunConfiguration
{

  private AppContext appContext;
  private GwtCompileNavigationHandler handler;
  private Place place;

  private final FileAssociation GWT_FILE_ASSOCIATION;
  private final PublicService<?>[] services = new PublicService[1];

  public GwtClientPlugin() {
    GWT_FILE_ASSOCIATION = new FileAssociation() {
      @Override
      public boolean matches(String filename) {
        return filename.matches(".*gwt[.]*xml");
      }
    };
  }

  @Override
  public GwtCompilePlace getPlace() {
    return GwtCompilePlace.PLACE;
  }

  @Override
  public void initializePlugin(
    AppContext appContext, MultiPanel<?,?> masterPanel, Place parentPlace) {
    handler = new GwtCompileNavigationHandler(appContext, masterPanel, parentPlace);
    services[0] = PublicServices.createProvider(GwtCompilerService.class, handler);
    parentPlace.registerChildHandler(getPlace(), handler);
    this.place = parentPlace;
    this.appContext = appContext;
  }

  @Override
  public ImageResource getIcon(Resources res) {
    return res.gwtIcon();
  }

  @Override
  public void onClicked(ImageButton button) {
    GwtRecompileImpl gwtCompile = getCompilerSettings();
    PlaceNavigationEvent<GwtCompilePlace> action = GwtCompilePlace.PLACE.createNavigationEvent(gwtCompile);
    place.fireChildPlaceNavigation(action);

  }

  protected GwtCompileImpl getCompilerSettings() {
    GwtCompileImpl gwtCompile = handler.getValue();
    if (null==gwtCompile.getModule()||gwtCompile.getModule().length()==0) {
      gwtCompile.setModule("com.google.collide.plugin.StandalonePlugin");
    }
    if (null==gwtCompile.getSources()||gwtCompile.getSources().size()==0) {
       gwtCompile.setSources(JsoArray.<String>from(
          //our source list.  These come before gwt sdk
      "java","bin/gen", "plugin" // workspace relative source paths (hardcoded to collide)
      //ok to add jars as source if you wish
      ,"xapi-gwt-0.3.jar"
      ,"gson-2.2.1.jar"
      ,"guava-gwt-12.0.jar"
      ,"collide-client.jar"
      ))
      .setDependencies(JsoArray.<String>from(
          //our dependencies.  These come after gwt sdk
          "xapi-dev-0.3.jar"
          ,"elemental.jar"
          ,"client-src.jar"
          ,"client-common-src.jar"
          ,"client-scheduler-src.jar"
          ,"common-src.jar"
          ,"concurrencycontrol-src.jar"
          ,"model-src.jar"
          ,"media-src.jar"
          ,"waveinabox-import-0.3.jar"
          ,"jsr305.jar"
          ,"guava-12.0.jar"
      ));
    }
    return gwtCompile;
  }

  @Override
  public FileAssociation getFileAssociation() {
    return GWT_FILE_ASSOCIATION;
  }

  @Override
  public RunConfiguration getRunConfig() {
    return this;
  }

  @Override
  public String getId() {
    return "GWT_COMPILE";
  }

  @Override
  public String getLabel() {
    return "Compile GWT Module";
  }

  @Override
  public Element getForm() {
    return null;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void run(AppContext appContext, PathUtil file) {
    GwtRecompileImpl gwtCompile = getCompilerSettings();
    RequestResponseApi endpoint = 
        gwtCompile.isRecompile()
          ? appContext.getFrontendApi().RE_COMPILE_GWT
          : appContext.getFrontendApi().COMPILE_GWT;
    endpoint.send(gwtCompile , new ApiCallback<CompileResponse>() {
      @Override
      public void onMessageReceived(CompileResponse message) {
        CompilerState state = message.getCompilerStatus();
        X_Log.info("Gwt state",state);
        if (state == CompilerState.RUNNING) {
          
        }
      }
      @Override
      public void onFail(FailureReason reason) {
        Window.alert("Gwt compile failure: " + reason);
      }
    });
  }

  @Override
  public PublicService<?>[] getPublicServices() {
    return services ;
  }

}
