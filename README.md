# aws-iotbot
# IoTbot: Controlling and Integrating the GoPiGo Raspberry Pi robot with AWS IoT using Node.js, Android and Amazon Echo

The GoPiGo is a complete open source kit from Dexter Industries that allows to build your own robot car and it has a Raspberry Pi as its brain. It can become an Internet enabled device when connected to the AWS IoT service.

With this code the GoPiGo will turn into an AWS IoT device allowing an operator using the related Android app, using voice with the Alexa Skill or some other MQTT client to control the GoPiGo movements, servo and camera from anywhere as long as the GoPiGo is connected to the Internet via WiFi.

For each platform the code is available in the following folders with further instructions:

* iotbot-device: code to run the robot as an AWS IoT device
* iotbot-app: Android mobile app to remote control de robot
* iotbot-alexa_skill: AWS Lambda function to remote control the robot via voice with an Amazon Echo
