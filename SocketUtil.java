package com.vejoe.picar;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by Administrator on 2017/3/15 0005.
 */
public class SocketUtil {
    private final String SERVER_IP = "192.168.12.1";
    private final int CONTROL_PORT = 9001;

    private Socket socket = null;
    private OutputStream os;
    private boolean isConnected = false;
    public SocketUtil() {
    }

    public void connect() {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(SERVER_IP, CONTROL_PORT), 5000);
            os = socket.getOutputStream();
            isConnected = true;

            Log.e(getClass().getSimpleName(), "isConnected:" + isConnected);
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false;
            if (null != socket)
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    }

    public void closeSocket() {
        if (null != socket)
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (null != os)
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        isConnected = false;
    }

    public void sendData(byte[] data) {
        if (!isConnected)
            return;

        try {
            Log.e(getClass().getSimpleName(), "sendData");
            os.write(data);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
