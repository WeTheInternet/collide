package collide.demo.controller;

import xapi.log.X_Log;
import xapi.util.X_String;
import xapi.util.api.RemovalHandler;
import collide.client.filetree.AppContextFileTreeController;
import collide.client.filetree.FileTreeController;
import collide.client.filetree.FileTreeModel;
import collide.client.filetree.FileTreeModelNetworkController;
import collide.client.util.Elements;
import collide.demo.view.DemoView;

import com.google.collide.client.AppContext;
import com.google.collide.client.CollideSettings;
import com.google.collide.client.Resources;
import com.google.collide.client.code.NavigationAreaExpansionEvent;
import com.google.collide.client.code.ParticipantModel;
import com.google.collide.client.collaboration.CollaborationManager;
import com.google.collide.client.collaboration.DocOpsSavedNotifier;
import com.google.collide.client.collaboration.IncomingDocOpDemultiplexer;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.document.DocumentManager;
import com.google.collide.client.document.DocumentManager.LifecycleListener;
import com.google.collide.client.editor.Editor;
import com.google.collide.client.history.Place;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.ClientPluginService;
import com.google.collide.client.plugin.FileAssociation;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.search.TreeWalkFileNameSearchImpl;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.workspace.Header;
import com.google.collide.client.workspace.KeepAliveTimer;
import com.google.collide.client.workspace.UnauthorizedUser;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.client.workspace.WorkspaceShell;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.GetWorkspaceMetaDataResponse;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.client.DtoClientImpls.GetWorkspaceMetaDataImpl;
import com.google.collide.dto.client.DtoClientImpls.GetWorkspaceMetaDataResponseImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.plugin.client.launcher.LauncherService;
import com.google.collide.plugin.client.standalone.StandaloneCodeBundle;
import com.google.collide.shared.document.Document;
import com.google.collide.shared.plugin.PublicService;
import com.google.collide.shared.plugin.PublicServices;
import com.google.collide.shared.util.ListenerRegistrar.Remover;
import com.google.collide.shared.util.ListenerRegistrar.RemoverManager;
import com.google.gwt.resources.client.ImageResource;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.events.KeyboardEvent;
import elemental.events.KeyboardEvent.KeyCode;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

