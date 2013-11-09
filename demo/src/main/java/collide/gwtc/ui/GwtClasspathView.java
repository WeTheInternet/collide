package collide.gwtc.ui;

import xapi.util.X_String;

import com.google.collide.client.util.Elements;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.DOM;

import elemental.html.DivElement;
import elemental.html.LIElement;
import elemental.html.LabelElement;
import elemental.html.UListElement;

public class GwtClasspathView extends CompositeView<GwtController>{


  @UiTemplate("GwtClasspathView.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, GwtClasspathView> {}

  static MyBinder binder = GWT.create(MyBinder.class);
  
  public interface Css extends CssResource {
    String classpathContainer();
    String classpathInput();
  }

  
  public interface Resources extends 
    ClientBundle 
    {
    @Source("GwtClasspathView.css")
    Css gwtClasspathCss();
  }
  
  @UiField com.google.gwt.dom.client.UListElement classpath;
  @UiField com.google.gwt.dom.client.DivElement body;
  @UiField com.google.gwt.dom.client.LabelElement classpathLabel;
  @UiField(provided=true) Resources res;
  private JsoArray<String> deps;
  private JsoArray<String> srcs;

  
  public GwtClasspathView(Resources res, GwtCompileModel model) {
    this.res = res;
    binder.createAndBindUi(this);
    //hookup label; should be doing this in generator using ui:field...
    classpath.setId(DOM.createUniqueId());
    ((LabelElement)classpathLabel).setHtmlFor(classpath.getId());
    
    setClasspath(model);
    
    res.gwtClasspathCss().ensureInjected();
  }

  
  public static GwtClasspathView create(DivElement moduleContainer, Resources res,
      GwtCompileModel model) {
    GwtClasspathView mod = new GwtClasspathView(res, model);
    moduleContainer.appendChild((DivElement)mod.body);
    return mod;
  }
  
  private void setClasspath(GwtCompileModel model) {
    UListElement form = (UListElement) classpath;
    form.setInnerHTML("");//clear
    JsoArray<String> sources = JsoArray.create();
    JsoArray<String> deps = JsoArray.create();
    for (String src : model.getClasspath()){
      if (!X_String.isEmpty(src))
      (src.contains(".jar") ? deps : sources).add(src);
    }
    setClasspath(sources, deps);
  }

  public void setClasspath(JsonArray<String> src, JsonArray<String> dep) {
    UListElement form = (UListElement) classpath;
    form.setInnerHTML("");//clear
    this.srcs = JsoArray.create();
    this.deps = JsoArray.create();
    for (String source : src.asIterable()){
      if (!X_String.isEmptyTrimmed(source)){
        srcs.add(source);
        form.appendChild(buildElement(source));
      }
    }
    for (String source : dep.asIterable()){
      if (!X_String.isEmptyTrimmed(source)){
        deps.add(source);
        form.appendChild(buildElement(source));
      }
    }
  }

  private LIElement buildElement(String source) {
    LIElement li = Elements.createLiElement(res.gwtClasspathCss().classpathInput());
    // TODO add icon for jar v. folder, and add open handlers.
    if (source.contains(".jar")) {
      markupJar(li, source);
    } else {
      markupFolder(li, source);
    }
    return li;
  }

  protected void markupJar(LIElement li, String source) {
    li.setInnerHTML(
        jarIcon() +
        "<a href=\"" +
        openJarLink(source)+
    		"\">"+source+"</a>");
  }


  protected void markupFolder(LIElement li, String source) {
    li.setInnerHTML(
        folderIcon() +
        "<a href=\"" +
        openFileLink(source)+
        "\">"+source+"</a>");
  }
  
  protected String jarIcon() {
    return "";
  }
  
  protected String folderIcon() {
    return "";
  }
  
  protected String openJarLink(String source) {
    return "#!jar:"+source;
  }
  
  protected String openFileLink(String source) {
    return "#!file:"+source;
  }

  public JsoArray<String> getDependencies() {
    return deps;
  }

  public JsoArray<String> getSources() {
    return srcs;
  }


}
