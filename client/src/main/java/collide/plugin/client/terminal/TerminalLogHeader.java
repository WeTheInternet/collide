package collide.plugin.client.terminal;

import com.google.collide.dto.LogMessage;

import elemental.dom.Element;
/**
 * A simple interface to give to a {@link TerminalService} as a header element.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface TerminalLogHeader {

  String getName();
  void viewLog(LogMessage log);
  Element getElement();

}
