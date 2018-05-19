package com.example.abhi.texttospeechandspeechtotext;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

/**
 * Created by abhi on 28/2/18.
 */

public class HomieService extends Service {

    String host = "tcp://m11.cloudmqtt.com:18178";
    String topic1 = "temp";
    String topic2 = "gas";

    String username = "pcrxoqvj";
    String password = "3VxhZ-T5v4XR";
    MqttAndroidClient client;
    IMqttToken token = null;
    MqttConnectOptions options;


    public String lastmsg="";


    @Override
    public void onCreate() {
        super.onCreate();



        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), host, clientId);

        options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());


        try {
            token = client.connect(options);
        } catch (MqttException e) {
            e.printStackTrace();
        }

        try {
            token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    subscribtion();
                    Toast.makeText(getApplicationContext(),"success",Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(getApplicationContext(),"failure",Toast.LENGTH_SHORT).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String msg = new String(message.getPayload());

                sendBroadcast(topic,msg);

                lastmsg=msg;


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });


    }


    private void sendBroadcast (String topic,String msg){
        Intent intent = new Intent ("service_message"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("topic", topic);
        intent.putExtra("msg", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.

        //Toast.makeText(this, "ServiceChat Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //Toast.makeText(this, "Destroyed", Toast.LENGTH_LONG).show();
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribtion(){
        try {
            client.subscribe(new String[] {topic1,topic2},new int[] {2,2});

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


}
