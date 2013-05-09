package collide.demo.view;

public interface PanelController {

  void doMinimize();
  void doClose();
  void doMaximize();
  // To let subclasses call into custom functionality
  void callPlugin(Class<?> pluginClass, Object ... args);
  
  
}
