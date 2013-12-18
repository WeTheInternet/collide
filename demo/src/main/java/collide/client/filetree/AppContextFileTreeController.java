package collide.client.filetree;

import com.google.collide.client.AppContext;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.status.StatusManager;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.GetDirectory;
import com.google.collide.dto.GetDirectoryResponse;
import com.google.collide.dto.GetFileContents;
import com.google.collide.dto.GetFileContentsResponse;
import com.google.collide.dto.WorkspaceTreeUpdate;

public class AppContextFileTreeController implements
    FileTreeController<com.google.collide.client.Resources> {

  private final AppContext appContext;

  public AppContextFileTreeController(AppContext appContext) {
    this.appContext = appContext;
  }

  @Override
  public MessageFilter getMessageFilter() {
    return appContext.getMessageFilter();
  }
  
  @Override
  public com.google.collide.client.Resources getResources() {
    return appContext.getResources();
  }

  @Override
  public void mutateWorkspaceTree(WorkspaceTreeUpdate msg, ApiCallback<EmptyMessage> callback) {
    appContext.getFrontendApi().MUTATE_WORKSPACE_TREE.send(msg, callback);
  }

  @Override
  public void getFileContents(GetFileContents getFileContents,
      ApiCallback<GetFileContentsResponse> callback) {
    appContext.getFrontendApi().GET_FILE_CONTENTS.send(getFileContents, callback);
  }
  
  @Override
  public void getDirectory(GetDirectory getDirectory, ApiCallback<GetDirectoryResponse> callback) {
    appContext.getFrontendApi().GET_DIRECTORY.send(getDirectory, callback);
  }

  @Override
  public StatusManager getStatusManager() {
    return appContext.getStatusManager();
  }

}
