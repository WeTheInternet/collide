package com.google.collide.client.plugin;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.Header.Resources;
import com.google.collide.shared.plugin.PublicService;
import com.google.gwt.resources.client.ImageResource;

public interface ClientPlugin <PlaceType extends Place>{

  PlaceType getPlace();

  void initializePlugin(
    AppContext appContext, MultiPanel<?,?> masterPanel, Place currentPlace);

  ImageResource getIcon(Resources res);

  void onClicked(ImageButton button);

  FileAssociation getFileAssociation();

  RunConfiguration getRunConfig();

  PublicService<?> [] getPublicServices();
}
