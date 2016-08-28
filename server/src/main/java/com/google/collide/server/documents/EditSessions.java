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

package com.google.collide.server.documents;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocumentSelection;
import com.google.collide.dto.FileContents;
import com.google.collide.dto.FileContents.ContentType;
import com.google.collide.dto.server.DtoServerImpls.ClientToServerDocOpImpl;
import com.google.collide.dto.server.DtoServerImpls.DocOpComponentImpl;
import com.google.collide.dto.server.DtoServerImpls.DocOpImpl;
import com.google.collide.dto.server.DtoServerImpls.DocumentSelectionImpl;
import com.google.collide.dto.server.DtoServerImpls.FileContentsImpl;
import com.google.collide.dto.server.DtoServerImpls.GetFileContentsImpl;
import com.google.collide.dto.server.DtoServerImpls.GetFileContentsResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.RecoverFromMissedDocOpsImpl;
import com.google.collide.dto.server.DtoServerImpls.RecoverFromMissedDocOpsResponseImpl;
import com.google.collide.dto.server.DtoServerImpls.ServerToClientDocOpImpl;
import com.google.collide.dto.server.DtoServerImpls.ServerToClientDocOpsImpl;
import com.google.collide.json.server.JsonArrayListAdapter;
import com.google.collide.server.documents.VersionedDocument.AppliedDocOp;
import com.google.collide.server.documents.VersionedDocument.DocumentOperationException;
import com.google.collide.server.participants.Participants;
import com.google.collide.server.shared.util.Dto;
import com.google.collide.shared.MimeTypes;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Backend service that maintains in-memory edit sessions for documents that are being
 * collaboratively edited.
 * 
 * All text mutations and document related changes are handled by this service.
 * 
 */
public class EditSessions extends BusModBase {
  private static final Gson gson = new GsonBuilder().registerTypeAdapter(
      DocOpComponentImpl.class, new DocOpComponentDeserializer()).serializeNulls().create();

  /**
   * Receives Document operations and applies them to the corresponding FileEditSession.
   * 
   *  If there is no associated FileEditSession, we need log an error since that probably means we
   * have a stale client.
   */
  class DocumentMutator implements Handler<Message<JsonObject>> {
    private final SelectionTracker selectionTracker = new SelectionTracker();

    @Override
    public void handle(Message<JsonObject> message) {
      ClientToServerDocOpImpl wrappedDocOp =
          ClientToServerDocOpImpl.fromJsonString(Dto.get(message));

      String resourceId = wrappedDocOp.getFileEditSessionKey();

      FileEditSession editSession = editSessions.get(resourceId);

      // Apply the DocOp.
      if (editSession != null) {
        List<String> docOps = ((JsonArrayListAdapter<String>) wrappedDocOp.getDocOps2()).asList();
        ServerToClientDocOpsImpl appliedDocOps = applyMutation(
            docOps, wrappedDocOp.getClientId(), wrappedDocOp.getCcRevision(),
            wrappedDocOp.getSelection(), resourceId, editSession);
        message.reply(Dto.wrap(appliedDocOps));
      }
    }

    private List<DocOp> deserializeDocOps(List<String> serializedDocOps) {
      List<DocOp> docOps = new ArrayList<DocOp>();     
      for (String serializedDocOp : serializedDocOps) {
        docOps.add(gson.fromJson(serializedDocOp, DocOpImpl.class));
      }
      return docOps;
    }

