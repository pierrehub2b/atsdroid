package com.ats.atsdroid;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class DeviceInfo {

    //----------------------------------------------------------------------------------
    // Singleton declaration
    //----------------------------------------------------------------------------------

    private static final DeviceInfo ourInstance = new DeviceInfo();

    public static DeviceInfo getInstance() {
        return ourInstance;
    }

    private DeviceInfo() {
        BluetoothAdapter btDevice = BluetoothAdapter.getDefaultAdapter();
        btAdapter = btDevice.getName();
        hostName = tryGetHostname();
        systemName = getAndroidVersion();
    }

    public static DeviceInfo getOurInstance() {
        return ourInstance;
    }

    //----------------------------------------------------------------------------------
    // Utils
    //----------------------------------------------------------------------------------

    private String getAndroidVersion(){
        double release=Double.parseDouble(Build.VERSION.RELEASE.replaceAll("(\\d+[.]\\d+)(.*)","$1"));
        String codeName="undefined";//below Jelly bean OR above Oreo
        if(release>=4.1 && release<4.4)codeName="Jelly Bean";
        else if(release<5)codeName="Kit Kat";
        else if(release<6)codeName="Lollipop";
        else if(release<7)codeName="Marshmallow";
        else if(release<8)codeName="Nougat";
        else if(release<9)codeName="Oreo";
        return codeName+" v"+release+", API Level: "+Build.VERSION.SDK_INT;
    }

    private String tryGetHostname() {
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

    private int displayWidth;
    private int displayHeight;
    private String systemName;
    private String systemRelease = Build.VERSION.RELEASE;
    private String deviceId = Build.ID;
    private String model = Build.MODEL;
    private String manufacturer = Build.MANUFACTURER;
    private String brand = Build.BRAND;
    private String version = Build.VERSION.RELEASE;
    private String hostName;
    private String btAdapter;

    public void initData(int width, int height){
        this.displayWidth = width;
        this.displayHeight = height;
    }

    public String getSystemName(){ return systemName; }
    public String getSystemRelease(){ return systemRelease; }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
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