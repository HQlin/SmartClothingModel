package com.skyfishjy.ripplebackground.sample;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.DeviceControlActivity;
import com.example.android.bluetoothlegatt.R;
import com.example.android.bluetoothlegatt.SampleGattAttributes;
import com.lin.dbhelper.DBManager;
import com.lin.dbhelper.Pulse;

import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryListActivity extends ListActivity {

	private static final String TAG = "HistoryListActivity";

	// ble
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

	private boolean mUpdating = false;
	private int index = 0;
	private HistoryAdapter mHistoryAdapter;

	private DBManager mgr;
	private List<Pulse> pulses = new ArrayList<Pulse>();
	String udateString = null;
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
			// Automatically connects to the device upon successful start-up
			// initialization.
			mBluetoothLeService.connect(mDeviceAddress);
			displayGattServices(mBluetoothLeService.getSupportedGattServices());
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
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				finish();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				displayGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				if (intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1) == 1) {
					System.out.println(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
					String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA).split("\n")[0];
					System.out.println(data);

					if (data.contains("finish")) {
						mUpdating = false;
						invalidateOptionsMenu();
						mgr.add(pulses);
						pulses.clear();
						DisplayListTask task = new DisplayListTask(HistoryListActivity.this);
						task.execute();
					}

					if (data.contains(",")) {
						String[] datas = data.split(",");
						String dateTime = changeDateTime(datas[0], 0);
						System.out.println("dateTime:" + datas[1]);
						Pulse pulse = new Pulse(datas[1], dateTime);
						pulses.add(pulse);
					}

					if (pulses.size() == 50) {
						mgr.add(pulses);
						pulses.clear();
						DisplayListTask task = new DisplayListTask(HistoryListActivity.this);
						task.execute();
					}
					if (mBluetoothLeService != null) {
						mBluetoothLeService.getRssi();
					}
				} else {
					// Toast.makeText(HistoryListActivity.this, "RSSI: " +
					// intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1),
					// Toast.LENGTH_SHORT).show();
					int rssi = intent.getIntExtra(BluetoothLeService.ACTION_RSSI, 1);
					if (rssi < -55) {
						if (!mp.isPlaying()) {
							mp.start();
						}
					} else {
						if (mp.isPlaying()) {
							mp.pause();
						}
					}
				}
			}
		}
	};

	/**
	 * mode == 0 : 20110101112233->2011-01-01 11:22:33 mode == 1 : 2011-01-01
	 * 11:22:33->20110101112233
	 * 
	 * @param s
	 * @param mode
	 * @return
	 */
	private String changeDateTime(String s, int mode) {
		String dateTime = null;
		if (mode == 0) {
			dateTime = s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8) + " " + s.substring(8, 10)
					+ ":" + s.substring(10, 12) + ":" + s.substring(12, 14);
		} else if (mode == 1) {
			dateTime = s.substring(0, 4) + s.substring(5, 7) + s.substring(8, 10) + s.substring(11, 13)
					+ s.substring(14, 16) + s.substring(17, 19);
		}
		return dateTime;
	}

	// Demonstrates how to iterate through the supported GATT
	// Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the
	// ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);

				if (uuid.equals("0000fff1-0000-1000-8000-00805f9b34fb")) {
					mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
					Toast.makeText(HistoryListActivity.this, "连接成功!", Toast.LENGTH_SHORT).show();
				}

				if (uuid.equals("0000fff2-0000-1000-8000-00805f9b34fb")) {
					mWriteCharacteristic = gattCharacteristic;
				}
			}
		}
	}

	// dataMode = 0:data is string
	// dataMode = 1:data is hex
	private void bleSend(String data, int dataMode) {
		byte[] value = new byte[20];
		value[0] = (byte) 0x00;
		if (data.length() > 0 && dataMode == 0) {
			// write string
			WriteBytes = data.getBytes();
		} else if (data.length() > 0 && dataMode == 1) {
			WriteBytes = hex2byte(data.getBytes());
		}
		mWriteCharacteristic.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
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
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		// ble
		final Intent intent = getIntent();
		if (intent != null) {
			mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
			mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
			Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
			bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
			registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		}

		// Initializes list view adapter.
		mHistoryAdapter = new HistoryAdapter(HistoryListActivity.this);
		setListAdapter(mHistoryAdapter);

		mgr = new DBManager(this);
		List<Pulse> pulses = mgr.query();
		if (pulses.size() != 0) {
			udateString = "update" + changeDateTime(pulses.get(pulses.size() - 1).getTime(), 1);
		} else {
			Date newDate2 = new Date(new Date().getTime() - 1 * 24 * 60 * 60 * 1000);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			udateString = "update" + simpleDateFormat.format(newDate2);
		}

		DisplayListTask task = new DisplayListTask(HistoryListActivity.this);
		task.execute();

		mp = MediaPlayer.create(this, R.raw.warning);
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

	class DisplayListTask extends AsyncTask<Void, String, String> {
		private Context context;

		DisplayListTask(Context context) {
			this.context = context;
		}

		/**
		 * 运行在UI线程中，在调用doInBackground()之前执行
		 */
		@Override
		protected void onPreExecute() {
			mHistoryAdapter.clear();
			// Toast.makeText(context,"显示历史列表",Toast.LENGTH_SHORT).show();
		}

		/**
		 * 后台运行的方法，可以运行非UI线程，可以执行耗时的方法
		 */
		@Override
		protected String doInBackground(Void... params) {
			List<Pulse> pulses = mgr.query();
			int count = pulses.size() / 50;
			while (count > 0 && pulses.size() != 0) {
				String s = null;
				if (count * 50 >= pulses.size()) {
					s = pulses.get((count - 1) * 50).getTime() + "\n~\n" + pulses.get(pulses.size() - 1).getTime();
				} else {
					s = pulses.get((count - 1) * 50).getTime() + "\n~\n" + pulses.get(count * 50).getTime();
				}
				publishProgress(s);
				count--;
			}
			return null;
		}

		/**
		 * 运行在ui线程中，在doInBackground()执行完毕后执行
		 */
		@Override
		protected void onPostExecute(String integer) {
			// Toast.makeText(context,"执行完毕",Toast.LENGTH_SHORT).show();
		}

		/**
		 * 在publishProgress()被调用以后执行，publishProgress()用于更新进度
		 */
		@Override
		protected void onProgressUpdate(String... values) {
			mHistoryAdapter.addHistory(values[0]);
			mHistoryAdapter.notifyDataSetChanged();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mDeviceName != null)
			bleSend("finish", 0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mDeviceName != null) {
			getMenuInflater().inflate(R.menu.main, menu);
			if (!mUpdating) {
				menu.findItem(R.id.menu_stop).setVisible(false);
				menu.findItem(R.id.menu_scan).setVisible(true);
				menu.findItem(R.id.menu_refresh).setActionView(null);
			} else {
				menu.findItem(R.id.menu_stop).setVisible(true);
				menu.findItem(R.id.menu_scan).setVisible(false);
				menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			bleSend(udateString, 0);
			mUpdating = true;
			invalidateOptionsMenu();
			break;
		case R.id.menu_stop:
			bleSend("finish", 0);
			mUpdating = false;
			invalidateOptionsMenu();
			break;
		}
		return true;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(HistoryListActivity.this, HistoryChatActivity.class);
		intent.putExtra(HistoryChatActivity.TITLE, mHistoryAdapter.getHistory(position));
		startActivity(intent);
	}

	private class HistoryAdapter extends BaseAdapter {
		private ArrayList<String> mHistorys;
		private LayoutInflater mInflater;// 得到一个LayoutInfalter对象用来导入布局

		/** 构造函数 */
		public HistoryAdapter(Context context) {
			super();
			this.mHistorys = new ArrayList<String>();
			this.mInflater = LayoutInflater.from(context);
		}

		public void addHistory(String mHistory) {
			if (!mHistorys.contains(mHistory)) {
				mHistorys.add(mHistory);
			}
		}

		public String getHistory(int position) {
			return mHistorys.get(position);
		}

		public void clear() {
			mHistorys.clear();
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mHistorys.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder holder;
			// 观察convertView随ListView滚动情况
			Log.v("MyListViewBase", "getView " + position + " " + convertView);
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.listitem_device, null);
				holder = new ViewHolder();
				/** 得到各个控件的对象 */
				holder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
				holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
				convertView.setTag(holder);// 绑定ViewHolder对象
			} else {
				holder = (ViewHolder) convertView.getTag();// 取出ViewHolder对象
			}
			/** 设置TextView显示的内容，即我们存放在动态数组中的数据 */
			holder.deviceName.setText(mHistorys.get(position));

			return convertView;
		}

	}

	/** 存放控件 */
	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}

}
