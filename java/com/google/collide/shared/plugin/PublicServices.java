package com.google.collide.shared.plugin;

import javax.inject.Provider;

import xapi.inject.X_Inject;

import com.google.common.base.Preconditions;

import elemental.util.Collections;
import elemental.util.MapFromIntTo;

public class PublicServices {

  private final MapFromIntTo<PublicService<?>> services;

  private static final Provider<PublicServices> SINGLETON = X_Inject.singletonLazy(PublicServices.class);

  //don't construct these, we're running statically
  protected PublicServices() {
    services = Collections.mapFromIntTo();
  }

  @SuppressWarnings("unchecked")
  public static <S> S getService(Class<? super S> serviceClass) {
    PublicService<?> service = SINGLETON.get().services.get(serviceClass.hashCode());
    Preconditions.checkNotNull(service, "No service implementation registered for "+serviceClass);
    return (S)service.get();
  }

  public static <S> PublicService<S> createProvider(
      Class<? super S> serviceClass, S singleton) {
    return new PublicService.DefaultServiceProvider<S>(serviceClass, singleton);
  }
  public static <S> void registerService(
    Class<? super S> serviceClass, PublicService<S> provider) {
    MapFromIntTo<PublicService<?>> serviceMap = SINGLETON.get().services;
    PublicService<?> old = serviceMap.get(serviceClass.hashCode());
    if (old != null) {
      if (old.priority() > provider.priority())
        return;
    }
    serviceMap.put(serviceClass.hashCode(), provider);
  }


}
