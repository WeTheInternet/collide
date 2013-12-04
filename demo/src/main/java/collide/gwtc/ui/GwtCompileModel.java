package collide.gwtc.ui;

import java.util.Iterator;

import xapi.gwtc.api.ObfuscationLevel;
import xapi.gwtc.api.OpenAction;

import com.google.gwt.core.ext.TreeLogger.Type;

import elemental.util.ArrayOfString;
import elemental.util.Collections;

public class GwtCompileModel {

  private final class ClasspathIterable implements Iterable<String> {
    private final class Itr implements Iterator<String> {
      ArrayOfString src = getSources();
      ArrayOfString dep = getDependencies();
      int pos = 0;
      @Override
      public boolean hasNext() {
        if (dep == null) {
          return pos < src.length();
        }
        if (dep.isEmpty()) {
          dep = null;
          return pos < src.length();
        }
        if (pos == src.length()) {
          src = dep;
          pos = 0;
          return !src.isEmpty();
        }
        return true;
      }

      @Override
      public String next() {
        return src.get(pos++);
      }

      @Override
      public void remove() {}
    }
    @Override
    public Iterator<String> iterator() {
      return new Itr();
    }

  }
  private static final String ARG_AGGRESSIVE_OPTIMIZE = "XdisableAggressiveOptimization";
  private static final String ARG_AUTO_OPEN = "autoOpen";
  private static final String ARG_CAST_CHECKING = "XdisableCastChecking";
  private static final String ARG_CAST_METADATA = "XdisableClassMetadata";
  private static final String ARG_DEPENDENCIES = "dependencies";
  private static final String ARG_DEPLOY_DIR = "deployDir";
  private static final String ARG_DISABLE_THREADED = "disableThreadedWorkers";
  private static final String ARG_DRAFT_COMPILE = "draftCompile";
  private static final String ARG_ENABLE_ASSERT = "ea";
  private static final String ARG_ENABLE_CLOSURE = "XenableClosureCompiler";
  private static final String ARG_EXTRA_ARGS = "extraArgs";
  private static final String ARG_EXTRAS_DIR = "extra";
  private static final String ARG_FRAGMENTS = "XfragmentCount";
  private static final String ARG_GEN_DIR = "genDir";
  private static final String ARG_JVM_ARGS = "jvmArgs";
  private static final String ARG_LOCAL_WORKERS = "localWorkers";
  private static final String ARG_LOG_LEVEL = "logLevel";
  private static final String ARG_OBFUSCATION_LEVEL = "style";
  private static final String ARG_OPEN_ACTION = "openAction";
  private static final String ARG_OPTIMIZATION_LEVEL = "optimize";
  private static final String ARG_RUN_ASYNC_ENABLED = "XdisableRunAsync";
  private static final String ARG_SOURCE_DIRS = "src";
  private static final String ARG_SOYC = "soyc";
  private static final String ARG_SOYC_DETAILED = "XsoycDetailed";
  private static final String ARG_STRICT = "strict";
  private static final String ARG_SYS_PROPS = "sysProps";
  private static final String ARG_URL_TO_OPEN = "url";
  private static final String ARG_VALIDATE_ONLY = "validate";
  private static final String ARG_WORK_DIR = "workDir";
  private static final String NEW_LINE = "\n ";
  private static final String NEW_ITEM = NEW_LINE + "- ";
  private static final String NEW_LIST_ITEM = NEW_LINE + " - ";
  
  private boolean autoOpen;
  private boolean closureCompiler;
  private ArrayOfString dependencies = Collections.arrayOfString();
  private String deployDir;
  private boolean disableAggressiveOptimize;
  private boolean disableCastCheck;
  private boolean disableClassMetadata;
  private boolean disableRunAsync;
  private boolean disableThreadedWorkers;
  private boolean draftCompile;
  private boolean enableAssertions;
  private ArrayOfString extraArgs = Collections.arrayOfString();
  private String extrasDir;
  private int fragments = 10;
  private String genDir;
  private ArrayOfString jvmArgs = Collections.arrayOfString();
  private int localWorkers = 6;
  private Type logLevel = Type.INFO;
  private String moduleName;
  private ObfuscationLevel obfuscationLevel = ObfuscationLevel.PRETTY;
  private OpenAction openAction = OpenAction.IFRAME;
  private int optimizationLevel = 9;
  private ArrayOfString sources = Collections.arrayOfString();
  private boolean soyc;
  private boolean soycDetailed;
  private boolean strict;
  private ArrayOfString systemProperties = Collections.arrayOfString();
  private String urlToOpen = "/static/$module/index.html";
  private boolean validateOnly;
  private String workDir;

