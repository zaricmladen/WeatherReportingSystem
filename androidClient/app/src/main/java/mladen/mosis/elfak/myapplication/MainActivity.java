package mladen.mosis.elfak.myapplication;

import static com.google.firebase.messaging.Constants.TAG;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private Button btnSignOut, btnTimePeriod, btnReadNow;
    private TextView tvCurrentData;
    private ProgressBar progressBar;

    MqttAndroidClient mqttAndroidClient;

    final String serverUri = "tcp://139.59.207.210:1883";

    String clientId = "WR_android_app";
    String clientUsn = "zara";
    String clientPasswd = "anthra";
    final String publishTopic = "command_topic";
    final String dataTopic = "TempHumid_topic";
    final String publishMessage = "Read";
    private boolean receivedResponse=false;

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnTimePeriod = findViewById(R.id.btnTimePeriodData);
        btnReadNow = findViewById(R.id.readNowBtn);
        tvCurrentData = findViewById(R.id.tvCurrData);
        progressBar = findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.GONE);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        setBtnOnClickListeners();
        androidMqttClientInit();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                isSignedIn();
            }
        };

        firebaseFCMInit();


    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

    }

    @Override
    protected void onResume() {
        mAuth.addAuthStateListener(mAuthListener);
        tvCurrentData.setText("Please click the button below to get the latest data.");
        super.onResume();
    }

    @Override
    protected void onStop() {
        mAuth.removeAuthStateListener(mAuthListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void isSignedIn() {
        FirebaseUser user = mAuth.getCurrentUser();
        if(user == null) {
            Intent intent = new Intent(MainActivity.this, SplashScreenActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setBtnOnClickListeners() {

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });


        btnTimePeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ListViewActivity.class);
                startActivity(intent);
                //finish();
            }
        });

        btnReadNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvCurrentData.setText("Retrieving data...");
                mqttPublishMessage();
            }
        });

    }

    private void androidMqttClientInit() {
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if(!reconnect)
                    Log.d("Android mqtt client:", "Connected to broker " + serverURI  + ".");
                else
                    Log.d("Android mqtt client: ", "Reconnected to broker " + serverURI + "." );

            }

            @Override
            public void connectionLost(Throwable cause) {
                progressBar.setVisibility(View.GONE);
                Log.d("Android mqtt client: ", "Connection lost - " + cause.toString());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                receivedResponse=true;
                progressBar.setVisibility(View.GONE);
                Log.d("Android mqtt client: ", "New message from topic -" + topic + " | " + message.getPayload().toString());
                JSONObject jo = new JSONObject(new String(message.getPayload()));
                String showString = formatString(jo);
                tvCurrentData.setText(showString);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("Android mqtt client: ", "Message sent to broker - " + token.toString());
            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setUserName(clientUsn);
        mqttConnectOptions.setPassword(clientPasswd.toCharArray());

        try {
            Log.d("Android mqtt client: ", "Connecting to mqtt broker...");
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqttSubscribeToTopic(dataTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("Android mqtt client: ", "Connection failed - " + exception.toString());
                }
            });

        }
        catch(MqttException e) {
            Log.d("Android mqtt client: ", "Connection failed - " + e.toString());
        }
    }

    private void mqttSubscribeToTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("Android mqtt client: ", "Successfully subscribed to " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("Android mqtt client: ", "Failed subscription to " + topic);
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }

    }

    private void mqttPublishMessage(){

        try {
            if(!mqttAndroidClient.isConnected()){
                Log.d("Android mqtt client: ", "Message was not sent due to lost connection with broker");
                progressBar.setVisibility(View.GONE);
            }
            else {
                MqttMessage message = new MqttMessage();
                message.setPayload(publishMessage.getBytes());
                mqttAndroidClient.publish(publishTopic, message);
                progressBar.setVisibility(View.VISIBLE);
                receivedResponse=false;

                new CountDownTimer(15000, 1) {

                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        if(!receivedResponse) {
                            tvCurrentData.setText("Please click the button below to get the latest data");
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "An error has occured while retrieving the data.", Toast.LENGTH_LONG).show();
                        }
                    }
                }.start();

            }

        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void firebaseFCMInit() {

        askNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        // Log token
                        Log.d(TAG, token);
                    }
                });

        FirebaseMessaging.getInstance().subscribeToTopic("weatherzaraanthra")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Subscribed";
                        if (!task.isSuccessful()) {
                            msg = "Subscribe failed";
                        }
                        Log.d(TAG, msg);
                    }
                });

    }

    private String formatString(JSONObject jo) {
        try {
            String hour, minutes, seconds;
            if(jo.getInt("h") < 10)
                hour = "0"+jo.getString("h");
            else
                hour = jo.getString("h");
            if(jo.getInt("m") < 10)
                minutes = "0"+jo.getString("m");
            else
                minutes = jo.getString("m");
            if(jo.getInt("s") < 10)
                seconds = "0"+jo.getString("s");
            else
                seconds = jo.getString("s");

            String showString = hour + ":" + minutes + ":" + seconds + " UTC \n";
            showString+= "T: " + jo.getString("temperature") + "\u2103 | H: " + jo.getString("humidity") + "%";
            return showString;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        }


}