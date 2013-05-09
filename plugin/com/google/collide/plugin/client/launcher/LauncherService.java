package com.google.collide.plugin.client.launcher;

public interface LauncherService {

  void openInIframe(String id, String url);
  
  void openInNewWindow(String id, String url);
  
}
