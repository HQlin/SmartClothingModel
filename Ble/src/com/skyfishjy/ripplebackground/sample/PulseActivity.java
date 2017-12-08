package com.skyfishjy.ripplebackground.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.DeviceControlActivity;
import com.example.android.bluetoothlegatt.R;
import com.example.android.bluetoothlegatt.SampleGattAttributes;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class PulseActivity extends Activity {

	private static final String TAG = "PulseActivity";
	private final static int MENU_HISTORY = Menu.FIRST;
	
	private PulseView pulseView;
	
	//ble
	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    private BluetoothGattCharacteristic mWriteCharacteristic;
    byte[] WriteBytes = new byte[20];
    private MediaPlayer mp; 

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                if(mp.isPlaying()){
    				mp.stop();
    			}
                finish();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	if(intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1)==1){
            		System.out.println(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).split("\n")[0];
                    System.out.println(data);
                    //实时脉搏数据：pul:80	                //当前脉搏：80次
                    if(data.contains("pul:")){
                    	int addy = Integer.parseInt(getSubstring(data, 4));
                        pulseView.updateChart(addy);
                    }    
                    if(mBluetoothLeService!=null){
                    	mBluetoothLeService.getRssi();
                    }
            	} else {
            		//Toast.makeText(PulseActivity.this, "RSSI: " + intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1), Toast.LENGTH_SHORT).show();
            		int rssi = intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1);
            		if(rssi < -55){            			
            			if(!mp.isPlaying()){            				
            			    mp.start();
            			}
            		} else {           			
            			if(mp.isPlaying()){
            				mp.pause();
            			}
            		}
            	}           
            }
        }
    };
    
    private String getSubstring(String string, int index){
    	String substring = string.substring(index);
    	return substring;
    }
    
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                
                if(uuid.equals("0000fff1-0000-1000-8000-00805f9b34fb")){
                	mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                	Toast.makeText(PulseActivity.this,"连接成功!",Toast.LENGTH_SHORT).show();
                }
                
                if(uuid.equals("0000fff2-0000-1000-8000-00805f9b34fb")){
                	mWriteCharacteristic = gattCharacteristic;
                }
            }
        }
    }
    
    //dataMode = 0:data is string
    //dataMode = 1:data is hex
    private void bleSend(String data, int dataMode){
    	byte[] value = new byte[20];
        value[0] = (byte) 0x00;
        if (data.length() > 0 && dataMode == 0) {
            //write string
            WriteBytes = data.getBytes();
        } else if (data.length() > 0 && dataMode == 1) {
            WriteBytes = hex2byte(data.getBytes());
        }
        mWriteCharacteristic.setValue(value[0],
                BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mWriteCharacteristic.setValue(WriteBytes);

    	mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
    }
    
    public static byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("长度不是偶数");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		pulseView = new PulseView(this);
		// 全屏显示
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(pulseView.getmView());
		
		//ble
		final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        
        mp = MediaPlayer.create(this,R.raw.warning); 
        mp.setLooping(true);
        try {
			mp.prepare();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_HISTORY, 0, "查看历史记录").setIcon(android.R.drawable.ic_menu_save);
		return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
            case MENU_HISTORY:
            	Intent intent = new Intent(PulseActivity.this,HistoryListActivity.class);
            	intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
            	startActivity(intent);
    			Toast.makeText(getApplicationContext(), "更新数据库",Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

}
