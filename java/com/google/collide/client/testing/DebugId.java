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

package com.google.collide.client.testing;

/**
 * Element debug IDs used by integration test to locate elements.
 */
public enum DebugId {
  // Debug
  DEBUG_BREAKPOINT_SLIDER,

  // DeployPopup
  DEPLOY_POPUP_BASE,
  DEPLOY_POPUP_DEPLOY_BUTTON,
  DEPLOY_POPUP_DONE_BUTTON,
  DEPLOY_POPUP_EDIT_BUTTON,
  DEPLOY_POPUP_APP_IDS_DROPDOWN,

  // ManageMembership
  MANAGE_MEMBERSHIP_PENDING_REQUESTS_TITLE,
  MANAGE_MEMBERSHIP_DONE_BUTTON,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_INPUT,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_BUTTON,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_CANCEL_BUTTON,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_ROLE_BUTTON,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_SEND_EMAIL_CHECKBOX,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_SEND_EMAIL_LABEL,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_COPY_SELF_CHECKBOX,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_COPY_SELF_LABEL,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_TOGGLE_MESSAGE_BUTTON,
  MANAGE_MEMBERSHIP_ADD_MEMBERS_PRIVATE_MESSAGE_INPUT,
  MANAGE_MEMBERSHIP_ROW,
  MANAGE_MEMBERSHIP_ROW_ADD_MEMBER_ROLE_BUTTON,
  MANAGE_MEMBERSHIP_ROW_CHANGE_MEMBER_ROLE_BUTTON,
  MANAGE_MEMBERSHIP_ROW_IGNORE_BUTTON,
  MANAGE_MEMBERSHIP_ROW_IGNORED_CONTENT,
  MANAGE_MEMBERSHIP_ROW_UNDO_IGNORE_BUTTON,
  MANAGE_MEMBERSHIP_ROW_UNDO_REVOKE_BUTTON,
  MANAGE_MEMBERSHIP_ROW_BLOCK_BUTTON,
  MANAGE_MEMBERSHIP_ROW_UNBLOCK_BUTTON,

  // ProjectNavigation
  PROJECT_NAVIGATION_PROJECT_LINK,

  // ProjectMenu
  PROJECT_MENU_REQUEST_MEMBERSHIP,
  PROJECT_MENU_PENDING_REQUEST_LABEL,
  PROJECT_MENU_PENDING_MANAGE_MEMBERS_BUTTON,

  // MemberRoleDropdown
  MEMBER_ROLE_DROPDOWN_ROW,

  // RequestMembership
  REQUEST_MEMBERSHIP_BASE,
  REQUEST_MEMBERSHIP_CANCEL_BUTTON,
  REQUEST_MEMBERSHIP_SEND_BUTTON,

  // StatusPresenter
  STATUS_PRESENTER,

  // WorkspaceHeader
  WORKSPACE_HEADER_SHARE_BUTTON,

  // WorkspaceListing
  WORKSPACE_LISTING_ROW;

  /**
   * Gets key for setting debug attribute.
   * <p>
   * The method is also used by JS test code to ensure consistent key value.
   */
  public static String getAttributeKey(String key) {
    return getIdKey() + "_" + key;
  }

  /**
   * Gets key for setting debug ID.
   * <p>
   * The method is also used by JS test code to ensure consistent key value.
   */
  public static String getIdKey() {
    return "collideid";
  }
}
