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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

public abstract class ConnectionBle {
	
	protected static final String TAG = MainActivity.TAG + "-CBle";
	protected static final boolean D = MainActivity.D;
	
	protected Chores mChores;
	protected BluetoothManager mBluetoothManager = null;
	protected BluetoothAdapter mAdapter = null;
	protected BluetoothDevice mDevice = null;
	protected BluetoothGatt mGatt = null;
	protected Map<UUID, BluetoothGattCharacteristic> mMap = new HashMap<UUID, BluetoothGattCharacteristic>();
	protected volatile boolean isUpdatingAllStatusOngoing = true;
	
	abstract void setupService(BluetoothGattService gattService);
	abstract boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled);
	abstract BluetoothGattService getSpecificGattService();
	abstract int getTypeByGattCharacteristic(BluetoothGattCharacteristic characteristic);
	abstract BluetoothGattCharacteristic getGattCharacteristicByType(int which);
	abstract void readNextCharacteristic(boolean isSuccess);
	abstract boolean setCompanyUuid(byte[] data);
	abstract boolean setMajorId(byte[] data);
	abstract boolean setMinorId(byte[] data);
	abstract boolean setMeasuredPower(byte[] data);
	abstract boolean setLed(byte[] data);
	abstract boolean setAdvInterval(byte[] data);
	abstract boolean setOutputPower(byte[] data);
	
	public ConnectionBle(Chores parent, BluetoothAdapter adapter, BluetoothManager manager, BluetoothDevice device){
		
		mChores = parent;
		mDevice = device;
		mAdapter = adapter;
		mBluetoothManager = manager;
	}
	
	
	protected BluetoothGattService getSpecificGattService(UUID uuid) {
		
		if (mGatt == null)
			return null;

		return mGatt.getService(uuid);
	}

	
	protected byte[] extractData(BluetoothGattCharacteristic characteristic){

		if (D){ 
			Log.d(TAG, "extractData() : int FORMAT_UINT8 = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
			Log.d(TAG, "extractData() : int FORMAT_UINT16 = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1));
			Log.d(TAG, "extractData() : int FORMAT_UINT32 = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1));
		}		
		// Leave converting work to UI. We just report/write raw data honestly.
		byte[] rx = characteristic.getValue();
		return rx;
	}
	
	
	protected boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
		
		if (mAdapter == null || mGatt == null) {
			Log.e(TAG, "writeCharacteristic() : BluetoothAdapter not initialized");
			return false;
		}
		
		if (D) Log.d(TAG, "writeCharacteristic() : Try to write characteristic...");
		
		return mGatt.writeCharacteristic(characteristic);
	}
	
	
	protected boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
		
		if (mAdapter == null || mGatt == null) {
			Log.e(TAG, "readCharacteristic() : BluetoothAdapter not initialized");
			return false;
		}
		
		if (D) Log.d(TAG, "readCharacteristic() : Read characteristic with UUID = " + characteristic.getUuid());
		
		if (mGatt.readCharacteristic(characteristic))
			return true;
		else 
			Log.e(TAG, "readCharacteristic() : Failed to read characteristic with UUID = " + characteristic.getUuid());
		
		return false;
	}
	
	
	protected boolean sendData(BluetoothGattCharacteristic characteristic, byte[] data){
		
		if (mAdapter == null || mGatt == null) {
			Log.e(TAG, "sendData() : BluetoothAdapter not initialized");
			return false;
		}		
		isUpdatingAllStatusOngoing = false;
		
		if (null == characteristic){
			Log.e(TAG, "sendData() : characteristic is null");
			return false;
		}
		characteristic.setValue(data);
		return writeCharacteristic(characteristic);
	}
	
	
	protected void onDataAvailable(BluetoothGattCharacteristic characteristic){
		mChores.onDataAvailable(mDevice, getTypeByGattCharacteristic(characteristic), extractData(characteristic));
	}
	
	
	protected final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
		
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

			if (BluetoothProfile.STATE_CONNECTED == newState) {

				if (D){
					Log.d(TAG, "onConnectionStateChange() : Connected to GATT server.");
				
					// Attempts to discover services after successful connection.
					Log.d(TAG, "onConnectionStateChange() : Attempting to start service discovery:");
				}				
				mChores.onConnectionBleEstablished(ConnectionBle.this);
				
				if (!mGatt.discoverServices())
					Log.e(TAG, "onConnectionStateChange() : Error on discoverServices()");				
			} else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
				
				if (D) Log.d(TAG, "onConnectionStateChange() : Disconnected from GATT server.");
				// TODO releaseResource()
				mChores.onConnectionBleDisconnected(ConnectionBle.this);				
			}
		}
		
		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			
			if (status == BluetoothGatt.GATT_SUCCESS) {
				mChores.onRssiRead(mDevice, rssi, mDevice.getType());
			} else {
				Log.e(TAG, "onReadRemoteRssi() : Error status = " + status);
			}
		};

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			
			if (status == BluetoothGatt.GATT_SUCCESS) {
				
				if (D) {
					List<BluetoothGattService> mServiceList = gatt.getServices();
					
					if (null != mServiceList){
						
						for (BluetoothGattService s : mServiceList){
							Log.i(TAG, "onServicesDiscovered() : Service UUID = " + s.getUuid());
							List<BluetoothGattCharacteristic> mCharList = s.getCharacteristics();
						
							if (null != mCharList){
								
								for (BluetoothGattCharacteristic c : mCharList){
									Log.i(TAG, "    Characteristic UUID = " + c.getUuid());
								}
							} else {
								Log.e(TAG, "onServicesDiscovered() : mCharList is null !!!");
							}
						}						
					} else {
						Log.e(TAG, "onServicesDiscovered() : mServiceList is null !!!");
					}
				}				
				BluetoothGattService gattService = getSpecificGattService();

				if (null != gattService){
					setupService(gattService);
				} else {
					Log.i(TAG, "onServicesDiscovered() : No interested service ! Disconnect !");
					disconnect();
				}
			} else {
				Log.e(TAG, "onServicesDiscovered() : Error status = " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			
			if (status == BluetoothGatt.GATT_SUCCESS) {
				onDataAvailable(characteristic);
			} else {
				Log.e(TAG, "onCharacteristicRead() : Error status = " + status);
			}
			
			if (isUpdatingAllStatusOngoing){
				readNextCharacteristic(status == BluetoothGatt.GATT_SUCCESS);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			onDataAvailable(characteristic);
		}
		
		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
			
			if (status == BluetoothGatt.GATT_SUCCESS) {
				Log.e(TAG, "onCharacteristicWrite() : Succeeded to write characteristic with UUID = " + characteristic.getUuid());
			} else {
				Log.e(TAG, "onCharacteristicWrite() : Failed to write characteristic with UUID = " + characteristic.getUuid() + " Error status = " + status);
			}
		}
		
	};
	

	protected boolean connect() {
		
		if (null == mAdapter) {
			Log.e(TAG, "connect() : BluetoothAdapter not initialized or unspecified address.");
			return false;
		}
		mGatt = mDevice.connectGatt(mChores.getContext(), false, mGattCallback);
		
		if (D) Log.d(TAG, "connect() : Trying to create a new connection.");
		return true;
	}


	protected void disconnect() {
		
		if (null == mAdapter || null == mGatt) {
			Log.e(TAG, "disconnect() : BluetoothAdapter not initialized");
			return;
		}
		mGatt.disconnect();
	}


	protected void releaseResource() {
		
		if (mGatt == null) {
			return;
		}
		mGatt.close();
		mGatt = null;
		
		if (null != mMap){
			mMap.clear();
			mMap = null;
		}
	}

	
	protected boolean readRssi() {
		
		if (mAdapter == null || mGatt == null) {
			Log.e(TAG, "readRssi() : BluetoothAdapter not initialized");
			return false;
		}
		return mGatt.readRemoteRssi();
	}


	protected BluetoothDevice getDevice(){
		return mDevice;
	}	
}
