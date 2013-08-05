package com.google.collide.plugin.client.gwt;

import com.google.collide.client.common.BaseResources;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.LogMessage;
import com.google.collide.json.client.JsoArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

public class GwtLogView extends UiComponent<GwtLogView.View>{

  int scrollHeight = 0;
  protected GwtLogView() {
  }
  public GwtLogView(com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res) {
    super();
    View view = new View(res);
    setView(view);
    view.setDelegate(new ViewEventsImpl());
  }

  public static GwtLogView create(DivElement gwtLogContainer, com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res){
    GwtLogView log = new GwtLogView();
    com.google.collide.plugin.client.gwt.GwtLogView.View view = new View(res);
    log.setView(view);
    gwtLogContainer.appendChild(view.root);
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
  }

  public interface Resources extends 
    ClientBundle 
    ,BaseResources.Resources
    {
    @Source("GwtLogView.css")
    Css gwtLogCss();
    
  }
  
  public static interface ViewEvents{
  }
  
  public static class ViewEventsImpl implements ViewEvents{
    
  }
  
  public static class View extends CompositeView<ViewEvents>{

    @UiTemplate("GwtLogView.ui.xml")
    interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, View> {
    }

    static MyBinder binder = GWT.create(MyBinder.class);
    
    @UiField
    DivElement root;

    @UiField(provided = true)
    com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res;
    
    @UiField
    DivElement logHeader;
    @UiField
    DivElement logBody;
    @UiField
    DivElement background;
    
    public View(com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res) {
      this.res = res;
      binder.createAndBindUi(this);
    }
    
  }

  
  public void addLog(LogMessage log) {
    //get or make a tab header for the desired module
    Css css = getView().res.gwtLogCss();
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
    
    elemental.html.DivElement el = Elements.createDivElement(clsName);
    el.setInnerHTML(log.getMessage().replaceAll("\t", " &nbsp; &nbsp; ").replaceAll("  ", " &nbsp;"));
    //TODO: use a page list-view so we can avoid painting 6,000,000 elements at once
    enqueue(el);
  }
  
  private final JsoArray<elemental.html.DivElement> cached = JsoArray.create();
  private final JsoArray<elemental.html.DivElement> els = JsoArray.create();
  private ScheduledCommand cmd;
  private void enqueue(elemental.html.DivElement el) {
    els.add(el);
    if (cmd == null){
      Scheduler.get().scheduleDeferred(
          (cmd = new ScheduledCommand() {
            @Override
            public void execute() {
              cmd = null;
              int size = els.size();
              elemental.html.DivElement into = (elemental.html.DivElement)getView().logBody;
              if (size>500){
                //TODO: put in paging instead of truncation
                elemental.html.DivElement pager = Elements.createDivElement();
                pager.setInnerHTML("...Logs truncated, (" +cached.size()+") elements hidden...");
                els.unshift(pager);
                cached.addAll(els.splice(0, size-500));
                Log.debug(getClass(), "Log overflow; cache-size: "+cached.size()+", element-size: "+els.size());
                into.setInnerHTML("");
              }
              for (elemental.html.DivElement el : els.asIterable()){
                into.appendChild(el);
              }
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
  
}
