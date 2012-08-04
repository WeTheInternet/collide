package com.google.collide.shared.util;

public interface Channel <T>{
  public void send(T t);
  public T receive();
}
