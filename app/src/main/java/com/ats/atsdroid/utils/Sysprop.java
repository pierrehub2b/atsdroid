package com.ats.atsdroid.utils;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.app.UiModeManager;
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
	
	public static class BooleanException extends Exception {
		private static final String BAD_VALUE = "bad value";
		
		public BooleanException(String errorMessage) {
			super(errorMessage);
		}
	}
	
	private static final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
	private static final UiDevice device = UiDevice.getInstance(instrument);
	
	/* public static JSONObject getJSON() throws Settings.SettingNotFoundException, JSONException {
		JSONObject json = new JSONObject();
		json.put(String.valueOf(DeviceInfo.PropertyName.airplaneModeEnabled), isAirplaneModeEnabled());
		json.put(String.valueOf(DeviceInfo.PropertyName.nightModeEnabled), isNightModeEnabled());
		json.put(String.valueOf(DeviceInfo.PropertyName.wifiEnabled), isWifiEnabled());
		json.put(String.valueOf(DeviceInfo.PropertyName.bluetoothEnabled), isBluetoothEnabled());
		json.put(String.valueOf(DeviceInfo.PropertyName.orientation), getDeviceOrientation());
		json.put(String.valueOf(DeviceInfo.PropertyName.brightness), getBrightness());
		json.put(String.valueOf(DeviceInfo.PropertyName.volume), getVolume());
		return json;
	} */
	
	public static void setProperty(String name, String value) throws BooleanException, RemoteException {
		DeviceInfo.PropertyName property = DeviceInfo.PropertyName.valueOf(name);
		switch (property) {
			/* case airplaneModeEnabled:
				boolean enabled = value.equals("on");
				enableAirplaneMode(enabled);
				break;
			case nightModeEnabled:
				enabled = value.equals("on");
				setNightModeEnabled(enabled);
				break; */
			case wifiEnabled:
				boolean enabled = booleanFromString(value);
				setWifiEnabled(enabled);
				break;
			case bluetoothEnabled:
				enabled = booleanFromString(value);
				setBluetoothEnabled(enabled);
				break;
			case lockOrientationEnabled:
				enabled = booleanFromString(value);
				setLockOrientationEnabled(enabled);
				break;
			case orientation:
				int orientationValue = Integer.parseInt(value);
				setDeviceOrientation(orientationValue);
				break;
			/* case brightness:
				int brightnessValue = Integer.parseInt(value);
				setBrightness(brightnessValue);
				break; */
			case volume:
				int volumeValue = Integer.parseInt(value);
				setVolume(volumeValue);
				break;
		}
	}
	
	private static boolean booleanFromString(String value) throws BooleanException {
		value = value.toLowerCase();
		
		if (value.equals("true") || value.equals("1") || value.equals("on")) {
			return true;
		} else if (value.equals("false") || value.equals("0") || value.equals("off")) {
			return false;
		} else {
			throw new BooleanException(BooleanException.BAD_VALUE);
		}
	}
	
	public static String getPropertyValue(String name) throws IllegalStateException, IllegalArgumentException {
		DeviceInfo.PropertyName property = DeviceInfo.PropertyName.valueOf(name);
		switch (property) {
			/* case airplaneModeEnabled:
				try {
					return String.valueOf(isAirplaneModeEnabled());
				} catch (Settings.SettingNotFoundException e) {
					e.printStackTrace();
					return "";
				}
			case nightModeEnabled:
				return String.valueOf(isNightModeEnabled()); */
			case wifiEnabled:
				return String.valueOf(isWifiEnabled());
			case bluetoothEnabled:
				return String.valueOf(isBluetoothEnabled());
			case lockOrientationEnabled:
				return "";
			case orientation:
				return String.valueOf(getDeviceOrientation());
			/* case brightness:
				try {
					return String.valueOf(getBrightness());
				} catch (Settings.SettingNotFoundException e) {
					e.printStackTrace();
					return "";
				} */
			case volume:
				return String.valueOf(getVolume());
			default:
				throw new IllegalStateException("Unexpected value: " + property);
		}
	}
	
	private static void setDeviceOrientation(int value) throws RemoteException {
		if (value == UiAutomation.ROTATION_FREEZE_90) {
			device.setOrientationLeft();
		} else if (value == UiAutomation.ROTATION_FREEZE_270) {
			device.setOrientationRight();
		} else {
			device.setOrientationNatural();
		}
	}
	
	private static int getDeviceOrientation() {
		return device.getDisplayRotation();
	}
	
	private static void setWifiEnabled(Boolean enabled) {
		WifiManager wifiManager = (WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiManager.setWifiEnabled(enabled);
	}
	
	private static boolean isWifiEnabled() {
		WifiManager wifiManager = (WifiManager)getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
		Settings.Global.putInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
		
		
		/* Intent intent = new Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent); */
	}
	
	private static boolean isAirplaneModeEnabled() throws Settings.SettingNotFoundException {
		return 1 == Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
	}
	
	private static void setVolume(int value) {
		AudioManager audioManager;
		audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
	}
	
	private static int getVolume() {
		AudioManager audioManager;
		audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}
	
	private static void setBrightness(int value) {
		// 0 to 255
		// Settings.System.putInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);
	}
	
	private static int getBrightness() throws Settings.SettingNotFoundException {
		return Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
	}
	
	private static void setLockOrientationEnabled(boolean enabled) throws RemoteException {
		if (enabled) {
			device.freezeRotation();
		} else {
			device.unfreezeRotation();
		}
	}
	
	private static void setNightModeEnabled(boolean enabled) {
		UiModeManager uiManager = (UiModeManager)getContext().getSystemService(Context.UI_MODE_SERVICE);
		uiManager.setNightMode(enabled ? UiModeManager.MODE_NIGHT_YES : UiModeManager.MODE_NIGHT_NO);
	}
	
	private static boolean isNightModeEnabled() {
		return AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
	}
	
	private static Context getContext() {
		return InstrumentationRegistry.getTargetContext();
	}
}
