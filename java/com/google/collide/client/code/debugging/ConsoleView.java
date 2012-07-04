// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.code.debugging;

import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessage;
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessageLevel;
import com.google.collide.client.code.debugging.DebuggerApiTypes.ConsoleMessageType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObject;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectSubType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.RemoteObjectType;
import com.google.collide.client.code.debugging.DebuggerApiTypes.StackTraceItem;
import com.google.collide.client.util.CssUtils;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.dom.DomUtils;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.mvp.CompositeView;
import com.google.collide.mvp.UiComponent;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.AnchorElement;
import elemental.html.Element;

/**
 * Console View.
 *
 */
public class ConsoleView extends UiComponent<ConsoleView.View> {

  public interface Css extends CssResource {
    String root();
    String consoleMessages();
    String messageRoot();
    String messageLink();
    String repeatCountBubble();
    String consoleObject();
    String consolePrimitiveValue();
    String consoleDebugLevel();
    String consoleErrorLevel();
    String consoleLogLevel();
    String consoleTipLevel();
    String consoleWarningLevel();
    String consoleStackTrace();
    String consoleStackTraceCollapsed();
    String consoleStackTraceExpanded();
    String consoleStackTraceController();
    String consoleStackTraceItem();
  }

  interface Resources extends ClientBundle,
      RemoteObjectTree.Resources,
      RemoteObjectNodeRenderer.Resources {
    @Source("ConsoleView.css")
    Css workspaceEditorConsoleViewCss();

    @Source("errorIcon.png")
    ImageResource errorIcon();

    @Source("warningIcon.png")
    ImageResource warningIcon();

    @Source("triangleRight.png")
    ImageResource triangleRight();

    @Source("triangleDown.png")
    ImageResource triangleDown();
  }

  /**
   * Listener of this pane's events.
   */
  interface Listener {
    void onLocationLinkClick(String url, int lineNumber);
  }

  /**
   * The view for the sidebar call stack pane.
   */
  static class View extends CompositeView<ViewEvents> {
    private final Css css;
    private final Resources resources;
    private final RemoteObjectNodeRenderer nodeRenderer;
    private final JsonArray<RemoteObjectTree> remoteObjectTrees = JsonCollections.createArray();

    private final Element consoleMessages;

    private final EventListener clickListener = new EventListener() {
      @Override
      public void handleEvent(Event evt) {
        Element target = (Element) evt.getTarget();
        if (target.hasClassName(css.messageLink())) {
          evt.preventDefault();
          evt.stopPropagation();

          AnchorElement anchor = (AnchorElement) target;
          int lineNumber = 0;
          String anchorText = anchor.getTextContent();
          int pos = anchorText.lastIndexOf(':');
          if (pos != -1) {
            try {
              lineNumber = (int) Double.parseDouble(anchorText.substring(pos + 1));
              // Safe convert from one-based number to zero-based.
              lineNumber = Math.max(0, lineNumber - 1);
            } catch (NumberFormatException e) {
              // Ignore.
            }
          }
          getDelegate().onLocationLinkClick(anchor.getHref(), lineNumber);
        } else if (target.hasClassName(css.consoleStackTraceController())) {
          Element messageRoot = CssUtils.getAncestorOrSelfWithClassName(target, css.messageRoot());
          if (messageRoot != null) {
            boolean expanded = messageRoot.hasClassName(css.consoleStackTraceExpanded());
            CssUtils.setClassNameEnabled(messageRoot, css.consoleStackTraceExpanded(), !expanded);
            CssUtils.setClassNameEnabled(messageRoot, css.consoleStackTraceCollapsed(), expanded);
          }
        }
      }
    };

    View(Resources resources) {
      this.resources = resources;
      css = resources.workspaceEditorConsoleViewCss();
      nodeRenderer = new RemoteObjectNodeRenderer(resources);

      consoleMessages = Elements.createDivElement(css.consoleMessages());
      consoleMessages.addEventListener(Event.CLICK, clickListener, false);

      Element rootElement = Elements.createDivElement(css.root());
      rootElement.appendChild(consoleMessages);
      setElement(rootElement);
    }

