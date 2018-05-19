package com.example.abhi.texttospeechandspeechtotext;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.oguzbabaoglu.cardpager.CardPager;
import com.skyfishjy.library.RippleBackground;
import com.tapadoo.alerter.Alerter;
import com.thefinestartist.finestwebview.FinestWebView;

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



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import io.ghyeok.stickyswitch.widget.StickySwitch;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private TextToSpeech tts;
    private ImageButton button;
    private TextView textView,request;
    private RippleBackground rippleBackground;
    private boolean doubleBackToExitPressedOnce;
    BottomSheetBehavior mBottomSheetBehavior;
    ArcProgress arcProgress;
    StickySwitch lightSwitch,fanSwitch;

    String host = "tcp://m11.cloudmqtt.com:18178";
    String topic1 = "temp";
    String topic2 = "gas";

    String username = "pcrxoqvj";
    String password = "3VxhZ-T5v4XR";
    MqttAndroidClient client;
    IMqttToken token = null;
    MqttConnectOptions options;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //font

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                    .setDefaultFontPath("fonts/CircularStd-Bold.ttf")
                    .setFontAttrId(R.attr.fontPath)
                    .build()
            );



        //speech recogniser
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

        arcProgress = (ArcProgress) findViewById(R.id.temp_progress);



        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }


                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });

        View bottomSheet = findViewById( R.id.bottom_sheet );
        bottomSheet.setScaleY(0.9F);
        bottomSheet.setScaleX(0.9F);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(250);

        final Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        bottomSheet.startAnimation(pulse);


        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheet.clearAnimation();

                }
                else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheet.startAnimation(pulse);
                    bottomSheet.setScaleY(0.9F);
                    bottomSheet.setScaleX(0.9F);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                bottomSheet.setScaleY(0.9F-slideOffset/20);
                bottomSheet.setScaleX(0.9F-slideOffset/20);
            }
        });
        //ripple animation
        rippleBackground =(RippleBackground)findViewById(R.id.content);
        AnimationDrawable animationDrawable = (AnimationDrawable) rippleBackground.getBackground();
        animationDrawable.setEnterFadeDuration(2500);
        animationDrawable.setExitFadeDuration(5000);
        animationDrawable.start();

        if(!isNetworkAvailable()) {
            Alerter.create(MainActivity.this)
                    .setText("Please switch on data")
                    .enableIconPulse(true)
                    .setBackgroundColor(R.color.colorBlue)
                    .setDuration(2000)
                    .show();
        }




        button = (ImageButton) findViewById(R.id.btnSpeak);
        textView = (TextView) findViewById(R.id.txtSpeechInput);
        request = (TextView) findViewById(R.id.request);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if ((ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {

                    Alerter.create(MainActivity.this)
                            .setText("Please allow permission of recording audio")
                            .enableIconPulse(true)
                            .setBackgroundColor(R.color.colorBlue)
                            .setDuration(2000)
                            .show();
                    //Toast.makeText(MainActivity.this, "Please allow permission of recording audio.", Toast.LENGTH_SHORT).show();

                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.RECORD_AUDIO}, 1);

                }
                else {
                    rippleBackground.startRippleAnimation();
                    speech.startListening(recognizerIntent);
                }
            }
        });


        //mqtt

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

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void doProgress(float value)
    {
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(arcProgress.getProgress(),value);
        valueAnimator.setDuration(2000);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                arcProgress.setProgress(Float.valueOf(animation.getAnimatedValue().toString()));
            }
        });
        valueAnimator.start();

    }


    BroadcastReceiver  BReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            //put here whaterver you want your activity to do with the intent received
            String topic = intent.getStringExtra("topic");
            String msg = intent.getStringExtra("msg");

            if(topic.equals("temp"))
            {
                Toast.makeText(getBaseContext(),msg,Toast.LENGTH_SHORT).show();
                try{
                    Float value = Float.parseFloat(msg);
                    doProgress(value);
                }
                catch (Exception e)
                {
                    Log.e("Parse",e.getMessage());
                }
            }
            if(topic.equals("gas"));

            Toast.makeText(getBaseContext(),topic+":"+msg,Toast.LENGTH_SHORT).show();

        }
    };

    private void subscribtion(){
        try {
            client.subscribe(new String[] {topic1,topic2},new int[] {2,2});

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private Boolean publish(String topic,String msg) {

        try {
            client.publish(topic,msg.getBytes(),2,false);
        } catch (MqttException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        if(rippleBackground.isRippleAnimationRunning())
            rippleBackground.stopRippleAnimation();
    }

    @Override
    public void onResults(Bundle results) {
        if(rippleBackground.isRippleAnimationRunning())
            rippleBackground.stopRippleAnimation();
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        //  for (String result : matches)
        text = matches.get(0);

        request.setText(text);

        text =" "+text.toLowerCase()+" ";

        if (text.contains(" what ") ||text.contains(" what's ") || text.contains(" why ")|| text.contains(" how ")|| text.contains(" when ")|| text.contains(" who ")|| text.contains(" where "))
            request.append("?");
        else
            request.append(".");

        if(text.contains("date"))
        {
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MMMM/yyyy");//dd/MM/yyyy
            Date now = new Date();
            textView.setText("" + sdfDate.format(now).replace("/"," "));
            speak("The date is " + sdfDate.format(now));
        }

        else if(text.contains("service"))
        {
            //Toast.makeText(getBaseContext(),"Starting..",Toast.LENGTH_SHORT).show();
            if(text.contains("start"))
            startService(new Intent(getBaseContext(),HomieService.class));
            if(text.contains("stop"))
            stopService(new Intent(getBaseContext(),HomieService.class));

        }

        else if(text.equals(" light "))
        {
            if(publish("temp","yo"))
                speak("done");
            else
                speak("could not process the request");
        }

        else if(text.contains("time")){
            SimpleDateFormat sdfDate = new SimpleDateFormat("hh:mm aa");//dd/MM/yyyy
            Date now = new Date();
            String[] strDate = sdfDate.format(now).split(":");
            if(strDate[1].contains("00"))
                strDate[1] = "o'clock";
            textView.setText("The time is " + sdfDate.format(now));
            speak("The time is " + sdfDate.format(now));

        }

        else if (text.contains("message") || text.contains("text")) {

            if ((ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED)) {

                Alerter.create(MainActivity.this)
                        .setText("Please allow permission of SMS")
                        .enableIconPulse(true)
                        .setBackgroundColor(R.color.colorBlue)
                        .setDuration(2000)
                        .show();
                //Toast.makeText(MainActivity.this, "Please allow permission of recording audio.", Toast.LENGTH_SHORT).show();

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        Manifest.permission.SEND_SMS}, 4);

            }
            else
            {
                String num = text.substring(text.lastIndexOf(" to ") + 4, text.indexOf(" saying "));
                num = num.replaceAll(" ", "");

                String msg = text.substring(text.lastIndexOf(" saying ") + 4, text.length());

                SmsManager smsManager = SmsManager.getDefault();
                //smsManager.sendTextMessage(num, null, msg, null, null);
                textView.setText("To - "+num+"\nMessage - "+msg);
            }
        }


        else if(text.contains("alarm")){
            tts.speak("Creating an alarm ", TextToSpeech.QUEUE_FLUSH, null);
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.deskclock");
            if (launchIntent != null) {
                startActivity(launchIntent);//null pointer check in case package name was not found
            }
            else //in case the app is not found
            {   Intent app_intent;
                app_intent = new Intent(Intent.ACTION_VIEW);
                app_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app_intent.setData(Uri.parse("https://play.google.com/store/details?id=" + "com.google.android.deskclock"));
                startActivity(app_intent);

            }
        }else if(text.contains("remind")||text.contains("reminder")){
            tts.speak("Creating a reminder ", TextToSpeech.QUEUE_FLUSH, null);
            if(text.contains("to")) {
                Calendar cal = Calendar.getInstance();
                Intent intent = new Intent(Intent.ACTION_EDIT);
                String s = text.substring(text.lastIndexOf(" to ") + 4, text.length());
                intent.putExtra("title", s);
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra("beginTime", cal.getTimeInMillis());
                intent.putExtra("allDay", false);
                intent.putExtra("rrule", "FREQ=DAILY");
                intent.putExtra("endTime", cal.getTimeInMillis() + 60 * 60 * 1000);
                startActivity(intent);
            }
            else
            {
                Calendar cal = Calendar.getInstance();
                Intent intent = new Intent(Intent.ACTION_EDIT);
                intent.putExtra("title", "Reminder");
                intent.setType("vnd.android.cursor.item/event");
                intent.putExtra("beginTime", cal.getTimeInMillis());
                intent.putExtra("allDay", false);
                intent.putExtra("rrule", "FREQ=DAILY");
                intent.putExtra("endTime", cal.getTimeInMillis() + 60 * 60 * 1000);
                startActivity(intent);
            }
        } else if (text.contains("watch")) {
            String s = text.substring(text.lastIndexOf("watch")+5,text.length());
            s=s.replace(" ","+");

            speak("Let's watch "+s);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query="+s)));

        } else if (text.contains("photo")||text.contains("capture")||text.contains("snap")||text.contains("pic")||text.contains("picture")||text.contains("selfie")) {

            speak("opening camera");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES));
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            startActivityForResult(intent, 1);

        } else if((text.contains("navigate") || text.contains("take me")) && text.contains("to")) {
            if(text.substring(text.length()-3,text.length()).equals(" to")) {
                tts.speak("Please specify a destination address.", TextToSpeech.QUEUE_FLUSH, null);
            } else {
                Log.d("to: ",Integer.toString(text.lastIndexOf("to+1")));
                Log.d("to: ",Integer.toString(text.length()));
                tts.speak("Starting navigation ", TextToSpeech.QUEUE_FLUSH, null);
                String s = text.substring(text.lastIndexOf(" to ") + 4, text.length());
                s = s.replace(" ", "+");
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=" + s);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        } else if (text.contains("mail") || (text.contains("email"))) {
            String address = text.substring(text.lastIndexOf(" to ")+4,text.length());
            address = address.replaceAll(" ","");
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.setPackage("com.google.android.gm");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Body");
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));

        } else if(text.contains("add") && text.contains("list") && text.contains("to")) {
            tts.speak("Adding to shopping list ", TextToSpeech.QUEUE_FLUSH, null);
            String list = text.substring(text.lastIndexOf("add")+4, text.lastIndexOf("to"));
            String listName = text.substring(text.lastIndexOf(" to ")+4,text.length());
            list = list.replaceAll(" ","\n");
            Intent keepIntent = new Intent(Intent.ACTION_SEND);
            keepIntent.setType("text/plain");
            keepIntent.setPackage("com.google.android.keep");
            keepIntent.putExtra(Intent.EXTRA_SUBJECT, listName);
            keepIntent.putExtra(Intent.EXTRA_TEXT, list);
            try {
                startActivity(keepIntent);
            } catch (Exception e) {
                Intent app_intent;
                app_intent = new Intent(Intent.ACTION_VIEW);
                app_intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app_intent.setData(Uri.parse("https://play.google.com/store/details?id=" + "com.google.android.keep"));
                startActivity(app_intent);
            }

        }
        else if(text.contains("hi") | text.contains("hello"))
        {
            int min = 65;
            int max = 80;

            Random r = new Random();
            int num = r.nextInt(max - min + 1) + min;

            if(num%3==0)
            {
                textView.setText("Hello user,\nwhat's on your mind today?");
                speak("Hello user, what's on your mind today?");
            }
            else if(num%3==1)
            {
                textView.setText("Hello user,\nwhat can I do for you today?");
                speak("Hello user, what can I do for you today?");
            }
            else
            {
                textView.setText("Hello user,\nhow can I help you today?");
                speak("Hello user, how can I help you today?");
            }

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    button.callOnClick();
                }
            }, 2000);



        }

        else if(text.contains("your") & text.contains("name"))
        {
            textView.setText("I am Homie");
            speak("I am, Homie");
        }

        else if(text.contains("+") || text.contains("-") || text.contains(" x ") || text.contains("X") || text.contains("/")) {
            if(text.contains(".")) {
                tts.speak("I am still learning to work with decimal numbers.", TextToSpeech.QUEUE_FLUSH, null);

            } else {
                String input = text.replaceAll("[^xX+-/0-9]", "");
                input = input.replace(" ", "");
                String parsedInteger = "";
                String operator = "";
                float aggregate = 0;
                int flag = 0;


                for (int i = 0; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (Character.isDigit(c)) {
                        parsedInteger += c;
                        Log.e("Text", "Ans=" + parsedInteger);
                    }

                    if (!Character.isDigit(c) || i == input.length() - 1) {
                        int parsed = Integer.parseInt(parsedInteger);
                        if (operator.equals("")) {
                            aggregate = parsed;
                            Log.e("Text", "Ans1=" + aggregate);
                        } else {
                            if (operator.equals("+")) {
                                aggregate += parsed;
                                Log.e("Text", "Ans2=" + aggregate);
                            } else if (operator.equals("-")) {
                                aggregate -= parsed;
                            } else if (operator.equals("x") || operator.equals("X")) {
                                aggregate *= parsed;

                            } else if (operator.equals("/")) {
                                if (Integer.valueOf(parsed) == 0) {
                                    flag = 1;
                                } else {
                                    aggregate /= parsed;
                                }
                            }
                        }
                        parsedInteger = "";
                        operator = "" + c;
                    }
                }
                if (flag == 0) {
                    textView.setText("It's " +aggregate);
                    tts.speak("It's " + aggregate, TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    tts.speak("That sounds like a trick question.", TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        }
        else {
            //tts.speak("You have said something that I did not understand, I will try to learn as I grow up!", TextToSpeech.QUEUE_FLUSH, null);
            if(text.contains("search")){
                String s = text.substring(text.lastIndexOf("search")+6, text.length());
                s=s.replace(" ", "+");
                s="https://www.google.co.in/search?q="+s;
                tts.speak("The web has returned following result", TextToSpeech.QUEUE_FLUSH, null);
                new FinestWebView.Builder(MainActivity.this).titleDefault("Search")
                        .toolbarScrollFlags(0)
                        .titleColorRes(R.color.colorTextBlack)
                        .statusBarColorRes(R.color.colorWhite)
                        .toolbarColorRes(R.color.colorWhite)
                        .iconDefaultColorRes(R.color.colorTextBlack)
                        .progressBarColorRes(R.color.colorTextBlack)
                        .menuSelector(R.drawable.selector_light_theme)
                        .dividerHeight(0)
                        .webViewBuiltInZoomControls(true)
                        .webViewDisplayZoomControls(true)
                        .webViewJavaScriptEnabled(true)
                        .showSwipeRefreshLayout(true)
                        .gradientDivider(false)
                        .setCustomAnimations(R.anim.fade_in_medium, R.anim.fade_out_medium, R.anim.fade_in_medium, R.anim.fade_out_medium)
                        .disableIconBack(false)
                        .disableIconClose(false)
                        .disableIconForward(false)
                        .disableIconMenu(false)
                        .show(s);
            } else{
                String s = text;
                s=s.replace(" ", "+");
                s="https://www.google.co.in/search?q="+s;
                tts.speak("The web has returned following result", TextToSpeech.QUEUE_FLUSH, null);
                new FinestWebView.Builder(MainActivity.this).titleDefault("Search")
                        .toolbarScrollFlags(0)
                        .titleColorRes(R.color.colorTextBlack)
                        .statusBarColorRes(R.color.colorWhite)
                        .toolbarColorRes(R.color.colorWhite)
                        .iconDefaultColorRes(R.color.colorTextBlack)
                        .progressBarColorRes(R.color.colorTextBlack)
                        .menuSelector(R.drawable.selector_light_theme)
                        .dividerHeight(0)
                        .webViewBuiltInZoomControls(true)
                        .webViewDisplayZoomControls(true)
                        .webViewJavaScriptEnabled(true)
                        .showSwipeRefreshLayout(true)
                        .gradientDivider(false)
                        .setCustomAnimations(R.anim.fade_in_medium, R.anim.fade_out_medium, R.anim.fade_in_medium, R.anim.fade_out_medium)
                        .disableIconBack(false)
                        .disableIconClose(false)
                        .disableIconForward(false)
                        .disableIconMenu(false)
                        .show(s);
            }
        }


    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(BReceiver, new IntentFilter("service_message"));

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BReceiver);

    }

    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    //This part of code checks if network is available
    public Boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo wifi = cm
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        android.net.NetworkInfo datac = cm
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if ((wifi != null & datac != null)
                && (wifi.isConnected() | datac.isConnected())) {
            return true;
        }else{
            return false;
        }
    }
}
