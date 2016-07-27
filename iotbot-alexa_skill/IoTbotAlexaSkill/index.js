var AWS = require('aws-sdk');
var iotdata = new AWS.IotData({endpoint: 'XXXXXXXXXXXXXX.iot.<region>.amazonaws.com', region: '<region>'});
var topic = "$aws/things/IoTbot/shadow/update";
var stop = "{\"state\": {\"reported\": {\"stop\": \"true\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var forward = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"true\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var back = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"true\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var left = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"true\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var right = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"true\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var picture = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
var lookLeft = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"true\",\"lookRight\": \"false\"}}}";
var lookRight = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"false\",\"lookRight\": \"true\"}}}";

exports.handler = function (event, context) {
    
    try {
        console.log("event.session.application.applicationId=" + event.session.application.applicationId);

        /**
         * Uncomment this if statement and populate with your skill's application ID to
         * prevent someone else from configuring a skill that sends requests to this function.
         */
        
        if (event.session.application.applicationId !== "<Alexa Skill Application ID>") {
             context.fail("Invalid Application ID");
        }
        
        if (event.session.new) {
            onSessionStarted({requestId: event.request.requestId}, event.session);
        }

        if (event.request.type === "LaunchRequest") {
            onLaunch(event.request,
                event.session,
                function callback(sessionAttributes, speechletResponse) {
                    context.succeed(buildResponse(sessionAttributes, speechletResponse));
                });
        } else if (event.request.type === "IntentRequest") {
            iotAction(event.request.intent.slots.Command.value, function(response){
                if (response !== null){
                    onIntent(event.request,
                        event.session,
                        function callback(sessionAttributes, speechletResponse) {
                            context.succeed(buildResponse(sessionAttributes, speechletResponse));
                        });
                        console.log(response);
                }
            });
        } else if (event.request.type === "SessionEndedRequest") {
            onSessionEnded(event.request, event.session);
            context.succeed();
        }
    } catch (e) {
        context.fail("Exception: " + e);
  }
};

/**
 * Called when the session starts.
 */
function onSessionStarted(sessionStartedRequest, session) {
    console.log("onSessionStarted requestId=" + sessionStartedRequest.requestId +
        ", sessionId=" + session.sessionId);
}

/**
 * Called when the user launches the skill without specifying what they want.
 */
function onLaunch(launchRequest, session, callback) {
    console.log("onLaunch requestId=" + launchRequest.requestId +
        ", sessionId=" + session.sessionId);

    // Dispatch to your skill's launch.
    getWelcomeResponse(callback);
}

/**
 * Called when the user specifies an intent for this skill.
 */
function onIntent(intentRequest, session, callback) {
    console.log("onIntent requestId=" + intentRequest.requestId +
        ", sessionId=" + session.sessionId);
  
    var intent = intentRequest.intent,
        intentName = intentRequest.intent.name;

    // Dispatch to your skill's intent handlers
    if ("RobotIntent" === intentName) {
        setCommandInSession(intent, session, callback);
    } else if ("AMAZON.HelpIntent" === intentName) {
        getWelcomeResponse(callback);
    } else {
        throw "Invalid intent";
    }
}

function setCommandInSession(intent, session, callback) {
    var cardTitle = intent.name;
    var commandSlot = intent.slots.Command;
    var repromptText = "";
    var sessionAttributes = {};
    var shouldEndSession = false;
    var speechOutput = "";

    if (commandSlot) {
        var command = commandSlot.value;
        sessionAttributes = createcommandAttributes(command);
        speechOutput = "IoTbot " + command + " command ";
        repromptText = "Do you want the robot to do anything else for you?";
    } else {
        speechOutput = "I didn't quite understand. Please try again";
        repromptText = "Still not sure " +
            "You can try by saying, ask the robot to take a picture";
    }

    callback(sessionAttributes,
         buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession));
}

function createcommandAttributes(command) {
    return {
        command: command
    };
}

function iotAction(command,callback){
        switch (command) {
        case 'forward':
            params = {
              topic: topic,
              payload: forward,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);          // successful response
            });
            break;
        case 'back':
            params = {
              topic: topic,
              payload: back,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);          // successful response
            });
            break;
        case 'stop':
            params = {
              topic: topic,
              payload: stop,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);         // successful response
            });
            break;
        case 'left':
            params = {
              topic: topic,
              payload: left,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);           // successful response
            });
            break;
        case 'right':
            params = {
              topic: topic,
              payload: right,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);         // successful response
            });
            break;
        case 'look right':
            params = {
              topic: topic,
              payload: lookRight,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);           // successful response
            });
            break;
        case 'look left':
            params = {
              topic: topic,
              payload: lookLeft,
              qos: 0
            };
            console.log ("preparing to publish lookLeft payload" );
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);           // successful response
            });    
            break;
        case 'take a picture':
            params = {
              topic: topic,
              payload: picture,
              qos: 0
            };
            iotdata.publish(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else     return callback(data);       // successful response
            });
            break;    
        default:
            context.fail(new Error('Unrecognized operation "' + operation + '"'));
    }


}

/**
 * Called when the user ends the session.
 * Is not called when the skill returns shouldEndSession=true.
 */
function onSessionEnded(sessionEndedRequest, session) {
    console.log("onSessionEnded requestId=" + sessionEndedRequest.requestId +
        ", sessionId=" + session.sessionId);
    // Add cleanup logic here
}

// --------------- Functions that control the skill's behavior -----------------------

function getWelcomeResponse(callback) {
    // If we wanted to initialize the session to have some attributes we could add those here.
    var sessionAttributes = {};
    var cardTitle = "Welcome";
    var speechOutput = "Welcome to the IoTbot on Alexa, " +
        "Please tell me what you want the robot to do";
    // If the user either does not reply to the welcome message or says something that is not
    // understood, they will be prompted again with this text.
    var repromptText = "Please tell me the command by saying, " +
        "ask the robot to take a picture or ask the robot to look right";
    var shouldEndSession = false;

    callback(sessionAttributes,
        buildSpeechletResponse(cardTitle, speechOutput, repromptText, shouldEndSession));
}




// --------------- Helpers that build all of the responses -----------------------

function buildSpeechletResponse(title, output, repromptText, shouldEndSession) {
    return {
        outputSpeech: {
            type: "PlainText",
            text: output
        },
        card: {
            type: "Simple",
            title: "SessionSpeechlet - " + title,
            content: "SessionSpeechlet - " + output
        },
        reprompt: {
            outputSpeech: {
                type: "PlainText",
                text: repromptText
            }
        },
        shouldEndSession: shouldEndSession
    };
}

function buildResponse(sessionAttributes, speechletResponse) {
    return {
        version: "1.0",
        sessionAttributes: sessionAttributes,
        response: speechletResponse
    };
}