public class DemoController 
implements ClientPlugin<WorkspacePlace>, LauncherService
{

  private AppContext appContext;
  private DocumentManager documentManager;
  private RemoverManager keyListenerRemoverManager;
  private TreeWalkFileNameSearchImpl searchIndex;
  private ParticipantModel participantModel;
  private CollaborationManager collaborationManager;
  private StandaloneCodeBundle codePanelBundle;
  protected KeepAliveTimer keepAliveTimer;
  private WorkspacePlace workspacePlace;
  private DemoView view;

  public DemoController(AppContext context) {
    this.appContext = context;
    this.keyListenerRemoverManager = new RemoverManager();
    this.searchIndex = TreeWalkFileNameSearchImpl.create();
    PublicServices.registerService(LauncherService.class, 
        new PublicService.DefaultServiceProvider<LauncherService>(LauncherService.class, this)
        );
  }
  

  public void initialize(DemoView view, final ClientPluginService plugins) {
    this.view = view;
    final Resources res = appContext.getResources();
    WorkspaceShell.View workspaceShellView = new WorkspaceShell.View(res, true);
    workspacePlace = WorkspacePlace.PLACE; // Used to come from event...
    workspacePlace.setIsStrict(false);
    FileTreeController<?> fileTreeController = new AppContextFileTreeController(appContext);
    FileTreeModelNetworkController.OutgoingController fileTreeModelOutgoingNetworkController = new FileTreeModelNetworkController.OutgoingController(
      fileTreeController);
    FileTreeModel fileTreeModel = new FileTreeModel(fileTreeModelOutgoingNetworkController);

    documentManager = DocumentManager.create(fileTreeModel, fileTreeController);

    searchIndex.setFileTreeModel(fileTreeModel);

    participantModel = ParticipantModel.create(appContext.getFrontendApi(), appContext.getMessageFilter());

    IncomingDocOpDemultiplexer docOpRecipient = IncomingDocOpDemultiplexer.create(appContext.getMessageFilter());
    collaborationManager = CollaborationManager.create(appContext, documentManager, participantModel,
      docOpRecipient);

    DocOpsSavedNotifier docOpSavedNotifier = new DocOpsSavedNotifier(documentManager, collaborationManager);

    FileTreeModelNetworkController fileNetworkController = FileTreeModelNetworkController.create(fileTreeModel, fileTreeController, workspacePlace);

    final Header header = Header.create(workspaceShellView.getHeaderView(), workspaceShellView, workspacePlace,
      appContext, searchIndex, fileTreeModel);

    final WorkspaceShell shell = WorkspaceShell.create(workspaceShellView, header);
    // Add a HotKey listener for to auto-focus the AwesomeBox.
    /* The GlobalHotKey stuff utilizes the wave signal event stuff which filters alt+enter as an unimportant
     * event. This prevents us from using the GlobalHotKey manager here. Note: This is capturing since the
     * editor likes to nom-nom keys, in the dart re-write lets think about this sort of stuff ahead of time. */
    final EventRemover eventRemover = Elements.getBody().addEventListener(Event.KEYDOWN, new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        KeyboardEvent event = (KeyboardEvent)evt;
        if (event.isAltKey() && event.getKeyCode() == KeyCode.ENTER) {
          appContext.getAwesomeBoxComponentHostModel().revertToDefaultComponent();
          header.getAwesomeBoxComponentHost().show();
        }
      }
    }, true);

    // Track this for removal in cleanup
    keyListenerRemoverManager.track(new Remover() {
      @Override
      public void remove() {
        eventRemover.remove();
      }
    });

    codePanelBundle = new StandaloneCodeBundle(appContext, shell, fileTreeController, fileTreeModel, searchIndex,
        documentManager, participantModel, docOpRecipient, workspacePlace){
      @Override
      protected ClientPluginService initPlugins(MultiPanel<?, ?> masterPanel) {
        return plugins;
      }
    };
    
    codePanelBundle.attach(true);
    codePanelBundle.setMasterPanel(view);

    // Attach to the DOM.
    view.append(shell);
    view.append(header);

    workspacePlace.fireEvent(new NavigationAreaExpansionEvent(false));

    documentManager.getLifecycleListenerRegistrar().add(new LifecycleListener() {
      
      @Override
      public void onDocumentUnlinkingFromFile(Document document) {
        X_Log.info("unlinked from file",document);
      }
      
      @Override
      public void onDocumentOpened(Document document, Editor editor) {
        X_Log.info("opened",document);
      }
      
      @Override
      public void onDocumentLinkedToFile(Document document,
          FileContents fileContents) {
        
      }
      
      @Override
      public void onDocumentGarbageCollected(Document document) {
        X_Log.info("gc'd",document);
        
      }
      
      @Override
      public void onDocumentCreated(Document document) {
        
      }
      
      @Override
      public void onDocumentClosed(Document document, Editor editor) {
        X_Log.info("closed",document);
      }
    });
    
    final GetWorkspaceMetaDataResponseImpl localRequest = GetWorkspaceMetaDataResponseImpl.make();
    final ApiCallback<GetWorkspaceMetaDataResponse> callback = new ApiCallback<GetWorkspaceMetaDataResponse>() {
      boolean once = true;
      @Override
      public void onMessageReceived(GetWorkspaceMetaDataResponse message) {
        if (!workspacePlace.isActive() && localRequest.getWorkspaceName() == null) {
          return;
        }
        if (once) {
          once = false;
          // Start the keep-alive timer at 5 second intervals.
          keepAliveTimer = new KeepAliveTimer(appContext, 5000);
          keepAliveTimer.start();
    
          codePanelBundle.enterWorkspace(true, workspacePlace, message);
        }
      }

      @Override
      public void onFail(FailureReason reason) {
        if (FailureReason.UNAUTHORIZED == reason) {
          /* User is not authorized to access this workspace. At this point, the components of the
           * WorkspacePlace already sent multiple requests to the frontend that are bound to fail with the
           * same reason. However, we don't want to gate loading the workspace to handle the rare case that
           * a user accesses a branch that they do not have permission to access. Better to make the
           * workspace load fast and log errors if the user is not authorized. */
          UnauthorizedUser unauthorizedUser = UnauthorizedUser.create(res);
          shell.setPerspective(unauthorizedUser.getView().getElement());
        }
      }
    };
    
    CollideSettings settings = CollideSettings.get();
    String file = settings.getOpenFile();
    if (!X_String.isEmptyTrimmed(file)) {
      localRequest.setWorkspaceName("Collide");
      JsoArray<String> files = JsoArray.create();
      files.add(file);
      localRequest.setLastOpenFiles(files);
      callback.onMessageReceived(localRequest);
    }
    // If nothing hardcoded in page, ask the server for data
    appContext.getFrontendApi().GET_WORKSPACE_META_DATA.send(GetWorkspaceMetaDataImpl.make(), callback);
  }

  private final MapFromStringTo<elemental.html.Window> openWindows = Collections.mapFromStringTo();


  @Override
  public RemovalHandler openInIframe(String id, String url) {
    return view.openIframe(id, url);
  }


  @Override
  public void openInNewWindow(String id, String url) {
    elemental.html.Window w;
    if (id == null) id = url;
    w = openWindows.get(id);
    if (w == null || w.isClosed()) {
      w = Browser.getWindow();
      w.open(url, id);
      openWindows.put(id, w);
    }else {
      w.getLocation().assign(url);
    }
  }


  @Override
  public WorkspacePlace getPlace() {
    return workspacePlace;
  }


  @Override
  public void initializePlugin(
      AppContext appContext, MultiPanel<?, ?> masterPanel, Place currentPlace) {
  }


  @Override
  public ImageResource getIcon(
      com.google.collide.client.workspace.Header.Resources res) {
    return null;
  }


  @Override
  public void onClicked(ImageButton button) {
    
  }


  @Override
  public FileAssociation getFileAssociation() {
    return null;
  }


  @Override
  public RunConfiguration getRunConfig() {
    return null;
  }


  @Override
  public PublicService<?>[] getPublicServices() {
    return new PublicService<?>[]{
        new PublicService.DefaultServiceProvider<LauncherService>(
            LauncherService.class, this)
    };
  }

}
