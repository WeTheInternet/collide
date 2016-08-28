package collide.gwtc.ui;

import xapi.gwtc.api.GwtManifest;
import xapi.util.api.ReceivesValue;
import xapi.util.api.RemovalHandler;

import com.google.collide.client.CollideSettings;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.GwtRecompile;
import com.google.collide.dto.shared.CookieKeys;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.DOM;

import elemental.client.Browser;
import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.DataListElement;
import elemental.html.DivElement;
import elemental.html.HTMLCollection;
import elemental.html.InputElement;
import elemental.html.LabelElement;
import elemental.html.OptionElement;
import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

public class GwtModuleView extends CompositeView<GwtController>{


  @UiTemplate("GwtModuleView.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, GwtModuleView> {
  }

  static MyBinder binder = GWT.create(MyBinder.class);

  public interface Css extends CssResource {
    String moduleInput();
    String centered();
  }


  public interface Resources extends
    ClientBundle
    {
    @Source("GwtModuleView.css")
    Css gwtModuleCss();
  }

  //TODO: turn into a module typeahead box.
  @UiField com.google.gwt.dom.client.InputElement input;
  @UiField com.google.gwt.dom.client.DivElement body;
  @UiField com.google.gwt.dom.client.LabelElement inputLabel;
  @UiField com.google.gwt.dom.client.Element data;
  private final DataListElement list;
  private MapFromStringTo<GwtRecompile> modules;
  private final ArrayOf<ReceivesValue<GwtRecompile>> listeners;

  public GwtModuleView(Resources res, GwtManifest model) {
    listeners = Collections.arrayOf();
    modules = Collections.mapFromStringTo();
    binder.createAndBindUi(this);
    
    input.setId(DOM.createUniqueId());
    data.setId(DOM.createUniqueId());
    
    InputElement in = (InputElement)input; 
    //associate our data list in the browser's typeahead
    (in).setAttribute("list", data.getId());
    //associate label to our input; this should be done in generator
    ((LabelElement)inputLabel).setHtmlFor(input.getId());

    setModuleTextbox(model.getModuleName());
    list = (elemental.html.DataListElement)data;
    //TODO restore module from cookie
    EventListener ev = new EventListener() {
      String was = "";
      @Override
      public void handleEvent(Event evt) {
        String is = input.getValue().trim();
        if (is.equals(was))return;
        was = is;
        GwtRecompile module = modules.get(is);
        if (module != null) {
          showModule(module);
        }
      }
    };
    in.setOninput(ev);
    res.gwtModuleCss().ensureInjected();
  }

  protected void showModule(GwtRecompile module) {
    setModuleTextbox(module.getModule());
    input.select();
    for (int i = 0, m = listeners.length(); i < m; i++) {
      ReceivesValue<GwtRecompile> listener = listeners.get(i);
      listener.set(module);
    }
  }

  public static GwtModuleView create(DivElement moduleContainer, Resources res,
      GwtManifest model) {
    GwtModuleView mod = new GwtModuleView(res, model);
    moduleContainer.appendChild((DivElement)mod.body);
    return mod;
  }

  private void setModuleTextbox(String module) {
    Log.info(getClass(), "Got Module",module);
    input.setValue(module);
    input.setDefaultValue(module);
  }
  
  public GwtRecompile getModule(String module) {
    return modules.get(module);
  }

  public void showResults(JsonArray<GwtRecompile> modules, GwtClasspathView classpath) {
    if (modules.size() == 0)
      return;
    CollideSettings settings = CollideSettings.get();
    
    String requested = settings.getModule();
    GwtRecompile best = null;
    if (requested == null) {
      requested = Browser.getWindow().getLocalStorage().getItem(CookieKeys.GWT_COMPILE_TARGET);
    }
    if (requested != null) {
      for (GwtRecompile compile : modules.asIterable()) {
        if (requested.equals(compile.getModule())) {
          best = compile;
          break;
        }
      }
    }
    
    this.modules = Collections.mapFromStringTo();
    HTMLCollection opts = (list).getOptions();
    int m = opts.length();
    if (m == 0) {
      m = modules.size();
      for (int i = 0; i < m; i++) {
        list.appendChild(createOption(modules.get(i), i == 0));
      }
    } else {
      // There are modules, let's merge
      MapFromStringTo<Node> existing = Collections.mapFromStringTo();
      for (int i = 0; i < m; i++) {
        Node opt = opts.item(i);
        Node attr = opt.getAttributes().getNamedItem("value");
        if (attr != null)
          existing.put(attr.getNodeValue(), opt);
      }
      m = modules.size();
      for (int i = 0; i < m; i++) {
        GwtRecompile module = modules.get(i);
        Node duplicate = existing.get(module.getModule());
        if (duplicate != null) {
          // TODO check revision # and take freshest
          duplicate.removeFromParent();
        }
        OptionElement opt = createOption(module, i == 0);
        list.appendChild(opt);
      }
    }
    if (best == null)
     best = modules.get(0);
    setModuleTextbox(best.getModule());
    classpath.setClasspath(best.getSources(), best.getDependencies());
  }

  protected OptionElement createOption(GwtRecompile module, boolean selected) {
    modules.put(module.getModule(), module);
    OptionElement option = Browser.getDocument().createOptionElement();
    option.setDefaultSelected(selected);
    option.setAttribute("name", module.getModule());
    option.setText(module.getModule());
    option.setValue(module.getModule());
    return option;
  }

  public RemovalHandler addSelectListener(final ReceivesValue<GwtRecompile> receivesValue) {
    listeners.push(receivesValue);
    return new RemovalHandler() {
      @Override
      public void remove() {
        listeners.remove(receivesValue);
      }
    };
  }

  public String getModule() {
    return input.getValue();
  }

}
