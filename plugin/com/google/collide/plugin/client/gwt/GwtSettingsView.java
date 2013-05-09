package com.google.collide.plugin.client.gwt;

import xapi.log.X_Log;

import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.shared.CookieKeys;
import com.google.collide.mvp.CompositeView;
import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;

import elemental.dom.Element;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DivElement;

public class GwtSettingsView extends CompositeView<GwtController>{


  @UiTemplate("GwtSettings.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, GwtSettingsView> {
  }

  static MyBinder binder = GWT.create(MyBinder.class);
  

  public interface Css extends CssResource {
    String settingsContainer();
    String urlBox();
    String urlBoxContainer();
  }

  
  public interface Resources extends 
    ClientBundle 
    {
    @Source("GwtSettings.css")
    Css gwtSettingsCss();
  }
  
  @UiField com.google.gwt.dom.client.DivElement body;
  @UiField(provided=true) com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res;


  //run taget radios
  @UiField InputElement radioIframe;
  @UiField LabelElement labelIframe;
  @UiField InputElement radioSelf;
  @UiField LabelElement labelSelf;
  @UiField InputElement radioWindow;
  @UiField LabelElement labelWindow;
  @UiField InputElement radioNoOpen;
  @UiField LabelElement labelNoOpen;

  @UiField com.google.gwt.dom.client.DivElement containerToOpen;
  @UiField InputElement inputToOpen;
  @UiField LabelElement labelToOpen;
  
  //log level radios
  @UiField InputElement radioLogAll;
  @UiField LabelElement labelLogAll;
  @UiField InputElement radioLogSpam;
  @UiField LabelElement labelLogSpam;
  @UiField InputElement radioLogDebug;
  @UiField LabelElement labelLogDebug;
  @UiField InputElement radioLogTrace;
  @UiField LabelElement labelLogTrace;
  @UiField InputElement radioLogInfo;
  @UiField LabelElement labelLogInfo;
  @UiField InputElement radioLogWarn;
  @UiField LabelElement labelLogWarn;
  @UiField InputElement radioLogError;
  @UiField LabelElement labelLogError;
  