    private void appendConsoleMessage(ConsoleMessage message, DebuggerState debuggerState) {
      boolean forceObjectFormat = false;
      JsonArray<RemoteObject> parameters;
      if (message.getType() != null) {
        switch (message.getType()) {
          case TRACE:
            // Discard all parameters.
            parameters = JsoArray.from(DebuggerApiUtils.createRemoteObject("console.trace()"));
            break;
          case ASSERT:
            parameters = JsoArray.from(DebuggerApiUtils.createRemoteObject("Assertion failed:"));
            parameters.addAll(message.getParameters());
            break;
          case DIR:
          case DIRXML:
            // Use only first parameter, if any.
            parameters = message.getParameters().size() > 0 ?
                message.getParameters().slice(0, 1) :
                JsoArray.from(DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT);
            forceObjectFormat = true;
            break;
          case ENDGROUP:
            // TODO: Support console.group*() some day.
            // Until then, just ignore the console.groupEnd().
            return;
          default:
            parameters = message.getParameters();
            break;
        }
      } else {
        parameters = message.getParameters();
      }

      if (parameters.size() == 0) {
        if (StringUtils.isNullOrEmpty(message.getText())) {
          parameters = JsoArray.from(DebuggerApiTypes.UNDEFINED_REMOTE_OBJECT);
        } else {
          parameters = JsoArray.from(DebuggerApiUtils.createRemoteObject(message.getText()));
        }
      }

      Element messageElement = Elements.createDivElement(css.messageRoot());

      // Add message link first.
      JsonArray<StackTraceItem> stackTrace = message.getStackTrace();
      StackTraceItem topFrame = stackTrace.isEmpty() ? null : stackTrace.get(0);
      if (topFrame != null && !StringUtils.isNullOrEmpty(topFrame.getUrl())) {
        messageElement.appendChild(formatLocationLink(
            topFrame.getUrl(), topFrame.getLineNumber(), topFrame.getColumnNumber()));
      } else if (!StringUtils.isNullOrEmpty(message.getUrl())) {
        messageElement.appendChild(formatLocationLink(
            message.getUrl(), message.getLineNumber(), 0));
      }

      // Add the Stack Trace expand/collapse controller.
      final boolean shouldDisplayStackTrace = !stackTrace.isEmpty() &&
          (message.getType() == ConsoleMessageType.TRACE ||
              message.getLevel() == ConsoleMessageLevel.ERROR);
      if (shouldDisplayStackTrace) {
        if (ConsoleMessageType.TRACE.equals(message.getType())) {
          messageElement.addClassName(css.consoleStackTraceExpanded());
        } else {
          messageElement.addClassName(css.consoleStackTraceCollapsed());
        }
        messageElement.appendChild(Elements.createSpanElement(css.consoleStackTraceController()));
      }

      // Add all message arguments.
      for (int i = 0, n = parameters.size(); i < n; ++i) {
        if (i > 0) {
          messageElement.appendChild(Elements.createTextNode(" "));
        }
        messageElement.appendChild(
            formatRemoteObjectInConsole(parameters.get(i), debuggerState, forceObjectFormat));
      }

      if (message.getLevel() != null) {
        switch (message.getLevel()) {
          case DEBUG:
            messageElement.addClassName(css.consoleDebugLevel());
            break;
          case ERROR:
            messageElement.addClassName(css.consoleErrorLevel());
            break;
          case LOG:
            messageElement.addClassName(css.consoleLogLevel());
            break;
          case TIP:
            messageElement.addClassName(css.consoleTipLevel());
            break;
          case WARNING:
            messageElement.addClassName(css.consoleWarningLevel());
            break;
        }
      }

      if (shouldDisplayStackTrace) {
        messageElement.appendChild(formatStackTrace(stackTrace));
      }

      updateConsoleMessageCount(messageElement, message.getRepeatCount());
      consoleMessages.appendChild(messageElement);
    }

    private void updateLastConsoleMessageCount(int repeatCount) {
      Element messageElement = (Element) consoleMessages.getLastChild();
      if (messageElement == null) {
        return;
      }
      updateConsoleMessageCount(messageElement, repeatCount);
    }

    private void updateConsoleMessageCount(Element messageElement, int repeatCount) {
      Element repeatCountElement = DomUtils.getFirstElementByClassName(
          messageElement, css.repeatCountBubble());
      if (repeatCountElement == null) {
        if (repeatCount > 1) {
          repeatCountElement = Elements.createSpanElement(css.repeatCountBubble());
          repeatCountElement.setTextContent(Integer.toString(repeatCount));
          messageElement.insertBefore(repeatCountElement, messageElement.getFirstChild());
        } else {
          // Do nothing.
        }
      } else {
        if (repeatCount > 1) {
          repeatCountElement.setTextContent(Integer.toString(repeatCount));
        } else {
          repeatCountElement.removeFromParent();
        }
      }
    }

    private Element formatRemoteObjectInConsole(RemoteObject remoteObject,
        DebuggerState debuggerState, boolean forceObjectFormat) {
      if (forceObjectFormat && remoteObject.hasChildren()) {
        return formatRemoteObjectInConsoleAsObject(remoteObject, debuggerState);
      }

      RemoteObjectType type = remoteObject.getType();
      RemoteObjectSubType subType = remoteObject.getSubType();
      if (type == RemoteObjectType.OBJECT && (subType == null ||
          subType == RemoteObjectSubType.ARRAY || subType == RemoteObjectSubType.NODE)) {
        // TODO: Display small ARRAYs inlined.
        // TODO: Display NODE objects as XML tree some day.
        return formatRemoteObjectInConsoleAsObject(remoteObject, debuggerState);
      }

      Element messageElement = Elements.createSpanElement(css.consolePrimitiveValue());
      if (!RemoteObjectType.STRING.equals(type)) {
        String className = nodeRenderer.getTokenClassName(remoteObject);
        if (!StringUtils.isNullOrEmpty(className)) {
          messageElement.addClassName(className);
        }
      }
      messageElement.setTextContent(remoteObject.getDescription());
      return messageElement;
    }

