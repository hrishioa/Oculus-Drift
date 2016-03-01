/*
 * Copyright 2011 Karel Bruneel (www.karelbruneel.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package bluetoothSteeringWheel.karelbruneel.com;

import bluetoothSteeringWheel.karelbruneel.com.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BluetoothSteeringWheel extends Activity implements SensorEventListener, BluetoothSPPConnectionListener {

	private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    	
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;

	private BluetoothSPPConnection mBluetoothSPPConnection;
	private BluetoothAdapter mBluetoothAdapter = null;

	private PowerManager.WakeLock wl;
	
	private boolean connected=false;

	private float gravity[];
	private boolean drive=false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initializing the gravity vector to zero.
        gravity = new float[3];
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;
        

        // Initializing the accelerometer stuff
        // Register this as SensorEventListener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);   
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        
       
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        //If bluetooth is not activated, ask the user to activate
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Initializing the bluetooth SPP connection.
        // Register this as the BluetoothSPPConnectionListener.
        mBluetoothSPPConnection = new BluetoothSPPConnection(this); // Registers the   
        
        // Initializing the "connect" button.
        // Register this an the OnClickListener.
        Button bt = (Button) findViewById(R.id.connect);
        bt.setText("Connect");
        bt.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// When not connected, start the DeviceListActivity that allows the user to select a device.
				// The activity will return a bluetoothdevice and on that return, the onActivityResult() (see below)
				// member function is called.
				if (!connected) {
					Intent serverIntent = new Intent(v.getContext(), DeviceListActivity.class);
					startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				// When connected, close the bluetooth connection.
				} else {
					mBluetoothSPPConnection.close();
				}
			}
		});

        // Initializing the "Start driving!" button.
        Button startButton = (Button) findViewById(R.id.start);
        startButton.setText("Start driving!");
        //Register an OnClickListener
        startButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// Toggle between "Start driving!" and "Stop driving!".
				if (!drive) {
			        Button bt = (Button) findViewById(R.id.start);
			        bt.setText("Stop driving!");
					drive = true;
				} else {
			        Button bt = (Button) findViewById(R.id.start);
			        bt.setText("Start driving!");
					drive = false;
				}
			}
		});

        // Getting a WakeLock. This insures that the phone does not sleep
        // while driving the robot.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire();
        
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		
		// Close the bluetooth connection
		mBluetoothSPPConnection.close();
		
		// Release the WakeLock so that the phone can go to sleep to preserve battery.
		wl.release();
	}

    
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    	switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
        	if (resultCode == Activity.RESULT_OK) {
            	String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mBluetoothSPPConnection.open(device);                
            }
            break;
        case REQUEST_ENABLE_BT:
        	if (resultCode == Activity.RESULT_OK) {
        	}
        	break;
        }
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
	    float size=(float) Math.sqrt(Math.pow(gravity[0], 2)+Math.pow(gravity[1], 2)+Math.pow(gravity[2], 2));
	    byte x = (byte) (128*gravity[0]/size);
	    byte y = (byte) (128*gravity[1]/size);
	    byte z = (byte) (128*gravity[2]/size);

	    // Update the GUI
	    TextView xView = (TextView) findViewById(R.id.x);
		TextView yView = (TextView) findViewById(R.id.y);
		TextView zView = (TextView) findViewById(R.id.z);
	    xView.setText(Integer.toString(x));
	    yView.setText(Integer.toString(y));
	    zView.setText(Integer.toString(z));
	}

	public void bluetoothWrite(int bytes, byte[] buffer) {
		// This function is called when the bluetooth module sends data to the android device.
		// The function is executed in the main thread.
		
	    // Normalize the gravity vector and rescaled it so that every component fits one byte.
		float size=(float) Math.sqrt(Math.pow(gravity[0], 2)+Math.pow(gravity[1], 2)+Math.pow(gravity[2], 2));
	    byte x = (byte) (128*gravity[0]/size);
	    byte y = (byte) (128*gravity[1]/size);
	    byte z = (byte) (128*gravity[2]/size);
	    
	    // If we are in driving mode, send the 'c' character followed the gravity components.
	    if (drive) {
	      byte[] command = new byte[4];
	      command[0]='c';
	      command[1]=x;
	      command[2]=y;
	      command[3]=z;
	      mBluetoothSPPConnection.write(command);
	      
	    // If we are not in driving mode, send the 's' character.
	    } else {
          byte[] command = new byte[1];
	      command[0]='s';
		  mBluetoothSPPConnection.write(command);
	    }
	}

	public void onConnecting() {
		// This function is called on the moment the phone starts making a connecting with the bluetooth module.
		// The function is executed in the main thread.

		// Change the text in the connectionInfo TextView
		TextView connectionView = (TextView) findViewById(R.id.connectionInfo);
		connectionView.setText("Connecting...");
	}

	public void onConnected() {
		// This function is called on the moment a connection is realized between the phone and the bluetooth module.
		// The function is executed in the main thread.

		connected = true;

		// Change the text in the connectionInfo TextView
		TextView connectionView = (TextView) findViewById(R.id.connectionInfo);
		connectionView.setText("Connected to "+mBluetoothSPPConnection.getDeviceName());

		// Change the text in the connect button.
		Button bt = (Button) findViewById(R.id.connect);
		bt.setText("Disconnect");
		
		// Send the 's' character so that the communication can start.
		byte[] command = new byte[1];
		command[0]='s';
		mBluetoothSPPConnection.write(command);
	}

	public void onConnectionFailed() {
		// This function is called when the intended connection could not be realized.
		// The function is executed in the main thread.

		connected = false;
		
		// Change the text in the connectionInfo TextView
		TextView connectionView = (TextView) findViewById(R.id.connectionInfo);
		connectionView.setText("Connection failed!");

		// Change the text in the connect button.
		Button bt = (Button) findViewById(R.id.connect);
		bt.setText("Connect");
	}

	public void onConnectionLost() {
		// This function is called when the intended connection could not be realized.
		// The function is executed in the main thread.
		
		connected = false;
		
		// Change the text in the connectionInfo TextView
		TextView connectionView = (TextView) findViewById(R.id.connectionInfo);
		connectionView.setText("Not Connected!");

		// Change the text in the connect button.
		Button bt = (Button) findViewById(R.id.connect);
		bt.setText("Connect");
	}
}