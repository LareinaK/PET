package com.vejoe.picar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by Administrator on 2017/3/15 0005.
 */
public class MjpegSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    //display position of fps string
    public final static int POSITION_UPPER_LEFT  = 9;
    public final static int POSITION_UPPER_RIGHT = 3;
    public final static int POSITION_LOWER_LEFT  = 12;
    public final static int POSITION_LOWER_RIGHT = 6;

    //video size
    public final static int SIZE_STANDARD   = 1;
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;

    private MjpegViewThread thread;
    private MjpegInputStream dataIn = null;
    private boolean showFps = false;
    private boolean isRun = false;
    private boolean surfaceDone = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayTextBgColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;

    public MjpegSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        thread = new MjpegViewThread(holder);

        setFocusable(true);
        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);

        overlayTextColor = Color.WHITE;
        overlayTextBgColor = Color.BLACK;
        ovlPos = POSITION_LOWER_RIGHT;
        dispWidth = getWidth();
        dispHeight = getHeight();
    }

    public void startPlayBack() {
        if (dataIn != null) {
            isRun = true;
            thread.start();
        }
    }

    public void stopPlayBack() {
        isRun = false;
        boolean retry = true;
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public void setSource(MjpegInputStream source) {
        dataIn = source;
        startPlayBack();
    }

    public void setOverlayPvosition(int pos) {
        this.ovlPos = pos;
    }

    public void setDisplayMode(int mode) {
        this.displayMode = mode;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        surfaceDone = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int f, int w, int h) {
        thread.setSurfaceSize(w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        surfaceDone = false;
        stopPlayBack();
    }

    //Internal class for image rendering
    private class MjpegViewThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private int frameCounter = 0;
        private long start;
        private Bitmap overlay;

        public MjpegViewThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (surfaceHolder) {
                dispWidth = width;
                dispHeight = height;
            }
        }

        @Override
        public void run() {
            start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);
            Bitmap bmp;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps;
            while (isRun) {
                if (surfaceDone) {
                    try {
                        c = surfaceHolder.lockCanvas();
                        synchronized (surfaceHolder) {
                            try {
                                bmp = dataIn.readMjpegFrame();
                                destRect = destRect(bmp.getWidth(), bmp.getHeight());
                                c.drawColor(Color.BLACK);
                                c.drawBitmap(bmp, null, destRect, p);
                                if (showFps) {
                                    p.setXfermode(mode);
                                    if (overlay != null) {
                                        height = ((ovlPos & 1) == 1)? destRect.top : destRect.bottom - overlay.getHeight();
                                        width = ((ovlPos & 8) == 8) ? destRect.left : destRect.right - overlay.getWidth();
                                        c.drawBitmap(overlay, width, height, null);
                                    }
                                    p.setXfermode(null);

                                    frameCounter ++;
                                    if ((System.currentTimeMillis() - start) >= 1000) {
                                        fps = String.valueOf(frameCounter) + "fps";
                                        frameCounter = 0;
                                        start = System.currentTimeMillis();
                                        overlay = makeFpsOverlay(overlayPaint, fps);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } finally {
                        if (c != null) surfaceHolder.unlockCanvasAndPost(c);
                    }

                }
            }
        }

        private Rect destRect(int bmpWidth, int bmpHeight) {
            int tempX;
            int tempY;
            if (displayMode == MjpegSurfaceView.SIZE_STANDARD) {
                tempX = (dispWidth - bmpWidth) / 2;
                tempY = (dispHeight - bmpHeight) / 2;
                return new Rect(tempX, tempY, bmpWidth + tempX, bmpHeight + tempY);
            }

            if (displayMode == MjpegSurfaceView.SIZE_BEST_FIT) {
                float bmpRatio = (float) bmpWidth / (float) bmpHeight;
                bmpWidth = dispWidth;
                bmpHeight = (int) (dispWidth / bmpRatio);
                if (bmpHeight > dispHeight) {
                    bmpHeight = dispHeight;
                    bmpWidth = (int) (dispHeight * bmpRatio);
                }

                tempX = (dispWidth - bmpWidth) / 2;
                tempY = (dispHeight - bmpHeight) / 2;
                return new Rect(tempX, tempY, bmpWidth + tempX, bmpHeight + tempY);
            }

            if (displayMode == MjpegSurfaceView.SIZE_FULLSCREEN) {
                return new Rect(0, 0, dispWidth, dispHeight);
            }

            return null;
        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int width = b.width() + 2;
            int height = b.height() + 2;
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            p.setColor(overlayTextBgColor);
            c.drawRect(0, 0, width, height, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1, (height / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
            return bmp;
        }
    }
}
