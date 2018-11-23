package collide.client.editor;

/**
 * This class is a place to control the mode of operation of the editor.
 *
 * The current implementation is ...very hardcoded together,
 * and to inject more easily customizable control,
 * we will use this as the master class to expose new versions of internal components,
 * to do a more gradual refactoring.
 *
 * We can (probably) IoC this class out of existence later,
 * though it would likely still be handy for quick hardcoded overrides.
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/4/17.
 */
public interface EditorConfiguration {

    EditorToolbar getEditorToolbar();

}
