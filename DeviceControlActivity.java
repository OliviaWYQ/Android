/*
 * Copyright (C) 2015 Iasc CHEN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.iasc.microduino.bluejoypad;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.renderscript.Sampler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import me.iasc.microduino.ble.BleAsyncTask;
import me.iasc.microduino.ble.BluetoothLeService;

import java.util.Timer;
import java.util.TimerTask;
import java.lang.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends AbstractBleControlActivity implements SensorEventListener {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    private ToggleButton unlockButton;
    private ToggleButton BU_button;
    private ToggleButton GPS_button;

    private int FRONT_Y_2 = -64, FRONT_Y_1 = -20, BACK_Y_1 = 20, BACK_Y_2 = 64;

    private int ROTATE_LEFT = 1000, ROTATE_MIDDLE = 1500, ROTATE_RIGHT = 2000;
    private short CHANNEL_HIGH = 1900, CHANNEL_MID = 1500, CHANNEL_LOW = 1100;
    private short CHANNEL_RANGE = (short) (CHANNEL_HIGH - CHANNEL_LOW);
    private short CHANNEL_HALF_RANGE = (short) (CHANNEL_RANGE / 2);


    private VerticalSeekBar powerBar;
    private ImageButton rotateLeft, rotateRight;
    private TextView xView, yView, zView, vView, hView, lrView, fbView;

    private static Timer cmdSendTimer;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private int power = 0, rotate = 0, lrValue = 0, fbValue = 0;
    public double LatValue, LatValue1, LatValue2, LatValue3;
    public double LonValue, LonValue1, LonValue2, LonValue3;

    //public short LatValue1 = (short) ((int) LatValue);
    //public short LatValue2 = (short) ((int) ((LatValue - LatValue1) * 10000));
    //public short LatValue3 = (short) ((int) ((LatValue - LatValue1) * 100000000 - LatValue2 * 10000));

    //public int LatValue1 = (int) Math.floor(LatValue);
    //public int LatValue2 = (int) Math.floor((LatValue - LatValue1) * 10000);
    //public int LatValue3 = (int) Math.floor((LatValue - LatValue1) * 100000000 - LatValue2 * 10000);

    //public short LonValue1 = (short) ((int) LonValue);
    //public short LonValue2 = (short) ((int) ((LonValue - LonValue1) * 10000));
    //public short LonValue3 = (short) ((int) ((LonValue - LonValue1) * 100000000 - LonValue2 * 10000));

    private float gravity[];
    private static boolean isDriving = false;

    private PowerManager.WakeLock wl;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        // Initializing the gravity vector to zero.
        gravity = new float[3];
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;

        xView = (TextView) findViewById(R.id.x);
        yView = (TextView) findViewById(R.id.y);
        zView = (TextView) findViewById(R.id.z);

        vView = (TextView) findViewById(R.id.v);
        hView = (TextView) findViewById(R.id.h);

        lrView = (TextView) findViewById(R.id.lr);
        fbView = (TextView) findViewById(R.id.fb);

        // Initializing the accelerometer stuff
        // Register this as SensorEventListener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        powerBar = (VerticalSeekBar) findViewById(R.id.powerBar);
        powerBar.setProgress(0);
        powerBar.setMax(CHANNEL_RANGE);
        powerBar.setEnabled(false);

        rotateLeft = (ImageButton) findViewById(R.id.rotateLeft);
        rotateRight = (ImageButton) findViewById(R.id.rotateRight);
        rotateLeft.setEnabled(false);
        rotateRight.setEnabled(false);
        //BU_button.setEnabled(false);
        //GPS_button.setEnabled(false);

        powerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isDriving) {
                    if (progress < 0) progress = 0;
                    if (progress > CHANNEL_RANGE) progress = CHANNEL_RANGE;

                    power = CHANNEL_LOW + progress;

                    JoypadCommand.changeChannel(JoypadCommand.POWER, power);
                    updateUIPower(power);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore
            }
        });

        rotateLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isDriving) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        rotate = ROTATE_LEFT;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        rotate = ROTATE_MIDDLE;
                    }
                    JoypadCommand.changeChannel(JoypadCommand.ROTATE, rotate);
                    updateUIRotate(rotate);
                }

                return false;
            }
        });

        rotateRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isDriving) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        rotate = ROTATE_RIGHT;
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        rotate = ROTATE_MIDDLE;
                    }
                    JoypadCommand.changeChannel(JoypadCommand.ROTATE, rotate);
                    updateUIRotate(rotate);
                }
                return false;
            }
        });

        unlockButton = (ToggleButton) findViewById(R.id.toggleLockButton);

        unlockButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View v) {
                TextView L = (TextView) findViewById(R.id.L);
                if (unlockButton.isChecked()) {
                    powerBar.setEnabled(true);
                    rotateLeft.setEnabled(true);
                    rotateRight.setEnabled(true);
                    //BU_button.setEnabled(true);
                    //GPS_button.setEnabled(true);

                    power = 1150;
                    powerBar.setProgress(power - CHANNEL_LOW);
                    updateUIPower(power);

                    UnlockTask task = new UnlockTask();
                    task.execute();

                    //unlockButton.setTextOn("Unlocked\n");
                    L.setText("      Unlocked");
                    unlockButton.setBackgroundColor(Color.GREEN);
                } else {
                    // Slow Down
                    MinusPowerTask task = new MinusPowerTask();
                    task.execute();

                    power = 1000;
                    powerBar.setProgress(0);
                    updateUIPower(power);

                    powerBar.setEnabled(false);
                    rotateLeft.setEnabled(false);
                    rotateRight.setEnabled(false);

                    //unlockButton.setTextOff("Locked\n");
                    L.setText("       Locked");
                    unlockButton.setBackgroundColor(Color.RED);
                }
            }
        });

        BU_button = (ToggleButton) findViewById(R.id.toggleBUButton);
        BU_button.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View view) {
                TextView B = (TextView) findViewById(R.id.B);
                if (BU_button.isChecked()) {
                    JoypadCommand.useBU();
                    B.setText("    BUOn");
                    BU_button.setBackgroundColor(Color.GREEN);
                } else {
                    JoypadCommand.stopBU();
                    B.setText("    BUOff");
                    BU_button.setBackgroundColor(Color.RED);
                }
            }
        });

        GPS_button = (ToggleButton) findViewById(R.id.toggleGPSButton);
        GPS_button.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View v) {
                TextView G = (TextView) findViewById(R.id.G);
                if (GPS_button.isChecked()) {
                    String serviceString = Context.LOCATION_SERVICE;
                    LocationManager locationManager = (LocationManager) getSystemService(serviceString);
                    String provider = LocationManager.GPS_PROVIDER;
                    Location location = locationManager.getLastKnownLocation(provider);
                    getLocationInfo(location);
                    locationManager.requestLocationUpdates(provider, 2000, 5, locationListener);
                    JoypadCommand.useGP();
                    G.setText("GPSOn      ");
                    GPS_button.setBackgroundColor(Color.GREEN);
                } else {
                    JoypadCommand.stopGP();
                    JoypadCommand.initGPS();
                    G.setText("GPSOff      ");
                    GPS_button.setBackgroundColor(Color.RED);
                }
            }
        });

        // Getting a WakeLock. This insures that the phone does not sleep
        // while driving the robot.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void getLocationInfo(Location location) {
        String latInfo;
        String lonInfo;
        TextView lo1 = (TextView) findViewById(R.id.Lat);
        TextView lo2 = (TextView) findViewById(R.id.Lon);
        if (location != null) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();
            LatValue = lat;
            LatValue1 = (short) ((int) LatValue);
            LatValue2 = (short) ((int) ((LatValue - LatValue1) * 10000));
            LatValue3 = (short) ((int) ((LatValue - LatValue1) * 100000000 - LatValue2 * 10000));
            LonValue1 = (short) ((int) LonValue);
            LonValue2 = (short) ((int) ((LonValue - LonValue1) * 10000));
            LonValue3 = (short) ((int) ((LonValue - LonValue1) * 100000000 - LonValue2 * 10000));
            JoypadCommand JoypadCommand = new JoypadCommand();
            JoypadCommand.channel[4] = (short) LatValue1;
            JoypadCommand.channel[5] = (short) LatValue2;
            JoypadCommand.channel[8] = (short) LatValue3;
            JoypadCommand.channel[9] = (short) LonValue1;
            JoypadCommand.channel[10] = (short) LonValue2;
            JoypadCommand.channel[11] = (short) LonValue3;
            LonValue = lng;
            latInfo = "Lat:" + LatValue;
            lonInfo = "Lon:" + LonValue;
            lo1.setText(latInfo);
            lo2.setText(lonInfo);
        } else {
            Toast.makeText(DeviceControlActivity.this, "Please allow the program to use GPS.", 150000).show();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        /*GPS状态变化时触发*/
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
        /*GPS开启时触发*/
        @Override
        public void onProviderEnabled(String provider) {
            getLocationInfo(null);
        }
        /*GPS禁用时触发*/
        @Override
        public void onProviderDisabled(String provider) {
            getLocationInfo(null);
        }
        /*位置信息变化时触发*/
        @Override
        public void onLocationChanged(Location location) {
            getLocationInfo(location);
            Toast.makeText(DeviceControlActivity.this, "Your location has been changed.", 3000).show();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void updateReadyState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                wait_ble(2000);
                characteristicReady = true;
                isSerial.setText(getString(resourceId));
                unlockButton.setEnabled(true);
                toastMessage(getString(resourceId));
            }
        });
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We don't do anything when the accuracy of the accelerometer changes.
    }

    public void onSensorChanged(SensorEvent event) {
        // This function is called repeatedly. The tempo is set when the listener is register
        // see onCreate() method.

        // Lowpass filter the gravity vector so that sudden movements are filtered.
        float alpha = (float) 0.8;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Normalize the gravity vector and rescale it so that every component fits one byte.
        float size = (float) Math.sqrt(Math.pow(gravity[0], 2) + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
        byte x = (byte) (128 * gravity[0] / size);
        byte y = (byte) (128 * gravity[1] / size);
        byte z = (byte) (128 * gravity[2] / size);

        // Update the GUI
        updateUIXyz(x, y, z);
    }

    void updateUIXyz(final byte x, final byte y, final byte z) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isDriving) {
                    // Calculate Left, Right or Forward, back
                    lrValue = calcChannelValue(-y);
                    JoypadCommand.changeChannel(JoypadCommand.LR, lrValue);
                    fbValue = calcChannelValue(x);
                    JoypadCommand.changeChannel(JoypadCommand.FB, fbValue);
                }

                xView.setText("X: " + Integer.toString(x));
                yView.setText("Y: " + Integer.toString(y));
                zView.setText("Z: " + Integer.toString(z));

                lrView.setText("LR: " + Integer.toString(lrValue));
                fbView.setText("FB: " + Integer.toString(fbValue));
            }
        });
    }

    void updateUIPower(final int v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                vView.setText("PW: " + Integer.toString(v));
            }
        });
    }

    void updateUIRotate(final int v) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hView.setText("RT: " + Integer.toString(v));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer(cmdSendTimer);
    }

    private Timer startSentCmdTimer(long delay, long period) {
        Timer mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                byte[] cmd = JoypadCommand.compose();

                // Microduino BLE firmware only can use 18 bytes buffer, so I have to split the message.
                int bufferlen = BLE_MSG_BUFFER_LEN;
                byte[] buffer;
                for (int offset = 0; offset < cmd.length; offset += BLE_MSG_BUFFER_LEN) {
                    bufferlen = Math.min(BLE_MSG_BUFFER_LEN, cmd.length - offset);
                    buffer = new byte[bufferlen];

                    System.arraycopy(cmd, offset, buffer, 0, bufferlen);

                    // Log.v("BBuffer sub", "" + JoypadCommand.byteArrayToHexString(buffer));
                    sendMessage(buffer);
                    wait_ble(BLE_MSG_SEND_INTERVAL / 2);

                    buffer = null;
                }
            }
        }, delay, period);
        return mTimer;
    }

    private void stopTimer(Timer mTimer) {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    short calcChannelValue(int value) {
        short ret = CHANNEL_MID;
        if (value > BACK_Y_2) {
            ret = CHANNEL_LOW;
        } else if (BACK_Y_1 < value && value <= BACK_Y_2) {
            ret = (short) (CHANNEL_MID - (value - BACK_Y_1) * CHANNEL_HALF_RANGE / (BACK_Y_2 - BACK_Y_1));
        } else if (FRONT_Y_1 < value && value <= BACK_Y_1) {
            // Stable Range, Balance
            ret = CHANNEL_MID;
        } else if (FRONT_Y_2 < value && value <= FRONT_Y_1) {
            ret = (short) (CHANNEL_MID + (value - FRONT_Y_1) * CHANNEL_HALF_RANGE / (FRONT_Y_2 - FRONT_Y_1));
        } else {
            ret = CHANNEL_HIGH;
        }

        // Log.v(TAG, "calcChannelValue: " + ret);
        return ret;
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DeviceControl Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://me.iasc.microduino.bluejoypad/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "DeviceControl Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://me.iasc.microduino.bluejoypad/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private class UnlockTask extends BleAsyncTask {
        private final int WAIT_INTERVAL = 2000;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        @Override
        protected String doInBackground(String... params) {
            // Should send unlock cmd 2 seconds
            JoypadCommand.resetChannel(JoypadCommand.UNLOCK_CMD);
            cmdSendTimer = startSentCmdTimer(0, BLE_MSG_SEND_INTERVAL);
            wait_ble(getInterval());

            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            JoypadCommand.resetChannel(JoypadCommand.NORMAL_CMD);

            isDriving = true;
            toastMessage("Unlocked");
        }
    }

    private class MinusPowerTask extends BleAsyncTask {
        private final int WAIT_INTERVAL = 1000;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        @Override
        protected String doInBackground(String... params) {
            isDriving = false;

            JoypadCommand.resetChannel(JoypadCommand.DOWN_CMD);

            // Slow down
            short power = JoypadCommand.minusPower();
            while (power > 1000) {
                wait_ble(getInterval());
                power = JoypadCommand.minusPower();
            }

            JoypadCommand.resetChannel(JoypadCommand.LOCK_CMD);
            wait_ble(getInterval());

            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            stopTimer(cmdSendTimer);
            Log.v(TAG, "sendByteArrayMsgTask onPostExecute done :" + result);

            toastMessage("Stopped");
        }
    }
}