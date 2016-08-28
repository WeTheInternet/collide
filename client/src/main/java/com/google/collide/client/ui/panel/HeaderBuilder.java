package com.google.collide.client.ui.panel;

import elemental.dom.Element;

public interface HeaderBuilder {

  Element buildHeader(String title, Panel.Css css, Panel<?,?> into);

}
