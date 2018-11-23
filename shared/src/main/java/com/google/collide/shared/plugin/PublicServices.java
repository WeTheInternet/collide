package com.google.collide.shared.plugin;

import com.google.common.base.Preconditions;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.Out1;
import xapi.inject.X_Inject;

import javax.inject.Provider;

public class PublicServices {

  private final IntTo<PublicService<?>> services;

  private static final Out1<PublicServices> SINGLETON = X_Inject.singletonLazy(PublicServices.class);

  //don't construct these, we're running statically
  protected PublicServices() {
    services = X_Collect.newList(PublicService.class);
  }

  @SuppressWarnings("unchecked")
  public static <S> S getService(Class<? super S> serviceClass) {
    PublicService<?> service = SINGLETON.out1().services.get(serviceClass.hashCode());
    Preconditions.checkNotNull(service, "No service implementation registered for "+serviceClass);
    return (S)service.get();
  }

  public static <S> PublicService<S> createProvider(
      Class<? super S> serviceClass, S singleton) {
    return new PublicService.DefaultServiceProvider<>(serviceClass, singleton);
  }
  public static <S> void registerService(
    Class<? super S> serviceClass, PublicService<S> provider) {
    IntTo<PublicService<?>> serviceMap = SINGLETON.out1().services;
    PublicService<?> old = serviceMap.get(serviceClass.hashCode());
    if (old != null) {
      if (old.priority() > provider.priority())
        return;
    }
    serviceMap.set(serviceClass.hashCode(), provider);
  }


}
