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

import java.util.Arrays;
import java.util.UUID;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.util.Log;

public class RedBearBleMini extends ConnectionBle {
	
	private static final String TAG = MainActivity.TAG + "-RedBearBleMini";
	
	private static final String CLIENT_CHARACTERISTIC_CONFIG 	= "00002902-0000-1000-8000-00805f9b34fb";
	
	private static final String SERVICE_BEACON_SETTINGS 		= "B0702880-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_UUID 			= "B0702881-A295-A8AB-F734-031A98A512DE";	
	private static final String CHARACTERISTIC_MAJOR_ID 		= "B0702882-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_MINOR_ID 		= "B0702883-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_MEASURED_POWER 	= "B0702884-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_LED_SWITCH 		= "B0702885-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_ADV_INTERVAL		= "B0702886-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_OUTPUT_POWER		= "B0702887-A295-A8AB-F734-031A98A512DE";
	private static final String CHARACTERISTIC_FW_VERSION		= "B0702888-A295-A8AB-F734-031A98A512DE";

	private static final UUID UUID_SERVICE_BEACON_SETTINGS 		= UUID.fromString(SERVICE_BEACON_SETTINGS);
	private static final UUID UUID_CHARACTERISTIC_UUID			= UUID.fromString(CHARACTERISTIC_UUID);
	private static final UUID UUID_CHARACTERISTIC_MAJOR_ID		= UUID.fromString(CHARACTERISTIC_MAJOR_ID);
	private static final UUID UUID_CHARACTERISTIC_MINOR_ID		= UUID.fromString(CHARACTERISTIC_MINOR_ID);
	private static final UUID UUID_CHARACTERISTIC_MEASURED_POWER= UUID.fromString(CHARACTERISTIC_MEASURED_POWER);
	private static final UUID UUID_CHARACTERISTIC_LED_SWITCH	= UUID.fromString(CHARACTERISTIC_LED_SWITCH);
	private static final UUID UUID_CHARACTERISTIC_ADV_INTERVAL	= UUID.fromString(CHARACTERISTIC_ADV_INTERVAL);
	private static final UUID UUID_CHARACTERISTIC_OUTPUT_POWER	= UUID.fromString(CHARACTERISTIC_OUTPUT_POWER);
	private static final UUID UUID_CHARACTERISTIC_FW_VERSION	= UUID.fromString(CHARACTERISTIC_FW_VERSION);
	
	private static final UUID[] UUIDS = new UUID[] {UUID_SERVICE_BEACON_SETTINGS, UUID_CHARACTERISTIC_UUID,
													UUID_CHARACTERISTIC_MAJOR_ID, UUID_CHARACTERISTIC_MINOR_ID,
													UUID_CHARACTERISTIC_MEASURED_POWER, UUID_CHARACTERISTIC_LED_SWITCH,
													UUID_CHARACTERISTIC_ADV_INTERVAL, UUID_CHARACTERISTIC_OUTPUT_POWER,
													UUID_CHARACTERISTIC_FW_VERSION};
	
	protected static final int TYPE_CHARACTERISTIC_UUID 			= 1;	
	protected static final int TYPE_CHARACTERISTIC_MAJOR_ID 		= 2;
	protected static final int TYPE_CHARACTERISTIC_MINOR_ID 		= 3;
	protected static final int TYPE_CHARACTERISTIC_MEASURED_POWER 	= 4;
	protected static final int TYPE_CHARACTERISTIC_LED_SWITCH 		= 5;
	protected static final int TYPE_CHARACTERISTIC_ADV_INTERVAL		= 6;
	protected static final int TYPE_CHARACTERISTIC_OUTPUT_POWER		= 7;
	protected static final int TYPE_CHARACTERISTIC_FW_VERSION		= 8;
	protected static final int TYPE_CHARACTERISTIC_NULL				= UUIDS.length;
	
	private volatile int updateAllStatusIndex = TYPE_CHARACTERISTIC_UUID;	
	
	public RedBearBleMini(Chores parent, BluetoothAdapter adapter, BluetoothManager manager, BluetoothDevice device){		
		super(parent, adapter, manager, device);
	}
	
	
	@Override
	protected int getTypeByGattCharacteristic(BluetoothGattCharacteristic characteristic){
		
		if (null == characteristic){
			Log.e(TAG, "getTypeByGattCharacteristic() : characteristic is null");
			return -1;
		}		
		UUID u = characteristic.getUuid();
		
		if (D) Log.d(TAG, "getTypeByGattCharacteristic() : Data from Characteristic with UUID = " + characteristic.getUuid());
		
		return Arrays.asList(UUIDS).indexOf(u);
	}
	
	
	@Override
	protected BluetoothGattCharacteristic getGattCharacteristicByType(int which){
		
		if (which > UUIDS.length){
			Log.e(TAG, "getGattCharacteristicByType() : Cannot get corresponding UUID ! Index = " + which);
			return null;
		}
		return mMap.get(UUIDS[which]);
	}
	
	
	@Override	
	protected void setupService(BluetoothGattService gattService){		
		
		for (int i = 1; i < UUIDS.length; i++){
			BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(UUIDS[i]);		
			mMap.put(characteristic.getUuid(), characteristic);	
		}
		
		if (D) Log.d(TAG, "setupService() : " + mMap.size() + " characteristics added to HashMap");

		readNextCharacteristic(false);
	}
	

	@Override	
	protected boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
		
		if (mAdapter == null || mGatt == null) {
			Log.e(TAG, "setCharacteristicNotification() : BluetoothAdapter not initialized");
			return false;
		}
		boolean result = mGatt.setCharacteristicNotification(characteristic, enabled);
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		result &= mGatt.writeDescriptor(descriptor);		
		return result;
	}
	
	
	@Override
	protected synchronized void readNextCharacteristic(boolean isSuccess){
		
		if (isSuccess){
			
			if (++updateAllStatusIndex >= TYPE_CHARACTERISTIC_NULL){
				Log.i(TAG, "readNextCharacteristic() : Got final Characteristic ! Updating has completed !");
				isUpdatingAllStatusOngoing = false;
				return;
			}
		}
		BluetoothGattCharacteristic c = mMap.get(UUIDS[updateAllStatusIndex]);
		readCharacteristic(c);
	}
	
	
	@Override
	protected BluetoothGattService getSpecificGattService(){
		return super.getSpecificGattService(UUID_SERVICE_BEACON_SETTINGS);
	}


	@Override
	boolean setCompanyUuid(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_UUID);
		
		if (null == c){
			Log.e(TAG, "setCompanyUuid() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setMajorId(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_MAJOR_ID);
		
		if (null == c){
			Log.e(TAG, "setMajorId() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setMinorId(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_MINOR_ID);
		
		if (null == c){
			Log.e(TAG, "setMinorId() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setMeasuredPower(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_MEASURED_POWER);
		
		if (null == c){
			Log.e(TAG, "setMeasuredPower() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setLed(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_LED_SWITCH);
		
		if (null == c){
			Log.e(TAG, "setLed() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setAdvInterval(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_ADV_INTERVAL);
		
		if (null == c){
			Log.e(TAG, "setAdvInterval() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}


	@Override
	boolean setOutputPower(byte[] data) {
		
		BluetoothGattCharacteristic c = mMap.get(UUID_CHARACTERISTIC_OUTPUT_POWER);
		
		if (null == c){
			Log.e(TAG, "setOutputPower() : Cannot get characteristic from HashMap !");
			return false;
		}		
		return sendData(c, data);
	}	
}
