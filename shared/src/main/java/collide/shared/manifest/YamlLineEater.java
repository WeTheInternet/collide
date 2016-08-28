package collide.shared.manifest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlLineEater extends AbstractLineEater {

  private Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
  private List<Object> valueArray = new ArrayList<Object>();
  boolean inArray;
  LineEater eater;
  String indent = "";
  
  public YamlLineEater() {
    this(null);
  }
  
  public YamlLineEater(Stack parent) {
    super(parent);
  }
  
  @Override
  protected String isStartLine(String line) {
    if (inArray) {
      return null;
    }
    return line.endsWith(":") ? line.replaceAll(":$", "") : null;
  }
  
  @Override
  protected String isEndLine(String line) {
    if (inArray) {
      line = line.trim();
      if (line.startsWith("-")) {
        return null;
      }
      return line;
    }
    return super.isEndLine(line);
  }
  
  @Override
  protected boolean addValue(String line) {
    int ind = line.indexOf(':');
    if (ind == -1) {
      line = line.trim();
      ind = line.indexOf('-');
      if (ind == 0) {
        inArray = true;
        line = line.substring(ind+1).trim();
        valueArray.add(line);
        return true;
      } else {
        inArray = false;
      }
    } else {
      inArray = false;
      String key = line.substring(0, ind).trim();
      if (key.startsWith("-")) {
        key = key.substring(1).trim();
      }
      String value = line.substring(ind+1).trim();
      valueMap.put(key, value);
      return true;
    }
    return super.addValue(line);
  }
  
  @Override
  protected LineEater newChildEater(Stack stack, String name) {
    if (inArray) {
      tail = tail.parent;
      inArray = false;
    }
    YamlLineEater eater = new YamlLineEater(stack);
    eater.indent = indent + " ";
    valueMap.put(name.trim(), eater);
    return eater;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    if (valueArray.size() > 0){
      String startLine = indent;//+" ";
      for (Object o : valueArray) {
        b
        .append(startLine)
        .append("- ")
        .append(o);
        startLine = "\n"+indent;//+" ";
      }
      b.append("\n");
    }
    if (valueMap.size() > 0) {
      for (String key : valueMap.keySet()) {
        b.append(indent);
        Object value = valueMap.get(key);
        if (value instanceof String) {
          b
            .append(key)
            .append(": ")
            .append(value)
            .append("\n");
        } else {
          b
            .append(key)
            .append(":\n")
            .append(value);
        }
      }
    } 
    return b.toString();
  }

  public void eatAll(String string) {
    for (String line : string.split("\n")) {
      eat(line);
    }
  }
  
}
