load("vertx.js")
load("common/async_test.js")

var eb = vertx.eventBus;

// Begin Test Code.
var config = {
  password: "s3cret",
  session_timeout: 1000,
  keep_alive_timeout: 200
};

var tests = {

  testLoginFail: function(test) {
    // Supply an incorrect password.
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "not-the-s3cret"}, function(reply) {
      assert (reply.status == "denied", "Status was " + reply.status);
      assert (!reply.sessionID, "We should not have been allocated a session ID!");

      eb.send("participants.getParticipants", {}, function(message) {
        var message = JSON.parse(message.dto);
        assert (message.participants.length == 0,
               "We had " + message.participants.length + " collaborators instead of 0!");
        test.endAsync();
      });
    });

    // Supply no password when one was needed.
    test.startAsync();
    eb.send("participants.login", {username: "jaime"}, function(reply) {
      assert (reply.status == "denied", "Status was " + reply.status);
      assert (reply.reason == "needs-pass", "Reason was " + reply.reason);

      test.startAsync();
      eb.send("participants.getParticipants", {}, function(message) {
        var message = JSON.parse(message.dto);
        assert (message.participants.length == 0,
                "We had " + message.participants.length + " collaborators instead of 0!");
        test.endAsync();
      });

      test.endAsync();
    });
  },

  testLogin: function(test) {
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!");
      assert (!reply.activeClient, "We should not be provisioning a tracked active client tab on login!");
      test.endAsync();
    });
  },

  testAuthorise: function(test) {
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!");

      // This one should NOT provision a new active client.
      test.startAsync();
      eb.send("participants.authorise", {sessionID: reply.sessionID}, function(message) {
        assert (message.status == "ok", "Status was " + message.status);
        assert (message.username == "jaime", "Username was " + message.username);
        assert (!message.activeClient, "Active client provisioned when we didn't ask it to");

        // Double check that the server thinks we have no tabs open.
        test.startAsync();
        eb.send("participants.getParticipants", {}, function(message) {
          var message = JSON.parse(message.dto);
          assert (message.participants.length == 0,
                  "We had " + message.participants.length + " collaborators instead of 0!");

          // This one SHOULD provision a new active client.
          test.startAsync();
          eb.send("participants.authorise", {sessionID: reply.sessionID, createClient: true}, function(message) {
            assert (message.status == "ok", "Status was " + message.status);
            assert (message.username == "jaime", "Username was " + message.username);
            assert (message.activeClient, "Active Client was not provisioned!");

            // Double check that the server thinks we have 1 tab open.
            test.startAsync();
            eb.send("participants.getParticipants", {}, function(message) {
              var message = JSON.parse(message.dto);
              assert (message.participants.length == 1,
                      "We had " + message.participants.length + " collaborators instead of 1!");
              test.endAsync();
            });

            test.endAsync();
          });

          test.endAsync();
        });

        test.endAsync();
      });

      test.endAsync();
    });
  },

  testLogout: function(test) {
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!")

      eb.send("participants.logout", {sessionID: reply.sessionID}, function() {
        eb.send("participants.getParticipants", {}, function(message) {
          var message = JSON.parse(message.dto);
          assert (message.participants.length == 0, "Collab list is not empty!");
          test.endAsync();
        });
      });
      });
  },

  testBroadcast: function(test) {
    var broadcastMessage = {msg: "ping"};

    // START CLIENT "A" LOGS IN.
    test.startAsync();
    eb.send("participants.login", {username: "A", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!")

      // START "A" OPENS A TAB
      test.startAsync();
      eb.send("participants.authorise", {sessionID: reply.sessionID, createClient: true}, function(message) {
        assert (message.status == "ok", "Status was " + message.status);
        assert (message.username == "A", "Username was " + message.username);
        assert (message.activeClient, "Active Client was not provisioned!");

        // START EXPECT BROADCAST.
        test.startAsync();
        eb.registerHandler("client." + message.activeClient, function(message, replier) {
          var payload = JSON.parse(message.dto);

          // Receipt of the broadcast.
          assert (payload.msg == broadcastMessage.msg,
                  "Received unexpected broadcast payload");
          // END EXPECT BROADCAST.
          test.endAsync();
        });

        // START CLIENT "B" LOGS IN.
        test.startAsync();
        eb.send("participants.login", {username: "B", password: "s3cret"}, function(breply) {
          assert (breply.status == "ok", "Status was " + breply.status);
          assert (breply.sessionID, "We did NOT get a session ID!")

          // START "B" OPENS A TAB.
          test.startAsync();
          eb.send("participants.authorise", {sessionID: breply.sessionID, createClient: true}, function(message) {
             assert (message.status == "ok", "Status was " + message.status);
             assert (message.username == "B", "Username was " + message.username);
             assert (message.activeClient, "Active Client was not provisioned!");

             var bAddress = "client." + message.activeClient;

             // START "B" EXPECTS MESSAGE.
             test.startAsync();
             eb.registerHandler(bAddress, function(message, replier) {
               var payload = JSON.parse(message.dto);

               // Receipt of the broadcast.
               assert (payload.msg == broadcastMessage.msg,
                       "Received unexpected broadcast payload");
               // END "B" EXPECTS MESSAGE.
               test.endAsync();
             });

             // SEND BROADCAST.
             eb.send("participants.broadcast", {payload: JSON.stringify(broadcastMessage)});

             // END "B" OPENS A TAB.
             test.endAsync();
           });

           // END CLIENT "B" LOGS IN.
           test.endAsync();
        });

        // END "A" OPENS TAB.
        test.endAsync();
      });

      // END "A" LOGS IN.
      test.endAsync();
    });
  },

