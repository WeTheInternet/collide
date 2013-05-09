package com.google.collide.plugin.client.gwt;

import xapi.collect.impl.InitMapDefault;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;
import xapi.util.api.SuccessHandler;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.PluginContent;
import com.google.collide.client.common.BaseResources;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.ResizeBounds.BoundsBuilder;
import com.google.collide.client.util.logging.Log;
import com.google.collide.clientlibs.vertx.VertxBus.MessageHandler;
import com.google.collide.clientlibs.vertx.VertxBus.ReplySender;
import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtSettings;
import com.google.collide.dto.LogMessage;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.SearchResult;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.CompileResponseImpl;
import com.google.collide.dto.client.DtoClientImpls.GwtCompileImpl;
import com.google.collide.dto.client.DtoClientImpls.LogMessageImpl;
import com.google.collide.dto.shared.CookieKeys;
import com.google.collide.json.client.Jso;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.plugin.client.launcher.LauncherService;
import com.google.collide.plugin.client.terminal.TerminalService;
import com.google.collide.shared.plugin.PublicServices;
import com.google.collide.shared.util.DebugUtil;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;

public class GwtCompilerShell extends UiComponent<GwtCompilerShell.View> 
implements PluginContent, ConvertsValue<String, RunningGwtModule> {

  public interface Css extends CssResource {
    String container();

    String leftPanel();

    String moduleContainer();

    String next();

    String options();
    
    String otherpage();

    String pager();

    String previous();

    String radioLabel();

    String rightPanel();

    String second();

    String srcContainer();

    String settingsContainer();

    String snippet();

    String status();

    String statusHidden();

    String thispage();

    String title();
    
  }

  public interface Resources extends
    ClientBundle
    ,BaseResources.Resources
    ,GwtLogView.Resources
    ,GwtModuleView.Resources
    ,GwtClasspathView.Resources
    ,GwtSettingsView.Resources
    {
    @Source("collide/demo/public/green-radar-small.gif")
    ImageResource radarGreenSmall();
//
//    @Source("previous_page.png")
//    ImageResource prevPage();

    @Source("GwtCompilerShell.css")
    Css gwtCompilerCss();

  }

  public static class View extends CompositeView<GwtController> {

    @UiTemplate("GwtCompilerShell.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField(provided = true)
    final Resources res;

    @UiField(provided = true)
    final GwtCompileModel compileState;


    final Css css;

    @UiField
    DivElement status;

    @UiField DivElement buttonContainer;
    @UiField DivElement srcContainer;
    @UiField DivElement settingsContainer;
    @UiField DivElement moduleContainer;

    GwtModuleView gwtModule;
    GwtClasspathView gwtSrc;
    GwtSettingsView gwtSettings;

    @UiField
    com.google.gwt.dom.client.AnchorElement compileButton;
    @UiField
    com.google.gwt.dom.client.AnchorElement draftButton;
    @UiField
    com.google.gwt.dom.client.AnchorElement killButton;



    public View(AppContext context, GwtCompileModel model, Resources res) {
      this.res = res;
      this.css = res.gwtCompilerCss();
      this.compileState = model;
      model.setOpenTarget(model.getDefaultCompileTarget());
      model.setLogLevel(model.getDefaultLogLevel());
      
      setElement(Elements.asJsElement(binder.createAndBindUi(this)));
      

      ((AnchorElement)draftButton).setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onDraftButtonClicked();
        }
      });
      ((AnchorElement)compileButton).setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onCompileButtonClicked();
        }
      });
      ((AnchorElement)killButton).setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          getDelegate().onKillButtonClicked();
        }
      });

      gwtModule = GwtModuleView.create((elemental.html.DivElement)moduleContainer, res, model);
      gwtSrc = GwtClasspathView.create((elemental.html.DivElement)srcContainer, res, model);
      gwtSettings = GwtSettingsView.create((elemental.html.DivElement)settingsContainer, res, model);

      addListeners();
    }

    @Override
    public void setDelegate(GwtController delegate) {
      super.setDelegate(delegate);
      gwtModule.setDelegate(delegate);
      gwtSrc.setDelegate(delegate);
      gwtSettings.setDelegate(delegate);
    }
    

    protected void addListeners() {

//    RemovalHandler onRemove =  // TODO implement widget lifecycle / gc
        gwtModule.addSelectListener(new ReceivesValue<GwtCompile>(){
      @Override
      public void set(GwtCompile module) {
        gwtSrc.setClasspath(module.getSrc(), module.getDeps());
        gwtSettings.applySettings(module);
      }
    });
    }

    
    public void clear() {
      gwtSettings.refreshDom();
    }

    private Timer clear;
    
    public void updateStatus(String string) {
      if (clear != null){
        clear.cancel();
        clear = null;
      }
      if (X_String.isEmptyTrimmed(string)) {
        status.addClassName(res.gwtCompilerCss().statusHidden());
        status.getStyle().setHeight(0, Unit.PX);
      } else {
        status.getStyle().setHeight(30, Unit.PX);
        status.removeClassName(res.gwtCompilerCss().statusHidden());
        status.setInnerHTML("<pre>"+string+"</pre>");
        clear = new Timer() {
          @Override
          public void run() {
            clear = null;
            status.addClassName(res.gwtCompilerCss().statusHidden());
            status.getStyle().setHeight(0, Unit.PX);
          }
        };
        clear.schedule(15000);
      }
    }

    public GwtCompileImpl getValue() {
      GwtCompileImpl compile = GwtCompileImpl.make();
      //TODO send back copy instead of original
      String val = gwtModule.input.getValue();
      if (val != null && val.length()>0)
        compile.setModule(val);
      
      compile.setLogLevel(gwtSettings.getLogLevel());
      compile.setSrc(gwtSrc.getSources());
      compile.setDeps(gwtSrc.getDependencies());
      
      return compile;
    }

    public void clearStatus() {
      if (clear != null)
        clear.run();
    }
  }


  public class GwtControllerImpl implements GwtController{
    @Override
    public void onDraftButtonClicked() {
      context.getFrontendApi().COMPILE_GWT.send(getValue(), new ApiCallback<CompileResponse>() {
        @Override
        public void onMessageReceived(CompileResponse message) {
          if (message == null)
            Log.error(getClass(), "Null gwt status message received");
          else
            onStatusMessageReceived(message);
        }
        @Override
        public void onFail(FailureReason reason) {
  
        }
      });
    }
    @Override
    public void onCompileButtonClicked() {
      onDraftButtonClicked();//We aren't doing full compiles just yet
    }
  
    @Override
    public void onKillButtonClicked() {
      Window.alert("You are not authorized to kill active compiles");
//      
//      context.getFrontendApi().KILL_GWT.send(compile, new ApiCallback<CompileResponse>() {
//        @Override
//        public void onFail(FailureReason reason) {
//  
//        }
//        @Override
//        public void onMessageReceived(CompileResponse message) {
//  
//        }
//      });
    }
  
    @Override
    public void onStatusMessageReceived(CompileResponse status) {
      updateStatus(status);
    }
  
    @Override
    public void openIframe(String module, int port) {
      LauncherService service = PublicServices.getService(LauncherService.class);
      String url = getView().compileState.getUrl();
      if (X_String.isEmptyTrimmed(url))
          url = "xapi/gwt/"+module;
      url = url.replace("$module", module);
      service.openInIframe(module, url);
    }
    
    @Override
    public void openWindow(String module, int port) {
      LauncherService service = PublicServices.getService(LauncherService.class);
      service.openInNewWindow(module, "");
    }
    
    @Override
    public void setLogLevel(Type type) {
      assert type != null : "Sent null loglevel from "+DebugUtil.getCaller();
      level = type;
      switch(type){//also store a cookie
        case ALL:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_ALL));
          break;
        case SPAM:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_SPAM));
          break;
        case DEBUG:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_DEBUG));
          break;
        case TRACE:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_TRACE));
          break;
        case INFO:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_INFO));
          break;
        case WARN:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_WARN));
          break;
        case ERROR:
          getView().compileState.setCookie(CookieKeys.GWT_LOG_LEVEL, Character.toString(CookieKeys.GWT_LOG_LEVEL_ERROR));
          break;
      }
    }
    
    @Override
    public void recompile(GwtCompile existing) {
      getView().gwtModule.showModule(existing);
      getView().gwtSettings.applySettings(existing);
      getView().gwtSrc.setClasspath(existing.getSrc(), existing.getDeps());
      onDraftButtonClicked();
    }
    
  }


  public static GwtCompilerShell create(View view, AppContext context){
    return new GwtCompilerShell(view, context);
  }




  private AppContext context;
  private Type level;
  private final BoundsBuilder bounds = ResizeBounds.withMaxSize(640, -10).minSize(550, 400);

  protected void updateStatus(CompileResponse status) {
    loggers.get(status.getModule()).processResponse(status, getView());
    if(status.getCompilerStatus() == CompilerState.SERVING) {
      String page = getView().gwtSettings.getPageToOpen();
      if (!X_String.isEmptyTrimmed(page)) {
        if (getView().compileState.isOpenIframe()) {
          
        }
      }
    }
  }

  private static class LoggerMap extends InitMapDefault<String, RunningGwtModule> {
    public LoggerMap(ConvertsValue<String, RunningGwtModule> factory) {
      super(PASS_THRU, factory);
    }
  }
  
  private LoggerMap loggers;
  
  public GwtCompilerShell(View view, AppContext context) {
    super(view);
    this.context = context;
    loggers = new LoggerMap(this);
    view.setDelegate(createViewDelegate());
    context.getPushChannel().receive("gwt.log", new MessageHandler() {
      @Override
      public void onMessage(String message, ReplySender replySender) {
        Jso jso = Jso.deserialize(message);
        if (jso.getIntField("_type")==RoutingTypes.LOGMESSAGE){
          addLog(jso.<LogMessageImpl>cast());
        }else{
          updateStatus(jso.<CompileResponseImpl>cast());
        }
      }
    });
  }

  public GwtCompileImpl getValue() {
    return getView().getValue();
  }

  private com.google.collide.client.Resources getIframeResources() {
    return context.getResources();
  }

  
  
  protected void addLog(LogMessage log) {
    RunningGwtModule logger = loggers.get(log.getModule());
    PublicServices.getService(TerminalService.class).addLog(log, logger);
    Type type = log.getLogLevel();
    if (type == Type.WARN) {
      
    } else if (type == Type.ERROR) {
      
    }
  }

  protected GwtController createViewDelegate() {
    return new GwtControllerImpl();
  }

  /**
   * Updates with new results.
   *
   * @param message the message containing the new results.
   */
  public void showResults(GwtSettings message) {
    getView().clear();
    showResultsImpl(message.getModules());
  }

  @Override
  public Element getContentElement() {
    return getView().getElement();
  }

  @Override
  public void onContentDisplayed() {
    Log.info(getClass(), "Showing compiler controller");
  }

  /**
   * Updates the view to displays results and appropriate pager widgetry.
   *
   * @param page the page of "this" result page, one-based
   * @param pageCount the total number of pages
   * @param jsonArray the {@link SearchResult} items on this page.
   */
  private void showResultsImpl(JsonArray<GwtCompile> jsonArray) {
    getView().gwtModule.showResults(jsonArray, getView().gwtSrc);
  }

  public void setPlace(GwtCompilePlace.NavigationEvent place) {
    if (place.isRecompile()) {
      recompile(place.getModule(), null);
    }
  }

  @Override
  public void onContentDestroyed() {

  }

  @Override
  public BoundsBuilder getBounds() {
    return bounds;
  }
  
  @Override
  public String getNamespace() {
    return "Gwt Compiler";
  }

  @Override
  public RunningGwtModule convert(String from) {
    return new RunningGwtModule(from);
  }

  public void compile(GwtCompile module,
      SuccessHandler<CompileResponse> response) {
    
  }

  public void recompile(String module, SuccessHandler<CompileResponse> response) {
    GwtCompile existing = getView().gwtModule.getModule(module);
    if (existing != null) {
      getView().getDelegate().recompile(existing);
      // TODO: use a per-module multi-map of CompileResponse event listeners.
    }
  }

  public void kill(String module) {
    
  }
}
