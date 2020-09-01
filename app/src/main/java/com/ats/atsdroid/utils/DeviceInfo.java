/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package com.ats.atsdroid.utils;

import android.bluetooth.BluetoothAdapter;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.os.Build;
import android.support.test.uiautomator.UiDevice;
import android.util.DisplayMetrics;

import com.ats.atsdroid.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.stream.Stream;

public class DeviceInfo {

    //----------------------------------------------------------------------------------
    // Singleton declaration
    //----------------------------------------------------------------------------------

    private static DeviceInfo instance;

    public static DeviceInfo getInstance() {
        if(instance == null){
            instance  = new DeviceInfo();
        }
        return instance;
    }

    private DeviceInfo() {
        AtsAutomation.sendLogs("Get bluetooth adapter name\n");
        BluetoothAdapter btDevice = BluetoothAdapter.getDefaultAdapter();
        if (btDevice != null) {
            btAdapter = btDevice.getName();
        }


        hostName = tryGetHostname();
        systemName = getAndroidVersion();
    }

    //----------------------------------------------------------------------------------
    // Utils
    //----------------------------------------------------------------------------------

    private String getAndroidVersion(){
        double release = Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
        String codeName="undefined";//below Jelly bean OR above Oreo
        if(release>=4.1 && release<4.4)codeName="Jelly Bean";
        else if(release<5)codeName="Kit Kat";
        else if(release<6)codeName="Lollipop";
        else if(release<7)codeName="Marshmallow";
        else if(release<8)codeName="Nougat";
        else if(release<9)codeName="Oreo";
        else if(release<10)codeName="Pie";
        else if(release<11)codeName="Android 10";
        return codeName+" v"+release+", API Level: "+Build.VERSION.SDK_INT;
    }

    public static String tryGetHostname() {
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        AtsAutomation.sendLogs("getting IP Addresse" + inetAddress.getHostName() + "\n");
                        return inetAddress.getHostName();
                    }
                }
            }
        } catch (SocketException e) {
            return "error -> " + e.toString();
        }
        return "undefined";
    }



    //----------------------------------------------------------------------------------
    // Instance access
    //----------------------------------------------------------------------------------
    
    enum PropertyName {
        airplaneModeEnabled,
        nightModeEnabled,
        wifiEnabled,
        bluetoothEnabled,
        lockOrientationEnabled,
        orientation,
        brightness,
        volume;
    
        public static String[] getNames() {
            return Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
        }
    }
    
    private int port;

    private int deviceWidth;
    private int deviceHeight;
    private int channelWidth;
    private int channelHeight;

    private Matrix matrix;

    private final String systemName;
    private final String deviceId = Build.ID;
    private final String model = Build.MODEL;
    private final String manufacturer = Build.MANUFACTURER;
    private final String brand = Build.BRAND;
    private final String version = Build.VERSION.RELEASE;
    private String hostName;
    private String btAdapter;
    private UiDevice device;
    
    public void initDevice(int p, UiDevice d, String ipAddress){
        hostName = ipAddress;
        port = p;
        device = d;
        setupScreenInformation();
    }

    public void setupScreenInformation() {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        channelHeight = device.getDisplayHeight();
        channelWidth = device.getDisplayWidth();

        deviceWidth = Math.round((float)channelWidth / metrics.scaledDensity);
        deviceHeight = Math.round((float)channelHeight / metrics.scaledDensity);

        matrix = new Matrix();
        matrix.preScale((float)deviceWidth / (float)channelWidth, (float)deviceHeight / (float)channelHeight);
    }

    public Matrix getMatrix(){
        return matrix;
    }

    public void driverInfoBase(JSONObject obj, int height) throws JSONException {
        setupScreenInformation();
        obj.put("os", "android");
        obj.put("driverVersion", BuildConfig.VERSION_NAME);
        obj.put("systemName", systemName);
        obj.put("deviceWidth", deviceWidth);
        obj.put("deviceHeight", deviceHeight);
        obj.put("channelWidth", channelWidth);
        obj.put("channelHeight", channelHeight);
        obj.put("systemProperties", new JSONArray(PropertyName.getNames()));
    }
    
    public String getSystemName(){ return systemName; }
    public int getPort(){ return port; }
    public int getDeviceWidth() { return deviceWidth; }
    public int getDeviceHeight() { return deviceHeight; }
    public int getChannelWidth() {return channelWidth; }
    public int getChannelHeight() {
        return channelHeight;
    }
    public String getDeviceId() {
        return deviceId;
    }
    public String getModel() {
        return model;
    }
    public String getManufacturer() {
        return manufacturer;
    }
    public String getBrand() {
        return brand;
    }
    public String getVersion() {
        return version;
    }
    public String getHostName() {
        return hostName;
    }
    public String getBtAdapter() {
        return btAdapter;
    }
}