package collide.plugin;

import collide.gwtc.ui.GwtClientPlugin;
import collide.plugin.client.terminal.TerminalClientPlugin;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.ClientPluginService;
import xapi.annotation.inject.SingletonDefault;

@SingletonDefault(implFor=ClientPluginService.class)
public class PluginAggregator extends ClientPluginService{

  private final ClientPlugin<?>[] plugins;
  public PluginAggregator(){
    plugins = initPlugins();
  }

  protected ClientPlugin<?>[] initPlugins() {
    return new ClientPlugin[] {
      new TerminalClientPlugin()
      ,new GwtClientPlugin()
    };
  }


  @Override
  public ClientPlugin<?>[] plugins() {
    return plugins;
  }

  @Override
  public void cleanup() {
    super.cleanup();
  }
}
