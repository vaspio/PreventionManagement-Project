package com.project.test1;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.opencsv.CSVReader;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int READ_EXTERNAL_STORAGE_PERMISSION_CODE =999 ;
    final String TAG = "demo";
    static String device_id;
    boolean flag;
    int LOCATION_REQUEST_CODE = 1001;
    int timer_1,current_timer_1;
    int timer_2,current_timer_2;
    long time1;
    String IP;
    String Port;
    String connect_credentials;
    String file_pref;
    String clientId;
    Switch start_stop;
    MqttAndroidClient client;
    String topic="Android/";
    boolean gps,Start_Stop=false;
    FusedLocationProviderClient fusedLocationProviderClient;
    ArrayList<TimeStep> TimeSteps,rows_1,rows_2;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    NetworkCg networkChangeListener = new NetworkCg();
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        gps = sharedPreferences.getBoolean("manual", false);
        start_stop = findViewById(R.id.data);
        start_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Start_Stop = start_stop.isChecked();
                Log.d("demo","Checked "+Start_Stop);
            }
        });
        device_id = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else{
            if(!gps){
                startLocationService();
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(!gps){
                startLocationService();
            }
        }
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            //ask for permission
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
        }
        Parse("android_1.xml");
        Parse("android_2.xml");
        rows_1 = new ArrayList<TimeStep>();
        rows_2 = new ArrayList<TimeStep>();
        readFile("android_1.csv");
        readFile("android_2.csv");
        time1 = System.currentTimeMillis();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        IP = sharedPreferences.getString("edit_ip", "");
        Port = sharedPreferences.getString("edit_port", "");
        connect_credentials = IP+ ":" + Port;
        ConnectToBroker();
        file_pref = sharedPreferences.getString("list_preference_1","");
        timer_1 = sharedPreferences.getInt("file_1", 1);
        timer_2 = sharedPreferences.getInt("file_2", 1);
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("manual")) {
                    gps = sharedPreferences.getBoolean("manual", false);
                    //Log.d("demo", "changed " + gps);
                    if (gps) {
                        //file_pref = sharedPreferences.getString("list_preference_1","");
                       // timer_1 = sharedPreferences.getInt("file_1", 1);
                        //timer_2 = sharedPreferences.getInt("file_2", 1);
                        Log.d("demo","timer 1 "+timer_1+" timer2 "+timer_2+" file "+file_pref);
                        stopLocationService();
                    } else {
                        startLocationService();
                    }
                }
                else if (key.equals("edit_ip")) {
                    IP = sharedPreferences.getString("edit_ip", "");
                    DisconnectFromBroker();
                    connect_credentials = IP+ ":" + Port;
                    ConnectToBroker();
                    Log.d("demo", "changed " + IP);
                    Log.d("demo", "changed " + connect_credentials);
                } else if (key.equals("edit_port")) {
                    Port = sharedPreferences.getString("edit_port", "");
                    DisconnectFromBroker();
                    connect_credentials = IP+ ":" + Port;
                    ConnectToBroker();
                    Log.d("demo", "changed " + Port);
                } else if (key.equals("list_preference_1")) {
                    file_pref = sharedPreferences.getString("list_preference_1","");
                    Log.d("demo","file "+file_pref);
                }
                else if(key.equals("file_1")){
                    timer_1 = sharedPreferences.getInt("file_1", 1);
                }
                else if(key.equals("file_2")){
                    timer_2 = sharedPreferences.getInt("file_2", 1);
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }
    private void Parse(String filename){
        TimeSteps = new ArrayList<TimeStep>();
        try {
            InputStream is = getAssets().open(filename);
            XmlPullParserFactory parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
            parser.setInput(is,null);
            String tag = "" , text = "",text2="",text3="";
            int event = parser.getEventType();
            while (event!= XmlPullParser.END_DOCUMENT){
                tag = parser.getName();
                switch (event){
                    case XmlPullParser.START_TAG:
                        if(tag.equals("timestep"))
                            text = parser.getAttributeValue(null, "time");
                        break;
                    case XmlPullParser.END_TAG:
                        if(tag.equals("vehicle")){
                            text2 = parser.getAttributeValue(null, "x");
                            text3 = parser.getAttributeValue(null, "y");
                            TimeStep times = new TimeStep(text,text3,text2);
                            TimeSteps.add(times);
                        }
                        break;
                    default:
                        break;
                }
                event = parser.next();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        Log.d(TAG, " "+ TimeSteps.size());
        String filename1;
        if(filename=="android_1.xml"){
             filename1 = "android_1.csv";
        }
        else{
            filename1 = "android_2.csv";
        }
        Log.d("demo","filename "+filename1);
        File directoryDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        //File logDir = new File (directoryDownload, "CSV Folder"); //Creates a new folder in DOWNLOAD directory
        //logDir.mkdirs();
        File file = new File(directoryDownload,filename1);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file, true);
            //outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            for (int i = 0; i < TimeSteps.size(); i++) {
                outputStream.write((TimeSteps.get(i).getTime_step() + "," +TimeSteps.get(i).getLat()  + "," + TimeSteps.get(i).getLongit()+ "\n").getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isLocationServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(activityManager != null) {
            for (ActivityManager.RunningServiceInfo service :
                    activityManager.getRunningServices(Integer.MAX_VALUE)){
                if(LocationService.class.getName().equals(service.service.getClassName()))
                    if(service.foreground){
                        return true;
                    }

            }
        }
        return false;
    }
    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(),LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this,"location service started", Toast.LENGTH_SHORT).show();
        }
    }private void stopLocationService(){
        if(isLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this,"Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.i_set:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.xtp:
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Civil Protection App")
                        .setMessage("Are you sure you want to close this App?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stopLocationService();
                                finish();
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean ConnectToBroker(){
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(),
                connect_credentials,
                clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    flag =true;
                    if (client.isConnected()){
                        try {
                            client.subscribe("Notifications/",0);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        client.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {
                                long time2 = System.currentTimeMillis();
                                String jsonString = new String(message.getPayload());
                                JSONObject obj = new JSONObject(jsonString);
                                String dangerLevel = obj.getString("dangerLevel");
                                String distance = obj.getString("distanceFromIotCenter");
                                if((time2-time1)>6000){
                                    dangerMessage(dangerLevel, distance);
                                    Log.d("demo","dif1 = "+(time2-time1));
                                   time1 = System.currentTimeMillis();
                                }
                                  //  time1 = System.currentTimeMillis();
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {

                            }
                        });
                    }
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d("demo","not connected");
                    Toast.makeText(MainActivity.this,"not connected",Toast.LENGTH_LONG).show();
                    flag  = false;
                }
            });
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
        return flag;
    }
    public void DisconnectFromBroker() {
        try{
            client.unregisterResources();
            client.close();
            client.disconnect();
            client.setCallback(null);
            client = null;
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void pub() {
        SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        try{
            JSONObject dataObject = new JSONObject();
            dataObject.put("DeviceID",device_id);
            if(!gps){
                dataObject.put("Latitude",sharedPreferences.getString("Latitude",""));
                dataObject.put("Longitude",sharedPreferences.getString("Longitude",""));
            }else{
                switch (file_pref){
                    case "1":
                        //Log.d("demo","maybeeee");
                        //Log.d("demo","InSide ONe"+" "+current_timer_1+" "+rows_1.get(current_timer_1).getLat());
                        dataObject.put("Latitude",rows_1.get(current_timer_1).getLat());
                        dataObject.put("Longitude",rows_1.get(current_timer_1).getLongit());
                        break;
                    default:
                        //Log.d("demo","InSide TwO");
                        dataObject.put("Latitude",rows_2.get(current_timer_2).getLat());
                        dataObject.put("Longitude",rows_2.get(current_timer_2).getLongit());
                        break;
                }
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(dataObject.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Log.d("demo","name "+topic);
                client.publish(topic, message);
                //client.publish(topic, payload.getBytes(),0,false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    public void dangerMessage(String danger,String distance){
        final MediaPlayer mediaPlayer1 = MediaPlayer.create(this,R.raw.dangeraudio);
        final MediaPlayer mediaPlayer2 = MediaPlayer.create(this,R.raw.dangersound1);

        switch (danger) {
            case "high" :
                mediaPlayer1.start();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.
                        setTitle("DANGER CLOSE TO YOU")
                        .setMessage("High level of danger " + distance +" meters from you location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mediaPlayer1.stop();
                            }
                        })
                        .show();
                break;
            case "medium" :
                mediaPlayer2.start();
                AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                builder1.
                        setTitle("DANGER CLOSE TO YOU")
                        .setMessage("Medium level of danger " + distance +" meters from your location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mediaPlayer2.stop();
                            }
                        })
                        .show();
                break;
            default:
                break;
        }
    }
    private void readFile(String filename) {
        File currentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(currentDir, filename);
        if (file.isFile()) {
            try {CSVReader reader = new CSVReader(new FileReader(file.getAbsolutePath()));
                String[] nextLine;
                while ((nextLine = reader.readNext()) != null) {
                    if(filename=="android_1.csv"){
                        rows_1.add(new TimeStep(nextLine[0],nextLine[1],nextLine[2]));
                    }else{
                        rows_2.add(new TimeStep(nextLine[0],nextLine[1],nextLine[2]));
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            Log.d("demo","BROKEN");
    }
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Civil Protection App")
                .setMessage("Are you sure you want to close this App?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopLocationService();
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }
    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();

    }
    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }
    @Override
    protected void onResume(){
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                if(flag){
                    if(!gps){
                        Log.d("demo","auto");
                        pub();
                    }else{
                        if(Start_Stop){
                            SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                            current_timer_1 = sharedPreferences.getInt("timer_1",0);
                            current_timer_2 = sharedPreferences.getInt("timer_2",0);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            Log.d("demo","manual "+file_pref);
                            switch (file_pref){
                                case "1":
                                    //Log.d("demo","BINGO "+file_pref+" "+current_timer_1);
                                    if(current_timer_1<timer_1){
                                        pub();
                                        current_timer_1++;
                                        editor.putInt("timer_1",current_timer_1);
                                        editor.apply();
                                    }
                                    else{
                                        Log.d("demo","EOF");
                                    }
                                    break;
                                default:
                                    //Log.d("demo","BINGO "+file_pref+" "+current_timer_2);
                                    if(current_timer_2<timer_2){
                                        pub();
                                        current_timer_2++;
                                        editor.putInt("timer_2",current_timer_1);
                                        editor.apply();
                                    }
                                    else{
                                        Log.d("demo","EOF");
                                    }
                                    break;
                            }
                        }
                    }
                }
           }
        }, delay);
        super.onResume();
    }
    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
        super.onPause();
    }
}