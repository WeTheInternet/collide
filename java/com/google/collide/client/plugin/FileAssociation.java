package com.google.collide.client.plugin;

public interface FileAssociation {

  boolean matches(String filename);

  //return a form to embed in runtime popup

  //update the form from a give file model


  public static abstract class FileAssociationSuffix implements FileAssociation{
    private String suffix;

    public FileAssociationSuffix(String suffix){
      this.suffix = suffix;
    }
    @Override
    public boolean matches(String filename) {
      return filename.endsWith(suffix);
    }

  }
}
