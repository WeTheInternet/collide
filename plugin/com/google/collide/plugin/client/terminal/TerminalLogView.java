package com.google.collide.plugin.client.terminal;

import collide.client.common.CommonResources;
import collide.client.util.Elements;

import com.google.collide.client.code.PluginContent;
import com.google.collide.client.util.ResizeBounds;
import com.google.collide.client.util.ResizeBounds.BoundsBuilder;
import com.google.collide.dto.LogMessage;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.dom.Element;

public class TerminalLogView extends UiComponent<TerminalLogView.View> implements PluginContent{

  int scrollHeight = 0;
  private final BoundsBuilder bounds = ResizeBounds.withMaxSize(500, Integer.MAX_VALUE).minSize(350, 300);

  protected TerminalLogView() {
  }
  public TerminalLogView(View view, ViewEvents events) {
    super();
    setView(view);
    view.setDelegate(events);
  }

  public static TerminalLogView create(Resources res, ViewEvents delegate){
    View view = new View(res);
    TerminalLogView log = new TerminalLogView(view, delegate);
    return log;
  }



  public interface Css extends CssResource {
    String bottomPlaceHolder();

    String logAll();

    String logDebug();

    String logSpam();

    String logTrace();

    String logInfo();

    String logWarning();

    String logError();

    String logContainer();

    String logHeader();

    String logBody();

    String logPad();

    String cliContainer();
  }

  public interface Resources extends
    ClientBundle
    ,CommonResources.BaseResources
    {
    @Source("TerminalLogView.css")
    Css terminalLogCss();

  }

  public static interface ViewEvents{

    void run();
  }

  public static class View extends CompositeView<ViewEvents>{

    @UiTemplate("TerminalLogView.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);

    @UiField
    DivElement root;

    @UiField(provided = true)
    Resources res;

    @UiField
    DivElement logHeader;
    @UiField
    DivElement logBody;
    @UiField
    DivElement background;

    public View(Resources res) {
      this.res = res;
      setElement(Elements.asJsElement(
        binder.createAndBindUi(this)));
      logBody.setInnerHTML("Logger ready");

    }

    public void setHeader(TerminalLogHeader el) {
      logHeader.setInnerHTML("");
      Elements.asJsElement(logHeader).appendChild(el.getElement());
    }

  }


  public void addLog(LogMessage log) {
    if (header != null ) {
      header.viewLog(log);
    }
    Css css = getView().res.terminalLogCss();
    String clsName = css.logInfo();
    switch(log.getLogLevel()){
      case ERROR:
        clsName = css.logError();
        break;
      case WARN:
        clsName = css.logWarning();
        break;
      case INFO:
        clsName = css.logInfo();
        break;
      case TRACE:
        clsName = css.logTrace();
        break;
      case DEBUG:
        clsName = css.logDebug();
        break;
      case SPAM:
        clsName = css.logSpam();
        break;
      case ALL:
        clsName = css.logAll();
        break;
    }

    elemental.html.PreElement el = Elements.createPreElement(clsName);
    el.setInnerText(log.getMessage());
    //TODO: use a page list-view so we can avoid painting 6,000,000 elements at once
    enqueue(el);
  }

  private final JsoArray<elemental.dom.Element> cached = JsoArray.create();
  private final JsoArray<elemental.dom.Element> visible = JsoArray.create();
  private final JsoArray<elemental.dom.Element> pending = JsoArray.create();
  private ScheduledCommand cmd;
  private Type logLevel = Type.INFO;
  
  TerminalLogHeader header;

  private void enqueue(elemental.dom.Element el) {
    pending.add(el);
    if (cmd == null){
      Scheduler.get().scheduleDeferred(
          (cmd = new ScheduledCommand() {
            @Override
            public void execute() {
              cmd = null;
              visible.addAll(pending);
              int size = visible.size();
              elemental.html.DivElement into = (elemental.html.DivElement)getView().logBody;
              if (size>700){
                //TODO: put in paging / see more link instead of truncation
                elemental.html.DivElement pager = Elements.createDivElement();
                pager.setInnerHTML("...Logs truncated, (" +cached.size()+") elements hidden...");
                visible.unshift(pager);
                cached.addAll(visible.splice(0, size-500));
                if (cached.size() > 10000) {
                  cached.setLength(10000);
                }
                into.setInnerHTML("");
                for (elemental.dom.Element el : visible.asIterable()){
                  into.appendChild(el);
                }
              } else {
                for (elemental.dom.Element el : pending.asIterable()){
                  into.appendChild(el);
                }
              }
              pending.clear();
              updateScrolling();
            }
          })
      );
    }
  }
  private void updateScrolling() {
    elemental.html.DivElement body = (elemental.html.DivElement)getView().root;
    int sH = body.getScrollHeight(), sT = body.getScrollTop(), h = body.getClientHeight();
    if (scrollHeight - h <= sT){//user was scrolled to the bottom last update,
      body.setScrollTop(sH);//force back to bottom
    }
    scrollHeight = sH;
  }
  @Override
  public Element getContentElement() {
    return getView().getElement();
  }
  @Override
  public void onContentDisplayed() {

  }
  @Override
  public void onContentDestroyed() {

  }
  @Override
  public String getNamespace() {
    return "Terminal";
  }

  @Override
  public BoundsBuilder getBounds() {
    return bounds;
  }
  
  public void setHeader(TerminalLogHeader el) {
    header = el;
    getView().setHeader(el);
  }
  
}
