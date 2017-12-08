package com.skyfishjy.ripplebackground.sample;

import java.util.ArrayList;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.bluetoothlegatt.DeviceControlActivity;
import com.example.android.bluetoothlegatt.R;
import com.skyfishjy.library.RippleBackground;



public class MainActivity extends Activity {

    private ImageView foundDevice;
    private RippleBackground rippleBackground;
    
    //ble
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        rippleBackground=(RippleBackground)findViewById(R.id.content);

        final Handler handler=new Handler();

        foundDevice=(ImageView)findViewById(R.id.foundDevice);
        ImageView button=(ImageView)findViewById(R.id.centerImage);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rippleBackground.startRippleAnimation();
                scanLeDevice(true);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //foundDevice();
                    	rippleBackground.stopRippleAnimation();
                    	//Toast.makeText(MainActivity.this, "未搜索到指定设备！", Toast.LENGTH_SHORT).show();
                    }
                },SCAN_PERIOD);                
            }
        });
        
        //ble
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void foundDevice(){
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(400);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        ArrayList<Animator> animatorList=new ArrayList<Animator>();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleX", 0f, 1.2f, 1f);
        animatorList.add(scaleXAnimator);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(foundDevice, "ScaleY", 0f, 1.2f, 1f);
        animatorList.add(scaleYAnimator);
        animatorSet.playTogether(animatorList);
        foundDevice.setVisibility(View.VISIBLE);
        animatorSet.start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
    
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }
    
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(device.getAddress().equals("B4:99:4C:52:E8:5C")){
                    	final Intent intent = new Intent(MainActivity.this, PulseActivity.class);
                        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                        if (mScanning) {
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            mScanning = false;
                        }
                        startActivity(intent);
                        rippleBackground.stopRippleAnimation();
                    }
                }
            });
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, Menu.NONE, 1, "查询历史记录");  
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) { 
        switch(item.getItemId()){ 
            case Menu.NONE:  
            	Intent intent = new Intent(MainActivity.this,HistoryListActivity.class);
            	startActivity(intent);
                break; 
     
            default: break; 
        } 
        return false;//false表示继续传递到父类处理 
    } 
}
