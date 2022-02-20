package com.project.sensors;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final String TAG = "demo";
    int LOCATION_REQUEST_CODE = 1001;
    String IP;
    String Port;
    String connect_credentials;
    String manual_pos;
    String clientId;
    String device_id;
    String topic;

    MqttAndroidClient client;
    Parcelable mListState;
    private final String KEY_RECYCLER_STATE = "recycler_state";
    boolean gps,flag;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 1000;
    FusedLocationProviderClient fusedLocationProviderClient;
    BatteryManager bm;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    ArrayList<Sensor> sensorArrayList = new ArrayList<>();
    LinearLayoutManager layoutManager;
    RecyclerView recyclerView;
    SensorAdapter sensorAdapter;
    FloatingActionButton floatingActionButton;
    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result != null && result.getResultCode() == RESULT_OK) {
                if (result.getData() != null && result.getData().getStringExtra(PIckSensorType.KEY_NAME) != null) {
                    switch (result.getData().getStringExtra(PIckSensorType.KEY_NAME)) {
                        case "Smoke Sensor":
                            addSensor(1);
                            sensorAdapter.notifyItemInserted(sensorArrayList.size() - 1);
                            break;
                        case "UV Sensor":
                            addSensor(3);
                            sensorAdapter.notifyItemInserted(sensorArrayList.size() - 1);
                            break;
                        case "Thermal Sensor":
                            addSensor(4);
                            sensorAdapter.notifyItemInserted(sensorArrayList.size() - 1);
                            break;
                        case "Gas Sensor":
                            addSensor(2);
                            sensorAdapter.notifyItemInserted(sensorArrayList.size() - 1);
                            break;
                        default:
                            Log.d("demo", "Undefined ");
                            break;
                    }
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gps = sharedPreferences.getBoolean("manual", false);
        device_id = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        topic =  "IoT/" + "" + device_id;
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
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        IP = sharedPreferences.getString("edit_ip", "");
        Port = sharedPreferences.getString("edit_port", "");
        connect_credentials = IP+ ":" + Port;
        ConnectToBroker();
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("manual")) {
                    gps = sharedPreferences.getBoolean("manual", false);
                    Log.d("demo", "changed " + gps);
                    if (gps) {
                        manual_pos = sharedPreferences.getString("list_preference_1", "");
                        getManualPos(manual_pos);
                        stopLocationService();
                    } else {
                        startLocationService();
                    }
                } else if (key.equals("edit_ip")) {
                    IP = sharedPreferences.getString("edit_ip", "");
                    DisconnectFromBroker();
                    connect_credentials = IP+ ":" + Port;
                    ConnectToBroker();
                    Log.d("demo", "changed " + IP);
                    Log.d("demo", "changed " + connect_credentials);
                } else if (key.equals("edit_port")) {
                    Port = sharedPreferences.getString("edit_port","");
                    DisconnectFromBroker();
                    connect_credentials = IP+ ":" + Port;
                    ConnectToBroker();
                    connect_credentials = IP+ ":" + Port;
                    Log.d("demo", "changed " + Port);
                    Log.d("demo", "changed " + connect_credentials);
                } else if (key.equals("list_preference_1")) {
                    manual_pos = sharedPreferences.getString("list_preference_1", "");
                    getManualPos(manual_pos);
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        addSensor(1);
        addSensor(2);
        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PIckSensorType.class);
                startForResult.launch(intent);
            }
        });
        recyclerView = findViewById(R.id.rec_view);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        sensorAdapter = new SensorAdapter(sensorArrayList);
        recyclerView.setAdapter(sensorAdapter);

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return super.onCreateOptionsMenu(menu);
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

    public void getManualPos(String manual_pos){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        switch (manual_pos){
            case "1":
                editor.putString("Longitude", "23.76630586399502" );
                editor.putString("Latitude", "37.96809452684323");
                editor.apply();
                break;
            case "2":
                editor.putString("Longitude", "23.766603589104385");
                editor.putString("Latitude", "37.96799937191987");
                editor.apply();
                break;
            case "3":
                editor.putString("Longitude", "23.767174897611685");
                editor.putString("Latitude", "37.967779456380754");
                editor.apply();
                break;
            default:
                editor.putString("Longitude","23.76626294807113");
                editor.putString("Latitude", "37.96790421900921");
                editor.apply();
                break;
        }
        String s1 = sharedPreferences.getString("Longitude","");
        String s2 = sharedPreferences.getString("Latitude","");
        Log.d("demo","manual X and Y "+s1+" "+s2);
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
                        .setTitle("Sensors App")
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

    public void addSensor(int i){
        switch (i){
            case 1:
                this.sensorArrayList.add(new Sensor("Smoke Sensor",  R.drawable.smoke_sensor));
                break;
            case 2:
                this.sensorArrayList.add(new Sensor("Gas Sensor",  R.drawable.gas_sensor));
                break;
            case 3:
                this.sensorArrayList.add(new Sensor("UV Sensor",  R.drawable.uv_sensor));
                break;
            default:
                this.sensorArrayList.add(new Sensor("Thermal Sensor",  R.drawable.thermal_sensor));
                break;
        }
        return;
    }
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Sensors App")
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
    public boolean ConnectToBroker(){
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(),
                connect_credentials,
                clientId);
        //"tcp://broker.hivemq.com:1883"
        //
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d("demo","connected");
                    flag =true;
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
        try {
            JSONArray dataArray = new JSONArray();
            for(int i = 0;i<sensorArrayList.size();i++){
                JSONObject dataObject = new JSONObject();
                if(sensorArrayList.get(i).getCommunicate()){
                    dataObject.put("Sensor Type",sensorArrayList.get(i).getSensor_type());
                    dataObject.put("Sensor Number",i);
                    dataObject.put("Sensor Value",sensorArrayList.get(i).getValue());
                    dataArray.put(dataObject);
                }
            }
            JSONObject mainObject = new JSONObject();
            mainObject.put("Latitude",sharedPreferences.getString("Latitude",""));
            mainObject.put("Longitude",sharedPreferences.getString("Longitude",""));
            mainObject.put("Battery",bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
            mainObject.put("DeviceID",device_id);
            if(dataArray.length()!=0){
                mainObject.put("data",dataArray);
            }
            MqttMessage message = new MqttMessage();
            message.setPayload(mainObject.toString().getBytes(StandardCharsets.UTF_8));
            try {
                client.publish(topic, message);
                //client.publish(topic, payload.getBytes(),0,false);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume(){

        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                if(flag){
                    pub();
                }
            }
        }, delay);
        if (mListState != null) {
            layoutManager.onRestoreInstanceState(mListState);
        }
        super.onResume();

    }
    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
        super.onPause();
    }
    protected void onSaveInstanceState(Bundle state) {
        mListState = layoutManager.onSaveInstanceState();
        state.putParcelable(KEY_RECYCLER_STATE, mListState);
        super.onSaveInstanceState(state);
         //Save list state

    }
    protected void onRestoreInstanceState(Bundle state) {

        super.onRestoreInstanceState(state);
        if(state != null)
            mListState = state.getParcelable(KEY_RECYCLER_STATE);
    }
        //Retrieve list state and list/item positions

}