package com.project.sensors;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class PIckSensorType extends AppCompatActivity {
    public static final String KEY_NAME = "NAME";
    private Button button;
    private RadioGroup radioGroup;
    private RadioButton rd1;
    private RadioButton rd2;
    private RadioButton rd3;
    private RadioButton rd4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_sensor_type);
        RadioGroup radioGroup = findViewById(R.id.RdG);
        RadioButton rd1 = findViewById(R.id.sm_sensor);
        RadioButton rd2 = findViewById(R.id.uv_sensor);
        RadioButton rd3 = findViewById(R.id.thml_sensor);
        RadioButton rd4 = findViewById(R.id.gas_sensor);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.sm_sensor:
                        data.putExtra(KEY_NAME,"Smoke Sensor");
                        setResult(RESULT_OK, data);
                        finish();
                        break;
                    case R.id.uv_sensor:
                        data.putExtra(KEY_NAME,"UV Sensor");
                        setResult(RESULT_OK, data);
                        finish();
                        break;
                    case R.id.thml_sensor:
                        data.putExtra(KEY_NAME,"Thermal Sensor");
                        setResult(RESULT_OK, data);
                        finish();
                        break;
                    case R.id.gas_sensor:
                        data.putExtra(KEY_NAME,"Gas Sensor");
                        setResult(RESULT_OK, data);
                        finish();
                        break;
                    default:
                        break;
                }
            }
        });

    }
}