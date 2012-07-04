load("vertx.js")

// These tests will run in parallel. It is kind of a mess debugging the output.
var testSuites = ["participant_list_test.js", "edit_session_test.js", "file_tree_test.js"];
var testIndex = 0;
var anyFailed = false;

function nextTestSuite() {
  if (testIndex >= testSuites.length) {
    // TODO: Sync up and pull in API to exit a verticle.
    if (anyFailed) {
      stdout.println("TEST FAILED!");
    } else {
      stdout.println("All tests passed :).");
    }
    // TODO: exit code would be nice...
    vertx.exit();
    return;
  }
  vertx.deployVerticle(testSuites[testIndex ++]);
}

vertx.eventBus.registerHandler("async_test.done", function(message){
  anyFailed = anyFailed || message.failed;
  nextTestSuite();
});

nextTestSuite();