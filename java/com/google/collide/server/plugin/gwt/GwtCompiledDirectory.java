package com.google.collide.server.plugin.gwt;



public class GwtCompiledDirectory implements java.io.Serializable, org.vertx.java.core.shareddata.Shareable {

private static final long serialVersionUID = 24353247L;

  protected java.lang.String uri;
  protected java.lang.String warDir;
  protected java.lang.String workDir;
  protected java.lang.String deployDir;
  protected java.lang.String extraDir;
  protected java.lang.String genDir;
  protected java.lang.String logFile;
  protected java.lang.String sourceMapDir;

  public java.lang.String getUri() {
    return uri;
  }

  public GwtCompiledDirectory setUri(java.lang.String v) {
    uri = v;
    return this;
  }

  public java.lang.String getWarDir() {
    return warDir;
  }

  public GwtCompiledDirectory setWarDir(java.lang.String v) {
    warDir = v;
    return this;
  }

  public java.lang.String getWorkDir() {
    return workDir;
  }

  public GwtCompiledDirectory setWorkDir(java.lang.String v) {
    workDir = v;
    return this;
  }

  public java.lang.String getDeployDir() {
    return deployDir;
  }

  public GwtCompiledDirectory setDeployDir(java.lang.String v) {
    deployDir = v;
    return this;
  }


  public java.lang.String getExtraDir() {
    return extraDir;
  }

  public GwtCompiledDirectory setExtraDir(java.lang.String v) {
    extraDir = v;
    return this;
  }

  
  public java.lang.String getGenDir() {
    return genDir;
  }

  public GwtCompiledDirectory setGenDir(java.lang.String v) {
    genDir = v;
    return this;
  }

  
  public java.lang.String getLogFile() {
    return logFile;
  }

  public GwtCompiledDirectory setLogFile(java.lang.String v) {
    logFile = v;
    return this;
  }

  
  public java.lang.String getSourceMapDir() {
    return sourceMapDir;
  }

  public GwtCompiledDirectory setSourceMapDir(java.lang.String v) {
    sourceMapDir = v;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (!(o instanceof GwtCompiledDirectory)) {
      return false;
    }
    GwtCompiledDirectory other = (GwtCompiledDirectory) o;
    if (this.uri == null) {
      if (other.uri != null)
        return false;
    }else if (!this.uri.equals(other.uri)) {
      return false;
    }

    if (this.warDir == null) {
      if (other.warDir != null)
        return false;
    }else if (!this.warDir.equals(other.warDir)) {
        return false;
    }
    
    if (this.workDir == null) {
      if (other.workDir != null)
        return false;
    }else if (!this.workDir.equals(other.workDir)) {
        return false;
    }
    if (this.deployDir == null) {
      if (other.deployDir != null)
        return false;
    }else if (!this.deployDir.equals(other.deployDir)) {
        return false;
    }
    if (this.extraDir == null) {
      if (other.extraDir != null)
        return false;
    }else if (!this.extraDir.equals(other.extraDir)) {
        return false;
    }
    if (this.genDir == null) {
      if (other.genDir != null)
        return false;
    }else if (!this.genDir.equals(other.genDir)) {
        return false;
    }
    if (this.logFile == null) {
      if (other.logFile != null)
        return false;
    }else if (!this.logFile.equals(other.logFile)) {
        return false;
    }
    if (this.sourceMapDir == null) {
      if (other.sourceMapDir != null)
        return false;
    }else if (!this.sourceMapDir.equals(other.sourceMapDir)) {
        return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = hash * 31 + (null == uri ? 0 : uri.hashCode() );
    hash = hash * 31 + (null == warDir ? 0 :warDir.hashCode());
    hash = hash * 31 + (null == workDir ? 0 : workDir.hashCode());
    hash = hash * 31 + (null == deployDir ? 0 : deployDir.hashCode());
    hash = hash * 31 + (null == extraDir ? 0 : extraDir.hashCode());
    hash = hash * 31 + (null == genDir ? 0 : genDir.hashCode());
    hash = hash * 31 + (null == logFile ? 0 : logFile.hashCode());
    hash = hash * 31 + (null == sourceMapDir ? 0 : sourceMapDir.hashCode());
    return hash;
  }

  public int getType() {
    return 129;
  }



  public String toString() {
    StringBuilder b = new StringBuilder();
    
    append(uri,b);
    append(warDir,b);
    append(workDir,b);
    append(deployDir,b);
    append(extraDir,b);
    append(genDir,b);
    append(logFile,b);
    append(sourceMapDir,b);

    return b.toString();
  }

  private void append(String string, StringBuilder b) {
    if (string != null){
      b.append(string.replaceAll(" ", "%20"));
    }
    b.append(" ");
  }

  public static GwtCompiledDirectory fromString(String encoded) {
    GwtCompiledDirectory compile = new GwtCompiledDirectory();
    String[] bits = encoded.split(" ");
    compile.uri = bits[0].replaceAll("%20", " ");
    compile.warDir = bits[1].replaceAll("%20", " ");
    compile.workDir = bits[2].replaceAll("%20", " ");
    compile.deployDir = bits[3].replaceAll("%20", " ");
    compile.extraDir = bits[4].replaceAll("%20", " ");
    compile.genDir = bits[5].replaceAll("%20", " ");
    compile.logFile = bits[6].replaceAll("%20", " ");
    compile.sourceMapDir = bits[7].replaceAll("%20", " ");
    return compile;
  }
  

}