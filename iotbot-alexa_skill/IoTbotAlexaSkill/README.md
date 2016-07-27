# IoTbotAlexaSkill

Lambda function containing the Alexa Skill to Control the IoTbot using an Amazon Echo.

Handler: index.handler

Role: 
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "iot:*"
            ],
            "Resource": "*"
       }
    ]
}
```
