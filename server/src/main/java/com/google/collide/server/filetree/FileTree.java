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

package com.google.collide.server.filetree;

import com.google.collide.dto.DirInfo;
import com.google.collide.dto.FileInfo;
import com.google.collide.dto.Mutation;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.TreeNodeInfo;
import com.google.collide.dto.WorkspaceTreeUpdate;
import com.google.collide.dto.server.DtoServerImpls.*;
import com.google.collide.json.server.JsonArrayListAdapter;
import com.google.collide.server.participants.Participants;
import com.google.collide.server.shared.BusModBase;
import com.google.collide.server.shared.util.Dto;
import com.google.collide.shared.util.PathUtils;
import com.google.collide.shared.util.PathUtils.PathVisitor;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import xapi.log.X_Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Backend service that manages the representation of "files and folders" in the workspace
 * directory.
 * <p>
 * This service is responsible for applying all file tree mutations to the files on disk.
 * <p>
 * It is also responsible for managing meta-data about files (like resource identifiers).
 */
public class FileTree extends BusModBase {

  /**
   * Local data model extension. Note that toJsonElement is NOT overridden since extra fields are
   * private to us, we don't want them serialized.
   */
  private static interface NodeInfoExt extends TreeNodeInfo {
    Path getPath();
  }

  private static class DirInfoExt extends DirInfoImpl implements NodeInfoExt {
    private final Path path;
    private final Map<String, NodeInfoExt> children = new HashMap<String, NodeInfoExt>();

    public DirInfoExt(Path path, long resourceId) {
      this.path = path;
      super.setNodeType(TreeNodeInfo.DIR_TYPE);
      super.setFileEditSessionKey(Long.toString(resourceId));
      if (path.toString().length() == 0) {
        // root
        super.setName("/");
      } else {
        super.setName(path.getFileName().toString());
      }
    }

    @Override
    public Path getPath() {
      return path;
    }

    public void addChild(DirInfoExt dir) {
      NodeInfoExt prior = children.put(dir.getPath().getFileName().toString(), dir);
      assert prior == null;
      super.addSubDirectories(dir);
      if (isPackage()) {
        dir.setIsPackage(true);
      }
    }

    public void addChild(FileInfoExt file) {
      NodeInfoExt prior = children.put(file.getPath().getFileName().toString(), file);
      assert prior == null;
      super.addFiles(file);
    }

    public NodeInfoExt getChild(String name) {
      return children.get(name);
    }

    public Iterable<NodeInfoExt> getChildren() {
      return children.values();
    }

    public NodeInfoExt removeChild(String name) {
      NodeInfoExt removed = children.remove(name);
      assert removed != null : "Cannot remove non-existent child " + name;
      if (removed.getNodeType() == TreeNodeInfo.FILE_TYPE) {
        JsonArrayListAdapter<FileInfo> list = (JsonArrayListAdapter<FileInfo>) super.getFiles();
        super.clearFiles();
        boolean didRemove = false;
        for (FileInfo item : list.asList()) {
          if (item == removed) {
            didRemove = true;
            break;
          }
          super.addFiles((FileInfoImpl) item);
        }
      } else {
        JsonArrayListAdapter<DirInfo> list =
            (JsonArrayListAdapter<DirInfo>) super.getSubDirectories();
        super.clearSubDirectories();
        boolean didRemove = false;
        for (DirInfo item : list.asList()) {
          if (item == removed) {
            didRemove = true;
            break;
          }
          super.addSubDirectories((DirInfoImpl) item);
        }
      }
      return removed;
    }
  }

  private static class FileInfoExt extends FileInfoImpl implements NodeInfoExt {
    private final Path path;

    public FileInfoExt(Path path, long resourceId, long fileSize) {
      this.path = path;
      super.setName(path.getFileName().toString());
      super.setNodeType(TreeNodeInfo.FILE_TYPE);
      super.setFileEditSessionKey(Long.toString(resourceId));
      super.setSize(Long.toString(fileSize));
    }

    @Override
    public Path getPath() {
      return path;
    }
  }

