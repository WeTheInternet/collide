package com.google.collide.shared.plugin;

import javax.inject.Provider;

public interface PublicService <S> extends Provider<S>{

  Class<? super S> classKey();

  int priority();

  public static class DefaultServiceProvider <S> implements PublicService<S> {

    private S service;
    private Class<? super S> key;

    public DefaultServiceProvider(Class<? super S> key, S service) {
      this.service = service;
      this.key = key;
    }

    @Override
    public S get() {
      return service;
    }

    @Override
    public Class<? super S> classKey(){
      return key;
    }

    @Override
    public int priority() {
      return Integer.MIN_VALUE;
    }

  }

}
