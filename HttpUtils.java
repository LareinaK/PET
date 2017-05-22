package com.vejoe.picar;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Administrator on 2017/3/15 0005.
 */
public class HttpUtils {

    /**
     * Send Post request to server?
     * @param strUrlPath Address of server, in string format.
     * @param params request content
     * @param encode encodes the content use the Charset named by encode
     * @return
     */
    public static String submitPostData(String strUrlPath, Map<String, String> params, String encode) {
        byte[] data = getRequestData(params, encode).toString().getBytes();
        try {

            URL url = new URL(strUrlPath);

            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setConnectTimeout(30000);//set connection timeout
            httpURLConnection.setDoInput(true);//open the input stream to get data from server
            httpURLConnection.setDoOutput(true);//open the output stream to submit data to server
            httpURLConnection.setRequestMethod("POST");//set to submit data in Post mode
            httpURLConnection.setUseCaches(false);//cannot use cache in Post mode
            //set the type of the request body to text type
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //set the length of the request body
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(data.length));
            //get the output stream and writes data to the server
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(data);

            int response = httpURLConnection.getResponseCode();//get server response code
            Log.e("HttpUtils", "response:" + response);
            if(response == HttpURLConnection.HTTP_OK) {
                InputStream inptStream = httpURLConnection.getInputStream();
                return dealResponseResult(inptStream);//processing server response results
            }
        } catch (IOException e) {
            //e.printStackTrace();
            return "err: " + e.getMessage().toString();
        }
        return "-1";
    }

    /**
     * Encapsulated request body information
     * @param params request content
     * @param encode encodes the content use the Charset named by encode
     * @return
     */
    public static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();
        try {
            for(Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);//delete the last "&"
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    /**
     * Processing server response results (converting input stream to string)
     * @param inputStream input stream
     * @return
     */
    public static String dealResponseResult(InputStream inputStream) {
        String resultData = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len = 0;
        try {
            while((len = inputStream.read(data)) != -1) {
                byteArrayOutputStream.write(data, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }
}
