// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.workspace;

import xapi.log.X_Log;

import com.google.collide.client.AppContext;
import com.google.collide.client.common.BaseResources;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.ui.dropdown.AutocompleteController;
import com.google.collide.client.ui.dropdown.AutocompleteController.AutocompleteHandler;
import com.google.collide.client.ui.dropdown.DropdownController;
import com.google.collide.client.ui.dropdown.DropdownController.BaseListener;
import com.google.collide.client.ui.dropdown.DropdownController.DropdownPositionerBuilder;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.ui.dropdown.DropdownWidgets.DropdownInput;
import com.google.collide.client.ui.list.SimpleList.ListItemRenderer;
import com.google.collide.client.ui.menu.AutoHideComponent;
import com.google.collide.client.ui.menu.AutoHideComponent.AutoHideModel;
import com.google.collide.client.ui.menu.AutoHideView;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.util.ClientStringUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.dto.RunTarget;
import com.google.collide.dto.client.DtoClientImpls.RunTargetImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.RegExpUtils;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.DOM;

import elemental.dom.Node;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.KeyboardEvent;
import elemental.util.ArrayOfString;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;
//import com.google.gwt.user.client.ui.HTMLPanel;

/**
 * A popup for user selection of the run target.
 */
public class RunButtonTargetPopup
    extends AutoHideComponent<RunButtonTargetPopup.View, AutoHideModel> {

  public static RunButtonTargetPopup create(AppContext context, Element anchorElement,
      Element triggerElement, FileNameSearch searchIndex, MapFromStringTo<RunConfiguration> runConfigs) {
    MapFromStringTo<elemental.html.InputElement> inputs = Collections.mapFromStringTo();
    View view = new View(context.getResources(), runConfigs, inputs);
    return new RunButtonTargetPopup(view, anchorElement, triggerElement, searchIndex,runConfigs,inputs);
  }



  /**
   * An enum which handles the different types of files that can be parsed out of a user's file
   * input. Each enum value also provides the appropriate labels and formatter for the user provided
   * values.
   */
  public enum RunTargetType {
    /**
     * Represents any other value typed into the dropdown. This file will be executed on the
     * resource serving servlet.
     */
    FILE("Query", "?query string (optional)", new InputFormatter() {
      @Override
      public String formatUserInput(String file, String userValue) {
        if (userValue.isEmpty()) {
          return StringUtils.ensureStartsWith(file, "/");
        } else {
          return StringUtils.ensureStartsWith(file, "/")
              + StringUtils.ensureStartsWith(userValue, "?");
        }
      }
    });

    /**
     * An object which formats some output based on the file name and query value provided by the
     * user.
     */
    public interface InputFormatter {
      String formatUserInput(String file, String userValue);
    }

    /**
     * An object which can be used for run targets which do not need to provide a formatted output.
     */
    public static final class NoInputFormatter implements InputFormatter {
      @Override
      public String formatUserInput(String file, String userValue) {
        return "";
      }
    }

    private final String label;
    private final String placeHolder;
    private final InputFormatter inputFormatter;

    RunTargetType(String label, String placeHolder, InputFormatter inputFormatter) {
      this.label = label;
      this.placeHolder = placeHolder;
      this.inputFormatter = inputFormatter;
    }

    /**
     * Parses the user's input and determines the type of run target.
     */
    public static RunTargetType parseTargetType(String fileInput) {
      //check for registered plugin handlers
      return RunTargetType.FILE;
    }
  }

  public interface Css extends CssResource {
    String container();

    String radio();

    String stackedContainer();

    String smallPreviewText();

    String alwaysRunRow();

    String alwaysRunInput();

    String alwaysRunLabel();

    String alwaysRunUrl();

    String appUrlLabel();

    String autocompleteFolder();

    String listItem();

    /* Sets the dropdown opacity to 1 while the menu is visible */
    String stayActive();
  }

  public interface Resources extends BaseResources.Resources, DropdownWidgets.Resources {
    @Source("RunButtonTargetPopup.css")
    Css runButtonTargetPopupCss();

    @Source("appengine-small.png")
    ImageResource appengineSmall();
  }

  public interface ViewEvents {
    void onAlwaysRunInputChanged();

    void onPathInputChanged();

    void onItemChanged(RunConfiguration runConfig);
  }

  public class ViewEventsImpl implements ViewEvents {
    @Override
    public void onAlwaysRunInputChanged() {
      selectRadio("ALWAYS_RUN");
      getView().setDisplayForRunningApp();
    }

    @Override
    public void onPathInputChanged() {
      selectRadio("ALWAYS_RUN");
      getView().setDisplayForRunningApp();
    }

    @Override
    public void onItemChanged(RunConfiguration config) {
      selectRadio(config.getId());
      getView().setDisplayForRunningApp();
    }

  }

  private static final int MAX_AUTOCOMPLETE_RESULTS = 4;

  private final FileNameSearch searchIndex;
  private final PositionController positionController;
  private AutocompleteController<PathUtil> autocompleteController;

  private final MapFromStringTo<RunConfiguration> plugins;
  private final MapFromStringTo<elemental.html.InputElement> inputs;

  private RunButtonTargetPopup(
      View view, Element anchorElement, Element triggerElement, FileNameSearch searchIndex,
      MapFromStringTo<RunConfiguration> runConfigs, MapFromStringTo<elemental.html.InputElement> inputs) {
    super(view, new AutoHideModel());
    view.setDelegate(new ViewEventsImpl());

    this.searchIndex = searchIndex;
    this.plugins = runConfigs;
    this.inputs = inputs;
    setupAutocomplete();

    // Don't eat outside clicks and only hide when the user clicks away
    addPartnerClickTargets(
        Elements.asJsElement(triggerElement), autocompleteController.getDropdown().getElement());
    setCaptureOutsideClickOnClose(false);
    setDelay(-1);

    // Position Controller
    positionController =
        new PositionController(new PositionController.PositionerBuilder().setVerticalAlign(
            VerticalAlign.BOTTOM).setHorizontalAlign(HorizontalAlign.LEFT)
            .buildAnchorPositioner(Elements.asJsElement(anchorElement)), getView().getElement());
  }

  private void setupAutocomplete() {
    AutocompleteHandler<PathUtil> handler = new AutocompleteHandler<PathUtil>() {
      @Override
      public JsonArray<PathUtil> doCompleteQuery(String query) {
        PathUtil searchPath = PathUtil.WORKSPACE_ROOT;
        // the PathUtil.createExcluding wasn't used here since it doesn't make
        // /test2/ into a path containing /test2 but instead makes it /
        if (query.lastIndexOf(PathUtil.SEP) != -1) {
          searchPath = new PathUtil(query.substring(0, query.lastIndexOf(PathUtil.SEP)));
          query = query.substring(query.lastIndexOf(PathUtil.SEP) + 1);
        }

        // Only the non folder part (if it exists) is supported for wildcards
        RegExp reQuery = RegExpUtils.createRegExpForWildcardPattern(
            query, ClientStringUtils.containsUppercase(query) ? "" : "i");
        return searchIndex.getMatchesRelativeToPath(searchPath, reQuery, MAX_AUTOCOMPLETE_RESULTS);
      }

      @Override
      public void onItemSelected(PathUtil item) {
        getView().runAlwaysDropdown.getInput().setValue(item.getPathString());
        getView().setDisplayForRunningApp();
      }
    };

    BaseListener<PathUtil> clickListener = new BaseListener<PathUtil>() {
      @Override
      public void onItemClicked(PathUtil item) {
        getView().runAlwaysDropdown.getInput().setValue(item.getPathString());
        getView().setDisplayForRunningApp();
        //TODO(james) switch to gwt/ant/maven if the current file is of the correct format
        //.gwt.xml, build.xml or pom.xml
      }
    };

    ListItemRenderer<PathUtil> itemRenderer = new ListItemRenderer<PathUtil>() {
      @Override
      public void render(elemental.dom.Element listItemBase, PathUtil itemData) {
        elemental.html.SpanElement fileNameElement = Elements.createSpanElement();
        elemental.html.SpanElement folderNameElement = Elements.createSpanElement(
            getView().res.runButtonTargetPopupCss().autocompleteFolder());

        listItemBase.appendChild(fileNameElement);
        listItemBase.appendChild(folderNameElement);

        int size = itemData.getPathComponentsCount();
        if (size == 1) {
          fileNameElement.setTextContent(itemData.getPathComponent(0));
          folderNameElement.setTextContent("");
        } else {
          fileNameElement.setTextContent(itemData.getPathComponent(size - 1));
          folderNameElement.setTextContent(" - " + itemData.getPathComponent(size - 2));
        }
      }
    };

    Positioner positioner = new DropdownPositionerBuilder().buildAnchorPositioner(
        getView().runAlwaysDropdown.getInput());
    DropdownController<PathUtil> autocompleteDropdown = new DropdownController.Builder<PathUtil>(
        positioner, null, getView().res, clickListener, itemRenderer).setInputTargetElement(
        getView().runAlwaysDropdown.getInput())
        .setShouldAutoFocusOnOpen(false).setKeyboardSelectionEnabled(true).build();

    autocompleteController = AutocompleteController.create(
        getView().runAlwaysDropdown.getInput(), autocompleteDropdown, handler);
  }

  public void updateCurrentFile(String filePath) {
    getView().runPreviewCurrentFile.setInnerText(
        StringUtils.ensureNotEmpty(filePath, "Select a file"));
  }

  private void selectRadio(String mode) {
    elemental.html.InputElement preview = Elements.asJsElement(getView().runPreviewRadio);
    elemental.html.InputElement always = Elements.asJsElement(getView().runAlwaysRadio);
    X_Log.info("Selected",mode);
    preview.setChecked("PREVIEW_CURRENT_FILE".equals(mode));
    always.setChecked("ALWAYS_RUN".equals(mode));
    ArrayOfString keys = plugins.keys();
    for (int i = 0; i < keys.length(); i++) {
      String key = keys.get(i);
      elemental.html.InputElement input = inputs.get(key);
      if (input != null)
        input.setChecked(plugins.get(key).getId().equals(mode));
    }
  }

  public void setRunTarget(RunTarget runTarget) {

    selectRadio(runTarget.getRunMode());
    getView().runAlwaysDropdown.getInput().setValue(StringUtils.nullToEmpty(
        runTarget.getAlwaysRunFilename()));
    getView().userExtraInput.setValue(StringUtils.nullToEmpty(runTarget.getAlwaysRunUrlOrQuery()));

    //XXX this is where we set previous gwt/ant/maven run Strings

  }

  public RunTarget getRunTarget() {
    RunTargetImpl runTarget = RunTargetImpl.make();

    runTarget.setRunMode(
        isChecked(getView().runPreviewRadio)? "PREVIEW_CURRENT_FILE"
        : isChecked(getView().runAlwaysRadio) ? "ALWAYS_RUN"
        : getChecked());

    runTarget.setAlwaysRunFilename(getView().runAlwaysDropdown.getInput().getValue());
    runTarget.setAlwaysRunUrlOrQuery(getView().userExtraInput.getValue());

    return runTarget;
  }

  private String getChecked() {
    ArrayOfString keys = inputs.keys();
    for (int i = 0; i < keys.length(); i++) {
      String key = keys.get(i);
      if (inputs.get(key).isChecked()) {
        return key;
      }
    }
    return "ALWAYS_RUN";
  }

  private boolean isChecked(InputElement input){
    return Elements.<InputElement, elemental.html.InputElement>asJsElement(input).isChecked();
  }

  @Override
  public void show() {
    // Position Ourselves
    positionController.updateElementPosition();

    // Update UI before we show
    getView().setDisplayForRunningApp();

    super.show();
  }

  public static class View extends AutoHideView<ViewEvents> {
    @UiTemplate("RunButtonTargetPopup.ui.xml")
    interface RunButtonDropdownUiBinder extends UiBinder<Element, View> {
    }

    private static RunButtonDropdownUiBinder uiBinder = GWT.create(RunButtonDropdownUiBinder.class);

    @UiField(provided = true)
    final Resources res;

    // Preview Current File Stuff
    @UiField
    InputElement runPreviewRadio;

    @UiField
    LabelElement runPreviewLabel;

    @UiField
    SpanElement runPreviewCurrentFile;

    //Run gwt stuff
//    @UiField
//    InputElement runGwtRadio;
//
//    @UiField
//    LabelElement runGwtLabel;
//
//    @UiField
//    DivElement runGwtRow;

    // Full Query URL
    @UiField
    DivElement container;

    // Run always stuff
    @UiField
    DivElement runAlwaysRow;

    @UiField
    InputElement runAlwaysRadio;

    @UiField
    LabelElement runAlwaysLabel;

    final DropdownInput runAlwaysDropdown;

    // Query and URL Box
    @UiField
    LabelElement userExtraLabel;

    @UiField
    InputElement userExtraInput;

    // Full Query URL
    @UiField
    DivElement runHintText;


    public View(Resources resources, MapFromStringTo<RunConfiguration> runConfigs,
      MapFromStringTo<elemental.html.InputElement> inputs) {
      this.res = resources;

      Element element = uiBinder.createAndBindUi(this);
      setElement(Elements.asJsElement(element));
      Elements.getBody().appendChild((Node) element);

      // Workaround for inability to set both id and ui:field in a UiBinder XML
      runAlwaysRadio.setId(DOM.createUniqueId());
      runAlwaysLabel.setHtmlFor(runAlwaysRadio.getId());
      userExtraLabel.setHtmlFor(runAlwaysRadio.getId());

      runPreviewRadio.setId(DOM.createUniqueId());
      runPreviewLabel.setHtmlFor(runPreviewRadio.getId());

      // Create the dropdown
      runAlwaysDropdown = new DropdownInput(resources);
      runAlwaysDropdown.getInput()
          .addClassName(resources.runButtonTargetPopupCss().alwaysRunInput());
      runAlwaysDropdown.getInput().setAttribute("placeholder", "Enter filename");
      Elements.asJsElement(runAlwaysRow).appendChild(runAlwaysDropdown.getContainer());

      //create dynamic run configs
      ArrayOfString keys = runConfigs.keys();
      for (int i = 0; i < keys.length(); i++) {
        String key = keys.get(i);
        final RunConfiguration runConfig = runConfigs.get(key);
        String label = runConfig.getLabel();
        if (label == null) {
          //no label; this run config modifies hardcoded configs
        }else {
          //create an input radio, and possibly inject extra controls
          elemental.html.DivElement el = Elements.createDivElement();
          elemental.html.InputElement input = Elements.createInputElement(
            resources.baseCss().radio()
            +" "+resources.runButtonTargetPopupCss().radio()
            );
          input.setId(DOM.createUniqueId());
          input.setType("radio");
          input.setName("runType");
          inputs.put(key, input);
          elemental.html.DivElement body = Elements.createDivElement(resources.runButtonTargetPopupCss().stackedContainer());
          body.setInnerHTML("<div><label>"+label+"</label></div>");
          ((elemental.html.LabelElement)body.getFirstChildElement().getFirstChildElement()).setHtmlFor(input.getId());
          //now, add pluggable forms, if any, for the runtarget
          elemental.dom.Element extra = runConfig.getForm();
          if (extra!=null) {
            body.appendChild(extra);
          }
          input.addEventListener(Event.CHANGE, new EventListener() {
            @Override
            public void handleEvent(Event evt) {
              if (getDelegate() != null) {
                getDelegate().onItemChanged(runConfig);
              }
            }
          },true);
          el.appendChild(input);
          el.appendChild(body);
          Elements.asJsElement(container).appendChild(el);
        }
      }

      setDisplayForRunningApp(RunTargetType.FILE);
      attachHandlers();
    }

    public void attachHandlers() {
      runAlwaysDropdown.getInput().addEventListener(Event.INPUT, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onAlwaysRunInputChanged();
          }
        }
      }, false);

      Elements.asJsElement(userExtraInput).addEventListener(Event.INPUT, new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            getDelegate().onPathInputChanged();
          }
        }
      }, false);

      EventListener onEnterListener = new EventListener() {
        @Override
        public void handleEvent(Event evt) {
          if (getDelegate() != null) {
            KeyboardEvent keyEvent = (KeyboardEvent) evt;
            if (keyEvent.getKeyCode() == KeyboardEvent.KeyCode.ENTER) {
              getDelegate().onPathInputChanged();
              evt.stopPropagation();
              hide();
            }
            //TODO(james) listen for down key, and load more results when at bottom of list
          }
        }
      };

      runAlwaysDropdown.getInput().addEventListener(Event.KEYUP, onEnterListener, false);
      Elements.asJsElement(userExtraInput).addEventListener(Event.KEYUP, onEnterListener, false);
    }

    /**
     * Sets up the view based on the user's current file input value.
     */
    public void setDisplayForRunningApp() {
      RunTargetType type = RunTargetType.parseTargetType(runAlwaysDropdown.getInput().getValue());
      setDisplayForRunningApp(type);
    }

    /**
     * Sets up the view for the supplied app type.
     */
    public void setDisplayForRunningApp(RunTargetType appType) {
      userExtraInput.setAttribute("placeholder", appType.placeHolder);
      userExtraLabel.setInnerText(appType.label);

      String fileName = runAlwaysDropdown.getInput().getValue();
      String queryText = userExtraInput.getValue();
      if (fileName.isEmpty()) {
        runHintText.setInnerText("Type a filename");
      } else {
        runHintText.setInnerText(appType.inputFormatter.formatUserInput(fileName, queryText));
      }
    }
  }

  public void renderPlugins() {

  }
}
