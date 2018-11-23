package collide.client.editor;

import com.google.collide.client.util.PathUtil;
import com.google.collide.mvp.ShowableComponent;
import com.google.collide.mvp.ShowableUiComponent;

/**
 * Basic interface for the editor toolbar; we are going to move the legacy
 * implementation into {@link com.google.collide.client.code.DefaultEditorToolBar},
 * so we can plug in something custom for wti/xapi files.
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/4/17.
 */
public interface EditorToolbar extends ShowableComponent {

    ShowableUiComponent<?> asComponent();

    void setCurrentPath(PathUtil path, String lastAppliedRevision);

}
