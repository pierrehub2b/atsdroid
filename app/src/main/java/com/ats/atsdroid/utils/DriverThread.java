package com.ats.atsdroid.utils;

public class DriverThread implements Runnable {

    private AtsAutomation automation;
    private boolean running = true;
    private CaptureScreenServer screenCapture;

    public DriverThread(AtsAutomation automation){
        this.automation = automation;

        this.screenCapture = new CaptureScreenServer(automation);
        (new Thread(this.screenCapture)).start();
    }

    public int getScreenCapturePort(){
        return screenCapture.getPort();
    }

    public void stop(){
        screenCapture.stop();
        running = false;
    }

    @Override
    public void run() {
        while(running){
            automation.wakeUp();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}
        }
        automation.sleep();
    }
}
