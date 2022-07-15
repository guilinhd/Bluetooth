package com.guilinhd.bluetoothserver;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SocketRunnable implements Runnable {
    private final String TAG = "SocketRunnable";
    private Handler handler;
    private BluetoothSocket socket;
    private ArrayList<BluetoothSocket> sockets;
    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SocketRunnable(Handler handler, BluetoothSocket socket, ArrayList<BluetoothSocket> sockets){
        this.handler = handler;
        this.socket = socket;
        this.sockets = sockets;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        try {
            //region 接收客户端发的消息
            InputStream is = socket.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String msg = "";

            while ((msg = br.readLine()) != null){
                //应答客户端
                sendMessage(msg);

                //region 更新消息
                Message message = Message.obtain();
                message.what = 3;
                message.obj = socket.getRemoteDevice().getName() + "说:"
                        + msg + ", 时间:"
                        + sd.format(new Date());
                handler.sendMessage(message);
                //endregion
            }
            //endregion
        }
        catch (Exception ex){
            //region 客户端下线
            Message message = Message.obtain();
            message.what = -1;
            message.obj = socket.getRemoteDevice().getName() + ", 下线了";

            handler.sendMessage(message);
            //endregion

            sockets.remove(socket);
        }
    }

    private void sendMessage(String message){
        try {
            OutputStream os = socket.getOutputStream();
            PrintStream ps = new PrintStream(os);

            ps.println(message);
            ps.flush();

        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
