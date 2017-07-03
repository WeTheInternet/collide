package collide.xapi.nav;

import collide.xapi.nav.XapiPlace.NavigationEvent;
import collide.xapi.plugin.XapiRunnerService;
import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.history.PlaceNavigationHandler;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.ui.panel.PanelModel;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/26/16.
 */
public class XapiNavigationHandler extends PlaceNavigationHandler<XapiPlace.NavigationEvent> implements
    XapiRunnerService {

    private final AppContext context;
    private final MultiPanel<? extends PanelModel,?> contentArea;
    private final Place currentPlace;

    public XapiNavigationHandler(
        AppContext context,
        MultiPanel<? extends PanelModel, ?> contentArea,
        Place currentPlace
    ) {
        this.context = context;
        this.contentArea = contentArea;
        this.currentPlace = currentPlace;
    }

    @Override
    protected void enterPlace(NavigationEvent navigationEvent) {

    }
}
