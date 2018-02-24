package com.example.soumyakant_sahoo.utkarshsockapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_LINEAR_ACCELERATION;
import static android.hardware.Sensor.TYPE_STEP_COUNTER;
import static android.hardware.Sensor.TYPE_ORIENTATION;

public class MainActivity extends AppCompatActivity {

    private SensorManager mSensorManager;
    private SensorEventListener listener;
    private EditText editText;
    private Button button,button_start,button_stop;
    private Socket socket;
    private String value="";

    private static final int SERVERPORT = 5000;
    private String SERVER_IP="";
    private Map<String ,String > sensorData=new HashMap<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Timer timer = new Timer();
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        editText = findViewById(R.id.editText);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SERVER_IP = editText.getText().toString().trim();
            }

        });

        button_start=findViewById(R.id.btn_start);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(SERVER_IP.isEmpty()){
                    Toast.makeText(MainActivity.this, "Enter a valid IP", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(socket==null || !socket.isConnected())
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InetAddress serverAddr = null;
                            try {
                                serverAddr = InetAddress.getByName(SERVER_IP);
                                socket = new Socket(serverAddr, SERVERPORT);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
            }
        });

        button_stop=findViewById(R.id.btn_stop);
        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSensorManager.unregisterListener(listener);
                timer.cancel();
            }
        });
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(socket!=null && socket.isConnected() && !sensorData.isEmpty())
                            try {
                                JSONObject jsonObject = new JSONObject(sensorData);
                                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                                out.println(jsonObject.toString());
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                    }
                }).start();
            }
        },0,500);

        listener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                float dataset[];
                value = "";
                if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
                    float zOrient, xOrient;
                    zOrient = event.values[0];
                    xOrient = event.values[1];
                    sensorData.put("z x", Float.toString(zOrient)+" "+Float.toString(xOrient));
                }
//                if (sensor.getType() == TYPE_GYROSCOPE) {
//                    dataset = event.values;
//                    for (float data : dataset) {
//                        value += data + " ";
//                    }
//                }
                if (sensor.getType() == TYPE_LINEAR_ACCELERATION) {
                    dataset = event.values;
                    for (float data : dataset) {
                        value += data + " ";
                    }
                    sensorData.put("Linear", value);
                }
                if (sensor.getType() == TYPE_STEP_COUNTER) {
                    dataset = event.values;
                    for (float data : dataset) {
                        value += data + " ";
                    }
                    sensorData.put("Step Counter", value);
                }
            }
        };

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
