package com.google.collide.server.shared.util;

import static collide.shared.collect.Collections.asArray;

import java.util.ArrayList;
import java.util.List;

import xapi.collect.api.IntTo;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;

import com.google.collide.dto.GwtCompile;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.server.DtoServerImpls.GwtCompileImpl;

public class DtoManifestUtil {


  public static GwtManifest loadCompile(GwtManifest manifest, GwtCompile compile) {
    loadRecompile(manifest, compile);

    manifest.setDeployDir(compile.getDeployDir());
    manifest.setExtraArgs(asArray(compile.getExtraArgs()));
    manifest.setExtrasDir(compile.getExtrasDir());
    manifest.setFragments(compile.getFragments());
    manifest.setGenDir(compile.getGenDir());
    manifest.setGwtVersion(compile.getGwtVersion());
    manifest.setClosureCompiler(compile.isClosureCompiler());
    manifest.setDisableAggressiveOptimize(compile.isDisableAggressiveOptimize());
    manifest.setDisableCastCheck(compile.isDisableCastCheck());
    manifest.setDisableClassMetadata(compile.isDisableClassMetadata());
    manifest.setDisableRunAsync(compile.isDisableRunAsync());
    manifest.setDisableThreadedWorkers(compile.isDisableThreadedWorkers());
    manifest.setDisableUnitCache(compile.isDisableUnitCache());
    manifest.setDraftCompile(compile.isDraftCompile());
    manifest.setEnableAssertions(compile.isEnableAssertions());
    manifest.setSoyc(compile.isSoyc());
    manifest.setSoycDetailed(compile.isSoycDetailed());
    manifest.setStrict(compile.isStrict());
    manifest.setValidateOnly(compile.isValidateOnly());
    manifest.setLocalWorkers(compile.getLocalWorkers());
    manifest.setLogLevel(compile.getLogLevel());
    manifest.setPort(compile.getPort());
    manifest.setSystemProperties(asArray(compile.getSystemProperties()));
    manifest.setUnitCacheDir(compile.getUnitCacheDir());
    manifest.setWorkDir(compile.getWorkDir());
    manifest.setWarDir(compile.getWarDir());
    
    return manifest;
  }
  
  public static GwtManifest loadRecompile(GwtManifest manifest, GwtRecompile compile) {
    manifest.setModuleName(compile.getModule());
    manifest.setAutoOpen(compile.getAutoOpen());
    if (compile.getLogLevel() != null) {
      manifest.setLogLevel(compile.getLogLevel());
    }
    if (compile.getOpenAction() != null) {
      manifest.setOpenAction(compile.getOpenAction());
    }
    if (compile.getObfuscationLevel() != null) {
      manifest.setObfuscationLevel(compile.getObfuscationLevel());
    }
    manifest.setPort(compile.getPort());
    for (String src : compile.getSources().asIterable()) {
      manifest.addSource(src);
    }
    for (String src : compile.getDependencies().asIterable()) {
      X_Log.info(src);
      manifest.addDependency(src);
    }
    return manifest;
  }

  public static GwtManifest newGwtManifest(GwtCompile compileRequest) {
    return loadCompile(new GwtManifest(), compileRequest);
  }

  public static GwtManifest newGwtManifest(GwtRecompile compileRequest) {
    return loadRecompile(new GwtManifest(), compileRequest);
  }
  

  public GwtCompileImpl toDto(GwtManifest m) {
    GwtCompileImpl gwtc = GwtCompileImpl.make();
    gwtc.setAutoOpen(m.isAutoOpen());
    gwtc.setLogLevel(m.getLogLevel());
    gwtc.setModule(m.getModuleName());
    gwtc.setObfuscationLevel(m.getObfuscationLevel());
    gwtc.setOpenAction(m.getOpenAction());
    gwtc.setPort(m.getPort());
    gwtc.setSources(asList(m.getSources()));
    gwtc.setDependencies(asList(m.getDependencies()));
    
    gwtc.setDeployDir(m.getDeployDir());
    gwtc.setExtraArgs(asList(m.getExtraArgs()));
    gwtc.setExtrasDir(m.getExtrasDir());
    gwtc.setFragments(m.getFragments());
    gwtc.setGenDir(m.getGenDir());
    gwtc.setGwtVersion(m.getGwtVersion());
    gwtc.setIsClosureCompiler(m.isClosureCompiler());
    gwtc.setIsDisableAggressiveOptimize(m.isDisableAggressiveOptimize());
    gwtc.setIsDisableCastCheck(m.isDisableCastCheck());
    gwtc.setIsDisableClassMetadata(m.isDisableClassMetadata());
    gwtc.setIsDisableRunAsync(m.isDisableRunAsync());
    gwtc.setIsDisableThreadedWorkers(m.isDisableThreadedWorkers());
    gwtc.setIsDisableUnitCache(m.isDisableUnitCache());
    gwtc.setIsDraftCompile(m.isDraftCompile());
    gwtc.setIsEnableAssertions(m.isEnableAssertions());
    gwtc.setIsRecompile(false);
    gwtc.setIsSoyc(m.isSoyc());
    gwtc.setIsSoycDetailed(m.isSoycDetailed());
    gwtc.setIsStrict(m.isStrict());
    gwtc.setIsValidateOnly(m.isValidateOnly());
    gwtc.setLocalWorkers(m.getLocalWorkers());
    gwtc.setLogLevel(m.getLogLevel());
    gwtc.setPort(m.getPort());
    gwtc.setSystemProperties(asList(m.getSystemProperties()));
    gwtc.setUnitCacheDir(m.getUnitCacheDir());
    
    return gwtc;
  }

  private List<String> asList(IntTo<String> sources) {
    return sources == null ? new ArrayList<String>() : sources.asList();
  }

}
