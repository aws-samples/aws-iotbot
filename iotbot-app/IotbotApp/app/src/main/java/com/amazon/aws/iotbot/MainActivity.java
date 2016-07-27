package com.amazon.aws.iotbot;


import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.demo.iotbotremote.R;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Endpoint Prefix = random characters at the beginning of the custom AWS
    // IoT endpoint
    // describe endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    // endpoint prefix string is XXXXXXX
    private static final String CUSTOMER_SPECIFIC_ENDPOINT_PREFIX = "<IoT Endpoint Prefix>";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "<Cognito Unatuthenticated Pool ID";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "<IoT Policy ARN>";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iotbot.bks";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "iotbot";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "iotbot";

    // S3 bucket
    private static final String BUCKET = "<S3 Bucket name>";

    // S3 key
    private static final String KEY = "image.jpg";


    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    Button btnForward;
    Button btnBack;
    Button btnStop;
    Button btnLeft;
    Button btnRight;
    Button btnPicture;
    Button btnLookLeft;
    Button btnLookRight;

    ImageView imageFromS3;

    Bitmap bitmap;

    AmazonS3Client s3Client;
    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;

    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    String topic = "$aws/things/IoTbot/shadow/update";
    String stop = "{\"state\": {\"reported\": {\"stop\": \"true\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String forward = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"true\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String back = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"true\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String left = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"true\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String right = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"true\",\"back\": \"false\",\"picture\": \"false\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String picture = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"false\",\"lookRight\": \"false\"}}}";
    String lookLeft = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"true\",\"lookRight\": \"false\"}}}";
    String lookRight = "{\"state\": {\"reported\": {\"stop\": \"false\",\"forward\": \"false\",\"left\": \"false\",\"right\": \"false\",\"back\": \"false\",\"picture\": \"true\",\"lookLeft\": \"false\",\"lookRight\": \"true\"}}}";

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar myActionBar = getActionBar();
        myActionBar.setDisplayShowHomeEnabled(true);
        myActionBar.setDisplayUseLogoEnabled(true);


        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);

        btnForward = (Button) findViewById(R.id.fwd);
        btnForward.setOnClickListener(publishForward);

        btnBack = (Button) findViewById(R.id.back);
        btnBack.setOnClickListener(publishBack);

        btnLeft = (Button) findViewById(R.id.left);
        btnLeft.setOnClickListener(publishLeft);

        btnRight = (Button) findViewById(R.id.right);
        btnRight.setOnClickListener(publishRight);

        btnStop = (Button) findViewById(R.id.stop);
        btnStop.setOnClickListener(publishStop);

        btnPicture = (Button) findViewById(R.id.camera);
        btnPicture.setOnClickListener(publishCamera);

        btnLookLeft = (Button) findViewById(R.id.lookLeft);
        btnLookLeft.setOnClickListener(publishLookLeft);

        btnLookRight = (Button) findViewById(R.id.lookRight);
        btnLookRight.setOnClickListener(publishLookRight);

        imageFromS3 = (ImageView) findViewById(R.id.img);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, region, CUSTOMER_SPECIFIC_ENDPOINT_PREFIX);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        //S3 Connection
        s3Client = new AmazonS3Client(credentialsProvider);


        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    btnConnect.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest = new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult = mIotAndroidClient
                                .createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " + createKeysAndCertificateResult.getCertificateId()
                                        + " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest = new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnConnect.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }

        imageFromS3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetFromS3();
            }
        });
    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected to AWS IoT Endpoint " + CUSTOMER_SPECIFIC_ENDPOINT_PREFIX);

                                    try {
                                        final String topic = "localstatus";
                                        mqttManager.publishString("IoTbot Remote connected", topic, AWSIotMqttQos.QOS0);
                                        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                                                new AWSIotMqttNewMessageCallback() {
                                                    @Override
                                                    public void onMessageArrived(final String topic, final byte[] data) {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                try {

                                                                    String message = new String(data, "UTF-8");
                                                                    Log.d(LOG_TAG, "Message arrived:");
                                                                    Log.d(LOG_TAG, "   Topic: " + topic);
                                                                    Log.d(LOG_TAG, " Message: " + message);

                                                                    if (message.contains("hello")) {
                                                                        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                                                                        toast.show();
                                                                    }

                                                                    if (message.contains("S3")) {
                                                                        GetFromS3();
                                                                    }

                                                                } catch (UnsupportedEncodingException e) {
                                                                    Log.e(LOG_TAG, "Message encoding error.", e);
                                                                }
                                                            }

                                                            ;

                                                        });
                                                    }
                                                });
                                    } catch (Exception e) {
                                        Log.e(LOG_TAG, "Subscription error.", e);
                                    }


                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }


        }
    };

    View.OnClickListener publishForward = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(forward, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishBack = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(back, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishLeft = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(left, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishRight = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(right, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishStop = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(stop, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishCamera = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {

                mqttManager.publishString(picture, topic, AWSIotMqttQos.QOS0);


            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }


        }
    };

    View.OnClickListener publishLookLeft = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(lookLeft, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener publishLookRight = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.publishString(lookRight, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };



    public void GetFromS3(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    S3ObjectInputStream content = s3Client.getObject(BUCKET, KEY).getObjectContent();
                    byte[] bytes = IOUtils.toByteArray(content);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    setBitmap(bitmap);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageFromS3.setScaleType(ImageView.ScaleType.FIT_XY);
                            imageFromS3.setImageBitmap(getBitmap());
                        }
                    });
                } catch (Exception e) {
                    Log.e(LOG_TAG,
                            "Exception occurred when retrieving image from S3",
                            e);
                }
            }
        }).start();
    }
}
