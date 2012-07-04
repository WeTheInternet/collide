load("vertx.js")
load("common/async_test.js")

var eb = vertx.eventBus;

var tests = {

  writeSomeTests: function(test) {
    // test.startAsync();
    // TODO:  Write some tests.
    // test.endAsync();
  }

};

new AsyncTestRunner("com.google.collide.server.documents.EditSessions", {}).run(tests);
