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

package com.google.collide.client.code;

import com.google.collide.dto.client.DtoClientImpls;

/**
 * Model object for a participant. This extends the
 * {@link com.google.collide.dto.Participant} class used for data
 * transfer.
 */
public class Participant extends DtoClientImpls.ParticipantImpl {

  private static final String DISPLAY_NAME_KEY = "__displayName";

  private static final String DISPLAY_EMAIL_KEY = "__email";

  private static final String COLOR_KEY = "__color";

  private static final String IS_SELF_KEY = "__isSelf";

  static Participant create(
      com.google.collide.dto.Participant participantDto, String displayName,
      String displayEmail, String color, boolean isSelf) {
    DtoClientImpls.ParticipantImpl participantDtoImpl =
        (DtoClientImpls.ParticipantImpl) participantDto;

    // TODO: Wrap Participant instead of adding fields to the DTO.
    participantDtoImpl.addField(DISPLAY_NAME_KEY, displayName);
    participantDtoImpl.addField(DISPLAY_EMAIL_KEY, displayEmail);
    participantDtoImpl.addField(COLOR_KEY, color);
    participantDtoImpl.addField(IS_SELF_KEY, isSelf);

    return participantDtoImpl.cast();
  }

  protected Participant() {
  }

  public final String getColor() {
    return getStringField(COLOR_KEY);
  }

  public final String getDisplayName() {
    return isSelf() ? "Me" : getStringField(DISPLAY_NAME_KEY);
  }

  public final String getDisplayEmail() { 
    return getStringField(DISPLAY_EMAIL_KEY);
  }

  public final boolean isSelf() {
    return getBooleanField(IS_SELF_KEY);
  }
}
