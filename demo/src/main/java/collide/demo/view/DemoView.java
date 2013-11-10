package collide.demo.view;

import xapi.log.X_Log;
import xapi.util.api.RemovalHandler;
import collide.client.util.Elements;
import collide.demo.view.SplitPanel;
import collide.gwtc.GwtCompileStatus;
import collide.gwtc.GwtcController;
import collide.gwtc.ui.GwtCompilePlace;
import collide.gwtc.ui.GwtCompilerShell;
import collide.gwtc.ui.GwtStatusListener;
import collide.gwtc.view.GwtcModuleControlView;

import com.google.collide.client.code.FileContent;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.client.ui.panel.PanelModel.Builder;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.Header;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.collide.dto.CompileResponse;
import com.google.collide.dto.CompileResponse.CompilerState;
import com.google.collide.dto.client.DtoClientImpls.GwtCompileImpl;
import com.google.collide.mvp.ShowableUiComponent;
import com.google.collide.mvp.UiComponent;
import com.google.collide.plugin.client.standalone.StandaloneConstants;
import com.google.collide.plugin.client.terminal.TerminalLogView;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;

import elemental.client.Browser;
import elemental.dom.Document;
import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DivElement;
import elemental.html.IFrameElement;
import elemental.html.ImageElement;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

class ControllerView extends com.google.collide.client.ui.panel.MultiPanel.View<PanelModel>
{

  Element header, core;
  public ControllerView(Element element, boolean detached) {
    super(element, detached);
    header = Browser.getDocument().createDivElement();
    element.appendChild(header);
    core = Browser.getDocument().createDivElement();
    element.appendChild(core);
  }

  @Override
  public Element getContentElement() {
    return core;
  }
  @Override
  public Element getHeaderElement() {
    return header;
  }
}

