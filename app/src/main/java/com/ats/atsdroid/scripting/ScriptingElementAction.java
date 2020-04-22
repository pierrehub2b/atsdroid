package com.ats.atsdroid.scripting;

import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.utils.AtsAutomation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptingElementAction {
    private static Pattern tapPattern = Pattern.compile("/tap(d+)/");
    private static Pattern longPressPattern = Pattern.compile("longPress\\(([^\\)]*)\\)");

    public static void execute(String script, AbstractAtsElement element, AtsAutomation automation) throws Exception {

        Matcher m = longPressPattern.matcher(script);
        if (m.find()) {
            int value = Integer.valueOf(m.group(1));
            element.longPress(automation, value);
            return;
        }

        throw new Exception("bad scripting action");
    }
}
