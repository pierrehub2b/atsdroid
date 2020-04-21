package com.ats.atsdroid.scripting;

import com.ats.atsdroid.utils.AtsAutomation;

public class ScriptingAction {

    protected AtsAutomation automation;
    protected String action;
    protected String value;

    public ScriptingAction(String script, AtsAutomation automation) throws Exception {
        String[] parameters = script.split("=");

        this.action = parameters[0];
        this.value = parameters[1];
        this.automation = automation;
    }

    public void execute() throws Exception {}
}
