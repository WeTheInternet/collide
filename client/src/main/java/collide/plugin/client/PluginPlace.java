package collide.plugin.client;

import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationEvent;
import com.google.collide.json.client.JsoStringMap;
import com.google.collide.json.shared.JsonStringMap;

public class PluginPlace extends Place{

  public class NavigationEvent extends PlaceNavigationEvent<PluginPlace> {

    public static final String SHOW_HIDE = "show";
    private Boolean show;

    protected NavigationEvent(Boolean show) {
      super(PluginPlace.this);
      this.show = show;
    }

    public boolean isShow() {
      return Boolean.TRUE.equals(show);
    }

    public boolean isHide() {
      return Boolean.FALSE.equals(show);
    }

    @Override
    public JsonStringMap<String> getBookmarkableState() {
      JsoStringMap<String> state = JsoStringMap.create();
      if (show != null) {
        state.put(SHOW_HIDE, Boolean.toString(show));
      }
      return state;
    }


  }

  public static final PluginPlace PLACE = new PluginPlace();

  protected PluginPlace() {
    super("collide");
  }

  @Override
  public PlaceNavigationEvent<? extends Place> createNavigationEvent(JsonStringMap<String> decodedState) {
    Boolean show = null;
    if (decodedState.containsKey(NavigationEvent.SHOW_HIDE))
      show = Boolean.parseBoolean(decodedState.get(NavigationEvent.SHOW_HIDE));
    return new NavigationEvent(show);
  }

}
