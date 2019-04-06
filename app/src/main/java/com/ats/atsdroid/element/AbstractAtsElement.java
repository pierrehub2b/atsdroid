package com.ats.atsdroid.element;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ats.atsdroid.R;
import com.ats.atsdroid.utils.AtsAutomation;
import com.ats.atsdroid.utils.TextRect;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public abstract class AbstractAtsElement {

    protected String id = UUID.randomUUID().toString();
    protected String tag;
    protected boolean clickable;
    protected Rect bounds = new Rect(0,0,0,0);

    protected boolean numeric;

    protected AccessibilityNodeInfo node;
    protected AbstractAtsElement[] children;

    public void loadBounds(AccessibilityNodeInfo node){
        node.getBoundsInScreen(bounds);
    }

    public synchronized void loadChildren(AccessibilityNodeInfo node){

        this.node = node;

        final int count = node.getChildCount();
        children = new AbstractAtsElement[count];
        for(int i=0; i<count; i++){
            children[i] = new AtsElement(node.getChild(i));
        }
    }

    public AbstractAtsElement[] getChildren(){
        return children;
    }

    public String getId(){
        return id;
    }

    public int getHeight(){
        return bounds.height();
    }

    public String getViewId(){return null;}

    //--------------------------------------------------------------------------------------------
    // Actions
    //--------------------------------------------------------------------------------------------

    public void click(AtsAutomation automation, int offsetX, int offsetY){
        node.refresh();
        node.getBoundsInScreen(bounds);
        automation.clickAt(bounds.left + offsetX, bounds.top + offsetY);

        automation.highlightElement(bounds);
    }

    public void clearText(AtsAutomation automation){
        if(node.getText() != null && node.getText().length() > 0) {

            int len = node.getText().length();

            final Bundle bdl = new Bundle();
            bdl.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
            bdl.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, bdl);
            automation.deleteBackButton();

            if(len > 0) { //well the field cannot be cleared this way ... let's try another way ...
                //node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                for (int i = 0; i < len; i++) {
                    automation.deleteBackButton();
                }

                len = node.getText().length(); // last chance to clear the field
                if(len >0) {
                    for (int i = 0; i < len; i++) {
                        automation.deleteForward();
                    }
                }
            }
        }
    }

    public void inputText(AtsAutomation automation, String value){
        if (numeric || isNumeric(value)) {
            for (String s : value.split("")) {
                if(".".equals(s) || ",".equals(s)){
                    automation.pressNumericKey(KeyEvent.KEYCODE_NUMPAD_DOT);
                }else{
                    try {
                        automation.pressNumericKey(KeyEvent.KEYCODE_0 + Integer.parseInt(s));
                    } catch (NumberFormatException e) {}
                }
            }
        } else {
            sendKeyString(value);
        }
    }

    private boolean sendKeyString(String value){
        final Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
    }

    public String swipe(AtsAutomation automation, int offsetX, int offsetY, int directionX, int directionY){
        node.refresh();
        node.getBoundsInScreen(bounds);
        automation.swipe(bounds.left + offsetX, bounds.top + offsetY, directionX, directionY);

        return (bounds.left + offsetX) + ":" + (bounds.top + offsetY);
    }

    //--------------------------------------------------------------------------------------------
    //
    //--------------------------------------------------------------------------------------------

    public JSONObject getJsonObject(){
        final JSONObject props = new JSONObject();
        try{
            props.put("id", id);
            props.put("tag", tag);
            props.put("clickable", clickable);
            props.put("x", bounds.left);
            props.put("y", bounds.top);
            props.put("width", bounds.width());
            props.put("height", bounds.height());

            final JSONArray childrenArray = new JSONArray();
            for (AbstractAtsElement child : children){
                childrenArray.put(child.getJsonObject());
            }
            props.put("children", childrenArray);
        } catch (JSONException e) {}
        return props;
    }

    public void drawElements(Canvas canvas, Resources res){

        for (AbstractAtsElement child : children){
            child.drawElements(canvas, res);
        }

        switch (tag){
            case "ImageView" :
                draw(canvas, BitmapFactory.decodeResource(res, R.drawable.image), 20, 75, 133,182);
                break;
            case "TextInputLayout" :
                node.refresh();
                drawText(canvas, node.getText(), -8);
                break;
            case "Switch" :
                node.refresh();
                if(node.isChecked()){
                    drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.button_toggle_selected));
                }else{
                    drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.button_toggle));
                }
                break;
            case "EditText" :
                node.refresh();
                drawInputText(canvas, node.getText(), node.isFocused(),36);
                break;
            case "Button" :
                drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.button_default));
                break;
            case "ImageButton" :
                drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.button_picture));
                break;
            case "RadioButton" :
                node.refresh();
                if(node.isChecked()){
                    drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.radio_button_selected));
                }else{
                    drawIcon(canvas, BitmapFactory.decodeResource(res, R.drawable.radio_button));
                }
                break;
            case "TextView" :
                node.refresh();
                drawText(canvas, node.getText(), 5);
                break;
            default:
                node.refresh();
                drawText(canvas, node.getText(), 5);
                break;
        }
    }

    private void drawInputText(Canvas canvas, CharSequence text, boolean focused, int offsetY){

        final Paint paint = new Paint();

        paint.setStyle(Paint.Style.FILL);
        if(focused) {
            paint.setColor(Color.argb(255, 255, 255, 255));
        }else{
            paint.setColor(Color.argb(255, 240, 240, 245));
        }
        canvas.drawRect(bounds, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setColor(Color.argb(255, 16, 111, 170));

        canvas.drawRect(bounds, paint);

        if(text != null && text.length() > 0){
            final Paint fontPaint = new Paint();
            fontPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
            fontPaint.setColor( Color.BLUE );
            fontPaint.setAntiAlias( true );
            fontPaint.setTextSize( 32 );

            canvas.drawText(text.toString(), bounds.left + 8, bounds.top + offsetY, fontPaint);
        }
    }

    private void drawText(Canvas canvas, CharSequence text, int offsetY) {
        if (text != null && text.length() > 0) {
            final TextRect textRect;
            {
                final Paint fontPaint = new Paint();
                fontPaint.setColor(Color.BLACK);
                fontPaint.setAntiAlias(true);
                fontPaint.setTextSize(32);

                textRect = new TextRect(fontPaint);
            }
            textRect.prepare(text.toString(), bounds.width(), bounds.height());
            textRect.draw(canvas, bounds.left + 5, bounds.top + offsetY);
        }
    }

    private void draw(Canvas canvas, Bitmap icon, int alpha, int red, int green, int blue){
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(alpha, red, green, blue);

        canvas.drawRect(bounds, paint);

        paint.setARGB(255, 255, 255, 255);
        paint.setXfermode( new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        canvas.drawBitmap(icon, bounds.left + 10, bounds.top + 10, paint);
    }

    private void drawIcon(Canvas canvas, Bitmap icon){
        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setARGB(255, 220, 220, 220);
        paint.setXfermode( new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        canvas.drawBitmap(icon, bounds.exactCenterX()-32, bounds.exactCenterY()-32, paint);
    }

    //------------------------------------------------------------------------------------------------

    private static boolean isNumeric(String strNum) {
        return strNum.matches("-?\\d+(\\.\\d+)?");
    }
}