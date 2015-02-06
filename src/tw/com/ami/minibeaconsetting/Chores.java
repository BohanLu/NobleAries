/*
 * Copyright (C) 2015 American Megatrends, Inc. AMI
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
 *      
 * @author Bohan Lu
 *      
 */
package tw.com.ami.minibeaconsetting;

import java.util.concurrent.atomic.AtomicInteger;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public class Chores {
	
	private static final String TAG = MainActivity.TAG + "-Chores";
	private static final boolean D = MainActivity.D;

	private boolean isBleSupported = false;
	private boolean isLollipopApi = (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH);
	private BluetoothManager mBluetoothManager = null;
	private BluetoothAdapter mAdapter;
	private Object mLeScanner = null;
	private Object mScanCallback = null;	
	private Context mContext;
	private ConnectionBle mConnection = null;
	private AtomicInteger aiDeviceType;
	
	// Add support device type here
	protected static final int DEVICE_TYPE_REDBEAR_BLE_MINI = 0;	

	public Chores(Context context){
		
		mContext = context;
		isBleSupported = mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
		aiDeviceType = new AtomicInteger(DEVICE_TYPE_REDBEAR_BLE_MINI);
		
		if (isBleSupported){
			initialize();
		} else {
			Log.e(TAG, "No Bluetooth low energy support ???");
		}	
	}
	
	
	private boolean initialize(){
		
		/* 
		 * For API level 18 and above, get a reference to BluetoothAdapter
		 * through
		 * BluetoothManager.
		 */
		if (null == mBluetoothManager){
			mBluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
			
			if (mBluetoothManager == null) {
				Log.e(TAG, "initialize() : Unable to initialize BluetoothManager.");
				return false;
			}
		}
		mAdapter = mBluetoothManager.getAdapter();
		
		if (mAdapter == null) {
			Log.e(TAG, "initialize() : Unable to obtain a BluetoothAdapter.");
			return false;
		}
		
		if (isLollipopApi){
			mScanCallback = new ScanCallback(){
				
				@Override
				public void onScanFailed(int errorCode){
					Log.e(TAG, "onScanFailed() : Error = " + errorCode);
				}
				
				@Override
				public void onScanResult(int callbackType, ScanResult result){
					
					if (null != result){
						BluetoothDevice device = result.getDevice();
						
						if (null != device){
							onDeviceFound(device,  device.getName(), device.getAddress(), result.getRssi(), device.getType());
						} else 
							Log.e(TAG, "onScanResult() : Cannot get BluetoothDevice !!!");
					} else {
						Log.e(TAG, "onScanResult() : Cannot get ScanResult !!!");
					}					
				}
			};
		}
		
		return true;
	}
	
	
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback(){

		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {			
			onDeviceFound(device,  device.getName(), device.getAddress(), rssi, device.getType());
		}
		
	};
	
	
	private final BroadcastReceiver mBR = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			
			if (D) Log.d(TAG, "onReceive() : Action = " + action);
			
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
				int prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE , BluetoothAdapter.ERROR);
				int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				
				if (BluetoothAdapter.STATE_ON == newState){					
					((MainActivity)mContext).runOnUiThread(new UiRunnable(){

						@Override
						public void run() {
							((MainActivity)mContext).onBluetoothTurnOn();
						}
						
					});	
					startScan();
				} else if (BluetoothAdapter.STATE_OFF == newState){
					((MainActivity)mContext).runOnUiThread(new UiRunnable(){

						@Override
						public void run() {
							((MainActivity)mContext).onBluetoothTurnOff();
						}
						
					});	
				}
			}  
			else if(action.equals(BluetoothDevice.ACTION_FOUND)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
				onDeviceFound(device,  device.getName(), device.getAddress(), rssi, device.getType());
			}
			else {
				Log.e(TAG, "onReceive() : Unknown msg : " + action);
			}
		}
		
	};
	
	
	private boolean startDiscoveringBle(){
		
		if (D) Log.d(TAG, "+++ startDiscoveringBle() +++");
		
		if (mAdapter.isDiscovering()){
			Log.i(TAG, "startDiscoveringBle() : Already in classic discovering mode");
			return true;
		}
		
		if (isLollipopApi){
			Log.i(TAG, "startDiscoveringBle() : Choose startScan()");
			mLeScanner = mAdapter.getBluetoothLeScanner();
			
			if (null != mLeScanner){
				((BluetoothLeScanner)mLeScanner).startScan((ScanCallback)mScanCallback);
				return true;
			}
			
			// TODO
			// return mAdapter.startScan(mScanCallback); ???
		} else {
			Log.i(TAG, "startDiscoveringBle() : Choose startLeScan()");
			return mAdapter.startLeScan(mLeScanCallback);
		}
		return true;
	}
	
	
	private boolean cancelDiscoveringBle(){
		
		if (D) Log.d(TAG, "+++ cancelDiscoveringBle() +++");
		
		if (mAdapter.isDiscovering()){
			Log.i(TAG, "cancelDiscoveringBle() : In classic discovering mode");
			return false;
		}
		
		if (isLollipopApi){
			Log.i(TAG, "cancelDiscoveringBle() : Choose stopScan()");
			
			if (null != mLeScanner){
				((BluetoothLeScanner)mLeScanner).stopScan((ScanCallback)mScanCallback);
				return true;
			}
			
			// TODO
			// return mAdapter.stopScan(mScanCallback); ???
		} else {
			Log.i(TAG, "cancelDiscoveringBle() : Choose stopLeScan()");
			mAdapter.stopLeScan(mLeScanCallback);
		}
		return true;
	}

	
	private abstract class UiRunnable implements Runnable {
		
		protected BluetoothDevice device = null;
		protected String name = null, address = null;
		protected int type = -1, rssi = -1;
		protected byte[] data = null;		
		
		public UiRunnable() {
			
		}
		
		// For device found
		public UiRunnable(BluetoothDevice device, String name, String address, int rssi, int type) {

			this.device = device;
			this.name = name;
			this.address = address;
			this.rssi = rssi;
			this.type = type;
		}
		
		// For data received
		public UiRunnable(BluetoothDevice device, int type, byte[] data) {

			this.device = device;
			this.data = data;
			this.type = type;
		}	
	}
	
	
	private void onDeviceFound(BluetoothDevice device, String name, String address, int rssi, int type){
		
		((MainActivity)mContext).runOnUiThread(new UiRunnable(device, name, address, rssi, type){

			@Override
			public void run() {
				((MainActivity)mContext).onDeviceFound(device, name, address, rssi, type);
			}			
		});	
	}
	
	
	private ConnectionBle createConnectionByDeviceType(BluetoothDevice device){
		
		ConnectionBle tmp = null;
		int type = aiDeviceType.get();
		
		switch (type){
		case DEVICE_TYPE_REDBEAR_BLE_MINI:
			tmp = new RedBearBleMini(this, mAdapter, mBluetoothManager, device);
			break;
		// TODO : Add support device type here
		default :
			Log.e(TAG, "createConnectionByDeviceType() : Unknown device type = " + type);
		}
		return tmp;
	}
	
	
	protected void onConnectionBleEstablished(ConnectionBle connection){
		
		if (null == mConnection){
			mConnection = connection;
			BluetoothDevice device = mConnection.getDevice();
			((MainActivity)mContext).runOnUiThread(new UiRunnable(device, device.getName(), device.getAddress(), -1, device.getType()){

				@Override
				public void run() {
					((MainActivity)mContext).onConnectionBleEstablished(device, name, address, rssi, type);
				}
				
			});	
		} else {
			Log.e(TAG, "onConnectionBleEstablished() : mConnection is not null");
		}		
	}
	
	
	protected void onConnectionBleDisconnected(ConnectionBle connection){
		
		if (null != mConnection){
			
			if (connection.equals(mConnection)){				
				BluetoothDevice device = mConnection.getDevice();				
				mConnection.releaseResource();
				mConnection = null;
				((MainActivity)mContext).runOnUiThread(new UiRunnable(device, device.getName(), device.getAddress(), -1, device.getType()){

					@Override
					public void run() {
						((MainActivity)mContext).onConnectionBleDisconnected(device, name, address, rssi, type);
					}					
				});	
			} else {
				Log.e(TAG, "onConnectionBleDisconnected() : mConnection not matched");
			}
		} else {
			Log.e(TAG, "onConnectionBleDisconnected() : mConnection is null");
		}		
	}
	
	
	protected void onRssiRead(BluetoothDevice device, int rssi, int type){		
		// TODO
	}
	
	
	protected void onDataAvailable(BluetoothDevice device, int whichEvent, byte[] data){
		
		((MainActivity)mContext).runOnUiThread(new UiRunnable(device, whichEvent, data){

			@Override
			public void run() {
				((MainActivity)mContext).onDataAvailable(device, type, data);
			}			
		});	
	}
	
	
	protected Context getContext(){
		return mContext;
	}
	
	
	protected boolean updateStatus(){
		
		if (isBleSupported){
			
			if (mAdapter.isEnabled()){
				((MainActivity)mContext).onBluetoothTurnOn();
				startScan();
			} else {
				((MainActivity)mContext).onBluetoothTurnOff();
				
				if (BluetoothAdapter.STATE_OFF == mAdapter.getState())
					mAdapter.enable();
			}
			return true;
		} else {
			Log.e(TAG, "updateStatus() : No Bluetooth low energy support ???");
			return false;
		}
	}
	
	
	protected void register(){
		
		if (D) Log.d(TAG, "+++ register +++");
		
		if (isBleSupported){
			IntentFilter filter = new IntentFilter();
		    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		    filter.addAction(BluetoothDevice.ACTION_FOUND);
		    mContext.registerReceiver(mBR, filter);
		} else {
			Log.e(TAG, "register() : No Bluetooth low energy support ???");
		}
	}
	
	
	protected void unregister(){
		
		if (D) Log.d(TAG, "+++ register +++");
		
		if (isBleSupported){
			mContext.unregisterReceiver(mBR);
		} else {
			Log.e(TAG, "unregister() : No Bluetooth low energy support ???");
		}		
	}

	
	protected boolean startScan(){
		
		if (isBleSupported){
			return startDiscoveringBle();
		} else {
			Log.e(TAG, "startScan() : No Bluetooth low energy support ???");
			return false;
		}	
	}
	
	
	protected boolean stopScan(){
		
		if (isBleSupported){
			return cancelDiscoveringBle();
		} else {
			Log.e(TAG, "stopScan() : No Bluetooth low energy support ???");
			return false;
		}
	}
	
	
	protected boolean connect(BluetoothDevice device){
		
		if (D) Log.d(TAG, "+++ connect +++");
		
		if (isBleSupported){
			stopScan();
			ConnectionBle tmp = createConnectionByDeviceType(device);
			
			if (null == tmp){
				Log.e(TAG, "connect() : Cannot get connection object !");
				return false;
			}
			return tmp.connect();
		} else {
			Log.e(TAG, "connect() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean disconnect(BluetoothDevice device){
		
		if (isBleSupported){
			
			if (null != mConnection){
				
				if (device.equals(mConnection.getDevice())){				
					mConnection.disconnect();
					startScan();
					return true;
				} else 
					Log.e(TAG, "disconnect() : Device not matched");
			} else {
				Log.e(TAG, "disconnect() : mConnection is null");
			} 
		} else {
			Log.e(TAG, "disconnect() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setCompanyUuid(byte[] uuid){

		if (isBleSupported){
			
			if (null != mConnection){
				return mConnection.setCompanyUuid(uuid);
			} else {
				Log.e(TAG, "setCompanyUuid() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setCompanyUuid() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setMajorId(byte[] id){

		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setMajorId(id);
			} else {
				Log.e(TAG, "setMajorId() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setMajorId() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setMinorId(byte[] id){

		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setMinorId(id);
			} else {
				Log.e(TAG, "setMinorId() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setMinorId() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setMeasuredPower(byte[] value){

		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setMeasuredPower(value);
			} else {
				Log.e(TAG, "setMeasuredPower() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setMeasuredPower() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setLed(byte[] value){

		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setLed(value);
			} else {
				Log.e(TAG, "setLed() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setLed() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setAdvInterval(byte[] value){
		
		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setAdvInterval(value);
			} else {
				Log.e(TAG, "setAdvInterval() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setAdvInterval() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected boolean setOutputPower(byte[] value){

		if (isBleSupported){

			if (null != mConnection){
				return mConnection.setOutputPower(value);
			} else {
				Log.e(TAG, "setOutputPower() : mConnection is null");
			}
		} else {
			Log.e(TAG, "setOutputPower() : No Bluetooth low energy support ???");
		}
		return false;
	}
	
	
	protected void setDeviceType(int value){

		if (isBleSupported){

			if (null != aiDeviceType){
				aiDeviceType.set(value);
			} else {
				Log.e(TAG, "setOutputPower() : aiDeviceType is null");
			}
		} else {
			Log.e(TAG, "setOutputPower() : No Bluetooth low energy support ???");
		}
		return;
	}

}
