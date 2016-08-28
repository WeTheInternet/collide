package com.google.collide.plugin.shared;

import io.vertx.core.shareddata.Shareable;

import java.util.Map;

public class CompiledDirectory implements java.io.Serializable, Shareable {

private static final long serialVersionUID = 24353247L;

  protected String uri;
  protected String warDir;
  protected String workDir;
  protected String deployDir;
  protected String extraDir;
  protected String genDir;
  protected String logFile;
  protected String sourceDir;
  protected Map<String, String> userAgentMap;
  protected int port;

  public String getUri() {
    return uri;
  }

  public CompiledDirectory setUri(String v) {
    uri = v;
    return this;
  }

  public String getWarDir() {
    return warDir;
  }

  public CompiledDirectory setWarDir(String v) {
    warDir = v;
    return this;
  }

  public String getWorkDir() {
    return workDir;
  }

  public CompiledDirectory setWorkDir(String v) {
    workDir = v;
    return this;
  }

  public String getDeployDir() {
    return deployDir;
  }

  public CompiledDirectory setDeployDir(String v) {
    deployDir = v;
    return this;
  }


  public String getExtraDir() {
    return extraDir;
  }

  public CompiledDirectory setExtraDir(String v) {
    extraDir = v;
    return this;
  }


  public String getGenDir() {
    return genDir;
  }

  public CompiledDirectory setGenDir(String v) {
    genDir = v;
    return this;
  }


  public String getLogFile() {
    return logFile;
  }

  public CompiledDirectory setLogFile(String v) {
    logFile = v;
    return this;
  }


  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @param port the port to set
   * @return
   */
  public CompiledDirectory setPort(int port) {
    this.port = port;
    return this;
  }

  public String getSourceMapDir() {
    return sourceDir;
  }

  public CompiledDirectory setSourceMapDir(String v) {
    sourceDir = v;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) {
      return false;
    }
    if (!(o instanceof CompiledDirectory)) {
      return false;
    }
    CompiledDirectory other = (CompiledDirectory) o;
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
    if (this.sourceDir == null) {
      if (other.sourceDir != null)
        return false;
    }else if (!this.sourceDir.equals(other.sourceDir)) {
        return false;
    }
    return this.port == other.port;
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
    hash = hash * 31 + (null == sourceDir ? 0 : sourceDir.hashCode());
    return hash;
  }

  public int getType() {
    return 129;
  }



  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();

    append(uri,b);
    append(warDir,b);
    append(workDir,b);
    append(deployDir,b);
    append(extraDir,b);
    append(genDir,b);
    append(logFile,b);
    append(sourceDir,b);
    append(String.valueOf(port),b);

    return b.toString();
  }

  private void append(String string, StringBuilder b) {
    if (string != null){
      b.append(string.replaceAll(" ", "%20"));
    }
    b.append(" ");
  }

  public static CompiledDirectory fromString(String encoded) {
    CompiledDirectory compile = new CompiledDirectory();
    String[] bits = encoded.split(" ");
    compile.uri = bits[0].replaceAll("%20", " ");
    compile.warDir = bits[1].replaceAll("%20", " ");
    compile.workDir = bits[2].replaceAll("%20", " ");
    compile.deployDir = bits[3].replaceAll("%20", " ");
    compile.extraDir = bits[4].replaceAll("%20", " ");
    compile.genDir = bits[5].replaceAll("%20", " ");
    compile.logFile = bits[6].replaceAll("%20", " ");
    compile.sourceDir = bits[7].replaceAll("%20", " ");
    compile.port = Integer.parseInt(bits[8]);
    return compile;
  }

  /**
   * @return the userAgentMap
   */
  public Map<String, String> getUserAgentMap() {
    return userAgentMap;
  }

  /**
   * @param userAgentMap the userAgentMap to set
   * @return
   */
  public CompiledDirectory setUserAgentMap(Map<String, String> userAgentMap) {
    this.userAgentMap = userAgentMap;
    return this;
  }



}
