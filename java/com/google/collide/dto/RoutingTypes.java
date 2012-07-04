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

package com.google.collide.dto;

/**
 * Stable routing types for all DTOs.
 * 
 * NOTE: If you add a new DTO, ONLY add to the bottom of the list.
 * 
 *
 */
public class RoutingTypes {
  private RoutingTypes() {    
  }

  public static final int ADDMEMBERSRESPONSE = 1;
  public static final int ADDPROJECTMEMBERS = 2;
  public static final int ADDWORKSPACEMEMBERS = 3;
  public static final int BEGINUPLOADSESSION = 4;
  public static final int CHANGEROLEINFO = 5;
  public static final int CLIENTTOSERVERDOCOP = 6;
  public static final int CODEBLOCK = 7;
  public static final int CODEBLOCKASSOCIATION = 8;
  public static final int CODEERRORS = 9;
  public static final int CODEERRORSREQUEST = 10;
  public static final int CODEGRAPHREQUEST = 11;
  public static final int CODEGRAPHRESPONSE = 12;
  public static final int CODEREFERENCE = 13;
  public static final int CONFLICTCHUNKRESOLVED = 14;
  public static final int CREATEAPPENGINEAPPSTATUS = 15;
  public static final int CREATEPROJECT = 16;
  public static final int CREATEPROJECTRESPONSE = 17;
  public static final int CREATEWORKSPACE = 18;
  public static final int CREATEWORKSPACERESPONSE = 19;
  public static final int CUBEPING = 20;
  public static final int DEPLOYWORKSPACE = 21;
  public static final int DEPLOYWORKSPACESTATUS = 22;
  public static final int DIFFSTATSDTO = 23;
  public static final int EMPTYMESSAGE = 24;
  public static final int INVALIDATIONMESSAGE = 25;
  public static final int ENDUPLOADSESSION = 26;
  public static final int ENTERWORKSPACE = 27;
  public static final int ENTERWORKSPACERESPONSE = 28;
  public static final int FILEPOSITION = 29;
  public static final int GETAPPENGINECLUSTERTYPE = 30;
  public static final int GETDEPLOYINFORMATION = 31;
  public static final int GETDEPLOYINFORMATIONRESPONSE = 32;
  public static final int GETDIRECTORY = 33;
  public static final int GETDIRECTORYRESPONSE = 34;
  public static final int GETFILECONTENTS = 35;
  public static final int GETFILECONTENTSRESPONSE = 36;
  public static final int GETFILEDIFF = 37;
  public static final int GETFILEDIFFRESPONSE = 38;
  public static final int GETFILEREVISIONS = 39;
  public static final int GETFILEREVISIONSRESPONSE = 40;
  // public static final int GETFILETREE = 41;
  // public static final int GETFILETREERESPONSE = 42;
  public static final int GETOWNINGPROJECT = 43;
  public static final int GETOWNINGPROJECTRESPONSE = 44;
  public static final int GETPROJECTBYID = 45;
  public static final int GETPROJECTBYIDRESPONSE = 46;
  public static final int GETPROJECTMEMBERS = 47;
  public static final int GETPROJECTMEMBERSRESPONSE = 48;
  public static final int GETPROJECTSRESPONSE = 49;
  public static final int GETSTAGINGSERVERINFORESPONSE = 50;
  public static final int GETSYNCSTATE = 51;
  public static final int GETSYNCSTATERESPONSE = 52;
  public static final int GETTEMPLATES = 53;
  public static final int GETTEMPLATESRESPONSE = 54;
  public static final int GETUSERAPPENGINEAPPIDS = 55;
  public static final int GETUSERAPPENGINEAPPIDSRESPONSE = 56;
  public static final int GETWORKSPACECHANGESUMMARY = 57;
  public static final int GETWORKSPACECHANGESUMMARYRESPONSE = 58;
  public static final int GETWORKSPACEMEMBERS = 59;
  public static final int GETWORKSPACEMEMBERSRESPONSE = 60;
  public static final int GETWORKSPACEPARTICIPANTS = 61;
  public static final int GETWORKSPACEPARTICIPANTSRESPONSE = 62;
  public static final int GETWORKSPACE = 63;
  public static final int GETWORKSPACERESPONSE = 64;
  public static final int IMPORTASSOCIATION = 65;
  public static final int INHERITANCEASSOCIATION = 66;
  public static final int KEEPALIVE = 67;
  public static final int KEEPALIVERESPONSE = 68;
  public static final int LEAVEWORKSPACE = 69;
  public static final int LOADTEMPLATE = 70;
  public static final int LOADTEMPLATERESPONSE = 71;
  public static final int LOGFATALRECORD = 72;
  public static final int LOGFATALRECORDRESPONSE = 73;
  public static final int LOGMETRIC = 74;
  public static final int LOGMETRICS = 75;
  public static final int MEMBERSHIPCHANGEDPAYLOAD = 76;
  public static final int NODECONFLICTDTO = 77;
  public static final int NODEMUTATIONDTO = 78;
  public static final int PARTICIPANTSINFO = 79;
  public static final int RECOVERFROMDROPPEDTANGOINVALIDATION = 80;
  public static final int RECOVERFROMDROPPEDTANGOINVALIDATIONRESPONSE = 81;
  public static final int RECOVERFROMMISSEDDOCOPS = 82;
  public static final int RECOVERFROMMISSEDDOCOPSRESPONSE = 83;
  public static final int REFRESHWORKSPACE = 84;
  public static final int REQUESTPROJECTMEMBERSHIP = 85;
  public static final int RESOLVECONFLICTCHUNK = 86;
  public static final int RESOLVETREECONFLICT = 87;
  public static final int RESOLVETREECONFLICTRESPONSE = 88;
  public static final int SEARCH = 89;
  public static final int SEARCHRESPONSE = 90;
  public static final int SERVERERROR = 91;
  public static final int SERVERTOCLIENTDOCOP = 92;
  public static final int SERVERTOCLIENTDOCOPS = 93;
  public static final int SETACTIVEPROJECT = 94;
  public static final int SETPROJECTHIDDEN = 95;
  public static final int SETPROJECTROLE = 96;
  public static final int SETROLERESPONSE = 97;
  public static final int SETSTAGINGSERVERAPPID = 98;
  public static final int SETWORKSPACEARCHIVESTATE = 99;
  public static final int SETWORKSPACEARCHIVESTATERESPONSE = 100;
  public static final int SETWORKSPACEROLE = 101;
  public static final int SETUPMIMIC = 102;
  public static final int STACKTRACEELEMENTDTO = 103;
  public static final int SUBMIT = 104;
  public static final int SUBMITRESPONSE = 105;
  public static final int SUBMITTEDWORKSPACE = 106;
  public static final int SYNC = 107;
  public static final int THROWABLEDTO = 108;
  public static final int TYPEASSOCIATION = 109;
  public static final int UNDOLASTSYNC = 110;
  public static final int UPDATEPROJECT = 111;
  public static final int UPDATEUSERWORKSPACEMETADATA = 112;
  public static final int UPDATEWORKSPACE = 113;
  public static final int UPDATEWORKSPACERUNTARGETS = 114;
  public static final int GETWORKSPACEMETADATARESPONSE = 115;
  public static final int WORKSPACEINFO = 116;
  public static final int WORKSPACETREEUPDATE = 117;
  public static final int WORKSPACETREEUPDATEBROADCAST = 118;
  public static final int INVALIDXSRFTOKENSERVERERROR = 119;
  public static final int ENDUPLOADSESSIONFINISHED = 120;
  public static final int RETRYALREADYTRANSFERREDUPLOAD = 121;
  public static final int GETWORKSPACEMETADATA = 122;
}
