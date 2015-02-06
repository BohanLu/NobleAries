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

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity {
	
	protected static final String TAG = "MiniBeacon";
	protected static final boolean D = true;
	
	private Context mContext;
	private Chores mChores;
	private BluetoothAdapter mAdapter;
	private BluetoothDevice mSelectedDevice, mConnectedDevice; 
	private ToggleButton mTgBtnConnect;
	private Button mBtnSetUuid, mBtnSetMajorId, mBtnSetMinorId, mBtnSetMeasuredPower, 
					mBtnSetLed, mBtnSetAdvInterval, mBtnSetOutputPower;
	private TextView mTxVwConnectedDeviceInfo;
	private EditText mEdTxUuid, mEdTxMajorId, mEdTxMinorId, mEdTxMeasuredPower, mEdTxAdvInterval;
	private Spinner mSpnrDeviceType, mSpnrLed, mSpnrOutputPower;
	private ListView mLsVwDevices;
	private ArrayAdapter<String> mArAdDeviceTypes, mArAdDevices, mArAdLedOnOff, mArAdOutputPower;
	
	// BLEMini (0x00)... This part must be consistent with one defined in Chores.DEVICE_TYPE_XXX
	private static final String[] DEVICE_TYPES = {"RedBear BLE Mini", };
	
	// OFF (0x00), ON (0x01)
	private static final String[] LED_CONTROL = {"OFF", "ON"};
	
	// -23dBm (0x00), -6dBm (0x01), 0dBm (0x02) or 4dBm (0x03)
	private static final String[] OUTPUT_POWER = {"-23dBm", "-6dBm", "0dBm", "4dBm"};
	
	// For converting 
	private static final String STRING_HEX_NUM = "0123456789ABCDEF";
	private static final char[] CHAR_ARRAY_HEX_NUM = STRING_HEX_NUM.toCharArray();

	// Regular expression
	private static final String UUID_PATTERN 	= "[a-fA-F[0-9]]{32}";
	private static final String UUID_PATTERN_SP = "[a-fA-F[0-9]]{8}-[a-fA-F[0-9]]{4}-[a-fA-F[0-9]]{4}-[a-fA-F[0-9]]{4}-[a-fA-F[0-9]]{12}";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTgBtnConnect				= (ToggleButton) findViewById(R.id.tgbtn_connect);
		mBtnSetUuid					= (Button) findViewById(R.id.btn_set_uuid);
		mBtnSetMajorId				= (Button) findViewById(R.id.btn_set_major_id);
		mBtnSetMinorId				= (Button) findViewById(R.id.btn_set_minor_id);
		mBtnSetMeasuredPower 		= (Button) findViewById(R.id.btn_set_measured_power);
		mBtnSetLed					= (Button) findViewById(R.id.btn_set_led);
		mBtnSetAdvInterval			= (Button) findViewById(R.id.btn_set_adv_interval);
		mBtnSetOutputPower			= (Button) findViewById(R.id.btn_set_output_power);
		mTxVwConnectedDeviceInfo	= (TextView) findViewById(R.id.txvw_device_info);
		mEdTxUuid					= (EditText) findViewById(R.id.edtx_uuid);
		mEdTxMajorId				= (EditText) findViewById(R.id.edtx_major_id);
		mEdTxMinorId				= (EditText) findViewById(R.id.edtx_minor_id);
		mEdTxMeasuredPower			= (EditText) findViewById(R.id.edtx_measured_power);
		mEdTxAdvInterval			= (EditText) findViewById(R.id.edtx_adv_interval);
		mSpnrDeviceType				= (Spinner) findViewById(R.id.spnr_device_type);
		mSpnrLed					= (Spinner) findViewById(R.id.spnr_led);
		mSpnrOutputPower			= (Spinner) findViewById(R.id.spnr_output_power);
		mLsVwDevices				= (ListView) findViewById(R.id.lsvw_devicelist);
		
		mArAdDevices = new ArrayAdapter<String>(this, R.layout.device);
		mLsVwDevices.setAdapter(mArAdDevices);
		mLsVwDevices.setOnItemClickListener(mLsVwOnClickListener);
		
		mArAdDeviceTypes = new ArrayAdapter<String>(this, R.layout.device, DEVICE_TYPES);
		mSpnrDeviceType.setAdapter(mArAdDeviceTypes);
		mSpnrDeviceType.setOnItemSelectedListener(mSpnrDeviceTypeOnClickListener);
		
		mArAdLedOnOff = new ArrayAdapter<String>(this, R.layout.device, LED_CONTROL);
		mSpnrLed.setAdapter(mArAdLedOnOff);
		
		mArAdOutputPower = new ArrayAdapter<String>(this, R.layout.device, OUTPUT_POWER);
		mSpnrOutputPower.setAdapter(mArAdOutputPower);
		
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mSelectedDevice = null;
		mContext = this;
		mChores = new Chores(this);				
	}
	
	
	@Override
	protected void onResume(){
		
		super.onResume();
		Log.i(TAG, "+++ onResume() +++");
		mSelectedDevice = null;
		
		if (!mChores.updateStatus())
			mTgBtnConnect.setEnabled(false);
		setUiResourceEnabled(false);
		mChores.register();
	}
	
	
	@Override
	protected void onPause(){
		
		super.onStop();
		Log.i(TAG, "+++ onPause() +++");
		mChores.unregister();
	}
	
	
	private final OnItemClickListener mLsVwOnClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

			String info = ((TextView) view).getText().toString();
			
			if (D) Log.d(TAG, "onItemClick() : info = [" + info + "]");
			
            if ( null != info && info.length() >= 17) {                	
                // Get the device MAC address, which is the last 17 chars in the View
                String address = info.substring(info.length() - 17);

                if (D) Log.d(TAG, "onItemClick() : Bluetooth address = [" + address + "]");
                
                if (BluetoothAdapter.checkBluetoothAddress(address)){             	
                	mSelectedDevice = mAdapter.getRemoteDevice(address); 	
                } else {
                	Log.e(TAG, "onItemClick() : Invalid Bluetooth address = [" + address + "]");
                }
            }
            else {
                Log.e(TAG, "onItemClick() : Unknown item string = " + info);
            }      	
		}		
	};
	
	
	private final OnItemSelectedListener mSpnrDeviceTypeOnClickListener = new OnItemSelectedListener() {

		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			
			switch (position){
			// TODO : Add support
			// case Chores.DEVICE_TYPE_XXX:
			case Chores.DEVICE_TYPE_REDBEAR_BLE_MINI:
				mChores.setDeviceType(position);
				break;
			default :
				Log.e(TAG, "onItemSelected() : Unknown device type = " + position);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			// TODO Auto-generated method stub			
		}
		
	};
	
	
	private void setUiResourceEnabled(boolean isVisible){
		
		mBtnSetUuid.setEnabled(isVisible);
		mBtnSetMajorId.setEnabled(isVisible);
		mBtnSetMinorId.setEnabled(isVisible);
		mBtnSetMeasuredPower.setEnabled(isVisible);
		mBtnSetLed.setEnabled(isVisible);
		mBtnSetAdvInterval.setEnabled(isVisible);
		mBtnSetOutputPower.setEnabled(isVisible);
		mSpnrLed.setEnabled(isVisible);;
		mSpnrOutputPower.setEnabled(isVisible);
	}
	
	
	private void setUiResourceDefaultContents(){
		
		mTxVwConnectedDeviceInfo.setText(getResources().getText(R.string.default_device).toString());
		mEdTxUuid.setText(getResources().getText(R.string.default_uuid).toString());
		mEdTxMajorId.setText(getResources().getText(R.string.default_major_id).toString());
		mEdTxMinorId.setText(getResources().getText(R.string.default_minor_id).toString());
		mEdTxMeasuredPower.setText(getResources().getText(R.string.default_measured_power).toString());
		mEdTxAdvInterval.setText(getResources().getText(R.string.default_advertising_interval).toString());
		mSpnrLed.setSelection(mArAdLedOnOff.getPosition("OFF"));
		mSpnrOutputPower.setSelection(mArAdOutputPower.getPosition("0dBm"));
		
		synchronized(mArAdDevices){
			mArAdDevices.clear();
		}
	}
	
	
	/*
	 * Thanks for maybeWeCouldStealAVan from StackOverflow forum
	 */
	private String convertByteToHexString(byte[] bytes){
		
		if (null == bytes){
			return null;
		}
		
		char[] hexChars = new char[bytes.length * 2];
		for ( int i = 0; i < bytes.length; i++ ) {
			int v = bytes[i] & 0xFF;
			hexChars[i * 2] = CHAR_ARRAY_HEX_NUM[v >>> 4];
			hexChars[i * 2 + 1] = CHAR_ARRAY_HEX_NUM[v & 0x0F];
		}
		return new String(hexChars);
	}
	

	private byte[] convertHexStringToByte(String str){
		
		if (null == str){
			return null;
		}
		
		int originalLength = str.length();
		String newStr = str.toUpperCase();
		
		if (0 != originalLength % 2){
			newStr = "0"+str.toUpperCase();
		}
		
		if (D) Log.d(TAG, "convertHexStringToByte() : Original input string = " + str + " New = " + newStr);
		
		char[] charBuffer = newStr.toCharArray();		
		int length = charBuffer.length;
		
		if (D) Log.d(TAG, "convertHexStringToByte() : charBuffer size = " + length);
		
		byte[] byteBuffer = new byte[length >> 1];
		length = byteBuffer.length;
		
		if (D) Log.d(TAG, "convertHexStringToByte() : byteBuffer size = " + length);
		
		for(int i = 0; i < length; i++) {
			int lV = STRING_HEX_NUM.indexOf((int)charBuffer[i * 2]);
			int rV = STRING_HEX_NUM.indexOf((int)charBuffer[i * 2 + 1]);
			
			if (D) Log.d(TAG, "lV = " + lV + " rV = " + rV); 
			
			byteBuffer[i] = (byte) (((lV << 4) & 0x00F0) | (rV & 0x000F));
		}
		return byteBuffer;
	}
	

	/*
	 * Convert 32 bytes UUID to format "8_BYTES - 4_BYTES - 4_BYTES - 4_BYTES - 12_BYTES" 
	 */
	private String convertUuidFormatToHumanFriendly(String str){
		
		int originalLength = str.length();
		String tmp = str;
		
		if (32 == originalLength){
			tmp = str.substring(0, 8) + "-" + str.substring(8, 12) + "-" + str.substring(12, 16) + "-" + str.substring(16, 20) + "-" + str.substring(20, 32);
		} else {
			Log.e(TAG, "convertUuidFormatToHumanFriendly() : Input length is invalid");
		}
		return tmp;
	}
	
	
	/*
	 * Check if input string is a valid UUID
	 */
	private byte[] checkAndConvertUuid(String str){
		
		if (null == str)
			return null;
		
		int length = str.length();
		String longStr = "";
		Pattern pattern;
		Matcher matcher;
		
		if (32 == length){
			pattern = Pattern.compile(UUID_PATTERN_SP);
			matcher = pattern.matcher(str);
			
			if (matcher.matches()){
				longStr = str;
			} else {
				Log.e(TAG, "checkAndConvertUuid() : Invalid UUID format ! Length = " + length);
				return null;
			}
		} else if (36 == length){
			pattern = Pattern.compile(UUID_PATTERN_SP);
			matcher = pattern.matcher(str);
			
			if (matcher.matches()){
				String[] tmp = str.split("-");
				
				for (int i = 0; i<tmp.length; i++){
					longStr = longStr.concat(tmp[i]);
				}
			} else {
				Log.e(TAG, "checkAndConvertUuid() : Invalid UUID format ! Length = " + length);
				return null;
			}
		} else {
			Log.e(TAG, "checkAndConvertUuid() : Invalid UUID format !");
			return null;
		}
		
		if (D) Log.d(TAG, "checkAndConvertUuid() : longStr = " + longStr);
		
		return convertHexStringToByte(longStr);
	}
	
	
	private boolean isValidIdValue(int value){
		return (value >= 0) && (value <= 65535);
	}
	
	
	protected void onBluetoothTurnOn(){
		
		mTgBtnConnect.setEnabled(true);
		setUiResourceDefaultContents();
	}
	
	
	protected void onBluetoothTurnOff(){
		
		setUiResourceEnabled(false);
		mTgBtnConnect.setEnabled(false);
		setUiResourceDefaultContents();
	}
	
	
	protected void onDeviceFound(BluetoothDevice device, String name, String address, int rssi, int type){
		
		String tmp = (name + "    RSSI = " + rssi + "    " + (BluetoothDevice.DEVICE_TYPE_CLASSIC == type ? "Classic" : "BLE") + "\n" + address);
		
		synchronized(mArAdDevices){		
			int count = mArAdDevices.getCount();
			
			if (D) Log.d(TAG, "onDeviceFound() : Address = [" + address + "]" + " Count = " + count);
			
			if (null == address){
				Log.e(TAG, "onDeviceFound() : address is null !!!");
				return;
			}
			
			if (0 < count){
				
				for (int i = 0; i < count; i++){
					String s = (String)mArAdDevices.getItem(i);
					
					if (D) Log.d(TAG, "onDeviceFound() : Retrived s = [" + s + "]");
					
					if (s.contains(address)){
						if (D) Log.d(TAG, "onDeviceFound() : Bingo...");
						mArAdDevices.remove(s);
						break;
					} 
				}				
				mArAdDevices.add(tmp);
			} else {
				mArAdDevices.add(tmp);
			}
		}
	}
	
	
	protected void onConnectionBleEstablished(BluetoothDevice device, String name, String address, int rssi, int type){
		
		mConnectedDevice = device;
		setUiResourceEnabled(true);
		mSpnrDeviceType.setEnabled(false);
		mTxVwConnectedDeviceInfo.setText("Connected with " + address + " " + name); 
	}
	
	
	protected void onConnectionBleDisconnected(BluetoothDevice device, String address, String name, int rssi, int type){
		
		mConnectedDevice = null;
		setUiResourceDefaultContents();
		setUiResourceEnabled(false);
		mSpnrDeviceType.setEnabled(true);
	}
	
	
	protected void onDataAvailable(BluetoothDevice device, int type, byte[] data){
		
		if (D) Log.d(TAG, "onDataAvailable() : Received " + data.length + " bytes");
		
		String tmp = "";
		
		if (RedBearBleMini.TYPE_CHARACTERISTIC_FW_VERSION == type){
			
			try {
				tmp = new String(data, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "onDataAvailable() : UnsupportedEncodingException");
			}
		} else if (RedBearBleMini.TYPE_CHARACTERISTIC_MEASURED_POWER == type){
			tmp = "FFFFFF" + convertByteToHexString(data);
		} else {
			tmp = convertByteToHexString(data);
		}
		
		if (D) Log.d(TAG, "onDataAvailable() : Received = " + data + " New = " + tmp);
		
		switch (type){
		case RedBearBleMini.TYPE_CHARACTERISTIC_UUID:
			mEdTxUuid.setText(convertUuidFormatToHumanFriendly(tmp));
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_MAJOR_ID:
			mEdTxMajorId.setText(Integer.valueOf(tmp, 16).toString());
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_MINOR_ID:
			mEdTxMinorId.setText(Integer.valueOf(tmp, 16).toString());
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_MEASURED_POWER:
			mEdTxMeasuredPower.setText(Integer.toString((int)Long.parseLong(tmp, 16)));
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_LED_SWITCH:
			mSpnrLed.setSelection(Integer.valueOf(tmp));
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_ADV_INTERVAL:
			mEdTxAdvInterval.setText(Integer.valueOf(tmp, 16).toString());
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_OUTPUT_POWER:
			mSpnrOutputPower.setSelection(Integer.valueOf(tmp));
			break;
		case RedBearBleMini.TYPE_CHARACTERISTIC_FW_VERSION:
			Log.i(TAG, "onDataAvailable() : FW version = " + tmp);
			mTxVwConnectedDeviceInfo.setText("Connected with " + device.getAddress() + " " + device.getName() + " Firmware version : " + tmp); 
			break;
		default:
			Log.e(TAG, "onDataAvailable() : Unknown event = " + type);
			break;
		}
	}
	
	
	public void onClickTgBtnConnect(View view){
		
		if (D) Log.d(TAG, "+++ onClickTgBtnConnect +++");
		
		if (null != mSelectedDevice){
			
			if (((ToggleButton)view).isChecked())
				mChores.connect(mSelectedDevice);
			else
				mChores.disconnect(mSelectedDevice);			
		} else {
			Log.e(TAG, "onClickTgBtnConnect() : mSelectedDevice is null");
			mTgBtnConnect.setChecked(false);
		} 
	}
	
	
	public void onClickBtnSetUuid(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetUuid +++");
		
		String uuid = mEdTxUuid.getText().toString();
		
		if (null != uuid){
			
			if (D) Log.d(TAG, "onClickBtnSetUuid() : UUID = [" + uuid + "]");
			
			byte[] tmp = checkAndConvertUuid(uuid);
			
			if (null != tmp){
				mChores.setCompanyUuid(tmp);
			} else 
				Log.e(TAG, "onClickBtnSetUuid() : Invalid format of UUID");
		} else {
			Log.e(TAG, "onClickBtnSetUuid() : There is no data in mEdTxUuid");
		}
	}
	
	
	public void onClickBtnSetMajorId(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetMajorId +++");
		
		String id = mEdTxMajorId.getText().toString();
		
		if (null != id){
			
			if (D) Log.d(TAG, "onClickBtnSetMajorId() : id = [" + id + "]");
			
			if (!isValidIdValue(Integer.valueOf(id))){
				Log.e(TAG, "onClickBtnSetMajorId() : Invalid ID value ! It should be 0 ~ 65535");
				return;
			}
			
			while (id.length() < 4){
				id = "0"+id;
			}

			byte[] tmp = convertHexStringToByte(id);
	
			if (null != tmp){
				if (!mChores.setMajorId(tmp))
					Log.e(TAG, "onClickBtnSetMajorId() : Fail to write characteristic");
			} else 
				Log.e(TAG, "onClickBtnSetMajorId() : Cannot convert to bytes !!!");
		} else {
			Log.e(TAG, "onClickBtnSetMajorId() : There is no data in mEdTxMajorId");
		}
	}
	
	
	public void onClickBtnSetMinorId(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetMinorId +++");
		
		String id = mEdTxMinorId.getText().toString();
		
		if (null != id){
			
			if (D) Log.d(TAG, "onClickBtnSetMinorId() : id = [" + id + "]");
			
			if (!isValidIdValue(Integer.valueOf(id))){
				Log.e(TAG, "onClickBtnSetMinorId() : Invalid ID value ! It should be 0 ~ 65535");
				return;
			}
			
			while (id.length() < 4){
				id = "0"+id;
			}
			
			byte[] tmp = convertHexStringToByte(id);
	
			if (null != tmp){
				mChores.setMinorId(tmp);
			} else 
				Log.e(TAG, "onClickBtnSetMinorId() : Cannot convert to bytes !!!");
		} else {
			Log.e(TAG, "onClickBtnSetMinorId() : There is no data in mEdTxMinorId");
		}
	}
	
	
	public void onClickBtnSetMeasuredPower(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetMeasuredPower +++");
		
		String value = mEdTxMeasuredPower.getText().toString();
		
		if (null != value){
			
			if (D) Log.d(TAG, "onClickBtnSetMeasuredPower() : value = [" + value + "]");
			
			int intValue = Integer.valueOf(value);
			
			if ((intValue > 0) || (intValue < -100)){
				Log.i(TAG, "onClickBtnSetAdvInterval() : Valid value of advertising interval is -100 ~ 0");
				return;
			}
			
			String hexValue = Integer.toHexString(intValue);
			
			if (D) Log.d(TAG, "onClickBtnSetMeasuredPower() : hexValue = [" + hexValue + "]");
			
			if (hexValue.length() > 2){
				hexValue = hexValue.substring(hexValue.length()-2);
			} else if (1 == hexValue.length()){
				hexValue = "0"+hexValue;
			}
			
			if (D) Log.d(TAG, "onClickBtnSetMeasuredPower() : hexValue = [" + hexValue + "]");
			
			byte[] tmp = convertHexStringToByte(hexValue);
			
			if (null != tmp){
				mChores.setMeasuredPower(tmp);
			} else 
				Log.e(TAG, "onClickBtnSetMeasuredPower() : Cannot convert to bytes !!!");
		} else {
			Log.e(TAG, "onClickBtnSetMeasuredPower() : There is no data in mEdTxMeasuredPower");
		}
	}
	
	
	public void onClickBtnSetLed(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetLed +++");
		
		int index = mSpnrLed.getSelectedItemPosition();
		String str = Integer.toString(index);
		
		if (D) Log.d(TAG, "onClickBtnSetLed() : index = [" + index + "] String = " + str);
		
		byte[] tmp = convertHexStringToByte(str);
		
		if (null != tmp){
			mChores.setLed(tmp);
		} else 
			Log.e(TAG, "onClickBtnSetLed() : Cannot convert to bytes !!!");
	}
	
	
	public void onClickBtnSetAdvInterval(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetAdvInterval +++");
		
		String value = mEdTxAdvInterval.getText().toString();
		
		if (null != value){
			
			if (D) Log.d(TAG, "onClickBtnSetAdvInterval() : value = [" + value + "]");
			
			int intValue = Integer.valueOf(value);
			
			if ((intValue <= 0) || (intValue > 10000)){
				Log.i(TAG, "onClickBtnSetAdvInterval() : Valid value of advertising interval is 100 ~ 10000 ms");
				return;
			}
			
			intValue += (5 - (intValue%5));
			
			if (D) Log.d(TAG, "onClickBtnSetAdvInterval() : Final intValue = " + intValue);
			
			String hexValue = Integer.toHexString(intValue);
			
			if (hexValue.length() > 2){
				hexValue = hexValue.substring(hexValue.length()-2);
			} else if (1 == hexValue.length()){
				hexValue = "0"+hexValue;
			}			
			byte[] tmp = convertHexStringToByte(Integer.toString(intValue));
			
			if (null != tmp){
				mChores.setAdvInterval(tmp);
			} else 
				Log.e(TAG, "onClickBtnSetAdvInterval() : Cannot convert to bytes !!!");
		} else {
			Log.e(TAG, "onClickBtnSetAdvInterval() : There is no data in mEdTxAdvInterval");
		}
	}
	
	
	public void onClickBtnSetOutputPower(View view){
		
		if (D) Log.d(TAG, "+++ onClickBtnSetOutputPower +++");
		
		int index = mSpnrOutputPower.getSelectedItemPosition();
		String str = Integer.toString(index);
		
		if (D) Log.d(TAG, "onClickBtnSetOutputPower() : index = [" + index + "]");
		
		byte[] tmp = convertHexStringToByte(str);
		
		if (null != tmp){
			mChores.setOutputPower(tmp);
		} else 
			Log.e(TAG, "onClickBtnSetOutputPower() : Cannot convert to bytes !!!");
	}
	
}
