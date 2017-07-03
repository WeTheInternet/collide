package collide.xapi.plugin;

import collide.gwtc.ui.GwtCompileNavigationHandler;
import collide.gwtc.ui.GwtCompilerService;
import collide.xapi.nav.XapiNavigationHandler;
import collide.xapi.nav.XapiPlace;
import com.google.collide.client.AppContext;
import com.google.collide.client.history.Place;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.FileAssociation;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.ui.button.ImageButton;
import com.google.collide.client.ui.panel.MultiPanel;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.Header.Resources;
import com.google.collide.shared.plugin.PublicService;
import com.google.collide.shared.plugin.PublicServices;
import elemental.dom.Element;

import com.google.gwt.resources.client.ImageResource;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/26/16.
 */
public class XapiPlugin implements ClientPlugin<XapiPlace>, RunConfiguration {

    private final FileAssociation XAPI_FILE_ASSOCIATION;
    private final PublicService<?>[] services = new PublicService[1];
    private XapiNavigationHandler handler;
    private Place place;
    private AppContext appContext;

    public XapiPlugin() {
        XAPI_FILE_ASSOCIATION = f->f.endsWith("xapi");
    }

    @Override
    public String getId() {
        return "XAPI";
    }

    @Override
    public String getLabel() {
        return "Run XApi File";
    }

    @Override
    public void run(AppContext appContext, PathUtil file) {

    }

    @Override
    public Element getForm() {
        return null;
    }

    @Override
    public XapiPlace getPlace() {
        return XapiPlace.PLACE;
    }

    @Override
    public void initializePlugin(
        AppContext appContext, MultiPanel<?, ?> masterPanel, Place currentPlace
    ) {
        handler = new XapiNavigationHandler(appContext, masterPanel, currentPlace);
        services[0] = PublicServices.createProvider(XapiRunnerService.class, handler);
        currentPlace.registerChildHandler(getPlace(), handler);
        this.place = currentPlace;
        this.appContext = appContext;
    }

    @Override
    public ImageResource getIcon(Resources res) {
        return res.xapiIcon();
    }

    @Override
    public void onClicked(ImageButton button) {

    }

    @Override
    public FileAssociation getFileAssociation() {
        return XAPI_FILE_ASSOCIATION;
    }

    @Override
    public RunConfiguration getRunConfig() {
        return this;
    }

    @Override
    public PublicService<?>[] getPublicServices() {
        return new PublicService<?>[0];
    }
}
