package com.ats.atsdroid.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.view.View;

import com.ats.atsdroid.BuildConfig;
import com.ats.atsdroid.R;
import com.ats.atsdroid.utils.DeviceInfo;

public class AtsView extends View {

    private Paint backGroundPaint;
    private Paint textPaint;
    private Bitmap bitmap;
    private int scaledSize;

    private Rect rectangle;

    public AtsView(Context context) {
        super(context);

        scaledSize = getResources().getDimensionPixelSize(R.dimen.font_size);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(scaledSize);
        textPaint.setShadowLayer(24.0f, 6.0f, 6.0f, Color.rgb(0,0,0));

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ats_logo2);
    }

    private Paint getBackGroundPaint(int w, int h){
        if(backGroundPaint == null){
            backGroundPaint = new Paint();
            backGroundPaint.setShader(new LinearGradient(0, 0, w, h, Color.rgb(60, 60, 60), Color.rgb(20, 20, 20), Shader.TileMode.CLAMP));
            backGroundPaint.setAlpha(220);
        }
        return backGroundPaint;
    }

    private int yPos = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(rectangle == null){
            int h = bitmap.getScaledHeight(canvas);
            int w = bitmap.getScaledWidth(canvas);
            rectangle = new Rect(0, 0, w + 80, h + (scaledSize * 12));
            setLeft((getWidth()-rectangle.width())/2);
            setTop((getHeight()-rectangle.height())/2);
        }

        canvas.drawRect(rectangle, getBackGroundPaint(rectangle.width(), rectangle.height()));
        canvas.drawBitmap(bitmap, 0, 0, null);

        yPos = scaledSize*6;
        canvas.drawText("ATS driver host : " + DeviceInfo.getInstance().getHostName() + ":" + DeviceInfo.getInstance().getPort(), 30, yPos, textPaint);
        yPos += scaledSize*1.4;
        canvas.drawText("ATS driver version : " + BuildConfig.VERSION_NAME, 30, yPos, textPaint);
        yPos += scaledSize*1.8;
        canvas.drawText("System name : " + DeviceInfo.getInstance().getSystemName(), 30, yPos, textPaint);
        yPos += scaledSize*1.4;
        canvas.drawText("Channel size : " + DeviceInfo.getInstance().getChannelWidth() + " x " + DeviceInfo.getInstance().getChannelHeight(), 30, yPos, textPaint);
        yPos += scaledSize*1.8;
        canvas.drawText("Device size : " + DeviceInfo.getInstance().getDeviceWidth() + " x " + DeviceInfo.getInstance().getDeviceHeight(), 30, yPos, textPaint);
        yPos += scaledSize*1.4;
        canvas.drawText("Device name : " + DeviceInfo.getInstance().getManufacturer() + " " + DeviceInfo.getInstance().getModel(), 30, yPos, textPaint);
    }
}