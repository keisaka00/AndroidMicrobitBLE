package jp.kichiro.androidmicrobitble;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity
{
    private static final int                   PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public static final String                 PRESS_TO_START_SCANNING = "Press to start scanning";
    public static final String                 PRESS_TO_CONNECT_TO_BBC_MICRO_BIT = "Press to connect to BBC micro:bit";
    private              BluetoothAdapter      mBtAdapter;
	private              BluetoothLeScanner    mBtScanner;
	private              MyScancallback        mBtScancallback;
	private              BluetoothDevice       mBleDevice_Microbit;
	private              BluetoothGatt         mGatt;
	private              BluetoothGattCallback mGattCallback;
	private              BluetoothGattService  mGattService;

	private final String ACCEL_SERVICE     = "e95d0753-251d-470a-a062-fa1922dfa9a8";
	private final String ACCEL_DATA        = "e95dca4b-251d-470a-a062-fa1922dfa9a8";
	private final String ACCEL_PERIOD      = "e95dfb24-251d-470a-a062-fa1922dfa9a8";
	private final String NOTIFY_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

	private boolean mScanned = false;   // ?????????????????????????????????????????????

	private final int PERMISSION_REQUEST = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//BLE??????????????????????????????????????????????????????????????????????????????????????????????????????
		if (! getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}

		// Android 6.0??????????????????????????????????????????????????????????????????
		// Wi-Fi???BLE???????????????????????????????????????????????????????????????????????????
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			// Android10?????????Bluetooth???Wi-Fi??????????????????
			// ACCESS_FINE_LOCATION?????????????????????????????????????????????????????????????????????
			if (this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
			}
		}

		//Bluetooth?????????????????????????????????
		BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = manager.getAdapter();

		//bluetooth??????????????????????????????????????????????????????????????????
		if (mBtAdapter == null || ! mBtAdapter.isEnabled()) {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, PERMISSION_REQUEST);
		}
		else {
			Button button = findViewById(R.id.button_connect);
			button.setText(PRESS_TO_START_SCANNING);
			button.setEnabled(true);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PERMISSION_REQUEST) {
			Button button = findViewById(R.id.button_connect);
			button.setEnabled(true);
		}
	}

	private       Handler handler;
	private final int     SCAN_PERIOD = 10000;

	//
	// BLE?????????????????????????????????
	//
	class MyScancallback extends ScanCallback
	{
		@Override
		public void onScanResult(int callbackType, ScanResult result)
		{
			if (mScanned == true) {
				// ?????????????????????????????? microbit????????????????????????????????????
                Log.i("scanResult", "Already implemented");
				return;
			}
			if (result.getDevice() == null) {
			    // ?????????????????????????????????
                Log.i("scanResult", "Could not get the device");
				return;
			}
			if (result.getDevice().getName() == null) {
                // ????????????????????????????????????
                Log.i("scanResult", "Could not get the device name");
			    return;
            }
			//Log.d("scanResult", result.getDevice().getAddress() + " - " + result.getDevice().getName());
			if (result.getDevice().getName().contains("BBC micro:bit")) {
                Log.i("scanResult", "Found a target ==> BBC micro:bit");

				//BLE?????????????????????
                mBleDevice_Microbit = result.getDevice();
                mScanned            = true;

				//UI????????????????????????????????????
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						Button button = findViewById(R.id.button_connect);
						button.setText(PRESS_TO_CONNECT_TO_BBC_MICRO_BIT);
						button.setEnabled(true);
					}
				});
				//??????????????????
				mBtScanner.stopScan(mBtScancallback);
				return;
			}
			else {
                Log.i("scanResult", "Could not find the target ==> BBC micro:bit");
            }
		}

		@Override
		public void onBatchScanResults(List<ScanResult> results)
		{
		}

		@Override
		public void onScanFailed(int errorCode)
		{
		}
	}

	//
    // ??????????????????????????????????????????
    //
	class MyGattcallback extends BluetoothGattCallback
	{
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
		{
			Log.d("GATT", "connection changed");
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				gatt.discoverServices();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status)
		{
			mGatt = gatt;
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.d("GATT", "services discover success");
				List<BluetoothGattService> list = gatt.getServices();
				for (BluetoothGattService service : list) {
					//??????????????????????????????
					if (service.getUuid().toString().equals(ACCEL_SERVICE)) {
                        mGattService = service;
						//???????????????
						BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(ACCEL_PERIOD));
						characteristic.setValue(160, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
						//Descriptor?????????
						characteristic = service.getCharacteristic(UUID.fromString(ACCEL_DATA));
						mGatt.setCharacteristicNotification(characteristic, true);
						BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(NOTIFY_DESCRIPTOR));
						descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
						mGatt.writeDescriptor(descriptor);
					}
				}
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
		{
            Log.d("GATT", "Characteristic Changed");
			if (characteristic.getUuid().toString().equals(ACCEL_DATA)) {
				byte[] t = characteristic.getValue();
				Log.d("GATT", String.format("%2d : %02X %02X %02X %02X %02X %02X", t.length, t[0], t[1], t[2], t[3], t[4], t[5]));
				final int x = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
				final int y = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 2);
				final int z = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 4);

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						TextView textView;
						textView = findViewById(R.id.accel_x);
						textView.setText("?????????X:" + x);
						textView = findViewById(R.id.accel_y);
						textView.setText("?????????Y:" + y);
						textView = findViewById(R.id.accel_z);
						textView.setText("?????????Z:" + z);
					}
				});
			}
		}
	}

	public void pushConnect(View view)
	{
		Button button = (Button) view;
		if (button.getText().equals(PRESS_TO_START_SCANNING)) {
            mBtScanner      = mBtAdapter.getBluetoothLeScanner();
            mBtScancallback = new MyScancallback();

			//?????????????????????10???????????????
			handler = new Handler();
			handler.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					mBtScanner.stopScan(mBtScancallback);
				}
			}, SCAN_PERIOD);
			//?????????????????????
			mBtScanner.startScan(mBtScancallback);
		}
		else if (button.getText().equals(PRESS_TO_CONNECT_TO_BBC_MICRO_BIT)) {
			if (mBleDevice_Microbit != null) {
                mGattCallback = new MyGattcallback();
				mBleDevice_Microbit.connectGatt(this, false, mGattCallback);
			}
		}
	}
}