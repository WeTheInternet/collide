package com.google.gwt.dev.codeserver;

import com.google.collide.plugin.shared.CompiledDirectory;
import com.google.collide.plugin.shared.IsRecompiler;
import org.apache.commons.io.FileUtils;
import xapi.inject.impl.LazyPojo;

import com.google.gwt.dev.cfg.ResourceLoader;
import com.google.gwt.dev.codeserver.Job.Result;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecompileController implements IsRecompiler {

  private Logger log = Logger.getLogger(getClass().getSimpleName());

  private LazyPojo<CompiledDirectory> compileDir =
      new LazyPojo<CompiledDirectory>(){
    @Override
    protected CompiledDirectory initialValue() {
      final Result res = initialize();
      final CompileDir dir = res.outputDir;

      // Try to look up the permutation map from war directory
      File warp = dir.getWarDir();

      File war = new File(warp,getModuleName()+File.separator+"compilation-mappings.txt");
      Map<String, String> permutations = new HashMap<>();
      if (war.exists()) {
        try (
          BufferedReader read = new BufferedReader(new FileReader(war));
        ){
        String line, strongName=null;
        while ((line = read.readLine())!=null) {
          if (line.startsWith("user.agent")) {
            if (strongName != null) {
              if (line.contains("safari")) {
                permutations.put("safari", strongName);
              }
              else if (line.contains("gecko1_8")) {
                permutations.put("gecko1_8", strongName);
              }
              else if (line.contains("gecko")) {
                permutations.put("gecko", strongName);
              }
              else if (line.contains("ie6")) {
                permutations.put("ie6", strongName);
              }
              else if (line.contains("ie8")) {
                permutations.put("ie8", strongName);
              }
              else if (line.contains("ie9")) {
                permutations.put("ie9", strongName);
              }
              else if (line.contains("opera")) {
                permutations.put("opera", strongName);
              }
            }
          } else {
            int ind = line.indexOf(".cache.js");
            if (ind > -1) {
              strongName = line.substring(0, ind);
            }
          }
        }
        }catch (Exception e) {
          e.printStackTrace();
        }
      }

      CompiledDirectory compiled = new CompiledDirectory()
        .setDeployDir(dir.getDeployDir().getAbsolutePath())
        .setExtraDir(dir.getExtraDir().getAbsolutePath())
        .setGenDir(dir.getGenDir().getAbsolutePath())
        .setLogFile(dir.getLogFile().getAbsolutePath())
        .setWarDir(dir.getWarDir().getAbsolutePath())
        .setWorkDir(dir.getWorkDir().getAbsolutePath())
        .setSourceMapDir(dir.findSymbolMapDir(getModuleName()).getAbsolutePath())
        .setUri(getModuleName())
        .setUserAgentMap(permutations)
      ;
      return compiled;
    };
    @Override
    public void reset() {
      //TODO: delete any files, if they exist
      super.reset();
    };
  };
  private final Recompiler recompiler;
  private PrintWriterTreeLogger logger;

  public RecompileController(Recompiler compiler) {
    this.recompiler = compiler;
  }

  public CompiledDirectory recompile(){
    CompiledDirectory toDestroy = null;
    if (compileDir.isSet()) {
      toDestroy = compileDir.get();
    }
    compileDir.reset();
    try {
      return compileDir.get();
    } finally {
      if (toDestroy != null) {
        destroy(toDestroy, 10000);
      }
    }
  }

  /**
   * Kill previous compile in a separate thread, to avoid wasting wall-time.

   * @param dir - The draft compile to destroy.
   */
  private void destroy(final CompiledDirectory dir, final int delay) {
    final File deployDir = new File(dir.getDeployDir());
    final File extraDir = new File(dir.getExtraDir());
    final File genDir = new File(dir.getGenDir());
    final File logDir = new File(dir.getLogFile());
    final File mapDir = new File(dir.getSourceMapDir());
    final File warDir = new File(dir.getWarDir());
    final File workDir = new File(dir.getWorkDir());
    Thread cleanup = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // wait thirty seconds before taking out the old compile
          Thread.sleep(delay);
          destroy(deployDir);
          int perDir = delay/30+1;
          Thread.sleep(perDir);
          destroy(extraDir);
          Thread.sleep(perDir);
          destroy(genDir);
          Thread.sleep(perDir);
          destroy(mapDir);
          Thread.sleep(perDir);
          destroy(warDir);
          Thread.sleep(perDir);
          destroy(workDir);
          Thread.sleep(perDir);
        } catch (InterruptedException e) {Thread.currentThread().interrupt();}
      }

      private void destroy(File file) {
        try {
          FileUtils.deleteDirectory(file);
        } catch (IOException e) {
          System.err.println("Error destroying "+file+" ; ");
          System.err.println("Check if directory is empty? "+file.exists());
        }
      }
    });
    cleanup.start();
  }

  public String getModuleName(){
    return recompiler.getOutputModuleName();
  }
  public ResourceLoader getResourceLoader(){
    return recompiler.getResourceLoader();
  }

  protected Result initialize(){
    Map<String, String> defaultProps = new HashMap<String, String>();
    defaultProps.put("user.agent", "safari,gecko1_8");
    defaultProps.put("locale", "en");
    defaultProps.put("compiler.useSourceMaps", "true");
    Result dir = null;
      try{
        final Options opts = new Options();
        logger = new PrintWriterTreeLogger();
        final Outbox box = new Outbox("id", recompiler, opts, logger);
        final Job job = new Job(box, defaultProps, logger, opts);
        dir = recompiler.recompile(job);
        return dir;
      }catch (Exception e) {
        e.printStackTrace();
        log.log(Level.SEVERE, "Unable to compile module.", e);
        throw new RuntimeException(e);
      }
    }

  public void cleanup() {
    if (compileDir.isSet()) {
      destroy(compileDir.get(), 1);
      compileDir.reset();
    }
  }

}
