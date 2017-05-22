package com.vejoe.picar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String CTR_URL = "http://192.168.12.1:9001/cmd";
    private static final String VIDEO_STREAM_URL = "http://192.168.12.1:8080/?action=stream";
    private SocketUtil socketUtil;
    private MjpegSurfaceView mjpegSurfaceView;
    private boolean useSocket = false;//use socket or use http
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //prohibit lock screen, also can use View.setKeepScreenOn(Boolean)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        InitHandler();
        //network connection cannot be done in main thread.
        if (!useSocket) {
            new ConnectTask().execute();
        } else {
            socketUtil = new SocketUtil();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    socketUtil.connect();
                }
            }).start();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("Exit?")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                        if (socketUtil != null)
                            socketUtil.closeSocket();
                        mjpegSurfaceView.stopPlayBack();
                    }
                })
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing here
                    }
                }).show();
    }

    public void InitHandler() {
        //get WiFi service
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //whether the WiFi is open
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        (findViewById(R.id.btnForward)).setOnTouchListener(touchListener);
        (findViewById(R.id.btnBack)).setOnTouchListener(touchListener);
        (findViewById(R.id.btnLeft)).setOnTouchListener(touchListener);
        (findViewById(R.id.btnRight)).setOnTouchListener(touchListener);

        mjpegSurfaceView = (MjpegSurfaceView) findViewById(R.id.mjpeg_stream_view);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mjpegSurfaceView.stopPlayBack();
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleActionDown(view);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    handleActionUp();
                default:
            }
            return true;
        }
    };

    private void handleActionDown(View v) {
        switch (v.getId()) {
            case R.id.btnForward:
                if (!useSocket) turn("t_up");
                else turn((byte) 0x01);
                break;
            case R.id.btnBack:
                if (!useSocket) turn("t_down");
                else turn((byte) 0x02);
                break;
            case R.id.btnLeft:
                if (!useSocket) turn("t_left");
                else turn((byte) 0x03);
                break;
            case R.id.btnRight:
                if (!useSocket) turn("t_right");
                else turn((byte) 0x04);
                break;
        }
    }

    private void handleActionUp() {
        //stop
        if (!useSocket) turn("t_stop");
        else turn((byte) 0x00);
    }

    //http method
    private void turn(final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String requestUrl = CTR_URL;
            Map<String, String> requestParams = new HashMap<String, String>();
            requestParams.put("id", id);
            HttpUtils.submitPostData(requestUrl, requestParams, "utf-8");
            }
        }).start();
    }

    //socket method
    byte[] data = {(byte) 0xff, 0x00, 0x00, 0x00, (byte) 0xff};
    private void turn(final byte id) {
        data[2] = id;
        socketUtil.sendData(data);
    }

    private class ConnectTask extends AsyncTask<Void, Void, MjpegInputStream> {
        private ProgressDialog dialog;

        public ConnectTask() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.setMessage("Connecting, please wait a moment...");
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected MjpegInputStream doInBackground(Void... voids) {
            //network connection cannot be done in main thread.
            return MjpegInputStream.read(VIDEO_STREAM_URL);
        }

        @Override
        protected void onPostExecute(MjpegInputStream stream) {
            dialog.dismiss();
            if (stream != null) {
                mjpegSurfaceView.setDisplayMode(MjpegSurfaceView.SIZE_FULLSCREEN);
                mjpegSurfaceView.setShowFps(true);
                mjpegSurfaceView.setOverlayPvosition(MjpegSurfaceView.POSITION_UPPER_RIGHT);
                mjpegSurfaceView.setSource(stream);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Warn");
                builder.setCancelable(true);
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage("Failed to connect to video streaming server!");
                builder.setPositiveButton("OK", null);
                builder.create().show();
            }
        }
    }
}
