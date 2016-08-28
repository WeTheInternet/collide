package com.google.collide.plugin.client.inspector;

import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.FileAssociation;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.Header.Resources;
import com.google.collide.shared.plugin.PublicService;
import com.google.gwt.resources.client.ImageResource;

public class InspectorPlugin 
implements ClientPlugin<InspectorPlace>
{

  @Override
  public InspectorPlace getPlace() {
    return InspectorPlace.PLACE;
  }

  @Override
  public void initializePlugin(
      AppContext appContext, MultiPanel<?, ?> masterPanel, Place currentPlace) {
  }

  @Override
  public ImageResource getIcon(Resources res) {
    return null;
  }

  @Override
  public void onClicked(ImageButton button) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public FileAssociation getFileAssociation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RunConfiguration getRunConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PublicService<?>[] getPublicServices() {
    // TODO Auto-generated method stub
    return null;
  }

}
