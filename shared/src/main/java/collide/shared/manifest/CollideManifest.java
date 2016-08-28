package collide.shared.manifest;

import static xapi.util.X_String.join;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import elemental.util.ArrayOf;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

public class CollideManifest {

  protected static final char separator = '\t';// use tabs as separator, so spaces can be used within items.
  protected static final String gwtEntry = "gwt"+separator;
  protected static final String gwtc = "gwtc\n";
  
  public static class GwtEntry {
    public final String[] modules;
    public final String[] sources;
    public final String[] dependencies;
    public GwtEntry(String modules, String sourcepath, String classpath, String extra) {
      this.modules = modules.split("\\s+");
      sources = sourcepath.split(";");// We use ; for path, so : can be used for maven artifacts
      dependencies = classpath.split(";");
    }
    @Override
    public String toString() {
      return gwtEntry+join(" ", modules)+separator+join(";", sources)+separator+join(";", dependencies);
    }
  }
  
  ArrayOf<GwtEntry> gwtEntries = Collections.arrayOf();
  MapFromStringTo<GwtManifest> gwtcEntries = Collections.mapFromStringTo();
  
  public CollideManifest(String raw) {
    parse(raw);
  }

  protected void parse(String raw) {
    if (raw.length()==0)return;
    for (String cmd : raw.split("\n")) {
      addEntry(cmd);
    }
  }

  public void addEntry(String cmd) {
    cmd = cmd.replaceAll("\\s*$", "");
    
    if (cmd.startsWith(gwtEntry)) {
      parseGwtEntry(cmd.substring(gwtEntry.length()));
    }
  }
  public void addGwtEntry(GwtEntry cmd) {
    for (int i = gwtEntries.length();i --> 0;){
      if (equal(gwtEntries.get(i).modules, cmd.modules)) {
        gwtEntries.removeByIndex(i);
        break;
      }
    }
    gwtEntries.push(cmd);
  }

  private boolean equal(String[] arr1, String[] arr2) {
    int i = arr1.length;
    if (i != arr2.length)
      return false;
    for (;i-->0;)
      if (!arr1[i].equals(arr2[i]))
        return false;
    return true;
  }

  protected void parseGwtEntry(String entry) {
    int from = entry.indexOf(separator);
    if (from == -1 && bail("gwt-module",entry))
      return;
    String modules = entry.substring(0, from);

    int to = entry.indexOf(separator, ++from);
    if (to == -1 && bail("gwt-sourcepath",entry))
      return;
    String sourcepath = entry.substring(from, to);
    
    from = to+1; // keep the syntax sane
    to = entry.indexOf(separator, from);
    String classpath;
    String extra = "";
    if (to == -1) {
      if (from == entry.length()){
        bail("gwt-classpath",entry);
        return;
      }
      classpath = entry.substring(from);
    } else {
      classpath = entry.substring(from, to);
      to = entry.indexOf(separator, to+1);
      if (to != -1 && to < entry.length()) {
        extra = entry.substring(to+1);
        // There's some extra data to append
      }
    }
    addGwtEntry(new GwtEntry(modules, sourcepath, classpath, extra));
  }

  private boolean bail(String type, String entry) {
    X_Log.error("Invalid " +type+" entry: "+entry);
    return true;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    
    for (int i = 0, m = gwtEntries.length(); i < m; i++) {
      b.append(gwtEntries.get(i));
    }
    ArrayOfString keys = gwtcEntries.keys();
    for (int i = 0, m = keys.length(); i < m; i++) {
      b.append(gwtcEntries.get(keys.get(i)));
    }
    
    return b.toString();
  }

  public ArrayOf<GwtEntry> getGwtEntries() {
    return gwtEntries.concat(Collections.<GwtEntry>arrayOf());
  }

  public void addGwtc(GwtManifest gwtc) {
    gwtcEntries.put(gwtc.getModuleName(), gwtc);
  }
  
}
