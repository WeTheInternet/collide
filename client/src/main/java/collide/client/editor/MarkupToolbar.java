package collide.client.editor;

import collide.client.util.Elements;
import com.google.collide.client.AppContext;
import com.google.collide.client.code.DefaultEditorToolBar;
import com.google.collide.client.code.EditorBundle;
import com.google.collide.client.code.FileSelectedPlace;
import com.google.collide.client.code.RightSidebarToggleEvent;
import com.google.collide.client.filehistory.FileHistoryPlace;
import com.google.collide.client.history.Place;
import com.google.collide.client.ui.menu.PositionController;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.WorkspacePlace;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.ShowableUiComponent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/4/17.
 */
public class MarkupToolbar extends ShowableUiComponent<MarkupToolbar.View>
    implements EditorToolbar {

    private final Place currentPlace;
    private final AppContext appContext;
    private final EditorBundle editorBundle;

    public MarkupToolbar(MarkupToolbar.View view, Place place,
        AppContext appContext, EditorBundle bundle) {
        super(view);
        this.currentPlace = place;
        this.appContext = appContext;
        this.editorBundle = bundle;
        view.setDelegate(new ViewEventsImpl());
    }

    /**
     * The View for the MarkupToolbar.
     */
    public static class View extends CompositeView<ViewEvents> {
        @UiTemplate("MarkupToolbar.ui.xml") interface MyBinder
            extends UiBinder<DivElement, MarkupToolbar.View> {
        }

        static MyBinder binder = GWT.create(MyBinder.class);

        @UiField
        DivElement toolButtons;

        @UiField
        DivElement historyButton;

        @UiField
        DivElement historyIcon;

        @UiField
        DivElement debugButton;

        @UiField
        DivElement debugIcon;

        @UiField(provided = true)
        final DefaultEditorToolBar.Resources res;

        public View(DefaultEditorToolBar.Resources res, boolean detached) {
            this.res = res;
            setElement(Elements.asJsElement(binder.createAndBindUi(this)));
            //attachHandlers();
            // Make these tooltips right aligned since they're so close to the edge of the screen.
            PositionController.PositionerBuilder positioner =
                new Tooltip.TooltipPositionerBuilder().setHorizontalAlign(
                    PositionController.HorizontalAlign.RIGHT)
                    .setPosition(PositionController.Position.OVERLAP);
            PositionController.Positioner historyTooltipPositioner =
                positioner.buildAnchorPositioner(Elements.asJsElement(historyIcon));
            new Tooltip.Builder(
                res, Elements.asJsElement(historyIcon), historyTooltipPositioner).setTooltipText(
                "Explore this file's changes over time.").build().setTitle("History");

            PositionController.Positioner debugTooltipPositioner =
                positioner.buildAnchorPositioner(Elements.asJsElement(debugIcon));
            new Tooltip.Builder(
                res, Elements.asJsElement(debugIcon), debugTooltipPositioner).setTooltipText(
                "Opens the debug panel where you can set breakpoints and watch expressions.")
                .build().setTitle("Debugging Controls");
            if (detached) {
                toolButtons.addClassName(res.editorToolBarCss().detached());
            }
        }
    }

    /**
     * Events reported by the DefaultEditorToolBar's View.
     */
    private interface ViewEvents {
        void onDebugButtonClicked();

        void onHistoryButtonClicked();
    }

    private class ViewEventsImpl implements ViewEvents {

        @Override
        public void onHistoryButtonClicked() {
        }

        @Override
        public void onDebugButtonClicked() {
            WorkspacePlace.PLACE.fireEvent(new RightSidebarToggleEvent());
        }
    }

    @Override
    public ShowableUiComponent<?> asComponent() {
        return this;
    }

    @Override public void setCurrentPath(PathUtil path, String lastAppliedRevision) {

    }

    @Override public void doShow() {

    }

    @Override public void doHide() {

    }
}
