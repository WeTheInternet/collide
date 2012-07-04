function assert(cond, message) {
  if (!cond) {
    stdout.println("Assert FAILED :( " + (message || ""));
    throw message || "";
  }
}

function AsyncTest(testName, testFunction, runner) {
  this.testName = testName;
  this.testFunction = testFunction;
  this.runner = runner;
  this.asyncActions = 0;
  this.runningFunction = false;
  this.timerId = -1;

  var me = this;
  this.run = function(deployId) {
    stdout.println("Running " + testName + " ... ");
    me.deployId = deployId;
    try {
      me.runningFunction = true;
      testFunction(me);
    } catch (e) {
      stdout.println("Exception: " + e);
      me.failed();
      return;
    } finally {
      me.runningFunction = false;
    }
    if (me.asyncActions > 0) {
      me.timerId = vertx.setTimer(2000, me.failed);
    } else {
      me.success();
    }
  };

  this.startAsync = function() {
    me.asyncActions++;
  };

  this.endAsync = function() {
    me.asyncActions--;
    if (!me.runningFunction && me.asyncActions <= 0) {
      me.success();
    }
  };

  this.failed = function() {
    me.runner.failed = true;
    vertx.undeployVerticle(me.deployId, function() {
      stdout.println("FAILED: " + testName);
      me.runner._nextTest();
    });
  };

  this.success = function() {
    if (me.timerId >= 0) {
      vertx.cancelTimer(me.timerId);
      me.timerId = -1;
    }
    vertx.undeployVerticle(me.deployId, function() {
      stdout.println("PASSED: " + testName);
      me.runner._nextTest();
    });
  };
}

function AsyncTestRunner(verticleName, config) {
  this.verticleName = verticleName;
  this.tests = [];

  var me = this;
  this.run = function(tests) {
    stdout.println("Starting Tests '" + verticleName + "'");

    // Collect all the tests.
    for (testName in tests) {
      me.tests.push({name: testName, test: tests[testName]});
    }
    me._nextTest();
  };

  // Run each test method with it's own verticle so they are hermetic.
  this._nextTest = function() {
    if(me.tests.length == 0) {
      if (me.failed) {
        stdout.println("\nFAILED: '" + verticleName + "'");
      } else {
        stdout.println("\nPASSED: '" + verticleName + "'");
      }
      vertx.eventBus.send("async_test.done", {testName: verticleName, failed: me.failed});
      return;
    }

    var test = me.tests[0];
    me.tests = me.tests.slice(1);

    var asyncTest = new AsyncTest(test.name, test.test, me);
    var deployId = vertx.deployVerticle(verticleName, config, 1, function() {
      asyncTest.run(deployId);
    });
  }
}
