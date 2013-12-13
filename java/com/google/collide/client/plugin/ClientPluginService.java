package com.google.collide.client.plugin;

import javax.inject.Provider;

import xapi.inject.X_Inject;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.shared.plugin.PublicService;
import com.google.collide.shared.plugin.PublicServices;

public class ClientPluginService {


  protected ClientPluginService() {
  }

  private static final Provider<ClientPluginService> SINGLETON = X_Inject.singletonLazy(ClientPluginService.class);

  public static ClientPluginService initialize(AppContext appContext, MultiPanel<?,?> masterPanel
    , Place workspacePlace) {

    ClientPluginService plugins = SINGLETON.get();
    plugins.init(appContext, masterPanel, workspacePlace);

    return plugins;
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  protected void init(AppContext appContext, MultiPanel<?, ?> masterPanel, Place workspacePlace) {
    for (ClientPlugin<?> plugin : plugins()) {
      plugin.initializePlugin(appContext, masterPanel, workspacePlace);
      PublicService<?>[] services = plugin.getPublicServices();
      if (services != null)
      for (PublicService service : plugin.getPublicServices()) {
        PublicServices.registerService(service.classKey(), service);
      }
    }
  }

  public static ClientPlugin<?>[] getPlugins() {
    return SINGLETON.get().plugins();
  }
  @SuppressWarnings("unchecked")
  public static <T extends ClientPlugin<?>> T getPlugin(Class<T> cls) {
    for (ClientPlugin<?> plugin : SINGLETON.get().plugins()) {
      if (cls.isAssignableFrom(plugin.getClass())) {
        return (T)plugin;
      }
    }
    return null;
  }
  public ClientPlugin<?>[] plugins() {
    return new ClientPlugin[0];
  }

  public void cleanup() {
    //notify any of our plugins that require cleanup.
  }

}
