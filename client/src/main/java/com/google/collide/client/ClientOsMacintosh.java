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

package com.google.collide.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

/**
 * Information about an "other" ClientOs.
 * 
 */
public class ClientOsMacintosh implements ClientOs {

  protected interface MessageStrings extends Messages {
    @DefaultMessage("Option")
    public String alt();

    @DefaultMessage("\u2325")
    public String altAbbr();

    @DefaultMessage("Control")
    public String ctrl();

    @DefaultMessage("Ctrl")
    public String ctrlAbbr();

    @DefaultMessage("Command")
    public String cmd();
    
    @DefaultMessage("\u2318")
    public String cmdAbbr();

    @DefaultMessage("Shift")
    public String shift();

    @DefaultMessage("\u21e7")
    public String shiftAbbr();
}
  
  private static final MessageStrings messages = GWT.create(MessageStrings.class);
  
  @Override
  public String actionKeyDescription() {
    return messages.cmd();
  }

  @Override
  public String actionKeyLabel() {
    return messages.cmdAbbr();
  }

  @Override
  public boolean isMacintosh() {
    return true;
  }

  @Override
  public String shiftKeyDescription() {
    return messages.shift();
  }

  @Override
  public String shiftKeyLabel() {
    return messages.shiftAbbr();
  }

  @Override
  public String altKeyDescription() {
    return messages.alt();
  }

  @Override
  public String altKeyLabel() {
    return messages.altAbbr();
  }

  @Override
  public String ctrlKeyDescription() {
    return messages.ctrl();
  }

  @Override
  public String ctrlKeyLabel() {
    return messages.ctrlAbbr();
  }
}