  private final GwtCompileModel model;
  
  
  public GwtSettingsView(com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res, GwtCompileModel gwtModel) {
    this.res = res;
    binder.createAndBindUi(this);
    
    this.model = gwtModel;
    
    res.gwtSettingsCss().ensureInjected();
    

    setLabelFor(radioIframe, labelIframe);
    setLabelFor(radioSelf, labelSelf);
    setLabelFor(radioWindow, labelWindow);
    setLabelFor(radioNoOpen, labelNoOpen);

    setLabelFor(inputToOpen, labelToOpen);
    ((elemental.html.InputElement)inputToOpen).setOnchange(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        model.setUrl(inputToOpen.getValue());
      }
    });

    addCompileTargetChangeListener((elemental.dom.Element)radioIframe,CookieKeys.OPEN_COMPILE_IN_IFRAME);
    addCompileTargetChangeListener((elemental.dom.Element)radioSelf,CookieKeys.OPEN_COMPILE_IN_SELF);
    addCompileTargetChangeListener((elemental.dom.Element)radioWindow,CookieKeys.OPEN_COMPILE_IN_WINDOW);
    addCompileTargetChangeListener((elemental.dom.Element)radioNoOpen,CookieKeys.DO_NOT_OPEN_COMPILE);


    setLabelFor(radioLogAll, labelLogAll);
    setLabelFor(radioLogSpam, labelLogSpam);
    setLabelFor(radioLogDebug, labelLogDebug);
    setLabelFor(radioLogTrace, labelLogTrace);
    setLabelFor(radioLogWarn, labelLogWarn);
    setLabelFor(radioLogInfo, labelLogInfo);
    setLabelFor(radioLogError, labelLogError);

    addLogLevelChangeListener((elemental.html.InputElement)radioLogAll, Type.ALL);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogSpam, Type.SPAM);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogDebug, Type.DEBUG);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogTrace, Type.TRACE);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogInfo, Type.INFO);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogWarn, Type.WARN);
    addLogLevelChangeListener((elemental.html.InputElement)radioLogError, Type.ERROR);

    refreshDom();
  }


  private void addLogLevelChangeListener(elemental.html.InputElement radio, final Type type) {
    radio.addEventListener("change",
        new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            getDelegate().setLogLevel(type);
          }
        }, false);
  }

  private void setLabelFor(InputElement radio, LabelElement label) {
    if (Strings.isNullOrEmpty(radio.getId()))
      radio.setId(DOM.createUniqueId());
    label.setHtmlFor(radio.getId());
  }

  private void addCompileTargetChangeListener(Element el, final String cookieKey) {
    el.addEventListener("change",
        new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            Cookies.setCookie(CookieKeys.GWT_COMPILE_TARGET, cookieKey);
            if (CookieKeys.OPEN_COMPILE_IN_IFRAME.equals(cookieKey)) {
              model.setOpenIframe(true);
              containerToOpen.getStyle().setHeight(50, Unit.PX);
            } else if (CookieKeys.OPEN_COMPILE_IN_WINDOW.equals(cookieKey)) {
              model.setOpenWindow(true);
              containerToOpen.getStyle().setHeight(50, Unit.PX);
            } else if (CookieKeys.OPEN_COMPILE_IN_SELF.equals(cookieKey)) {
              model.setOpenSelf(true);
              containerToOpen.getStyle().setHeight(0, Unit.PX);
            } else if (CookieKeys.DO_NOT_OPEN_COMPILE.equals(cookieKey)) {
              model.setNoOpen(true);
              containerToOpen.getStyle().setHeight(0, Unit.PX);
            }
          }
        },false);

  }

  protected void refreshDom() {
    if (model.isOpenIframe()){
      radioIframe.setChecked(true);
      containerToOpen.getStyle().setHeight(50, Unit.PX);
      inputToOpen.setValue(model.getUrl());
    }else if (model.isOpenWindow()){
      radioWindow.setChecked(true);
      containerToOpen.getStyle().setHeight(50, Unit.PX);
      inputToOpen.setValue(model.getUrl());
    }else if (model.isOpenSelf()){
      containerToOpen.getStyle().setHeight(0, Unit.PX);
      radioSelf.setChecked(true);
    }else if (model.isNoOpen()){
      containerToOpen.getStyle().setHeight(0, Unit.PX);
      radioNoOpen.setChecked(true);
    }
    switch (model.getLogLevel()){
      case ALL:
        radioLogAll.setChecked(true);break;
      case SPAM:
        radioLogSpam.setChecked(true);break;
      case DEBUG:
        radioLogDebug.setChecked(true);break;
      case TRACE:
        radioLogTrace.setChecked(true);break;
      case INFO:
        radioLogInfo.setChecked(true);break;
      case WARN:
        radioLogWarn.setChecked(true);break;
      case ERROR:
        radioLogError.setChecked(true);break;
    }
  }

  
  
  public static GwtSettingsView create(DivElement moduleContainer, com.google.collide.plugin.client.gwt.GwtCompilerShell.Resources res,
      GwtCompileModel model) {
    GwtSettingsView mod = new GwtSettingsView(res, model);
    moduleContainer.appendChild((DivElement)mod.body);
    return mod;
  }
  
  public void applySettings(GwtCompile module) {
    Type logLevel;
    
    logLevel = module.getLogLevel();
    if (logLevel != null)
      switch (logLevel) {
      case ALL:
      case SPAM:
      case DEBUG:
        radioLogDebug.setChecked(true);
        break;
      case TRACE:
        radioLogTrace.setChecked(true);
        break;
      case INFO:
        radioLogInfo.setChecked(true);
        break;
      case WARN:
        radioLogWarn.setChecked(true);
        break;
      case ERROR:
        radioLogError.setChecked(true);
        break;
      }
  }


  protected Type getLogLevel() {
    // order based on expected probability of popularity
    if (radioLogInfo.isChecked())
      return Type.INFO;
    if (radioLogWarn.isChecked())
      return Type.WARN;
    if (radioLogTrace.isChecked())
      return Type.TRACE;
    if (radioLogError.isChecked())
      return Type.ERROR;
    if (radioLogDebug.isChecked())
      return Type.DEBUG;
    if (radioLogSpam.isChecked())
      return Type.SPAM;
    if (radioLogAll.isChecked())
      return Type.ALL;
    return Type.INFO;
  }


  public String getPageToOpen() {
    return inputToOpen.getValue();
  }


}
