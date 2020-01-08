package com.ats.atsdroid.element;

import android.view.accessibility.AccessibilityNodeInfo;

public class AtsRootElement extends AbstractAtsElement {
    public AtsRootElement(AccessibilityNodeInfo node){
        this.tag = "root";
        loadBounds(node);
        loadChildren(node);
    }
}