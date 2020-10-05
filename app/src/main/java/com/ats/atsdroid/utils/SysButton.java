package com.ats.atsdroid.utils;

import android.app.Instrumentation;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;

public final class SysButton {
	
	enum ButtonType {
		app, back, delete, enter, home, menu, search,
		volumeUp, volumeDown;
		
		public static String[] getNames() {
			SysButton.ButtonType[] states = values();
			String[] names = new String[states.length];
			
			for (int i = 0; i < states.length; i++) {
				names[i] = states[i].name();
			}
			
			return names;
		}
	}
	
	public static boolean pressButtonType(String value) throws IllegalArgumentException, RemoteException {
		final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
		final UiDevice device = UiDevice.getInstance(instrument);
		
		ButtonType buttonType = ButtonType.valueOf(value);
		switch (buttonType) {
			case back:          return device.pressBack();
			case delete:        return device.pressDelete();
			case enter:         return device.pressEnter();
			case home:          return device.pressHome();
			case menu:          return device.pressMenu();
			case search:        return device.pressSearch();
			case app:           return device.pressRecentApps();
			case volumeUp:      return device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_UP);
			case volumeDown:    return device.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN);
			default:            return false;
		}
	}
}
