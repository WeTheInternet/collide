var Login =  window.Login || {};

(function () {

// Connect the event bus and display the login prompt.
var eb = new vertx.EventBus(window.location.protocol + '//' + window.location.hostname + ':' + window.location.port + '/eventbus');
eb.onopen = function() {
  transition("connecting", "username-dialog");
};

var username;
var password;
var showing = "connecting";

Login.doLoginWithoutPassword = function() {
  username = document.getElementById("username").value;
  login(username, null);
}

Login.doLoginWithPassword = function() {
  if (!username) {
    transition(showing, "username-dialog");
    return;
  }
  password = document.getElementById("password").value;
  login(username, password);
}

function login(username, password) {
  var credentials = password ?  {username: username, password: password} : {username: username};
  eb.send("participants.login", credentials, function(reply) {
    if (reply.status == "ok" && reply.sessionID) {
      // Hurray. Now install the auth cookie.
      installAuthCookie(reply.sessionID);
    } else {
      if (reply.reason == "needs-pass") {
        transition(showing, "password-dialog");
      } else {
        transition(showing, "fail-dialog");
      }
    }
  });
}

function installAuthCookie(sessionId) {
  var xhr = new XMLHttpRequest();
  xhr.open("POST", "/_auth", true)
  xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
  xhr.withCredentials = "true";

  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {
        eb.close();
        eb.onclose = function() {
          window.location.href = "/";
        };
      } else {
        transition(showing, "fail-dialog");
      }
    }
  }

  xhr.send("sessionID=" + sessionId + "&name=" + username);
}

// ELEM TRANSISTONS.
function transition(elemOutId, elemInId) {
  var elemOut = elemOutId;
  if (typeof elemOut == 'string')
	  elemOut = document.getElementById(elemOutId);
  var elemIn = document.getElementById(elemInId);
  removeClassName(elemOut, "showing");
  addClassName(elemOut, "toTheLeft");
  removeClassName(elemIn, "toTheRight");
  removeClassName(elemIn, "toTheLeft");
  addClassName(elemIn, "showing");
  showing = elemIn;
  showing.focus();
}
Login.transition = transition;

// STRING UTILITIES.
function addClassName(elem, className) {
  removeClassName(elem, className);
  elem.className = elem.className + " " + className;
}

function removeClassName(elem, className) {
  var elemClasses = elem.className;
  elemClasses = elemClasses.replace(className, "");

  // Leading and trailing whitespace does not matter wrt to style matching.
  // Nor does double whitespace. But to prevent memory leaking ws in the string,
  // we strip both double whitspace, as well as leading and trailing ws.
  elemClasses = elemClasses.replace("  ", " ");
  elemClasses = elemClasses.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
  elem.className = elemClasses;
}

})();