  /**
   * Receives and applies file tree mutations. Is responsible for subsequently broadcasting the
   * mutation to collaborators after successful application of the mutation.
   */
  class FileTreeMutationHandler implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      WorkspaceTreeUpdate update = WorkspaceTreeUpdateImpl.fromJsonString(Dto.get(message));
      synchronized (FileTree.this.lock) {
        try {
          for (Mutation mutation : update.getMutations().asIterable()) {
            final Path oldPath = resolvePathString(mutation.getOldPath());
            final Path newPath = resolvePathString(mutation.getNewPath());
            switch (mutation.getMutationType()) {
              case ADD:
                if (mutation.getNewNodeInfo().getNodeType() == TreeNodeInfo.DIR_TYPE) {
                  Files.createDirectory(newPath);
                } else {
                  assert mutation.getNewNodeInfo().getNodeType() == TreeNodeInfo.FILE_TYPE;
                  Files.createFile(newPath);
                }
                break;
              case COPY:
                System.out.println("copy: " + oldPath + " to: " + newPath);
                if (!Files.isDirectory(oldPath)) {
                  Files.copy(oldPath, newPath);
                  continue;
                }
                Files.walkFileTree(oldPath, new FileVisitor<Path>() {
                  @Override
                  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                      throws IOException {
                    Path target = newPath.resolve(oldPath.relativize(dir));
                    Files.copy(dir, target);
                    return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                      throws IOException {
                    Path target = newPath.resolve(oldPath.relativize(file));
                    Files.copy(file, target);
                    return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException exc)
                      throws IOException {
                    throw exc;
                  }

                  @Override
                  public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                      throws IOException {
                    if (exc != null) {
                      throw exc;
                    }
                    return FileVisitResult.CONTINUE;
                  }
                });
                break;
              case DELETE:
                if (!Files.isDirectory(oldPath)) {
                  Files.delete(oldPath);
                  continue;
                }
                Files.walkFileTree(oldPath, new FileVisitor<Path>() {
                  @Override
                  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                      throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException exc)
                      throws IOException {
                    throw exc;
                  }

                  @Override
                  public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                      throws IOException {
                    if (exc != null) {
                      throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                  }
                });

                break;
              case MOVE:
                expectMoves.add(new ExpectedMove(oldPath, newPath));
                Files.move(oldPath, newPath);
                break;
              default:
                throw new IllegalArgumentException(mutation.getMutationType().toString());
            }
          }

          EmptyMessageImpl response = EmptyMessageImpl.make();
          message.reply(Dto.wrap(response));
          // The file listener will broadcast the applied mutations to all clients.
        } catch (Exception exc) {
          exc.printStackTrace(System.out);
          ServerErrorImpl response = ServerErrorImpl.make();
          response.setFailureReason(FailureReason.SERVER_ERROR);
          StringWriter sw = new StringWriter();
          exc.printStackTrace(new PrintWriter(sw));
          response.setDetails(sw.toString());
          message.reply(Dto.wrap(response));
        }
      }
    }