    private ServerToClientDocOpsImpl applyMutation(List<String> serializedDocOps, String authorId,
        int ccRevision, DocumentSelection selection, String resourceId, FileEditSession editSession) {
      try {
        List<DocOp> docOps = deserializeDocOps(serializedDocOps);
        VersionedDocument.ConsumeResult result =
            editSession.consume(docOps, authorId, ccRevision, selection);

        // See if we need to update the selection
        checkForSelectionChange(
            authorId, resourceId, editSession.getDocument(), result.transformedDocumentSelection);

        // Construct the Applied DocOp that we want to broadcast.
        SortedMap<Integer, AppliedDocOp> appliedDocOps = result.appliedDocOps;
        List<ServerToClientDocOpImpl> appliedDocOpsList = Lists.newArrayList();
        for (Entry<Integer, VersionedDocument.AppliedDocOp> entry : appliedDocOps.entrySet()) {
          DocOpImpl docOp = (DocOpImpl) entry.getValue().docOp;
          ServerToClientDocOpImpl wrappedBroadcastDocOp = ServerToClientDocOpImpl.make()
              .setClientId(authorId).setAppliedCcRevision(entry.getKey()).setDocOp2(docOp)
              .setFileEditSessionKey(resourceId)
              .setFilePath(editSession.getSavedPath());
          appliedDocOpsList.add(wrappedBroadcastDocOp);
        }

        // Add the selection to the last DocOp if there was one.
        if (result.transformedDocumentSelection != null && appliedDocOpsList.size() > 0) {
          appliedDocOpsList.get(appliedDocOpsList.size() - 1)
              .setSelection((DocumentSelectionImpl) result.transformedDocumentSelection);
        }

        // Broadcast the applied DocOp all the participants, ignoring the sender.
        ServerToClientDocOpsImpl broadcastedDocOps =
            ServerToClientDocOpsImpl.make().setDocOps(appliedDocOpsList);
        vertx.eventBus().send("participants.broadcast", new JsonObject().putString(
            Participants.OMIT_SENDER_TAG, authorId).putString(
            "payload", broadcastedDocOps.toJson()));
        return broadcastedDocOps;
      } catch (DocumentOperationException e) {
        logger.error(String.format("Failed to apply DocOps [%s]", serializedDocOps));
      }
      return null;
    }

    private void checkForSelectionChange(String clientId, String resourceId,
        VersionedDocument document, DocumentSelection documentSelection) {
      /*
       * Currently, doc ops either contain text changes or selection changes (via annotation doc op
       * components). Both of these modify the user's selection/cursor.
       */
      selectionTracker.selectionChanged(clientId, resourceId, document, documentSelection);
    }
  }

