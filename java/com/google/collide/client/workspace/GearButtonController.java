package com.google.collide.client.workspace;

import com.google.collide.client.AppContext;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.clientlibs.vertx.VertxBus.MessageHandler;
import com.google.collide.clientlibs.vertx.VertxBus.ReplySender;
import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtStatus;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.GwtCompileImpl;
import com.google.collide.json.client.JsoArray;
import com.google.gwt.user.client.Window;

public class GearButtonController {

  private AppContext appContext;

  protected GearButtonController(final AppContext appContext) {
    this.appContext = appContext;
  }
  
  public static GearButtonController create(final AppContext appContext) {
    return new GearButtonController(appContext);
  }

  public void onGearButtonClicked() {
    
    GwtCompile gwtCompile = GwtCompileImpl.make()
    .setModule("com.google.collide.client.Collide")
    .setSrc(JsoArray.<String>from("java","bin/gen"
//        ,"deps/gwt-r11004/"
        ,"deps/gwt-r11004/gwt-user.jar"
        ,"deps/gwt-r11004/gwt-dev.jar"
        ,"deps/gwt-r11004/gwt-codeserver.jar"
        ,"deps/guava-12.0/guava-gwt-12.0.jar"
        ,"deps/elemental/elemental.jar"
    ,"deps/wave-r1342740/client-src.jar"
    ,"deps/wave-r1342740/client-common-src.jar"
    ,"deps/wave-r1342740/client-scheduler-src.jar"
    ,"deps/wave-r1342740/common-src.jar"
    ,"deps/wave-r1342740/concurrencycontrol-src.jar"
    ,"deps/wave-r1342740/model-src.jar"
    ,"deps/wave-r1342740/media-src.jar"
    ,"deps/wave-r1342740/waveinabox-import-0.3.jar"
    ,"deps/gson-2.2.1/gson-2.2.1.jar"
    ,"deps/guava-12.0/guava-12.0.jar"
    ,"deps/jsr-305/jsr305.jar"
    ,"deps/xapi-0.2/xapi-super-0.2.jar"
//        ,"../deps/gwt-user.jar"
//        ,"../deps/gwt-dev.jar"
        ))
    ;
    //        appContext.getFrontendApi().
    appContext.getFrontendApi().COMPILE_GWT.send(gwtCompile , new ApiCallback<GwtStatus>() {
      @Override
      public void onMessageReceived(GwtStatus message) {
//        Trigger the log viewer
      }
      @Override
      public void onFail(FailureReason reason) {
      }
    });
    
    appContext.getPushChannel().receive("gwt.status", new MessageHandler() {
      @Override
      public void onMessage(String message, ReplySender replySender) {
        Window.alert("gwt! "+message);
      }
    });
    appContext.getMessageFilter().registerMessageRecipient(RoutingTypes.GWTSTATUS, new MessageRecipient<GwtStatus>() {
      @Override
      public void onMessageReceived(GwtStatus message) {
        Window.alert("meow! "+message + " / "+message.getModule());
      }
    });
    
  }

}
