package collide.plugin.client.launcher;

import xapi.util.api.RemovalHandler;

public interface LauncherService {

  RemovalHandler openInIframe(String id, String url);

  void openInNewWindow(String id, String url);

}