  public GwtCompileModel() {
  }
  public GwtCompileModel(String moduleName) {
    this.moduleName = moduleName;
  }
  
  public GwtCompileModel addDependency(String dep) {
    assert !dependencies.contains(dep) : "Dependencies already contains "+dep;
    dependencies.push(dep);
    return this;
  }
  public GwtCompileModel addExtraArg(String extraArg) {
    assert !extraArgs.contains(extraArg) : "Extra args already contains "+extraArg;
    extraArgs.push(extraArg);
    return this;
  }
  
  public GwtCompileModel addJvmArg(String jvmArg) {
    assert !jvmArgs.contains(jvmArg) : "Jvm args already contains "+jvmArg;
    jvmArgs.push(jvmArg);
    return this;
  }
  public GwtCompileModel addSource(String src) {
    assert !sources.contains(src) : "Sources already contains "+src;
    sources.push(src);
    return this;
  }
  public GwtCompileModel addSystemProp(String sysProp) {
    assert !systemProperties.contains(sysProp) : "System Properties already contains "+sysProp;
    systemProperties.push(sysProp);
    return this;
  }
  public GwtCompileModel clearDependencies() {
    dependencies.setLength(0);
    return this;
  }
  public GwtCompileModel clearExtraArgs() {
    extraArgs.setLength(0);
    return this;
  }
  
  public GwtCompileModel clearJvmArgs() {
    jvmArgs.setLength(0);
    return this;
  }
  public GwtCompileModel clearSources() {
    sources.setLength(0);
    return this;
  }
  public GwtCompileModel clearSystemProps() {
    systemProperties.setLength(0);
    return this;
  }

  public ArrayOfString getDependencies() {
    return dependencies;
  }

  public String getDeployDir() {
    return deployDir;
  }

  public ArrayOfString getExtraArgs() {
    return extraArgs;
  }

  public String getExtrasDir() {
    return extrasDir;
  }

  public int getFragments() {
    return fragments;
  }

  public String getGenDir() {
    return genDir;
  }

  public int getLocalWorkers() {
    return localWorkers;
  }

  public Type getLogLevel() {
    return logLevel;
  }

  public String getModule() {
    return moduleName;
  }

  public ObfuscationLevel getObfuscationLevel() {
    return obfuscationLevel;
  }

  public OpenAction getOpenAction() {
    return openAction;
  }

  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  public ArrayOfString getSources() {
    return sources;
  }

  public ArrayOfString getSystemProperties() {
    return systemProperties;
  }

  public String getUrlToOpen() {
    return urlToOpen;
  }

  public String getWorkDir() {
    return workDir;
  }

  public boolean isAutoOpen() {
    return autoOpen;
  }

