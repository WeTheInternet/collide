load("vertx.js")
load("common/async_test.js")

var eb = vertx.eventBus;

var tests = {

    // TODO: rethink error requests?

    testGet : function(test) {
      test.startAsync();
      eb.send("tree.get", {dto: '{path: "/"}'}, function(reply) {
        reply = JSON.parse(reply.dto);
        // stdout.println(JSON.stringify(reply, null, '\t'));
        assert(reply.path == "/", "reply.path was " + reply.path);
        var dir = reply.baseDirectory;
        assert(dir != null, "dir was null");
        assert(dir.name == "/", "dir.name was " + dir.name);
        assert(dir.files.length >= 5, "dir.files was " + JSON.stringify(dir.files));
        assert(dir.subDirectories.length >= 1, "dir.subDirectories was " + JSON.stringify(dir.subDirectories));
        test.endAsync();
      });
      test.startAsync();
      eb.send("tree.get", {dto:'{path: "/common"}'}, function(reply) {
        reply = JSON.parse(reply.dto);
        // stdout.println(JSON.stringify(reply, null, '\t'));
        assert(reply.path == "/common", "reply.path was " + reply.path)
        var dir = reply.baseDirectory;
        assert(dir != null, "dir was null");
        assert(dir.name == "common", "dir.name was " + dir.name);
        assert(dir.files.length >= 1, "dir.files was " + JSON.stringify(dir.files));
        test.endAsync();
      });
      test.startAsync();
      eb.send("tree.get", {dto: '{path: "/foo/bar/baz"}'}, function(reply) {
        // TODO: should be error?
        reply = JSON.parse(reply.dto);
        // stdout.println(JSON.stringify(reply, null, '\t'));
        assert(reply.path == "/", "reply.path was " + reply.path)
        assert(reply.baseDirectory == null, "reply.baseDirectory was " + reply.baseDirectory)
        test.endAsync();
      });
    },
    testGetCurrentPaths : function(test) {
      test.startAsync();
      eb.send("tree.get", {dto: '{path: "/"}'}, function(reply) {
        // Get the initial tree.
        reply = JSON.parse(reply.dto);
        // stdout.println(JSON.stringify(reply, null, '\t'));
        assert(reply.path == "/", "reply.path was " + reply.path);
        var dir = reply.baseDirectory;
        assert(dir.files.length >= 5, "dir.files was " + JSON.stringify(dir.files));
        assert(dir.subDirectories.length >= 1, "dir.subDirectories was " + JSON.stringify(dir.subDirectories));

        // Now pick some items to try.
        var items = [
            dir,
            dir.files[0],
            dir.subDirectories[0],
            dir.subDirectories[0].files[0]
        ];
        var expected = [
            '/',
            '/' + items[1].name,
            '/' + items[2].name,
            '/' + items[2].name + '/' + items[3].name,
        ];

        var resourceIds = [];
        for ( var i = 0; i < items.length; ++i) {
          resourceIds.push(items[i].fileEditSessionKey);
        }
        resourceIds.push("invalid");
        expected.push(null);
        var request = {
          resourceIds : resourceIds
        };
        eb.send("tree.getCurrentPaths", request, function(reply) {
          // stdout.println(JSON.stringify(reply, null, '\t'));
          assert(JSON.stringify(expected) == JSON.stringify(reply.paths),
            "Expected: " + JSON.stringify(expected, null, '\t') +
            "\nActual: " + JSON.stringify(reply.paths, null, '\t'));
          test.endAsync();
        });
      });
    },
    testGetResourceIds : function(test) {
      test.startAsync();
      // Now pick some items to try.
      var paths = [
          "",
          "/",
          "common",
          "/common/",
          "common/async_test.js",
          "/common/async_test.js",
          "bogus"
      ];
      var expected = [
          true,
          true,
          true,
          true,
          true,
          true,
          false
      ];
      var request = {
          paths: paths
      };
      eb.send("tree.getResourceIds", request, function(reply) {
        // stdout.println(JSON.stringify(reply, null, '\t'));
        for (var i in reply.paths) {
          assert(reply.paths[i] == expected,
            "Expected: " + (expected ? "non-null" : "null") +
            "\nActual: " + reply.paths[i]);
        }
        test.endAsync();
      });
    },
    testMutate : function(test) {
      test.startAsync();
      var update = {
        mutations: [
          { mutationType: "ADD", newNodeInfo: { nodeType: 0}, newPath: "/foo/" },
          { mutationType: "ADD", newNodeInfo: { nodeType: 0}, newPath: "/foo/dir/" },
          { mutationType: "ADD", newNodeInfo: { nodeType: 1}, newPath: "/foo/1" },
          { mutationType: "ADD", newNodeInfo: { nodeType: 1}, newPath: "/foo/dir/2" },
        ],
      };
      eb.send("tree.mutate", {dto: JSON.stringify(update)});
      eb.registerHandler("participants.broadcast", function(message) {
        eb.unregisterHandler("participants.broadcast", arguments.callee);
        message = JSON.parse(message.payload);
        // stdout.println(JSON.stringify(message, null, '\t'));
        // TODO: assertions!

        update = {
          mutations: [
            { mutationType: "COPY", oldPath: "/foo/1", newPath: "/foo/3" },
            { mutationType: "COPY", oldPath: "/foo/", newPath: "/bar/"  },
          ],
        };
        eb.send("tree.mutate", {dto: JSON.stringify(update)});
        eb.registerHandler("participants.broadcast", function(message) {
          eb.unregisterHandler("participants.broadcast", arguments.callee);
          message = JSON.parse(message.payload);
          // stdout.println(JSON.stringify(message, null, '\t'));
          // TODO: assertions!

          update = {
            mutations: [
              { mutationType: "MOVE", oldPath: "/foo/3", newPath: "/foo/4" },
              { mutationType: "MOVE", oldPath: "/bar/", newPath: "/baz/"  },
            ],
          };
          eb.send("tree.mutate", {dto: JSON.stringify(update)});
          eb.registerHandler("participants.broadcast", function(message) {
            eb.unregisterHandler("participants.broadcast", arguments.callee);
            message = JSON.parse(message.payload);
            // stdout.println(JSON.stringify(message, null, '\t'));
            // TODO: assertions!
            assert(message.mutations.length == 2, "fail");
            for (var i in message.mutations) {
              var mutation = message.mutations[i];
              assert(mutation.mutationType == "MOVE", "fail");
              if (mutation.oldPath == "/bar/") {
                assert(mutation.newPath == "/baz/", "fail");
                assert(!mutation.newNodeInfo.isComplete, "fail")
              } else {
                assert(mutation.oldPath == "/foo/3", "fail");
                assert(mutation.newPath == "/foo/4", "fail");
              }
            }

            update = {
              mutations: [
                { mutationType: "DELETE", oldPath: "/foo/1" },
                { mutationType: "DELETE", oldPath: "/foo/dir/" },
                { mutationType: "DELETE", oldPath: "/foo/" },
                { mutationType: "DELETE", oldPath: "/baz/" },
              ],
            };
            eb.send("tree.mutate", {dto: JSON.stringify(update)});
            eb.registerHandler("participants.broadcast", function(message) {
              eb.unregisterHandler("participants.broadcast", arguments.callee);
              message = JSON.parse(message.payload);
              // stdout.println(JSON.stringify(message, null, '\t'));
              // TODO: assertions!

              test.endAsync();
            });
          });
        });
      });
    },
};

new AsyncTestRunner("com.google.collide.server.filetree.FileTree", {}).run(tests);
