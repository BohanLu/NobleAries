# NobleAries
Android App for setting Bluetooth Low Energy module
==========

This is an Android application for setting RedBear Lab's BLE Mini beacon. This module is based on TI's CC254X chipset. Application could read following characteristics from device with iBeacon firmware.

1. UUID
2. Major ID
3. Minor ID
4. Measured power
5. LED switch
6. Advertising interval
7. Output power
8. Firmware version

It also could modify these characteristics except for firmware version. Please refer to corresponding resources for the proper value of each characteristic. I tested this application successfully on Nexus 5 with Android 4.4.4 and 5.0.1. The firmware version of Mini beacon is "MiniBeacon_20131205". However, I think this application is not bug-free yet, and it is only a simple tool, so it might not be as good as you hope. :)

Dependency
==========

1. H/W<br/>
  Currently, RedBear Lab's BLE Mini beacon is the only hardware this app supports. But you might be able extend this app to support others.

2. Android<br/>
  Bluetooth Low Energy is supported after Android 4.3, which is at API level 18. Your mobile device must fill this requirement. 

3. Building environment<br/>
  Ubuntu 14.04 x86_64 + eclipse Luna 4.4.1 + OpenJDK 1.7.0_75

Resources
==========

1. RedBear Lab<br/>
  http://redbearlab.squarespace.com/ibeacon/
  https://github.com/RedBearLab/BLEMini

2. Apple iBeacon<br/>
  https://developer.apple.com/ibeacon/Getting-Started-with-iBeacon.pdf

3. AMI<br/>
  http://www.ami.com.tw/


License
==========

 Copyright (C) 2015 American Megatrends, Inc. AMI
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
      
 @author Bohan Lu
      