  public boolean isClosureCompiler() {
    return closureCompiler;
  }
  public boolean isDisableAggressiveOptimize() {
    return disableAggressiveOptimize;
  }
  public boolean isDisableCastCheck() {
    return disableCastCheck;
  }
  public boolean isDisableClassMetadata() {
    return disableClassMetadata;
  }
  public boolean isDisableRunAsync() {
    return disableRunAsync;
  }
  public boolean isDisableThreadedWorkers() {
    return disableThreadedWorkers;
  }
  public boolean isDraftCompile() {
    return draftCompile;
  }
  public boolean isEnableAssertions() {
    return enableAssertions;
  }
  public boolean isSoyc() {
    return soyc;
  }
  public boolean isSoycDetailed() {
    return soycDetailed;
  }
  public boolean isStrict() {
    return strict;
  }
  public boolean isValidateOnly() {
    return validateOnly;
  }
  public GwtCompileModel setAutoOpen(boolean autoOpen) {
    this.autoOpen = autoOpen;
    return this;
  }
  public GwtCompileModel setClosureCompiler(boolean closureCompiler) {
    this.closureCompiler = closureCompiler;
    return this;
  }
  public GwtCompileModel setDependencies(ArrayOfString dependencies) {
    this.dependencies = dependencies;
    return this;
  }
  public GwtCompileModel setDeployDir(String deployDir) {
    this.deployDir = deployDir;
    return this;
  }
  public GwtCompileModel setDisableAggressiveOptimize(boolean disableAggressiveOptimize) {
    this.disableAggressiveOptimize = disableAggressiveOptimize;
    return this;
  }
  public GwtCompileModel setDisableCastCheck(boolean disableCastCheck) {
    this.disableCastCheck = disableCastCheck;
    return this;
  }
  public GwtCompileModel setDisableClassMetadata(boolean disableClassMetadata) {
    this.disableClassMetadata = disableClassMetadata;
    return this;
  }
  public GwtCompileModel setDisableRunAsync(boolean disableRunAsync) {
    this.disableRunAsync = disableRunAsync;
    return this;
  }
  public GwtCompileModel setDisableThreadedWorkers(boolean disableThreadedWorkers) {
    this.disableThreadedWorkers = disableThreadedWorkers;
    return this;
  }
  public GwtCompileModel setDraftCompile(boolean draftCompile) {
    this.draftCompile = draftCompile;
    return this;
  }
  public GwtCompileModel setEnableAssertions(boolean enableAssertions) {
    this.enableAssertions = enableAssertions;
    return this;
  }
  public GwtCompileModel setExtraArgs(ArrayOfString extraArgs) {
    this.extraArgs = extraArgs;
    return this;
  }
  public GwtCompileModel setExtrasDir(String extrasDir) {
    this.extrasDir = extrasDir;
    return this;
  }
  public GwtCompileModel setFragments(int fragments) {
    this.fragments = fragments;
    return this;
  }
  public GwtCompileModel setGenDir(String genDir) {
    this.genDir = genDir;
    return this;
  }
  public GwtCompileModel setLocalWorkers(int localWorkers) {
    this.localWorkers = localWorkers;
    return this;
  }
  public GwtCompileModel setLogLevel(Type logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  public GwtCompileModel setModuleName(String moduleName) {
    this.moduleName = moduleName;
    return this;
  }

  public GwtCompileModel setObfuscationLevel(ObfuscationLevel obfuscationLevel) {
    this.obfuscationLevel = obfuscationLevel;
    return this;
  }

  public GwtCompileModel setOpenAction(OpenAction openAction) {
    this.openAction = openAction;
    return this;
  }

  public GwtCompileModel setOptimizationLevel(int optimizationLevel) {
    this.optimizationLevel = optimizationLevel;
    return this;
  }

  public GwtCompileModel setSources(ArrayOfString sources) {
    this.sources = sources;
    return this;
  }

  public GwtCompileModel setSoyc(boolean soyc) {
    this.soyc = soyc;
    return this;
  }

  public GwtCompileModel setSoycDetailed(boolean soycDetailed) {
    this.soycDetailed = soycDetailed;
    return this;
  }

  public GwtCompileModel setStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

  public GwtCompileModel setSystemProperties(ArrayOfString systemProperties) {
    this.systemProperties = systemProperties;
    return this;
  }

  public GwtCompileModel setUrlToOpen(String urlToOpen) {
    this.urlToOpen = urlToOpen;
    return this;
  }

  public GwtCompileModel setValidateOnly(boolean validateOnly) {
    this.validateOnly = validateOnly;
    return this;
  }

  public GwtCompileModel setWorkDir(String workDir) {
    this.workDir = workDir;
    return this;
  }
  
  @Override
  public String toString() {
    assert moduleName != null : "ModuleName is the only field that cannot be null";
    StringBuilder b = new StringBuilder(moduleName);
    b.append(":");
    if (deployDir != null) {
      b.append(NEW_LINE).append(ARG_DEPLOY_DIR).append(": ").append(deployDir);
    }
    if (extrasDir != null) {
      b.append(NEW_LINE).append(ARG_EXTRAS_DIR).append(": ").append(extrasDir);
    }
    if (fragments != 10) {
      b.append(NEW_LINE).append(ARG_FRAGMENTS).append(": ").append(fragments);
    }
    if (genDir != null) {
      b.append(NEW_LINE).append(ARG_GEN_DIR).append(": ").append(genDir);
    }
    if (localWorkers != 6) {
      b.append(NEW_LINE).append(ARG_LOCAL_WORKERS).append(": ").append(localWorkers);
    }
    if (logLevel != Type.INFO) {
      b.append(NEW_LINE).append(ARG_LOG_LEVEL).append(": ").append(logLevel.ordinal());
    }
    if (obfuscationLevel != ObfuscationLevel.PRETTY) {
      b.append(NEW_LINE).append(ARG_OBFUSCATION_LEVEL).append(": ").append(obfuscationLevel.ordinal());
    }
    if (openAction != OpenAction.IFRAME) {
      b.append(NEW_LINE).append(ARG_OPEN_ACTION).append(": ").append(openAction.ordinal());
    }
    if (optimizationLevel != 9) {
      b.append(NEW_LINE).append(ARG_OPTIMIZATION_LEVEL).append(": ").append(optimizationLevel);
    }
    if (urlToOpen != null) {
      b.append(NEW_LINE).append(ARG_URL_TO_OPEN).append(": ").append(urlToOpen);
    }
    if (workDir != null) {
      b.append(NEW_LINE).append(ARG_WORK_DIR).append(": ").append(workDir);
    }
    if (dependencies.length() > 0) {
      b.append("\n ").append(ARG_DEPENDENCIES).append(":");
      for (int i = 0, m = dependencies.length(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(dependencies.get(i));
      }
      b.append("\n");
    }
    if (extraArgs.length() > 0) {
      b.append("\n ").append(ARG_EXTRA_ARGS).append(":");
      for (int i = 0, m = extraArgs.length(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(extraArgs.get(i));
      }
      b.append("\n");
    }
    if (jvmArgs.length() > 0) {
      b.append("\n ").append(ARG_JVM_ARGS).append(":");
      for (int i = 0, m = jvmArgs.length(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(jvmArgs.get(i));
      }
      b.append("\n");
    }
    if (sources.length() > 0) {
      b.append("\n ").append(ARG_SOURCE_DIRS).append(":");
      for (int i = 0, m = sources.length(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(sources.get(i));
      }
      b.append("\n");
    }
    if (systemProperties.length() > 0) {
      b.append("\n ").append(ARG_SYS_PROPS).append(":");
      for (int i = 0, m = systemProperties.length(); i < m; i++) {
        b.append(NEW_LIST_ITEM).append(systemProperties.get(i));
      }
      b.append("\n");
    }
    if (autoOpen) {
      b.append(NEW_ITEM).append(ARG_AUTO_OPEN);
    }
    if (closureCompiler) {
      b.append(NEW_ITEM).append(ARG_ENABLE_CLOSURE);
    }
    if (disableAggressiveOptimize) {
      b.append(NEW_ITEM).append(ARG_AGGRESSIVE_OPTIMIZE);
    }
    if (disableCastCheck) {
      b.append(NEW_ITEM).append(ARG_CAST_CHECKING);
    }
    if (disableClassMetadata) {
      b.append(NEW_ITEM).append(ARG_CAST_METADATA);
    }
    if (disableRunAsync) {
      b.append(NEW_ITEM).append(ARG_RUN_ASYNC_ENABLED);
    }
    if (draftCompile) {
      b.append(NEW_ITEM).append(ARG_DRAFT_COMPILE);
    }
    if (enableAssertions) {
      b.append(NEW_ITEM).append(ARG_ENABLE_ASSERT);
    }
    if (soyc) {
      b.append(NEW_ITEM).append(ARG_SOYC);
    }
    if (soycDetailed) {
      b.append(NEW_ITEM).append(ARG_SOYC_DETAILED);
    }
    if (strict) {
      b.append(NEW_ITEM).append(ARG_STRICT);
    }
    if (disableThreadedWorkers) {
      b.append(NEW_ITEM).append(ARG_DISABLE_THREADED);
    }
    if (validateOnly) {
      b.append(NEW_ITEM).append(ARG_VALIDATE_ONLY);
    }
    
    return b.toString();
  }

  public ArrayOfString getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(ArrayOfString jvmArgs) {
    this.jvmArgs = jvmArgs;
  }
  public Iterable<String> getClasspath() {
    return new ClasspathIterable();
  }
  
}