  /**
   * Replies with the DocOps that were missed by the requesting client.
   */
  class DocOpRecoverer implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> event) {
      RecoverFromMissedDocOpsImpl req = RecoverFromMissedDocOpsImpl.fromJsonString(Dto.get(event));

      String resourceId = req.getFileEditSessionKey();
      FileEditSession editSession = editSessions.get(resourceId);

      if (editSession == null) {
        logger.error("No edit session for resourceId " + resourceId);

        // TODO: This is going to leave the reply handler hanging.
        return;
      }

      List<String> docOps = ((JsonArrayListAdapter<String>) req.getDocOps2()).asList();

      // If the client is re-sending any unacked doc ops, apply them first
      if (req.getDocOps2().size() > 0) {
        documentMutator.applyMutation(
            docOps, req.getClientId(), req.getCurrentCcRevision(), null, resourceId, editSession);
      }

      // Get all the applied doc ops the client doesn't know about
      SortedMap<Integer, VersionedDocument.AppliedDocOp> appliedDocOps =
          editSession.getDocument().getAppliedDocOps(req.getCurrentCcRevision() + 1);

      List<ServerToClientDocOpImpl> appliedDocOpsList = Lists.newArrayList();
      for (Entry<Integer, VersionedDocument.AppliedDocOp> entry : appliedDocOps.entrySet()) {
        DocOpImpl docOp = (DocOpImpl) entry.getValue().docOp;
        ServerToClientDocOpImpl wrappedBroadcastDocOp = ServerToClientDocOpImpl.make()
            .setClientId(req.getClientId()).setAppliedCcRevision(entry.getKey()).setDocOp2(docOp)
            .setFileEditSessionKey(resourceId)
            .setFilePath(editSession.getSavedPath());
        appliedDocOpsList.add(wrappedBroadcastDocOp);
      }

      RecoverFromMissedDocOpsResponseImpl resp =
          RecoverFromMissedDocOpsResponseImpl.make().setDocOps(appliedDocOpsList);
      event.reply(Dto.wrap(resp));
    }
  }

  /**
   * Creates a FileEditSession if there is not one already present and
   */
  class EditSessionCreator implements Handler<Message<JsonObject>> {

    /**
     * Whether or not to provision an edit session if one is missing, before sending the file
     * contents
     */
    private final boolean provisionEditSession;

    EditSessionCreator(boolean provisionEditSession) {
      this.provisionEditSession = provisionEditSession;
    }

    @Override
    public void handle(final Message<JsonObject> message) {
      final GetFileContentsImpl request = GetFileContentsImpl.fromJsonString(Dto.get(message));

      // Resolve the resource IDs from the requested path.
      vertx.eventBus().send("tree.getResourceIds",
          new JsonObject().putArray("paths", new JsonArray().addString(request.getPath())),
          new Handler<Message<JsonObject>>() {

            /**
             * Sends the contents of a file to the requester. The files will be served out of the
             * FileEditSession if the contents are being edited, otherwise they will simply be
             * served from disk.
             */
              @Override
            public void handle(Message<JsonObject> event) {
              JsonArray resourceIdArr = event.body.getArray("resourceIds");
              Object[] resourceIds = resourceIdArr.toArray();
              String resourceId = (String) resourceIds[0];

              String currentPath = stripLeadingSlash(request.getPath());
              FileEditSession editSession = editSessions.get(resourceId);

              // Create the DTO for the file contents response. We will build it up later in the
              // method.
              String mimeType = MimeTypes.guessMimeType(currentPath, false);
              FileContentsImpl fileContentsDto =
                  FileContentsImpl.make().setMimeType(mimeType).setPath(currentPath);

              if (editSession == null) {
                // We need to start a new edit session.
                String text = "";
                File file = new File(currentPath);
                try {
                  text = Files.toString(file, Charsets.UTF_8);
                } catch (IOException e) {
                  logger.error(
                      String.format("Failed to read text contents for path [%s]", currentPath));

                  // Send back a no file indicating that file does not exist.
                  sendContent(message, currentPath, null, false);
                  return;
                }

                if (provisionEditSession) {

                  // Provision a new edit session and fall through.
                  editSession =
                      new FileEditSessionImpl(resourceId, currentPath, text, null, logger);
                  editSessions.put(resourceId, editSession);                

                  // Update the last opened file.
                  vertx.eventBus().send("workspace.setLastOpenedFile",
                      new JsonObject().putString("resourceId", resourceId));
                } else {

                  // Just send the contents as they were read from disk and return.
                  String dataBase64 = MimeTypes.looksLikeImage(mimeType) ? StringUtils
                      .newStringUtf8(Base64.encodeBase64(text.getBytes())) : null;
                  fileContentsDto.setContents(dataBase64).setContentType(
                      dataBase64 == null ? ContentType.UNKNOWN_BINARY : ContentType.IMAGE);
                  sendContent(message, currentPath, fileContentsDto, true);
                  return;
                }
              }              

              // Populate file contents response Dto with information from the edit session.
              fileContentsDto.setFileEditSessionKey(resourceId)
                  .setCcRevision(editSession.getDocument().getCcRevision())
                  .setContents(editSession.getContents()).setContentType(ContentType.TEXT);

              // Extract the contents from the edit session before sending.
              sendContent(message, currentPath, fileContentsDto, true);
            }
          });
    }
  }

  void sendContent(
      Message<JsonObject> event, String path, FileContents fileContents, boolean fileExists) {
    GetFileContentsResponseImpl response = GetFileContentsResponseImpl.make()
        .setFileExists(fileExists).setFileContents((FileContentsImpl) fileContents);
    event.reply(Dto.wrap(response.toJson()));
  }

  /**
   * Iterates through all open, dirty edit sessions and saves them to disk.
   */
  class FileSaver implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      saveAll();
    }

    void saveAll() {
      Set<Entry<String, FileEditSession>> entries = editSessions.entrySet();
      Iterator<Entry<String, FileEditSession>> entryIter = entries.iterator();
      final JsonArray resourceIds = new JsonArray();
      while (entryIter.hasNext()) {
        Entry<String, FileEditSession> entry = entryIter.next();
        String resourceId = entry.getKey();
        FileEditSession editSession = entry.getValue();
        if (editSession.hasChanges()) {
          resourceIds.addString(resourceId);
        }
      }

      // Resolve the current paths of opened files in case they have been moved.
      eb.send("tree.getCurrentPaths", new JsonObject().putArray("resourceIds", resourceIds),
          new Handler<Message<JsonObject>>() {
              @Override
            public void handle(Message<JsonObject> event) {
              JsonArray currentPaths = event.body.getArray("paths");
              Iterator<Object> pathIter = currentPaths.iterator();
              Iterator<Object> resourceIter = resourceIds.iterator();

              if (currentPaths.size() != resourceIds.size()) {
                logger.error(String.format(
                    "Received [%d] paths in response to a request specifying [%d] resourceIds",
                    currentPaths.size(), resourceIds.size()));
              }

              // Iterate through all the resolved paths and save the files to disk.
              while (pathIter.hasNext()) {
                String path = (String) pathIter.next();
                String resourceId = (String) resourceIter.next();

                if (path != null) {
                  FileEditSession editSession = editSessions.get(resourceId);
                  if (editSession != null) {
                    try {
                      editSession.save(stripLeadingSlash(path));
                    } catch (IOException e) {
                      logger.error(String.format("Failed to save file [%s]", path), e);
                    }
                  }
                }
              }
            }
          });
    }
  }

  /**
   * Removes an edit session, and notifies clients that they should reload their opened document.
   */
  class EditSessionRemover implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      String resourceId = message.body.getString("resourceId");
      if (resourceId != null) {
        editSessions.remove(resourceId);
      }
      // TODO: Notify clients to reload their opened document.
    }
  }

  private final Map<String, FileEditSession> editSessions = new HashMap<String, FileEditSession>();
  private final FileSaver fileSaver = new FileSaver();
  private final DocumentMutator documentMutator = new DocumentMutator();
  private String addressBase;

  @Override
  public void start() {
    super.start();
    this.addressBase = getOptionalStringConfig("address", "documents");
    vertx.eventBus().registerHandler(addressBase + ".mutate", documentMutator);
    vertx.eventBus().registerHandler(
        addressBase + ".createEditSession", new EditSessionCreator(true));
    vertx.eventBus().registerHandler(
        addressBase + ".getFileContents", new EditSessionCreator(false));
    vertx.eventBus().registerHandler(addressBase + ".saveAll", fileSaver);
    vertx.eventBus().registerHandler(addressBase + ".removeEditSession", new EditSessionRemover());
    vertx.eventBus().registerHandler(addressBase + ".recoverMissedDocop", new DocOpRecoverer());

    // TODO: Handle content changes on disk and synthesize a docop to apply to the in-memory edit
    // session, and broadcast to all clients.

    // Set up a regular save interval to flush to disk.
    vertx.setPeriodic(1500, new Handler<Long>() {
        @Override
      public void handle(Long event) {
        fileSaver.saveAll();
      }
    });
  }

  /**
   * This verticle needs to take "workspace rooted paths", which begin with a leading '/', and make
   * them relative to the base directory for the associated classloader for this verticle. That is,
   * assume that all paths are relative to what our local view of '.' is on the file system. We
   * simply strip the leading slash.
   */
  private String stripLeadingSlash(String relative) {
    if (relative == null) {
      return null;
    } else {
      return relative.charAt(0) == '/' ? relative.substring(1) : relative;
    }
  }
}
