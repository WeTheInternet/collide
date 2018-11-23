package com.google.collide.server.maven;

import xapi.fu.Lazy;
import xapi.fu.lazy.ResettableLazy;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 *
 * Project configuration settings to use during maven builds.
 *
 * These settings can be used for any multi-module project configuration.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class MavenResources implements Serializable{
  private static final long serialVersionUID = -8124340238103827275L;
  private String srcRoot="";
  private String warSrcDir="";
  private String warTargetDir="";
  private String workDir="";


  private final ResettableLazy<File> srcRootFolder = new ResettableLazy<>(this::initSourceRoot);

  private final ResettableLazy<File> warSrcFolder = new ResettableLazy<>(this::initWarSource);

  private final ResettableLazy<File> warTargetFolder = new ResettableLazy<>(this::initWarTarget);

  private final ResettableLazy<File> workFolder = new ResettableLazy<>(this::initWorkFolder);

  /**
   * @return the srcRoot
   */
  public File getSrcRoot() {
    return srcRootFolder.out1();
  }

  /**
   * @param srcRoot the srcRoot to set
   */
  public void setSrcRoot(String srcRoot) {
    this.srcRoot = srcRoot;
    srcRootFolder.reset();
  }

  /**
   * @return the war Source Directory
   *
   * This is the folder containing your "clean" Web App Resource files (no generated source)
   */
  public File getWarSrcDir() {
    return warSrcFolder.out1();
  }

  /**
   * @param warSrcDir the war Source Directory to set
   */
  public void setWarSrcDir(String warSrcDir) {
    this.warSrcDir = warSrcDir;
    warSrcFolder.reset();
  }

  /**
   * @return the war Target Directory
   *
   * This is the folder to which your clean Web App BaseResources will be merged with generated war.
   */
  public File getWarTargetDir() {
    return warTargetFolder.out1();
  }

  /**
   * @param warTargetDir the war Target Directory to set
   */
  public void setWarTargetDir(String warTargetDir) {
    this.warTargetDir = warTargetDir;
    warTargetFolder.reset();
  }

  /**
   * @return the workDir, where temporary files are written.
   */
  public File getWorkDir() {
    return workFolder.out1();
  }

  /**
   * @param workDir the work Directory to set
   */
  public void setWorkDir(String workDir) {
    this.workDir = workDir;
    workFolder.reset();
  }


  //One-time initialization methods; only called the first time a property is accessed,
  //and the next access after a property is set.



  protected File initSourceRoot() {
    if (srcRoot.length()==0){
      throw new RuntimeException("You MUST specifiy a source root directory to use MavenConfig.  ");
    }
    File file = new File(srcRoot);
    if (!file.exists())
      if (!file.mkdirs())
        throw new Error("Could not create MavenConfig source root directory: "+file+".  Please ensure this file exists, and is writable.");
    return file;
  }

  protected File initWarSource() {
    if (warSrcDir.length()==0){
      warSrcDir = "war";
    }
    File file = new File(warSrcDir);
    if (!file.exists()){//absolute lookup failed; try relative
      file = new File(getSrcRoot(),warSrcDir);
    }
    if (!file.exists())
      if (!file.mkdirs())
        throw new Error("Could not create war source directory: "+file+".  Please ensure this file exists, and is writable.");
    return file;
  }

  protected File initWarTarget() {
    if (warTargetDir.length()==0){
      warTargetDir = "war";
    }
    File file = new File(warTargetDir);
    if (!file.exists()){//absolute lookup failed; try relative
      file = new File(getSrcRoot(),warTargetDir);
    }
    if (!file.exists())
      if (!file.mkdirs())
        throw new Error("Could not create war target directory: "+file+".  Please ensure this file exists, and is writable.");
    return file;
  }

  protected File initWorkFolder() {
    if (workDir.length()==0){
      workDir = "/tmp";
    }
    File file = new File(workDir);
    if (!file.exists()){//absolute lookup failed; try relative
      try {
        //create new temp file to detect /tmp folder directory.
        file = File.createTempFile("project", "tmp");
        file.deleteOnExit();//clean up after ourselves
        file = file.getParentFile();
        file.deleteOnExit();
      } catch (IOException e) {
        throw new Error("Could not create work directory: "+file+".  Please ensure this file exists, and is writable.",e);
      }
    }
    return file;
  }


}