    private Path resolvePathString(String pathString) {
      if (pathString == null) {
        return null;
      }
      return root.getPath().resolve(stripSlashes(pathString));
    }
  }

  /**
   * Replies to the requester with the File Tree rooted at the path requested by the requester.
   */
  class FileTreeGetter implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      GetDirectoryImpl request = GetDirectoryImpl.fromJsonString(Dto.get(message));
      final GetDirectoryResponseImpl response = GetDirectoryResponseImpl.make();
      synchronized (FileTree.this.lock) {
        response.setRootId(Long.toString(currentTreeVersion));
        PathUtils.walk(request.getPath(), "/", new PathVisitor() {
          @Override
          public void visit(String path, String name) {
            // Special case root.
            if ("/".equals(path)) {
              response.setBaseDirectory(root);
              response.setPath(path);
              return;
            }
            // Search for the next directory.
            DirInfo lastDir = response.getBaseDirectory();
            if (lastDir != null) {
              for (DirInfo dir : lastDir.getSubDirectories().asIterable()) {
                if (dir.getName().equals(name)) {
                  response.setBaseDirectory((DirInfoImpl) dir);
                  response.setPath(path);
                  return;
                }
              }
            }
            // Didn't find it.
            response.setBaseDirectory(null);
          }
        });
      }
      message.reply(Dto.wrap(response));
    }
  }

  /**
   * Takes in a list of resource IDs and returns a list of String paths for the resources as they
   * currently exist.
   */
  class PathResolver implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      JsonArray resourceIds = message.body().getJsonArray("resourceIds");
      JsonObject result = new JsonObject();
      JsonArray paths = new JsonArray();
      result.put("paths", paths);
      synchronized (FileTree.this.lock) {
        for (Object id : resourceIds) {
          assert id instanceof String;
          NodeInfoExt node = resourceIdToNode.get(id);
          if (node == null) {
            paths.addNull();
          } else {
            paths.add('/' + node.getPath().toString());
          }
        }
      }
      message.reply(result);
    }
  }

  /**
   * Takes in a list of paths and replies with a list of resource IDs. A resource ID is a stable
   * identifier for a resource that survives across renames/moves. This identifier is currently only
   * stable for the lifetime of this verticle.
   */
  class ResourceIdResolver implements Handler<Message<JsonObject>> {
    @Override
    public void handle(Message<JsonObject> message) {
      JsonArray paths = message.body().getJsonArray("paths");
      JsonObject result = new JsonObject();
      JsonArray resourceIds = new JsonArray();
      result.put("resourceIds", resourceIds);
      synchronized (FileTree.this.lock) {
        for (Object path : paths) {
          NodeInfoExt found = findResource(stripSlashes((String) path));
          if (found == null) {
            resourceIds.addNull();
          } else {
            resourceIds.add(found.getFileEditSessionKey());
          }
        }
      }
      message.reply(result);
    }

    private NodeInfoExt findResource(String path) {
      if (path.length() == 0) {
        return root;
      }
      DirInfoExt cur = root;
      while (true) {
        int pos = path.indexOf('/');
        if (pos < 0) {
          // Last item.
          return cur.getChild(path);
        } else {
          String component = path.substring(0, pos);
          NodeInfoExt found = cur.getChild(component);
          // Better be a directory or we can't search anymore.
          if (!(found instanceof DirInfoExt)) {
            return null;
          }
          cur = (DirInfoExt) found;
          path = path.substring(pos + 1);
        }
      }
    }
  }

  /**
   * Scans the file tree, or a subsection, adding new nodes to the tree. Also sets up watchers to
   * listen for file and directory changes.
   */
  private class TreeScanner {
    public final Stack<DirInfoExt> parents = new Stack<FileTree.DirInfoExt>();
    private final FileVisitor<Path> visitor = new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
        DirInfoExt dir = new DirInfoExt(path, resourceIdAllocator++);
        if (packages.contains(path)) {
          dir.setIsPackage(true);
        }
        if (blacklist.contains(dir.getName()))
          return FileVisitResult.SKIP_SUBTREE;
        if (parents.isEmpty()) {
          // System.out.println("scanning from: " + path.toAbsolutePath() + '/');
          root = dir;
        } else {
          parents.peek().addChild(dir);
        }
        resourceIdToNode.put(dir.getFileEditSessionKey(), dir);
        parents.push(dir);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        // System.out.println("add: /" + path);
        FileInfoExt file = new FileInfoExt(path, resourceIdAllocator++, attrs.size());
        parents.peek().addChild(file);
        resourceIdToNode.put(file.getFileEditSessionKey(), file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        System.out.println("visitFileFailed: " + file);
        throw exc;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          System.out.println("postVisitDirectory failed: " + dir);
          throw exc;
        }
        DirInfoExt dirInfo = parents.pop();
        dirInfo.setIsComplete(true);
        WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.OVERFLOW);
        if (!blacklist.contains(dirInfo.getName()))
          watchkeyToDir.put(key, dirInfo);
        return FileVisitResult.CONTINUE;
      }
    };

    /**
     * @param start the path to start scanning from
     * @param parent the parent node to scan under
     */
    public void walk(Path start, DirInfoExt parent) throws IOException {
      assert parents.isEmpty();
      parents.push(parent);
      Files.walkFileTree(start, visitor);
      parents.pop();
      assert parents.isEmpty();
    }

    /**
     * @param root the path to start scanning from
     */
    public void walkFromRoot(Path root) throws IOException {
      assert root != null;
      assert parents.isEmpty();
      Files.walkFileTree(root, visitor);
      assert parents.isEmpty();
    }
  }

  /** NOT IDEAL: a lock for communicating between the threads. */
  final Object lock = new Object();

  /** The root of the tree. */
  DirInfoExt root = null;

  /** The tree is versioned to reconcile racey client mutations. */
  long currentTreeVersion = 0;

  /** Simple in-memory allocator for resource Ids. */
  long resourceIdAllocator = 0;

  static class ExpectedMove {
    public ExpectedMove(Path oldPath, Path newPath) {
      this.oldPath = oldPath;
      this.newPath = newPath;
    }

    final Path oldPath;
    final Path newPath;
    NodeInfoExt oldNode = null;
    NodeInfoExt newNode = null;
  }

  final List<ExpectedMove> expectMoves = new ArrayList<ExpectedMove>();


  /** Map resourceId to node. */
  HashMap<String, NodeInfoExt> resourceIdToNode = new HashMap<String, NodeInfoExt>();

  /** Scans to find new files. */
  final TreeScanner treeScanner = new TreeScanner();

  /** The watch service for listening for tree changes. */
  WatchService watchService;

  /** A map of watch keys to directories. */
  final Map<WatchKey, DirInfoExt> watchkeyToDir = new HashMap<WatchKey, DirInfoExt>();

  Thread watcherThread = null;

  HashSet<String> blacklist = new HashSet<>();

  HashSet<Path> packages = new HashSet<>();

  @Override
  public void start() {
    super.start();

    blacklist.add("classes");
    blacklist.add("eclipse");
    blacklist.add(".git");
    blacklist.add(".idea");

    String webRoot = getOptionalStringConfig("webRoot", "");
    if (webRoot.length() > 0) {
      JsonArray packages = getOptionalArrayConfig("packages", new JsonArray());
      for (Object pkg : packages) {
        this.packages.add(Paths.get(String.valueOf(pkg)));
      }
    } else {
      X_Log.info(getClass(), "No webRoot property specified for FileTree");
    }

    try {
      watchService = FileSystems.getDefault().newWatchService();
      Path rootPath = new File("").toPath();
      treeScanner.walkFromRoot(rootPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    vertx.eventBus().consumer("tree.mutate", new FileTreeMutationHandler());
    vertx.eventBus().consumer("tree.get", new FileTreeGetter());
    vertx.eventBus().consumer("tree.getCurrentPaths", new PathResolver());
    vertx.eventBus().consumer("tree.getResourceIds", new ResourceIdResolver());


    /*
     * This is not the one true vertx way... but it's easier for now! The watcher thread and the
     * vertx thread really do need to sync up regarding tree state.
     */
    watcherThread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          try {
            processAllWatchEvents(watchService.take());
          } catch (InterruptedException e) {
            // Just exit the thread.
            return;
          } catch (Exception e) {
            e.printStackTrace(System.out);
          }
        }
      }
    });
    watcherThread.setDaemon(true);
    watcherThread.start();
  }

  @Override
  public void stop() throws Exception {
    synchronized (this.lock) {
      watcherThread.interrupt();
    }
    watcherThread.join();
    super.stop();
  }

  void processAllWatchEvents(WatchKey key) {
    List<NodeInfoExt> adds = new ArrayList<NodeInfoExt>();
    List<NodeInfoExt> removes = new ArrayList<NodeInfoExt>();
    List<NodeInfoExt> modifies = new ArrayList<NodeInfoExt>();
    HashMap<Path, ExpectedMove> movesByOld = new HashMap<Path, ExpectedMove>();
    HashMap<Path, ExpectedMove> movesByNew = new HashMap<Path, ExpectedMove>();
    List<ExpectedMove> completedMoves = new ArrayList<ExpectedMove>();
    boolean treeDirty = false;
    long treeVersion;
    // System.out.println("-----");
    synchronized (this.lock) {
      treeVersion = this.currentTreeVersion;
      // Grab all the outstanding moves.
      for (ExpectedMove move : this.expectMoves) {
        movesByOld.put(move.oldPath, move);
        movesByNew.put(move.newPath, move);
      }
      this.expectMoves.clear();
      do {
        DirInfoExt parent = watchkeyToDir.get(key);

        // process events
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind().type() == Path.class) {
            Path path = (Path) event.context();
            Path resolved = parent.getPath().resolve(path);
            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
              treeDirty = true;
              try {
                treeScanner.walk(resolved, parent);
                NodeInfoExt added = parent.getChild(resolved.getFileName().toString());
                ExpectedMove move = movesByNew.get(resolved);
                if (move != null) {
                  move.newNode = added;
                } else {
                  adds.add(added);
                }
              } catch (IOException e) {
                // Just ignore it for now.
                continue;
              }
            } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
              NodeInfoExt modified = parent.getChild(resolved.getFileName().toString());
              modifies.add(modified);
            } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
              treeDirty = true;
              NodeInfoExt removed = parent.removeChild(resolved.getFileName().toString());
              ExpectedMove move = movesByOld.get(resolved);
              if (move != null) {
                move.oldNode = removed;
              } else {
                unmapResourceIds(removed);
                removes.add(removed);
              }
            } else {
              assert false : "Unknown event type: " + event.kind().name();
            }
          } else {
            assert event.kind() == StandardWatchEventKinds.OVERFLOW;
            System.out.print(event.kind().name() + ": ");
            System.out.println(event.count());
            // TODO: reload the entire tree???
          }
        }

        // reset the key
        boolean valid = key.reset();
        if (!valid) {
          // object no longer registered
          watchkeyToDir.remove(key);
        }

        // Process all available events without blocking to minimize jitter.
        key = watchService.poll();
      } while (key != null);

      if (treeDirty) {
        treeVersion = currentTreeVersion++;
      }

      // Post-process moves.
      for (ExpectedMove move : movesByOld.values()) {
        if (move.oldNode == null) {
          if (move.newNode == null) {
            // Got nothing, put it back on the queue.
            this.expectMoves.add(move);
          } else {
            // Convert to a create.
            adds.add(move.newNode);
          }
        } else {
          if (move.newNode == null) {
            // Convert to a delete.
            unmapResourceIds(move.oldNode);
            removes.add(move.oldNode);
          } else {
            // Completed the move.
            completedMoves.add(move);
            // Update the edit session key to retain identity.
            TreeNodeInfoImpl newNode = (TreeNodeInfoImpl) move.newNode;
            newNode.setFileEditSessionKey(move.oldNode.getFileEditSessionKey());
            resourceIdToNode.put(newNode.getFileEditSessionKey(), move.newNode);
          }
        }
      }
      // TODO: post-process deletes so that child deletes are subsumed by parent deletes.
    }

    // Notify the edit session verticle of the changes.
    JsonObject message = new JsonObject();
    JsonArray messageDelete = new JsonArray();
    JsonArray messageModify = new JsonArray();
    message.put("delete", messageDelete);
    message.put("modify", messageModify);

    // Broadcast a tree mutation to all clients.
    WorkspaceTreeUpdateBroadcastImpl broadcast = WorkspaceTreeUpdateBroadcastImpl.make();

    for (NodeInfoExt node : adds) {
      System.out.println("add: " + pathString(node));
      // Edit session doesn't care.
      // Broadcast to clients.
      MutationImpl mutation =
          MutationImpl.make().setMutationType(Mutation.Type.ADD).setNewPath(pathString(node));
      /*
       * Do not strip the node; in the case of a newly scanned directory (e.g. recursive copy), its
       * children to not get their own mutations, it's just a single tree.
       */
      mutation.setNewNodeInfo((TreeNodeInfoImpl) node);
      broadcast.getMutations().add(mutation);
    }
    for (NodeInfoExt node : removes) {
      System.out.println("del: " + pathString(node));
      // Edit session wants deletes.
      messageDelete.add(node.getFileEditSessionKey());
      // Broadcast to clients.
      MutationImpl mutation =
          MutationImpl.make().setMutationType(Mutation.Type.DELETE).setOldPath(pathString(node));
      broadcast.getMutations().add(mutation);
    }
    for (ExpectedMove move : completedMoves) {
      System.out.println("mov: " + pathString(move.oldNode) + " to: " + pathString(move.newNode));
      // Edit session doesn't care.
      // Broadcast to clients.
      MutationImpl mutation = MutationImpl.make()
          .setMutationType(Mutation.Type.MOVE).setNewPath(pathString(move.newNode))
          .setOldPath(pathString(move.oldNode));
      // Strip the node; the client should already have the children.
      mutation.setNewNodeInfo(stripChildren(move.newNode));
      broadcast.getMutations().add(mutation);
    }
    for (NodeInfoExt node : modifies) {
      if (node == null) {
        continue;
      }
      System.out.println("mod: " + pathString(node));
      // Edit session wants modifies.
      messageModify.add(node.getFileEditSessionKey());
      // No broadcast, edit session will handle.
    }
    vertx.eventBus().send("documents.fileSystemEvents", message);
    if (treeDirty) {
      broadcast.setNewTreeVersion(Long.toString(treeVersion));
      vertx.eventBus().send("participants.broadcast", new JsonObject().put(
          Participants.PAYLOAD_TAG, broadcast.toJson()));
    }
  }

  /**
   * Strips out directory children for broadcast.
   */
  private TreeNodeInfoImpl stripChildren(NodeInfoExt newNode) {
    if (newNode.getNodeType() == TreeNodeInfo.FILE_TYPE) {
      return (TreeNodeInfoImpl) newNode;
    }
    DirInfoImpl dir = (DirInfoImpl) newNode;
    if ((dir.hasFiles() && dir.getFiles().size() > 0) || dir.hasSubDirectories()
        && dir.getSubDirectories().size() > 0) {
      // Make a copy; modifying the node would change the real tree.
      dir = DirInfoImpl.fromJsonElement(dir.toJsonElement());
      dir.clearFiles();
      dir.clearSubDirectories();
      dir.setIsComplete(false);
    }
    return dir;
  }

  private String pathString(NodeInfoExt node) {
    if (node.getNodeType() == TreeNodeInfo.FILE_TYPE) {
      return '/' + node.getPath().toString();
    } else {
      return '/' + node.getPath().toString() + '/';
    }
  }

  /**
   * @param removed
   */
  private void unmapResourceIds(NodeInfoExt removed) {
    NodeInfoExt didRemove = resourceIdToNode.remove(removed.getFileEditSessionKey());
    assert removed == didRemove;
    if (removed instanceof DirInfoExt) {
      DirInfoExt dir = (DirInfoExt) removed;
      for (NodeInfoExt child : dir.getChildren()) {
        unmapResourceIds(child);
      }
    }

  }

  /**
   * This verticle needs to take "workspace rooted paths", which begin with a leading '/', and make
   * them relative to the base directory for the associated classloader for this verticle. That is,
   * assume that all paths are relative to what our local view of '.' is on the file system. We
   * simply strip the leading slash. Also strip a trailing slash.
   */
  private String stripSlashes(String relative) {
    if (relative == null) {
      return null;
    } else if (relative.length() == 0) {
      return relative;
    } else {
      relative = relative.charAt(0) == '/' ? relative.substring(1) : relative;
      int last = relative.length() - 1;
      if (last >= 0) {
        relative = relative.charAt(last) == '/' ? relative.substring(0, last) : relative;
      }
      return relative;
    }
  }
}
