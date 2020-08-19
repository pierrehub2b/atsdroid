package com.ats.atsdroid.scripting;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.exceptions.DriverException;
import com.ats.atsdroid.exceptions.SyntaxException;
import com.ats.atsdroid.utils.AtsAutomation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptingExecutor {

    private static final Pattern scriptPattern = Pattern.compile("([^)]*)\\(([^)]*)\\)");

    private final AtsAutomation automation;
    private final String[] actions;

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

    public @Nullable
    String execute(AbstractAtsElement element) throws Throwable {

        for (String action : actions)
        {
            Matcher m = scriptPattern.matcher(action);
            if (m.find()) {
                String functionName = m.group(1);
                String parameter = m.group(2);

                try {
                    Method method = this.getClass().getDeclaredMethod(functionName, AbstractAtsElement.class, String.class);
                    Object obj = method.invoke(this, element, parameter);
                    if (obj instanceof String) {
                        return (String)obj;
                    }
                } catch (NoSuchMethodException e) {
                    throw new DriverException(DriverException.UNKNOWN_FUNCTION);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            } else {
                throw new SyntaxException(SyntaxException.INVALID_METHOD);
            }
        }

        return null;
    }

    private String cmd(AbstractAtsElement element, String command) throws Exception {
        StringBuilder output = new StringBuilder();

        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine())!= null) {
            output.append(line).append("n");
        }

        return output.toString();
    }

    private void longPress(AbstractAtsElement element, String value) throws SyntaxException {
        try {
            int intValue = Integer.parseInt(value);
            element.longPress(automation, intValue);
        } catch (NumberFormatException e) {
            throw new SyntaxException(SyntaxException.INVALID_PARAMETER);
        }
    }

    private void tap(AbstractAtsElement element, String value) throws SyntaxException {
        try {
            int intValue = Integer.parseInt(value);
            element.click(automation, intValue);
        } catch (NumberFormatException e) {
            throw new SyntaxException(SyntaxException.INVALID_PARAMETER);
        }
    }

    private void setAirPlaneMode(AbstractAtsElement element, String value) throws DriverException {
        if (automation.usbMode) {
            Boolean booleanValue = Boolean.parseBoolean(value);

            // Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, booleanValue ? 1 : 0);

            ConnectivityManager mgr = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                if(mgr != null) {
                    Method airPlane = mgr.getClass().getDeclaredMethod("setAirplaneMode", boolean.class);
                    airPlane.invoke(mgr, booleanValue);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new DriverException(DriverException.UNKNOWN_ERROR);
            }
        } else {
            throw new DriverException(DriverException.UNAVAILABLE_FEATURE);
        }
    }

    private void setWifiEnabled(AbstractAtsElement element, String value) throws DriverException {
        if (automation.usbMode) {
            boolean booleanValue = Boolean.parseBoolean(value);

            WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifi.setWifiEnabled(booleanValue);
        } else {
            throw new DriverException(DriverException.UNAVAILABLE_FEATURE);
        }
    }

    private void setBluetoothEnabled(AbstractAtsElement element, String value) {
        boolean booleanValue = Boolean.parseBoolean(value);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (booleanValue) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.disable();
        }
    }

    private void setOrientation(AbstractAtsElement element, String value) throws SyntaxException {
        if (value.equals("portrait") || value.equals("landscape")) {
            ContentResolver contentResolver = getContentResolver();
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(contentResolver, Settings.System.USER_ROTATION, 3);
        } else {
            throw new SyntaxException(SyntaxException.INVALID_PARAMETER);
        }
    }

    private String getBluetoothName() {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }
}
