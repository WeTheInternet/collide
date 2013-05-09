package com.google.collide.client.plugin;

import com.google.collide.client.AppContext;
import com.google.collide.client.util.PathUtil;

import elemental.dom.Element;

public interface RunConfiguration {

  String getId();

  String getLabel();

  void run(AppContext appContext, PathUtil file);

  Element getForm();



}
