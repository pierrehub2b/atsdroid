package com.ats.atsdroid.automation;

import android.support.test.uiautomator.UiObject2;

import java.util.List;

public interface IAtsElement {
    public void click();
    public String inputText(String value);
    public List<AtsElement> getParents();
    public List<UiObject2> getChildren(String tag);
}