public class DemoView 
extends MultiPanel<PanelModel, ControllerView>
{

  
  private Element browser, editor, compiler, header;
  private ShowableUiComponent<?> bar;
  private final SplitPanel middleBar;
  private final SplitPanel bottomBar;
  private final SplitPanel verticalSplit;
  
  public DemoView() {
    super(new ControllerView(findBody(), true));
    
    middleBar = new SplitPanel(false);
    bottomBar = new SplitPanel(false);
    verticalSplit = new SplitPanel(true);
    
    header = Browser.getDocument().createDivElement();
    header.getStyle().setHeight(58, "px");
    Browser.getDocument().getBody().appendChild(header);
    Element el = getView().getElement();
    el.getStyle().setTop(58, "px");
    
    verticalSplit.addChild(middleBar.getElement(), 0.75);
    verticalSplit.addChild(bottomBar.getElement(), 0.25);
    el.appendChild(verticalSplit.getElement());
    
    bar = new ShowableUiComponent<View<?>>() {
      // We aren't using the toolbar just yet.
      @Override
      public void hide() {
      }
      @Override
      public void show() {
      }
    };
    
    
    browser = createElement(-1, 350);
    browser.setId(StandaloneConstants.FILES_PANEL);
    
    editor = createElement(-1, -1);
    editor.setId(StandaloneConstants.WORKSPACE_PANEL);

    compiler = Browser.getDocument().createDivElement();
    middleBar.addChild(compiler, 650);
    
    attachHandlers();
    
  }
  
  protected void attachHandlers() {
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        verticalSplit.refresh();
        middleBar.refresh();
        bottomBar.refresh();
      }
    });
  }

  private Element createElement(int index, double width) {
    DivElement el = Browser.getDocument().createDivElement();
    middleBar.addChild(el, width);
    return el;
  }

  private static Element findBody() {
    Document doc = Browser.getDocument();
    Element body = doc.getElementById("gwt_root");
    return body == null ? doc.getBody() : body;
  }

  private void append(Element element) {
    bottomBar.addChild(wrapChild(element), 0.2);
  }
  public void append(UiComponent<?> element) {
    Element el = wrapChild(element.getView().getElement());
    if (element instanceof WorkspaceShell) {
      browser.appendChild(el);
    } else if (element instanceof GwtCompilerShell) {
      compiler.appendChild(el);
    } else if (element instanceof Header) {
      header.appendChild(element.getView().getElement());
//      compiler.appendChild(el);
    } else {
      X_Log.warn("Unknown element type",element);
      getView().getElement().appendChild(el);
    }
  }

  private Element wrapChild(Element element) {
    return element;
  }

  FileContent file;
  
  @Override
  public void setContent(PanelContent panelContent) {
    setContent(panelContent, null);
  }
  
  @Override
  public void setContent(PanelContent panelContent, PanelModel settings) {
    X_Log.info("PanelController content set",panelContent);
    if (panelContent instanceof FileContent) {
      if (file != null) {
        PathUtil path = file.filePath();
        if (path != null && !path.equals(((FileContent) panelContent).filePath())){
            minimizeFile(path);
          }
          file.onContentDestroyed();
      }
      file = (FileContent)panelContent;
      
      // Ideally, we would reuse our uibinder instead of just clearing this
      editor.getFirstChildElement().getLastElementChild().setInnerHTML("");
      editor.getFirstChildElement().getLastElementChild().appendChild(file.getContentElement());
    } else if (panelContent instanceof TerminalLogView) {
      bottomBar.addChild(panelContent.getContentElement(), 500, 0);
    } else if (panelContent instanceof GwtCompilerShell){
//      Element el = wrapChild(((UiComponent<?>)panelContent).getView().getElement());
//      bottomBar.addChild(el, 0.3);
    } else if (panelContent instanceof UiComponent){
      append((UiComponent<?>)panelContent);
    } else {
      X_Log.warn("Unhandled panel type: ",panelContent.getClass(), panelContent);
      append(panelContent.getContentElement());
      throw new RuntimeException();
    }
    panelContent.onContentDisplayed();
  }
  
  public void minimizeFile(final PathUtil path) {
    DivElement el = Browser.getDocument().createDivElement();
    X_Log.info("minimizing",path);
//    CollideBootstrap.start(new SuccessHandler<AppContext>() {
//      @Override
//      public void onSuccess(AppContext arg0) {
////        Panel<Object, com.google.collide.client.ui.panel.Panel.View<Object>> panel = Panel.create(path, arg0.getResources(), ResizeBounds.withMaxSize(200, 64).build());
////        header.appendChild(panel.getView().getElement());
//      }
//    });
  }

  @Override
  public ShowableUiComponent<?> getToolBar() {
    return bar;
  }

  private static final class GwtCompileState {
    IFrameElement el;
    GwtCompileStatus status = GwtCompileStatus.Pending;
    Type logLevel = Type.ALL;
    public GwtcModuleControlView header;
  }
  
  private final MapFromStringTo<GwtCompileState> compileStates = Collections.mapFromStringTo();
  private GwtCompilerShell gwt;
  
  public RemovalHandler openIframe(final String id, final String url) {
    
    final GwtCompileState gwtc = getCompileState(id);
    IFrameElement iframe = gwtc.el;
    if (iframe == null) {
      DivElement sizer = Elements.createDivElement();
      sizer.getStyle().setPosition("absolute");
      sizer.getStyle().setLeft("0px");
      sizer.getStyle().setRight("10px");
      sizer.getStyle().setTop("50px");
      sizer.getStyle().setBottom("20px");

      gwtc.el = iframe = Browser.getDocument().createIFrameElement();
      iframe.getStyle().setWidth("100%");
      iframe.getStyle().setHeight("100%");

      iframe.setSrc(url);
      sizer.appendChild(iframe);
      
      final RemovalHandler[] remover = new RemovalHandler[1];
      gwtc.header = GwtcModuleControlView.create(new GwtcController() {
        @Override
        public void onReloadClicked() {
          GwtCompilePlace.PLACE.fireRecompile(id);
        }
        @Override
        public void onCloseClicked() {
          if (remover[0] != null) {
            removeCompileState(id);
            remover[0].remove();
            remover[0] = null;
          }
        }
        @Override
        public void onRefreshClicked() {
          gwtc.el.setSrc(url);
        }
      });
      
      Element wrapper = gwtc.header.getElement();
      gwtc.header.setHeader(id);
      wrapper.appendChild(sizer);
      wrapper.getStyle().setOverflow("hidden");
      remover[0] = bottomBar.addChild(wrapper, 450, 0);
    } else {
      iframe.setSrc("about:blank");
      iframe.setSrc(url);
    }
    iframe.scrollIntoViewIfNeeded(true);
    return new RemovalHandler() {
      @Override
      public void remove() {
        removeCompileState(id);
      }
    };
  }

  protected GwtCompileState getCompileState(String id) {
    GwtCompileState state = compileStates.get(id);
    if (state == null) {
      state = new GwtCompileState();
      compileStates.put(id, state);
    }
    return state;
  }
  protected void removeCompileState(String id) {
    compileStates.remove(id);
    // TODO kill any active compiles?
  }

  @Override
  public Builder<PanelModel> newBuilder() {
    return defaultBuilder();
  }

  public void initGwt(final GwtCompilerShell gwt) {
    this.gwt = gwt;

    gwt.addCompileStateListener(new GwtStatusListener() {
      
      @Override
      public void onLogLevelChange(String module, Type level) {
        GwtCompileState gwtc = getCompileState(module);
        gwtc.logLevel = level;
        switch (level) {
          case ERROR:
            gwtc.status = GwtCompileStatus.Fail;
            if (gwtc.header != null) {
              gwtc.header.setCompileStatus(gwtc.status);
            }
            break;
          case WARN:
            gwtc.status = GwtCompileStatus.Warn;
            if (gwtc.header != null) {
              gwtc.header.setCompileStatus(gwtc.status);
            }
            break;
        }
      }
      
      @Override
      public void onGwtStatusUpdate(CompileResponse status) {
        GwtCompileState gwtc = getCompileState(status.getModule());
        CompilerState state = status.getCompilerStatus();
        X_Log.info(getClass(), "State change",status.getModule(),state, new Exception());
        switch (state) {
          case FAILED:
            gwtc.status = GwtCompileStatus.Fail;
            if (gwtc.header != null) {
              gwtc.header.setCompileStatus(gwtc.status);
            }
            break;
          case FINISHED:
          case SERVING:
            if (gwtc.logLevel.ordinal() >= Type.ERROR.ordinal()) {
              gwtc.status = GwtCompileStatus.PartialSuccess;
            } else {
              gwtc.status = GwtCompileStatus.Success;
            }
            if (gwtc.header != null) {
              gwtc.header.setCompileStatus(gwtc.status);
            }
            break;
          case RUNNING:
            gwtc.status = GwtCompileStatus.Good;
            if (gwt.isAutoOpen()) {
              GwtCompileImpl value = gwt.getValue();
              gwt.getView().getDelegate().openIframe(value.getModule(), value.getPort());
            }
            if (gwtc.header != null) {
              gwtc.header.setCompileStatus(gwtc.status);
            }
          case BLOCKING:
          case UNLOADED:
        }
      }
    });
    append(gwt);
  }
  
}
