package com.ats.atsdroid.scripting;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import com.ats.atsdroid.utils.AtsAutomation;

import static android.provider.Settings.Global.AIRPLANE_MODE_ON;

public class ScriptingModeAction extends ScriptingAction {

    private static final String AIRPLANEMODE = "airplaneMode";

    public ScriptingModeAction(String script, AtsAutomation automation) throws Exception {
        super(script, automation);
    }

    @Override
    public void execute() throws Exception {
        super.execute();

        switch (action) {
            case AIRPLANEMODE:
                if (automation.usbMode == true) {
                    /* int airplaneMode = Settings.Global.getInt(InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), AIRPLANE_MODE_ON) == 0 ? 1 : 0;
                    Settings.Global.putInt(InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext().getContentResolver(), AIRPLANE_MODE_ON, airplaneMode);
                    int airplaneMode2 = Settings.Global.getInt(InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), AIRPLANE_MODE_ON); */

                    WifiManager wifi = (WifiManager)InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wifi.setWifiEnabled(false);
                } else {
                    throw new Exception("usb mode off");
                }

                /*

                 UiDevice device = UiDevice.getInstance(getInstrumentation());
    device.openQuickSettings();
    // Find the text of your language
    BySelector description = By.desc("Airplane mode");
    // Need to wait for the button, as the opening of quick settings is animated.
    device.wait(Until.hasObject(description), 500);
    device.findObject(description).click();
    getInstrumentation().getContext().sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
}
                 */
        }
    }
}
