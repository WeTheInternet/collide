package collide.demo.view;

import xapi.log.X_Log;

import com.google.collide.client.code.FileContent;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelContent;
import com.google.collide.client.ui.panel.PanelModel;
import com.google.collide.client.ui.panel.PanelModel.Builder;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.Header;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.collide.mvp.ShowableUiComponent;
import com.google.collide.mvp.UiComponent;
import com.google.collide.plugin.client.gwt.GwtCompilePlace;
import com.google.collide.plugin.client.gwt.GwtCompilerShell;
import com.google.collide.plugin.client.standalone.StandaloneConstants;
import com.google.collide.plugin.client.terminal.TerminalLogView;

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
  private SplitPanel body;
  
  public DemoView() {
    super(new ControllerView(findBody(), true));
    body = new SplitPanel(false);
    header = Browser.getDocument().createDivElement();
    header.getStyle().setHeight(65, "px");
    Browser.getDocument().getBody().appendChild(header);
    Element el = getView().getElement();
    el.getStyle().setTop(65, "px");
    el.appendChild(body.getElement());
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

    compiler = createElement(-1, 650);
    
//    terminal = createElement(-1, 0.25);
    
  }
  
  private Element createElement(int index, double width) {
    DivElement el = Browser.getDocument().createDivElement();
    body.addChild(el, width);
    return el;
  }

  private static Element findBody() {
    Document doc = Browser.getDocument();
    Element body = doc.getElementById("gwt_root");
    return body == null ? doc.getBody() : body;
  }

  private void append(Element element) {
    body.addChild(wrapChild(element), 0.2);
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
        body.addChild(panelContent.getContentElement(), 500, 4);
      }
      else if (panelContent instanceof UiComponent){
        append((UiComponent<?>)panelContent);
      } else {
        X_Log.warn("Unhandled panel type: ",panelContent.getClass(), panelContent);
        append(panelContent.getContentElement());
        throw new RuntimeException();
    }
    panelContent.onContentDisplayed();
//    append(panelContent);
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

  private final MapFromStringTo<IFrameElement> iframes = Collections.mapFromStringTo();
  
  public void openIframe(final String id, String url) {
    
    IFrameElement iframe = iframes.get(id);
    if (iframe == null) {
      iframe = Browser.getDocument().createIFrameElement();
      iframe.getStyle().setPosition("absolute");
      iframe.getStyle().setWidth("93%");
      iframe.getStyle().setHeight("85%");
      iframe.getStyle().setTop("50px");
      iframes.put(id, iframe);
      iframe.setSrc(url);
      DivElement wrapper = Browser.getDocument().createDivElement();
//      DivElement header = Browser.getDocument().createDivElement();
      ImageElement reload = Browser.getDocument().createImageElement();
      /**
       * Image credit: 
       * <a href='http://www.123rf.com/photo_15417761_reload-icon.html'>alexwhite / 123RF Stock Photo</a>
       * (license is paid, but requires attribution)
       */
      reload.setSrc("/static/Demo/reload.png");
      reload.getStyle().setCursor("pointer");
      reload.setOnclick(new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          GwtCompilePlace.PLACE.fireRecompile(id);
        }
      });
      PanelHeader headerWidget = PanelHeader.create(wrapper, null, null);
      Elements.asJsElement(headerWidget.icons).appendChild(reload);
      Element head = Elements.asJsElement(headerWidget.header);
      head.setInnerHTML(id);
      head.getStyle().setPosition("absolute");
      head.getStyle().setLeft("50px");
      head.getStyle().setMargin("0px");
      head.getStyle().setTop("5px");
      
//      wrapper.appendChild(headerWidget.getElement());
      wrapper.appendChild(iframe);
      wrapper.getStyle().setOverflow("hidden");
      body.addChild(wrapper, 450, 2);
    } else {
      iframe.setSrc("about:blank");
      iframe.setSrc(url);
    }
    iframe.scrollIntoViewIfNeeded(true);
  }

  @Override
  public Builder<PanelModel> newBuilder() {
    return defaultBuilder();
  }
  
}
