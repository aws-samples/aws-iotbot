
# IoTbot: Controlling and Integrating the GoPiGo Raspberry Pi robot with AWS IoT using Node.js

The GoPiGo is a complete kit from Dexter Industries that allows to build your own robot car and it has a Raspberry Pi as its brain. It can become an Internet enabled device when connected to the AWS IoT service.

Executing this code from the GoPiGo will make it an IoT device allowing an operator using the related Android app (see link bellow) or some other MQTT client to control the GoPiGo movements, servo and camera from anywhere as long as the GoPiGo is connected to the Internet via WiFi.

<p align="center">
  <img src="https://ksr-ugc.imgix.net/projects/932503/photo-original.jpg?w=1536&h=864&fit=fill&bg=FFFFFF&v=1397874284&auto=format&q=92&s=c2cb798aaee0b72ec4f4e11fa04f0fe9" width="500"/>
  <img src="https://media.amazonwebservices.com/blog/2015/deck_compute_chip_300_1.png"  />
</p>

# Requirements

* A GoPiGo (http://www.dexterindustries.com/GoPiGo/)
* https://github.com/DexterInd/GoPiGo/tree/master/Software/NodeJS
* https://github.com/aws/aws-iot-device-sdk-js 
* http://docs.aws.amazon.com/iot/latest/developerguide/iot-quickstart.html
* S3 bucket to upload images from the camera (low res due to the 128Kb AWS IoT payload limits)

# Getting Started

* Create a thing called IoTbot using the AWS CLI or the console (https://console.aws.amazon.com/iot/home)
* Create a Policy:
```
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "iot:*"
      ],
      "Resource": [
        "*"
      ],
      "Effect": "Allow"
    }
  ]
}
```
* Create the certitificates, download the files and attach the certificate to both the policy and the thing created earlier
* Create a S3 rule similar to the one bellow in order to upload files from the camera to the S3 bucket, make sure the IAM role has appropriate access (PutObject). The rule will listen on the 's3upload' topic, upload a JPG file to the bucket and it can be retrieved using a mobile app with access to the bucket (GetObject), for instance:
```
>aws iot get-topic-rule --rule-name S3 

{
    "rule": {
        "sql": "SELECT * FROM 's3upload'",
        "ruleDisabled": false,
        "actions": [
            {
                "s3": {
                    "roleArn": "arn:aws:iam::xxxxxxxxxx:role/aws_iot_s3",
                    "bucketName": "bucket_name",
                    "key": "image.jpg"
                }
            }
        ],
        "ruleName": "S3"
    }
} 
```
* Upload the certificates to your GoPiGo via SSH (http://www.dexterindustries.com/GoPiGo/getting-started-with-your-gopigo-raspberry-pi-robot-kit-2/4-connect-to-the-gopigo/)
* Add your certificates and region details (Line 62) and execute the code:
```
node iotbot.js
```
* Test with a MQTT client like MQTT.fx (http://docs.aws.amazon.com/iot/latest/developerguide/verify-pub-sub.html) by publishing the following to the "$aws/things/IoTbot/shadow/update" topic. Change from "false" to "true" to test the commands accordingly:

```
{
"state": {
      "reported": {
        "stop": "true",
        "forward": "false",
        "left": "false",
        "right": "false",
        "back": "false",
        "picture": "false",
        "lookLeft": "true",
        "lookRight": "false"
      }
    }
}
```

After configuring the IoTbot, install the Android app to remote control it: https://github.com/awslabs/aws-iotbot/tree/master/iotbot-app/IotbotApp
