/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.js.html;

import elemental.dom.DataTransferItem;
import elemental.html.DragEvent;

public class JsDragEvent extends elemental.js.events.JsMouseEvent implements DragEvent {

  protected JsDragEvent() { }

  @Override
  public final native DataTransferItem getDataTransferItem() /*-{
    return this.dataTransfer;
  }-*/;

  public final native void initDragEvent(String typeArg, boolean canBubbleArg, boolean cancelableArg, Object dummyArg, int detailArg, int screenXArg, int screenYArg, int clientXArg, int clientYArg, boolean ctrlKeyArg, boolean altKeyArg, boolean shiftKeyArg, boolean metaKeyArg, short buttonArg, elemental.events.EventTarget relatedTargetArg, DataTransferItem dataTransferArg) /*-{
    this.initDragEvent(typeArg, canBubbleArg, cancelableArg, dummyArg, detailArg, screenXArg, screenYArg, clientXArg, clientYArg, ctrlKeyArg, altKeyArg, shiftKeyArg, metaKeyArg, buttonArg, relatedTargetArg, dataTransferArg);
  }-*/;
}