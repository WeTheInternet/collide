package com.google.collide.plugin.client.gwt;

import java.util.Date;

import com.google.collide.dto.shared.CookieKeys;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.user.client.Cookies;

public class GwtCompileModel {

  private String module;
  private boolean openIframe;
  private boolean openSelf;
  private boolean openWindow;
  private boolean noOpen;
  private Type logLevel = Type.INFO;
  private final JsoArray<String> classpath = JsoArray.create();
  private String url = "/static/$module/index.html";

  /**
   * @return our classpath (mutable)
   */
  public JsoArray<String> getClasspath() {
    return classpath;
  }
  /**
   * @param classpath - The absolute or workspace-relative classpath to use
   * makes a defensive copy of the classpath param.
   */
  public void setClasspath(JsoArray<String> classpath) {
    this.classpath.clear();
    this.classpath.addAll(classpath);
  }
  /**
   * @return the module
   */
  public String getModule() {
    return module;
  }

  /**
   * @param module the module to set
   * @return 
   */
  public GwtCompileModel setModule(String module) {
    this.module = module;
    return this;
  }

  /**
   * @return the openIframe
   */
  public boolean isOpenIframe() {
    return openIframe || (!openWindow && !noOpen && !openSelf);
  }
  /**
   * @param openIframe the openIframe to set
   */
  public void setOpenIframe(boolean openIframe) {
    this.openIframe = openIframe;
    if (openIframe) {
      this.openWindow = false;
      this.openSelf = false;
      this.noOpen = false;
    }
  }

  /**
   * @return the openWindow
   */
  public boolean isOpenWindow() {
    return openWindow;
  }

  /**
   * @param openWindow the openWindow to set
   */
  public void setOpenWindow(boolean openWindow) {
    this.openWindow = openWindow;
    if (openWindow) {
      this.openIframe = false;
      this.openSelf = false;
      this.noOpen = false;
    }
  }

  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  /**
   * @return the noOpen
   */
  public boolean isNoOpen() {
    return noOpen;
  }

  /**
   * @param noOpen the noOpen to set
   */
  public void setNoOpen(boolean noOpen) {
    this.noOpen = noOpen;
    if (noOpen) {
      this.openWindow = false;
      this.openIframe = false;
      this.openSelf = false;
    }
  }

  public void setOpenTarget(String cookie) {
    if (CookieKeys.DO_NOT_OPEN_COMPILE.equals(cookie)) {
      setNoOpen(true);
    } else if (CookieKeys.OPEN_COMPILE_IN_IFRAME.equals(cookie)) {
      setOpenIframe(true);
    } else if (CookieKeys.OPEN_COMPILE_IN_SELF.equals(cookie)) {
      setOpenSelf(true);
    } else if (CookieKeys.OPEN_COMPILE_IN_WINDOW.equals(cookie)) {
      setOpenWindow(true);
    }
  }

  /**
   * @return the openSelf
   */
  public boolean isOpenSelf() {
    return openSelf;
  }

  /**
   * @param openSelf the openSelf to set
   */
  public void setOpenSelf(boolean openSelf) {
    this.openSelf = openSelf;
    if (openSelf){
      this.openIframe = false;
      this.openWindow = false;
      this.noOpen = false;
    }
  }

  public TreeLogger.Type getLogLevel() {
    return logLevel ;
  }
  
  public GwtCompileModel setLogLevel(Type level){
    this.logLevel = level;
    return this;
  }

  public Type getDefaultLogLevel() {
      String cookie = getCookie(CookieKeys.GWT_LOG_LEVEL);
      if (cookie != null){
        switch(cookie.charAt(0)){
          case CookieKeys.GWT_LOG_LEVEL_ALL:
            return Type.ALL;
          case CookieKeys.GWT_LOG_LEVEL_SPAM:
            return Type.SPAM;
          case CookieKeys.GWT_LOG_LEVEL_DEBUG:
            return Type.DEBUG;
          case CookieKeys.GWT_LOG_LEVEL_TRACE:
            return Type.TRACE;
          case CookieKeys.GWT_LOG_LEVEL_INFO:
            return Type.INFO;
          case CookieKeys.GWT_LOG_LEVEL_WARN:
            return Type.WARN;
          case CookieKeys.GWT_LOG_LEVEL_ERROR:
            return Type.ERROR;
        }
      }
      return Type.INFO;
  }


  public String getCookie(String cookie) {
    //TODO: also store this state on server / in config file.  Use an abstraction layer
    return Cookies.getCookie(cookie);
  }
  
  public void setCookie(String cookieName, String string) {
    Cookies.setCookie(cookieName, string, new Date(System.currentTimeMillis()+1000000000));
  }

  public String getDefaultCompileTarget() {
      return getCookie(CookieKeys.GWT_COMPILE_TARGET);
  }

  
}
