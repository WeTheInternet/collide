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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FileTreeExperiment {

  public static void main(String[] args) throws IOException, InterruptedException {
    final Map<WatchKey, Path> map = new HashMap<WatchKey, Path>();
    final Path rootPath = new File("").toPath();
    final WatchService watcher = FileSystems.getDefault().newWatchService();
    final Stack<Path> parents = new Stack<Path>();


    final FileVisitor<Path> visitor = new FileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs)
          throws IOException {
        if (parents.isEmpty()) {
          // root
          System.out.println("scanning from: " + path.toAbsolutePath() + '/');
        } else {
          System.out.println("add: /" + path + '/');
        }
        parents.push(path);
        WatchKey key = path.register(
            watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW);
        map.put(key, path);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        System.out.println("add: /" + path);
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
        parents.pop();
        return FileVisitResult.CONTINUE;
      }
    };
    Files.walkFileTree(rootPath, visitor);
    
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(1000);
          Files.createDirectory(rootPath.resolve("tmp"));
          Files.createDirectory(rootPath.resolve("tmp/dir"));
          Files.createFile(rootPath.resolve("tmp/file"));
          Files.createFile(rootPath.resolve("tmp/dir/file2"));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();

    for (;;) {
      // retrieve key
      WatchKey key = watcher.take();
      Path parent = map.get(key);
      System.out.println("-----");
      // process events
      for (WatchEvent<?> event : key.pollEvents()) {
        if (event.kind().type() == Path.class) {
          Path path = (Path) event.context();
          Path resolved = parent.resolve(path);
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            System.out.println("cre: /" + resolved);
            parents.push(parent);
            Files.walkFileTree(resolved, visitor);
            parents.pop();
          } else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            System.out.println("mod: /" + resolved);
          } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
            System.out.println("del: /" + resolved);
          } else {
            assert false : "Unknown event type: " + event.kind().name();
          }
        } else {
          assert event.kind() == StandardWatchEventKinds.OVERFLOW;
          System.out.print(event.kind().name() + ": ");
          System.out.println(event.count());
        }
      }

      // reset the key
      boolean valid = key.reset();
      if (!valid) {
        // object no longer registered
        map.remove(key);
      }
    }
  }
}
