var awsIot = require('aws-iot-device-sdk');
var Gopigo   = require('node-gopigo').Gopigo
var Commands = Gopigo.commands
var Robot = Gopigo.robot
var readline = require('readline')
var child_process = require ( 'child_process');
var fs = require ( 'fs');
var cmd = '/opt/vc/bin/raspistill --output /home/pi/image.jpg --nopreview -w 640 -h 480 --timeout 100 -q 20'

var servo_pos=90

var ultrasonicPin = 15
//var irreceiverPin = 8

var rl = readline.createInterface({
  input : process.stdin,
  output: process.stdout
});

var robot = new Robot({
  minVoltage: 5.5,
  criticalVoltage: 1.2,
  debug: true,
  ultrasonicSensorPin: ultrasonicPin,
  //IRReceiverSensorPin: irreceiverPin
})
robot.on('init', function onInit(res) {
  if (res) {
    console.log('GoPiGo Ready!\n')
    //askForCommand()
  } else {
    console.log('Something went wrong during the init.')
  }
})
robot.on('error', function onError(err) {
  console.log('Something went wrong')
  console.log(err)
})
robot.on('free', function onFree() {
  console.log('GoPiGo is free to go')
})
robot.on('halt', function onHalt() {
  console.log('GoPiGo is halted')
})
robot.on('close', function onClose() {
  console.log('GoPiGo is going to sleep')
})
robot.on('reset', function onReset() {
  console.log('GoPiGo is resetting')
})
robot.on('normalVoltage', function onNormalVoltage(voltage) {
  console.log('Voltage is ok ['+voltage+']')
})
robot.on('lowVoltage', function onLowVoltage(voltage) {
  console.log('(!!) Voltage is low ['+voltage+']')
})
robot.on('criticalVoltage', function onCriticalVoltage(voltage) {
  console.log('(!!!) Voltage is critical ['+voltage+']')
})
robot.init()

//Add your certificates and region details in the file system
var device = awsIot.device({
   keyPath: "xxxxxxxxxx-private.pem.key",
  certPath: "xxxxxxxxxx-certificate.pem.crt",
    caPath: "VeriSign-Class 3-Public-Primary-Certification-Authority-G5.pem",
  clientId: "IoTbot",
    region: "<AWS IoT Region>"
});

//Initializing Shadow State
var requestedState = {
    "state": {
          "reported": {
            "stop": "true",
            "forward": "false",
            "left": "false",
            "right": "false",
            "back": "false",
            "picture": "false",
            "lookLeft": "false",
            "lookRight": "false"
          }
        }
}

//Connecting and subscribing to Shadow Topics
device
  .on('connect', function() {
    console.log('Connected to AWS IoT' );
    console.log(JSON.stringify(device));
    device.subscribe('$aws/things/IoTbot/shadow/#');
    device.subscribe('$aws/things/IoTbot/#');
    device.subscribe('localstatus');
    device.publish('localstatus', 'GoPiGo connected!');
    device.publish('$aws/things/IoTbot/shadow/update', JSON.stringify(requestedState));
    });

//Listening for updates
device
  .on('message', function(topic, payload) {
    console.log('message', topic, payload.toString(),'\n');
    //In case there's an IoT Remote app controlling and it sent a msg to 'localstatus', let it know GoPiGo is alive
    if (topic == "localstatus" && payload.toString() == "IoTbot Remote connected"){
      device.publish('localstatus', 'GoPiGo says hello to IoTbot Remote!');
    }
    if (topic == "$aws/things/IoTbot/shadow/update"){
      requestedState = JSON.parse(payload.toString());
      console.log('Waiting for command from the mothership <Endpoint>.iot.<region>.amazonaws.com\n')
      handleRequest(requestedState);
    }
  });

//Receiving commands
function handleRequest(requestedState){
  console.log ("Passing on Request to IoTbot: " + JSON.stringify(requestedState));
  if(requestedState.state.reported.stop == "true"){
    var res = robot.motion.stop();
    console.log('::IoTbot Stopped::\n');
  };
  if(requestedState.state.reported.forward == "true"){
    var res = robot.motion.forward(false);
    console.log('::IoTbot Moving forward::' + res+ '\n');
  };
  if(requestedState.state.reported.left == "true"){
    var res = robot.motion.left();
    console.log('::IoTbot Turning left::' + res + '\n');
  };
  if(requestedState.state.reported.right == "true"){
    var res = robot.motion.right();
    console.log('::IoTbot Turning right::' + res + '\n');
  };
  if(requestedState.state.reported.back == "true"){
    var res = robot.motion.backward(false);
    console.log('::IoTbot Moving backward::' + res + '\n');
  };
  if(requestedState.state.reported.picture == "true"){
    child_process.exec (cmd, function (error, stdout, stderr) { 
      if (error) { 
        console.log (error); 
      };
      console.log('::IoTbot Taking Low Res Picture::' + '\n');
      fs.readFile('/home/pi/image.jpg', function (error, data) { 
        if (error) { 
          console.log (error);
        } else {
          device.publish('s3upload',data);
          console.log('::Picture sent to S3::' + '\n');
        };
      });
    });    
  };
  if(requestedState.state.reported.lookLeft == "true"){
    servo_pos=servo_pos+20;
    if (servo_pos > 180){
      servo_pos=180
    };
    if (servo_pos < 0){
      servo_pos=0
    };
    var res = robot.servo.move(servo_pos);
    console.log('::IoTbot Looking left 20 degrees::' + res + '\n');
  };
  if(requestedState.state.reported.lookRight == "true"){
    servo_pos=servo_pos-20;
    if (servo_pos > 180){
      servo_pos=180
    };
    if (servo_pos < 0){
      servo_pos=0
    };
    var res = robot.servo.move(servo_pos);
    console.log('::IoTbot Looking left 20 degrees::' + res + '\n');
  };
}
