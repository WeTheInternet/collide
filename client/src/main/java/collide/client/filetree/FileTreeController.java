package collide.client.filetree;

import com.google.collide.client.code.FileTreeSection;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.ui.dropdown.DropdownWidgets;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.dto.EmptyMessage;
import com.google.collide.dto.GetDirectory;
import com.google.collide.dto.GetDirectoryResponse;
import com.google.collide.dto.GetFileContents;
import com.google.collide.dto.GetFileContentsResponse;
import com.google.collide.dto.WorkspaceTreeUpdate;

public interface FileTreeController
  <R extends
  DropdownWidgets.Resources &
  Tooltip.Resources &
  FileTreeNodeRenderer.Resources &
  FileTreeSection.Resources> {

  R getResources();

  void mutateWorkspaceTree(WorkspaceTreeUpdate msg, ApiCallback<EmptyMessage> apiCallback);

  StatusManager getStatusManager();

  void getDirectory(
      GetDirectory getDirectoryAndPath,
      ApiCallback<GetDirectoryResponse> apiCallback
  );

  MessageFilter getMessageFilter();

  void getFileContents(
      GetFileContents getFileContents,
      ApiCallback<GetFileContentsResponse> apiCallback
  );

}
