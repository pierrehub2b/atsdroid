package com.ats.atsdroid.utils;

import android.app.Instrumentation;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.support.v7.app.AppCompatDelegate;

public class Sysprop {
	
	enum PropertyName {
		airplaneModeEnabled,
		nightModeEnabled,
		wifiEnabled,
		bluetoothEnabled,
		lockOrientationEnabled,
		orientation,
		brightness,
		volume
	}
	
	private static final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
	private static final UiDevice device = UiDevice.getInstance(instrument);
	private static final Context context = InstrumentationRegistry.getTargetContext();
	
	public static void setProperty(String name, String value) {
		PropertyName property = PropertyName.valueOf(name);
		switch (property) {
			case airplaneModeEnabled:
				boolean enabled = value.equals("on");
				enableAirplaneMode(enabled);
				break;
			case nightModeEnabled:
				enabled = value.equals("on");
				setNightModeEnabled(enabled);
				break;
			case wifiEnabled:
				enabled = value.equals("on");
				setWifiEnabled(enabled);
				break;
			case bluetoothEnabled:
				enabled = value.equals("on");
				setBluetoothEnabled(enabled);
				break;
			case lockOrientationEnabled:
				enabled = value.equals("on");
				try {
					setLockOrientationEnabled(enabled);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case orientation:
				try {
					setDeviceOrientation();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case brightness:
				int brightnessValue = Integer.parseInt(value);
				setBrightness(brightnessValue);
				break;
			case volume:
				int volumeValue = Integer.parseInt(value);
				setVolume(volumeValue);
				break;
		}
	}
	
	public static String getPropertyValue(String name) throws IllegalStateException, IllegalArgumentException {
		PropertyName property = PropertyName.valueOf(name);
		switch (property) {
			case airplaneModeEnabled:
				try {
					return String.valueOf(getAirplaneMode());
				} catch (Settings.SettingNotFoundException e) {
					e.printStackTrace();
					return "";
				}
			case nightModeEnabled:
				return String.valueOf(isNightModeEnabled());
			case wifiEnabled:
				return String.valueOf(isWifiEnabled());
			case bluetoothEnabled:
				return String.valueOf(isBluetoothEnabled());
			case lockOrientationEnabled:
				return "";
			case orientation:
				return String.valueOf(getDeviceOrientation());
			case brightness:
				try {
					return String.valueOf(getBrightness());
				} catch (Settings.SettingNotFoundException e) {
					e.printStackTrace();
					return "";
				}
			case volume:
				return String.valueOf(getVolume());
			default:
				throw new IllegalStateException("Unexpected value: " + property);
		}
	}
	
	private static void setDeviceOrientation() throws RemoteException {
		device.setOrientationLeft();
		device.setOrientationRight();
		device.setOrientationNatural();
	}
	
	private static int getDeviceOrientation() {
		return device.getDisplayRotation();
	}
	
	private static void setWifiEnabled(Boolean enabled) {
		WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(enabled);
	}
	
	private static boolean isWifiEnabled() {
		WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		return wifiManager.isWifiEnabled();
	}
	
	private static void setBluetoothEnabled(Boolean enabled) {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (enabled) {
			bluetoothAdapter.enable();
		} else {
			bluetoothAdapter.disable();
		}
	}
	
	private static boolean isBluetoothEnabled() {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		return bluetoothAdapter.isEnabled();
	}
	
	private static void enableAirplaneMode(Boolean enable) {
		Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
	}
	
	private static boolean getAirplaneMode() throws Settings.SettingNotFoundException {
		return 1 == Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
	}
	
	private static void setVolume(int value) {
		AudioManager audioManager;
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
	}
	
	private static int getVolume() {
		AudioManager audioManager;
		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}
	
	private static void setBrightness(int value) {
		Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
	}
	
	private static int getBrightness() throws Settings.SettingNotFoundException {
		return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
	}
	
	private static void setLockOrientationEnabled(boolean enabled) throws RemoteException {
		if (enabled) {
			device.freezeRotation();
		} else {
			device.unfreezeRotation();
		}
	}
	
	private static void setNightModeEnabled(boolean enabled) {
		AppCompatDelegate.setDefaultNightMode(enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
	}
	
	private static boolean isNightModeEnabled() {
		return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
	}
	
	/* private void setGeolocation(double lat, double long) {
    
    }
    
    private void getGeolocation() {
    
    } */
}