    private Element formatRemoteObjectInConsoleAsObject(RemoteObject remoteObject,
        DebuggerState debuggerState) {
      RemoteObjectTree remoteObjectTree =
          RemoteObjectTree.create(new RemoteObjectTree.View(resources), resources, debuggerState);
      remoteObjectTrees.add(remoteObjectTree);

      RemoteObjectNode newRoot = RemoteObjectNode.createRoot();
      RemoteObjectNode child = new RemoteObjectNode.Builder("", remoteObject)
          .setDeletable(false)
          .setWritable(false)
          .build();
      newRoot.addChild(child);
      remoteObjectTree.setRoot(newRoot);

      Element messageElement = Elements.createSpanElement(css.consoleObject());
      messageElement.appendChild(remoteObjectTree.getView().getElement());
      return messageElement;
    }

    private Element formatLocationLink(String url, int lineNumber, int columnNumber) {
      // TODO: Do real URL parsing to get the path's last component.
      String locationName = url;
      int pos = locationName.lastIndexOf('/');
      if (pos != -1) {
        locationName = locationName.substring(pos + 1);
        if (StringUtils.isNullOrEmpty(locationName)) {
          locationName = "/";
        }
      }

      AnchorElement anchor = Elements.createAnchorElement(css.messageLink());
      anchor.setHref(url);
      anchor.setTarget("_blank");
      anchor.setTitle(url);
      anchor.setTextContent(locationName + ":" + lineNumber);
      return anchor;
    }

    private Element formatStackTrace(JsonArray<StackTraceItem> stackTrace) {
      Element stackTraceElement = Elements.createDivElement(css.consoleStackTrace());
      for (int i = 0, n = stackTrace.size(); i < n; ++i) {
        StackTraceItem item = stackTrace.get(i);
        Element itemElement = Elements.createDivElement(css.consoleStackTraceItem());
        itemElement.appendChild(
            formatLocationLink(item.getUrl(), item.getLineNumber(), item.getColumnNumber()));
        itemElement.appendChild(Elements.createTextNode(
            StringUtils.ensureNotEmpty(item.getFunctionName(), "(anonymous function)")));
        stackTraceElement.appendChild(itemElement);
      }
      return stackTraceElement;
    }

    private void clearConsoleMessages() {
      for (int i = 0, n = remoteObjectTrees.size(); i < n; ++i) {
        remoteObjectTrees.get(i).teardown();
      }
      remoteObjectTrees.clear();
      consoleMessages.setInnerHTML("");
    }
  }

  /**
   * The view events.
   */
  private interface ViewEvents {
    // TODO: Add the button into the UI.
    void onClearConsoleButtonClick();
    void onLocationLinkClick(String url, int lineNumber);
  }

  static ConsoleView create(View view, DebuggerState debuggerState) {
    return new ConsoleView(view, debuggerState);
  }

  private final DebuggerState debuggerState;
  private Listener listener;

  private final DebuggerState.ConsoleListener consoleListener =
      new DebuggerState.ConsoleListener() {
        @Override
        public void onConsoleMessage(ConsoleMessage message) {
          getView().appendConsoleMessage(message, debuggerState);
        }

        @Override
        public void onConsoleMessageRepeatCountUpdated(ConsoleMessage message, int repeatCount) {
          getView().updateLastConsoleMessageCount(repeatCount);
        }

        @Override
        public void onConsoleMessagesCleared() {
          getView().clearConsoleMessages();
        }
      };

  private ConsoleView(View view, DebuggerState debuggerState) {
    super(view);

    this.debuggerState = debuggerState;
    debuggerState.getConsoleListenerRegistrar().add(consoleListener);

    view.setDelegate(new ViewEvents() {
      @Override
      public void onClearConsoleButtonClick() {
        if (ConsoleView.this.debuggerState.isActive()) {
          // TODO: Implement it in the debugger API.
        } else {
          getView().clearConsoleMessages();
        }
      }

      @Override
      public void onLocationLinkClick(String url, int lineNumber) {
        if (listener != null) {
          listener.onLocationLinkClick(url, lineNumber);
        }
      }
    });
  }

  void setListener(Listener listener) {
    this.listener = listener;
  }

  void show() {
    // Do nothing.
  }

  void hide() {
    getView().clearConsoleMessages();
  }
}
