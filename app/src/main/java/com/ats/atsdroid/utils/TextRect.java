package com.ats.atsdroid.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;

/**
 * Draw text in a given rectangle and automatically wrap lines.
 * This class is designed to be used in games and therefore tries
 * to minimize allocations after instantiation because those will
 * trigger the GC more often which causes a slight but noticeable
 * delay. So, if you want to use this in a game, you should
 * make the instance a class member, allocate it once and reuse it.
 *
 * @author mf@markusfisch.de
 */
public class TextRect
{
    // maximum number of lines; this is a fixed number in order
    // to use a predefined array to avoid ArrayList (or something
    // similar) because filling it does involve allocating memory
    static private int MAX_LINES = 256;

    // those members are stored per instance to minimize
    // the number of allocations to avoid triggering the
    // GC too much
    private FontMetricsInt metrics = null;
    private Paint paint = null;
    private int starts[] = new int[MAX_LINES];
    private int stops[] = new int[MAX_LINES];
    private int lines = 0;
    private int textHeight = 0;
    private Rect bounds = new Rect();
    private String text = null;
    private boolean wasCut = false;

    /**
     * Create reusable text rectangle (use one instance per font).
     *
     * @param paint - paint specifying the font
     */
    public TextRect( final Paint paint )
    {
        metrics = paint.getFontMetricsInt();
        this.paint = paint;
    }

    public int prepare(
            final String text,
            final int maxWidth,
            final int maxHeight )
    {
        lines = 0;
        textHeight = 0;
        this.text = text;
        wasCut = false;

        // get maximum number of characters in one line
        paint.getTextBounds(
                "i",
                0,
                1,
                bounds );

        final int maximumInLine = maxWidth / bounds.width();
        final int length = text.length();

        if( length > 0 )
        {
            final int lineHeight = -metrics.ascent + metrics.descent;
            int start = 0;
            int stop = maximumInLine > length ? length : maximumInLine;

            for( ;; )
            {
                // skip LF and spaces
                for( ; start < length; ++start )
                {
                    char ch = text.charAt( start );

                    if( ch != '\n' &&
                            ch != '\r' &&
                            ch != '\t' &&
                            ch != ' ' )
                        break;
                }

                for( int o = stop + 1; stop < o && stop > start; )
                {
                    o = stop;

                    int lowest = text.indexOf( "\n", start );

                    paint.getTextBounds(
                            text,
                            start,
                            stop,
                            bounds );

                    if( (lowest >= start && lowest < stop) ||
                            bounds.width() > maxWidth )
                    {
                        --stop;

                        if( lowest < start ||
                                lowest > stop )
                        {
                            final int blank = text.lastIndexOf( " ", stop );
                            final int hyphen = text.lastIndexOf( "-", stop );

                            if( blank > start &&
                                    (hyphen < start || blank > hyphen) )
                                lowest = blank;
                            else if( hyphen > start )
                                lowest = hyphen;
                        }

                        if( lowest >= start &&
                                lowest <= stop )
                        {
                            final char ch = text.charAt( stop );

                            if( ch != '\n' &&
                                    ch != ' ' )
                                ++lowest;

                            stop = lowest;
                        }

                        continue;
                    }

                    break;
                }

                if( start >= stop )
                    break;

                int minus = 0;

                // cut off lf or space
                if( stop < length )
                {
                    final char ch = text.charAt( stop - 1 );

                    if( ch == '\n' ||
                            ch == ' ' )
                        minus = 1;
                }

                if( textHeight + lineHeight > maxHeight )
                {
                    wasCut = true;
                    break;
                }

                starts[lines] = start;
                stops[lines] = stop - minus;

                if( ++lines > MAX_LINES )
                {
                    wasCut = true;
                    break;
                }

                if( textHeight > 0 )
                    textHeight += metrics.leading;

                textHeight += lineHeight;

                if( stop >= length )
                    break;

                start = stop;
                stop = length;
            }
        }

        return textHeight;
    }

    /**
     * Draw prepared text at given position.
     *
     * @param canvas - canvas to draw text into
     * @param left - left corner
     * @param top - top corner
     */
    public void draw(
            final Canvas canvas,
            final int left,
            final int top )
    {
        if( textHeight == 0 )
            return;

        final int before = -metrics.ascent;
        final int after = metrics.descent + metrics.leading;
        int y = top;

        --lines;
        for( int n = 0; n <= lines; ++n )
        {
            String t;

            y += before;

            if( wasCut &&
                    n == lines &&
                    stops[n] - starts[n] > 3 )
                t = text.substring( starts[n], stops[n] - 3 ).concat( "..." );
            else
                t = text.substring( starts[n], stops[n] );

            canvas.drawText(
                    t,
                    left,
                    y,
                    paint );

            y += after;
        }
    }

    /** Returns true if text was cut to fit into the maximum height */
    public final boolean wasCut()
    {
        return wasCut;
    }
}
