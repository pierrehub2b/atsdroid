package com.ats.atsdroid.scripting;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.utils.AtsAutomation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptingExecutor {

    private static Pattern scriptPattern = Pattern.compile("([^\\)]*)\\(([^\\)]*)\\)");

    private AtsAutomation automation;
    private String[] actions;

    private Context getApplicationContext() {
        return InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();
    }

    private ContentResolver getContentResolver() {
        return getApplicationContext().getContentResolver();
    }

    public ScriptingExecutor(AtsAutomation automation, String script) {
        this.automation = automation;
        this.actions = script.split(";");
    }

    public void execute(AbstractAtsElement element) throws Exception {

        for (String action : actions)
        {
            Matcher m = scriptPattern.matcher(action);
            if (m.find()) {
                String functionName = m.group(1);
                String parameter = m.group(2);

                try {
                    Method method = this.getClass().getDeclaredMethod(functionName, AbstractAtsElement.class, String.class);
                    method.invoke(this, element, parameter);
                } catch (NoSuchMethodException e) {
                    String cmdOutput = executeShellCommand(action);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else {
                // bad script
            }
        }
    }

    private String executeShellCommand(String command) {
        StringBuffer output = new StringBuffer();

        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "n");
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }

        return output.toString();
    }

    private void longPress(AbstractAtsElement element, String value) throws Exception {
        try {
            int intValue = Integer.parseInt(value);
            element.longPress(automation, intValue);
        } catch (NumberFormatException e) {
            // if value is not integer throw code exception
            // e.printStackTrace();
        }
    }

    private void tap(AbstractAtsElement element, String value) throws Exception {
        try {
            int intValue = Integer.parseInt(value);
            element.click(automation, intValue);
        } catch (NumberFormatException e) {
            // if value is not integer throw code exception
            // e.printStackTrace();
        }
    }

    private void setAirPlaneMode(AbstractAtsElement element, String value) {
        if (automation.usbMode == true) {
            Boolean booleanValue = Boolean.valueOf(value);
            // Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, booleanValue ? 1 : 0);

            ConnectivityManager mgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                if(mgr != null) {
                    Method airPlane = mgr.getClass().getDeclaredMethod("setAirplaneMode", boolean.class);
                    if(null != airPlane) {
                        airPlane.invoke(mgr, booleanValue);
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            // if usb mode 'on' throw driver exception
        }

        // if value is not boolean throw code exception
    }

    private void setWifiEnabled(AbstractAtsElement element, String value) {
        if (automation.usbMode == true) {
            Boolean booleanValue = Boolean.valueOf(value);

            WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(booleanValue);
        } else {
            
        }
    }

    private void setBluetoothEnabled(AbstractAtsElement element, String value){
        Boolean booleanValue = Boolean.valueOf(value);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (booleanValue) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.disable();
        }

        // if value is not boolean throw code exception
    }

    private void setOrientation(AbstractAtsElement element, String value) {
        if (value == "portrait" || value == "landscape") {
            ContentResolver contentResolver = getContentResolver();
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 3);
        } else {
            // if value not equals 'portrait' or 'landscape' throw code exception
        }
    }

    private String getBluetoothName() {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }
}