testSendTo: function(test) {
    var messageToA = {msg: "pingA"};
    var messageToB = {msg: "pingB"};

    // START CLIENT "A" LOGS IN.
    test.startAsync();
    eb.send("participants.login", {username: "A", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!")

      // START "A" OPENS A TAB
      test.startAsync();
      eb.send("participants.authorise", {sessionID: reply.sessionID, createClient: true}, function(message) {
        assert (message.status == "ok", "Status was " + message.status);
        assert (message.username == "A", "Username was " + message.username);
        assert (message.activeClient, "Active Client was not provisioned!");

        // START EXPECT MESSAGE.
        test.startAsync();
        eb.registerHandler("client." + message.activeClient, function(message, replier) {
          // Receipt of the message
          assert (message.payload == messageToA.payload, "Received unexpected broadcast payload " + message.payload);
          // END EXPECT BROADCAST.
          test.endAsync();
        });

        // START CLIENT "B" LOGS IN.
        test.startAsync();
        eb.send("participants.login", {username: "B", password: "s3cret"}, function(breply) {
          assert (breply.status == "ok", "Status was " + breply.status);
          assert (breply.sessionID, "We did NOT get a session ID!")

          // START "B" OPENS A TAB.
          test.startAsync();
          eb.send("participants.authorise", {sessionID: breply.sessionID, createClient: true}, function(message) {
             assert (message.status == "ok", "Status was " + message.status);
             assert (message.username == "B", "Username was " + message.username);
             assert (message.activeClient, "Active Client was not provisioned!");

             var bAddress = "client." + message.activeClient;

             // START "B" EXPECTS MESSAGE.
             test.startAsync();
             eb.registerHandler(bAddress, function(message, replier) {
               // Receipt of the broadcast.
               assert (message.payload == messageToB.payload,
                       "Received unexpected broadcast payload");
               // END "B" EXPECTS MESSAGE.
               test.endAsync();
             });

             // SEND MESSAGES.
             eb.send("participants.sendTo", {sendToUsersTabs: "A", payload: JSON.stringify(messageToA)});
             eb.send("participants.sendTo", {sendToClient: message.activeClient, payload: JSON.stringify(messageToA)});

             // END "B" OPENS A TAB.
             test.endAsync();
           });

           // END CLIENT "B" LOGS IN.
           test.endAsync();
        });

        // END "A" OPENS TAB.
        test.endAsync();
      });

      // END "A" LOGS IN.
      test.endAsync();
    });
  },

  testKeepAlives: function(test) {
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!")

      test.startAsync();
      eb.send("participants.authorise", {sessionID: reply.sessionID, createClient: true}, function(message) {
        assert (message.status == "ok", "Status was " + message.status);
        assert (message.username == "jaime", "Username was " + message.username);
        var activeClient = message.activeClient;
        assert (activeClient, "Active Client was not provisioned!");

        var checkWeAreInList = function() {
          test.startAsync();
          eb.send("participants.getParticipants", {}, function(message) {
            var message = JSON.parse(message.dto);
            var participants = message.participants;
            assert (participants.length == 1,
                    "We had " + participants.length + " collaborators instead of 1!");
            assert (participants[0].userDetails.displayEmail == "jaime",
                    "Incorrect username for added collaborator " + participants[0].userDetails.displayEmail);
             assert (participants[0].participant.id == activeClient,
                  "Active Client " + participants[0].participant.id + " did not match expected " + activeClient);
            test.endAsync();
          });
        }

        // Check right now to make sure the list has us in it.
        checkWeAreInList();

        // Keep the session alive a couple times. This should exceed the timeout.
        var timesToExtend = 3;
        test.startAsync();
        vertx.setTimer(150, function() {
          eb.send("participants.keepAlive", {activeClient: activeClient});
          checkWeAreInList();
          if (timesToExtend > 0) {
            test.startAsync();
            vertx.setTimer(150, this);
            timesToExtend--;
          }
          test.endAsync();
        });

        // Check a little later to make sure it gets GC'ed
        test.startAsync();
        vertx.setTimer(1000, function() {
          eb.send("participants.getParticipants", {}, function(message) {
            var message = JSON.parse(message.dto);
            assert (message.participants.length == 0,
                    "We had " + message.participants.length + " collaborators instead of 0!");
            test.endAsync();
          });
        });

        test.endAsync();
      });
      test.endAsync();
    });
  },

  testLoginSessionTimeout: function(test) {
    test.startAsync();
    eb.send("participants.login", {username: "jaime", password: "s3cret"}, function(reply) {
      assert (reply.status == "ok", "Status was " + reply.status);
      assert (reply.sessionID, "We did NOT get a session ID!")

      test.startAsync();
      eb.send("participants.authorise", {sessionID: reply.sessionID, createClient: true}, function(message) {
        assert (message.status == "ok", "Status was " + message.status);
        assert (message.username == "jaime", "Username was " + message.username);
        var activeClient = message.activeClient;
        assert (activeClient, "Active Client was not provisioned!");

        test.startAsync();
        eb.send("participants.getParticipants", {}, function(message) {
          var message = JSON.parse(message.dto);
          var participants = message.participants;
          assert (participants.length == 1,
                  "We had " + participants.length + " collaborators instead of 1!");
          assert (participants[0].userDetails.displayEmail == "jaime",
                  "Incorrect username for added collaborator " + participants[0].userDetails.displayEmail);
          assert (participants[0].participant.id == activeClient,
                  "Active Client " + participants[0].participant.id + " did not match expected " + activeClient);
          test.endAsync();
        });

        // Check a little later to make sure it gets GC'ed
        test.startAsync();
        vertx.setTimer(1200, function() {
          test.startAsync();
          eb.send("participants.getParticipants", {}, function(message) {
            var message = JSON.parse(message.dto);
            assert (message.participants.length == 0,
                    "We had " + message.participants.length + " collaborators instead of 0!");
            test.endAsync();
          });

          test.startAsync();
          eb.send("participants.authorise", {sessionID: reply.sessionID}, function(message) {
            assert (message.status == "denied", "Status was " + message.status);
            test.endAsync();
          });

          test.endAsync();
        });

        test.endAsync();
      });

      test.endAsync();
    });
  }

};

new AsyncTestRunner("com.google.collide.server.participants.Participants", config).run(tests);
