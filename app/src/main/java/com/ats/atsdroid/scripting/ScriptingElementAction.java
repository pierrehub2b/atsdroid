package com.ats.atsdroid.scripting;

import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.utils.AtsAutomation;

public class ScriptingElementAction extends ScriptingAction {

    private static final String LONGPRESS = "longPress";
    private static final String TAP = "tap";

    private AbstractAtsElement element;
    private int intValue;

    public ScriptingElementAction(AbstractAtsElement element, String script, AtsAutomation automation) throws Exception {
        super(script, automation);
        this.element = element;
        this.intValue = Integer.valueOf(this.value);
    }

    @Override
    public void execute() throws Exception {
        switch (action) {
            case LONGPRESS:
                element.longPress(automation, intValue);
                break;
            case TAP:
                element.click(automation, intValue);
                break;
            default:
                throw new Exception("bad scripting action");
        }
    }

    public void longPress(String value){
        // if value is not integer throw code exception
    }

    public void tap(String value){
        // if value is not integer throw code exception
    }

    public void setAirPlaneMode(String value){
        // if value is not boolean throw code exception
        // if usb mode 'on' throw driver exception
    }

    public void setWifiEnabled(String value){
        // if value is not boolean throw code exception
        // if usb mode 'on' throw driver exception
    }

    public void setBluetoothEnabled(String value){
        // if value is not boolean throw code exception
    }

    public void setOrientation(String value){
        // if value not equals 'portrait' or 'landscape' throw code exception
    }

    public String getBluetoothName(){
        return "";
    }

}
