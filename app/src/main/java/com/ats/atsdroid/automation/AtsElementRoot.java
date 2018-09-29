package com.ats.atsdroid.automation;

import android.support.test.uiautomator.UiObject2;

import java.util.ArrayList;
import java.util.List;

public class AtsElementRoot implements IAtsElement {

    private AtsAutomation automation;

    public AtsElementRoot(AtsAutomation automation){
        this.automation = automation;
    }

    public void click() {}
    public String inputText(String value) {
        return null;
    }
    public List<AtsElement> getParents(){ return new ArrayList<AtsElement>();}

    public List<UiObject2> getChildren(String tag){
        return automation.getDevice().findObjects(automation.getSelector(tag));
    }
}