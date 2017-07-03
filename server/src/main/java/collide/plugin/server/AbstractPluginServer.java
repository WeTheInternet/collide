package collide.plugin.server;

import com.google.collide.dto.CodeModule;
import com.google.collide.dto.server.DtoServerImpls.LogMessageImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.server.shared.BusModBase;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.log.X_Log;
import xapi.mvn.X_Maven;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_String;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractPluginServer // <C extends AbstractCompileThread<?>>
    extends BusModBase implements ServerPlugin {

  protected String libRoot;

  protected String webRoot;

  protected Lazy<List<String>> vertxJars = Lazy.deferred1(()->{
      // TODO: load vertx version from gradle.properties...
      ArtifactResult artifact = X_Maven.loadArtifact("io.vertx", "vertx-core", "3.3.2");
      List<String> cp = X_Maven.loadCompileDependencies(artifact.getArtifact());
//      artifact = X_Maven.loadArtifact("xerces", "xercesImpl", "2.11.0");
//      cp = X_Maven.loadCompileDependencies(artifact.getArtifact());
      return Collections.unmodifiableList(cp);
  });

  protected Lazy<List<String>> gwtJars = Lazy.deferred1(()->{
      ArtifactResult artifact = X_Maven.loadArtifact("net.wetheinter", "gwt-dev", X_Namespace.GWT_VERSION);
      Set<String> cp = new LinkedHashSet<>();
      cp.addAll(X_Maven.loadCompileDependencies(artifact.getArtifact()));
      artifact = X_Maven.loadArtifact("net.wetheinter", "gwt-user", X_Namespace.GWT_VERSION);
      cp.addAll(X_Maven.loadCompileDependencies(artifact.getArtifact()));
      artifact = X_Maven.loadArtifact("net.wetheinter", "gwt-codeserver", X_Namespace.GWT_VERSION);
      cp.addAll(X_Maven.loadCompileDependencies(artifact.getArtifact()));
      return Collections.unmodifiableList(new ArrayList<>(cp));
  });

  protected Lazy<List<String>> xapiGwtJar = Lazy.deferred1(()->{
      ArtifactResult artifact = X_Maven.loadArtifact("net.wetheinter", "xapi-gwt", X_Namespace.XAPI_VERSION);
      List<String> cp = X_Maven.loadCompileDependencies(artifact.getArtifact());
      return Collections.unmodifiableList(cp);
  });

  protected Lazy<String> serverJar = Lazy.deferred1(()->{
    ProtectionDomain domain = AbstractPluginServer.class.getProtectionDomain();
    if (domain != null) {
      CodeSource source = domain.getCodeSource();
      if (source != null) {
        URL location = source.getLocation();
        if (location != null) {
          return location.getPath().replace("-fat", "");
        }
      }
    }
    List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
    for (Iterator<String> itr = args.iterator(); itr.hasNext();) {
      if ("-jar".equals(itr.next())) {
        return itr.next().replace("-fat", "");
      }
    }
    throw new IllegalStateException("Unable to find server jar from runtime arguments: " + args);
  });

  protected static final String AUTH_COOKIE_NAME = "_COLLIDE_SESSIONID";

  @Override
  public abstract String getAddressBase();

  @Override
  public abstract Map<String,Handler<Message<JsonObject>>> getHandlers();


  @Override
  public void initialize(Vertx vertx) {
    this.eb = vertx.eventBus();
    String pluginBase = getAddressBase();
    for (Map.Entry<String, Handler<Message<JsonObject>>> handle : getHandlers().entrySet()){
      vertx.eventBus().consumer(pluginBase+"."+handle.getKey(), handle.getValue());
    }
  }

  public List<URL> getCompilerClasspath(final CodeModule request, final ReceivesValue<String> logger) {
    List<URL> list = new ArrayList<URL>(){
      private static final long serialVersionUID = 7809897000236224683L;
      @Override
      public boolean add(URL e) {
        if (e==null)return false;
        logger.set(
            LogMessageImpl.make()
            .setLogLevel(Type.TRACE)
            .setMessage("Adding "+e.toExternalForm()+" to classpath")
            .setModule(request.getModule())
        .toJson());
        return super.add(e);
      }
    };

    File webDir = new File(webRoot);
    File libDir = new File(libRoot);

    Set<String> dedup = new LinkedHashSet<>();//we want duplicate removal and deterministic ordering.

    boolean hadGwt = false;
    boolean hadXapi = false;

    // TODO add the jar containing the compiler class, if it exists on the classpath

    //add super-sources first (todo: implement)

    //add source folders
    for (String cp : request.getSources().asIterable()){
      if (!cp.endsWith(".jar")){
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src folder",cp, url);
          list.add(url);
        }
      }
    }

    //now, add all the jars listed as source
    for (String cp : request.getSources().asIterable()){

      if (cp.endsWith(".jar")) {
        if (cp.contains("xapi")) {
          hadXapi= true;
        }
        if (cp.contains("gwt-dev")) {
          hadGwt = true;
        }
        URL url = toUrl(webDir,cp);
        if (url != null && dedup.add(url.toExternalForm())){
          X_Log.debug("Adding src jar",cp, url);
          list.add(url);//needed for collide compile
        }
      }
    }

    String xapiVersion = System.getProperty("xapi.version", X_Namespace.XAPI_VERSION);//Hardcode X_Namespace.XAPI_VERSION for now
    if (!X_String.isEmpty(xapiVersion) && !hadXapi) {
      // only adds the uber jar if you did not depend on any xapi jars directly.
      // for smaller classpaths, you can specify a smaller subset of xapi dependencies;
      // the minimum recommended artifact to take is xapi-gwt-api.
      list.addAll(toUrl(dedup, xapiGwtJar.out1()));
    }

    if (!hadGwt) {
      list.addAll(toUrl(dedup, gwtJars.out1()));
    }

    JsonArray<String> deps = request.getDependencies();
    if (deps != null) {
      for (String cp : deps.asIterable()){
        URL url = toUrl(libDir,cp);
        if (url != null && dedup.add(url.toExternalForm())) {
          if (cp.endsWith(".jar")){
            X_Log.debug("Adding dependency jar",cp, url);
            list.add(url);//needed for collide compile
          }else{
            X_Log.debug("Adding dependency folder",cp, url);
            list.add(url);//needed for collide compile
          }
        }
      }

      try {
        list.add(new File(serverJar.out1()).toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }


      //required to run vertx threads
      for (String dep : vertxJars.out1()) {
        try {
          list.add(new File(dep).toURI().toURL());
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }

      //clear our deps and put our entire resolved classpath back
      deps.clear();
      for (URL url : list){
        deps.add(url.toExternalForm().replace("file:", ""));
      }
    }

    X_Log.warn(getClass(), list);

    return list;
  }

  protected List<URL> toUrl(Set<String> dedup, List<String> strings) {
    return strings
        .stream()
        .map(s -> s.indexOf(":") == -1 ? "file:" + s : s )
        .map(s -> {
          if (!dedup.add(s)) {
            return null;
          }
          try {
            return new URL(s);
          } catch (MalformedURLException e) {
            throw new RuntimeException(e);
          }
        })
        .filter(X_Fu::notNull)
        .collect(Collectors.toList());
  }

  public File getWebRoot() {
    return new File(webRoot);
  }

  public File getLibRoot() {
    return new File(libRoot);
  }

  @Override
  public void start() {
    super.start();
    libRoot = getMandatoryStringConfig("staticFiles");
    int ind = libRoot.indexOf("static");
    if (ind > 0)
      libRoot = libRoot.substring(0, ind);
    libRoot = libRoot + "lib"+File.separator;


    webRoot = getMandatoryStringConfig("webRoot");

    initialize(vertx);
  }


  protected URL toUrl(File cwd, String jar) {
    //TODO: allow certain whitelisted absolute uris
    //TODO: allow "virtual" filesystem uris, like ~/, /bin/, /lib/, /war/

    String path = cwd.getAbsolutePath();
    File file = new File(jar);
    try {
      if (file.exists()) {
          file = file.getCanonicalFile();
      } else {
        file = new File(path, jar).getCanonicalFile();
      }
    } catch (IOException e) {
      X_Log.warn(getClass(), "Error resolving canonical file for",file);
    }
    X_Log.trace(getClass(), "Resolving ",jar," to ", file);
    if (file.exists()){
      logger.info(getClass(), "Classpath file exists: "+file);
    }else{
      logger.warn(getClass(), "Classpath file does not exist! "+file);
      return null;
    }
    URI uri = URI.create("file:"+file.getAbsolutePath());
    try{
      return uri.toURL();
    }catch (Exception e) {
      e.printStackTrace();
      throw X_Debug.rethrow(e);
    }
  }

  public EventBus getEventBus() {
    return eb;
  }


}
