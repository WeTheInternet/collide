package collide.gwtc.view;

import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import collide.client.common.CanRunApplication;
import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeModelNetworkController.OutgoingController;
import collide.client.filetree.FileTreeNodeRenderer;
import collide.client.filetree.FileTreeNodeRenderer.Css;
import collide.client.util.Elements;
import collide.gwtc.GwtCompileStatus;
import collide.gwtc.GwtcController;
import collide.gwtc.resources.GwtcResources;

import com.google.collide.client.code.FileTreeSection;
import com.google.collide.client.code.debugging.DebuggingModelController;
import com.google.collide.client.code.debugging.DebuggingModelRenderer;
import com.google.collide.client.code.debugging.DebuggingSidebar;
import com.google.collide.client.code.debugging.EvaluationPopupController;
import com.google.collide.client.code.debugging.SourceMapping;
import com.google.collide.client.code.debugging.StaticSourceMapping;
import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.PushChannel;
import com.google.collide.client.communication.ResourceUriUtils;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.ui.menu.PositionController.AnchorPositioner;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Position;
import com.google.collide.client.ui.menu.PositionController.PositionerBuilder;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.popup.Popup;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.PopupBlockedInstructionalPopup;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.GetDirectory;
import com.google.collide.dto.GetDirectoryResponse;
import com.google.collide.dto.GetFileContents;
import com.google.collide.dto.GetFileContentsResponse;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.json.shared.JsonStringMap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.SpanElement;
import elemental.js.dom.JsElement;


public class GwtcModuleControlView {

  public interface FileResources extends DropdownWidgets.Resources,
  Tooltip.Resources,
  FileTreeNodeRenderer.Resources,
  FileTreeSection.Resources,
  Popup.Resources,
  DebuggingModelRenderer.Resources,
  PopupBlockedInstructionalPopup.Resources,
  EvaluationPopupController.Resources,
  DebuggingSidebar.Resources
  {
  }
  final FileResources resources = GWT.create(FileResources.class);

  // This is a class that binds our ui.xml file to GwtcModuleControlView class
  @UiTemplate("GwtcModuleControlView.ui.xml")
  interface MyBinder extends UiBinder<com.google.gwt.dom.client.DivElement, GwtcModuleControlView> {}

  // This is a generated instance of the above interface. It fills in this class's values.
  static MyBinder binder = GWT.create(MyBinder.class);
  
  // A provider for our resources; this object will create one and only one copy of resources,
  // and make sure the css is injected into the hostpage before returning the resource provider.
  private static final SingletonProvider<GwtcResources> resourceProvider = new SingletonProvider<GwtcResources>(){
    protected GwtcResources initialValue() {
      GwtcResources res = GWT.create(GwtcResources.class);
      res.panelHeaderCss().ensureInjected();
      return res;
    };
  };
  /**
   * Creates a new panel header with the given resources, all of which may be null.
   * 
   * @param moduleContainer - The element in which to append the header. 
   * If null, you must attach the header somewhere.
   * @param res - GwtcCss resource overrides.  If null, our defaults are applied.
   * @param model - A model for the panel, which contains values like isMaximizable, isClosable, etc.
   * 
   * @return - A GwtcModuleControlView widget.
   */
  public static GwtcModuleControlView create(GwtcController controller) {
    return new GwtcModuleControlView(null, controller);
  }


  // The elements from our .ui.xml file.  These are filled in by the compiler.
  @UiField public Element header;
  @UiField Element headerContainer;
  @UiField Element controls;
  @UiField Element close;
  @UiField Element icons;
  @UiField Element reload;
  @UiField Element status;
  @UiField Element generated;
  // This is our css data; the compiler uses this object when filling in values.
  @UiField(provided=true) final GwtcResources res;

  private GwtCompileStatus currentStatus = GwtCompileStatus.Pending;


  public GwtcModuleControlView(GwtcResources res, final GwtcController controller) {
    // Allow users to override default css.
    this.res = res == null ? resourceProvider.get() : res;
    // Calls into the generated ui binder, creating html elements and filling in our values.
    binder.createAndBindUi(this);
    
    Css css = GWT.<FileTreeNodeRenderer.Resources>create(FileTreeNodeRenderer.Resources.class).workspaceNavigationFileTreeNodeRendererCss();
    SpanElement contents = FileTreeNodeRenderer.renderNodeContents(css, "Generated", false, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        FileTreeSection files = testFileTree();
        generated.<JsElement>cast().appendChild(files.getView().getElement());
        files.getTree().renderTree(0);
        Popup popup = Popup.create(resources);
        popup.addPartner(generated.<JsElement>cast());
        popup.setContentElement(files.getView().getElement());
        popup.show(new PositionerBuilder()
          .setVerticalAlign(VerticalAlign.BOTTOM)
          .setPosition(Position.NO_OVERLAP)
          .buildAnchorPositioner(generated.<JsElement>cast()));
      }
    }, true);
    Elements.asJsElement(generated).appendChild(contents);
    
    reload.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        controller.onRefreshClicked();
      }
    });
    close.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        controller.onCloseClicked();
      }
    });
    status.<JsElement>cast().setOnclick(new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        if (currentStatus == null) {
          controller.onReloadClicked();
          return;
        }
        switch (currentStatus) {
          case PartialSuccess:
          case Pending:
          case Success:
          case Fail:
            controller.onReloadClicked();
          default:
            // TODO warn that a compile is in progress
        }
      }
    });
  }

  public elemental.dom.Element getElement() {
    return (elemental.dom.Element)headerContainer;
  }
  
  public void setCompileStatus(GwtCompileStatus compileStatus) {
    assert compileStatus !=  null;
    X_Log.info("Setting compile status", compileStatus.name(), this);
    if (currentStatus == compileStatus) {
      return;
    }
    status.removeClassName(classFor(currentStatus));
    status.addClassName(classFor(compileStatus));
    currentStatus = compileStatus;
  }

  private String classFor(GwtCompileStatus status) {
    switch (status) {
      case Pending:
        return res.panelHeaderCss().gear();
      case Success:
        return res.panelHeaderCss().success();
      case PartialSuccess:
        return res.panelHeaderCss().warn();
      case Good:
        return res.panelHeaderCss().radarGreen();
      case Warn:
        return res.panelHeaderCss().radarYellow();
      case Error:
        return res.panelHeaderCss().radarRed();
      case Fail:
        return res.panelHeaderCss().fail();
    }
    throw new IllegalStateException();
  }

  public void setHeader(String id) {
    header.setInnerHTML(id);
  }
  private FileTreeSection testFileTree() {
    Place place = new Place("Generated") {
      @Override
      public PlaceNavigationEvent<? extends Place> createNavigationEvent(
          final JsonStringMap<String> decodedState) {
        return new PlaceNavigationEvent<Place>(this) {
          @Override
          public JsonStringMap<String> getBookmarkableState() {
            return decodedState;
          }
        };
      }
    };
    FileTreeController<?> fileTreeController = new FileTreeController<FileResources>() {

      protected StatusManager statusManager;
      protected MessageFilter messageFilter;
      protected PushChannel pushChannel;
      protected FrontendApi frontendApi;
      
      {
        this.statusManager = new StatusManager();
        this.messageFilter = new MessageFilter();
        // Things that depend on message filter/frontendApi/statusManager
        this.pushChannel = PushChannel.create(messageFilter, statusManager);
        this.frontendApi = FrontendApi.create(pushChannel, statusManager);
      }
      
      @Override
      public FileResources getResources() {
        return resources;
      }

      @Override
      public void mutateWorkspaceTree(WorkspaceTreeUpdate msg, ApiCallback<EmptyMessage> callback) {
        frontendApi.MUTATE_WORKSPACE_TREE.send(msg, callback);
      }
      
      @Override
      public void getFileContents(GetFileContents getFileContents,
          ApiCallback<GetFileContentsResponse> callback) {
        frontendApi.GET_FILE_CONTENTS.send(getFileContents, callback);
      }
      
      @Override
      public StatusManager getStatusManager() {
        return statusManager;
      }

      @Override
      public void getDirectory(GetDirectory getDirectoryAndPath,
          ApiCallback<GetDirectoryResponse> callback) {
        frontendApi.GET_DIRECTORY.send(getDirectoryAndPath, callback);
      }

      @Override
      public MessageFilter getMessageFilter() {
        return messageFilter;
      }
    };
    OutgoingController networkController = new OutgoingController(fileTreeController);
    FileTreeModel fileTreeModel = new FileTreeModel(networkController);
    CanRunApplication runner = new CanRunApplication() {
      @Override
      public boolean runApplication(PathUtil applicationPath) {
        String baseUri = ResourceUriUtils.getAbsoluteResourceBaseUri();
        SourceMapping sourceMapping = StaticSourceMapping.create(baseUri);
        elemental.html.Window popup = DebuggingModelController.createOrOpenPopup(resources);
        if (popup != null) {
          String absoluteResourceUri = sourceMapping.getRemoteSourceUri(applicationPath);
          popup.getLocation().assign(absoluteResourceUri);
        }
        return false;
      }
    };
    return FileTreeSection.create(place, fileTreeController, fileTreeModel, runner);
  }
}
