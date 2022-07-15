package com.smartcloud.bluetoothclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText et_address;
    private EditText et_message_content;
    private TextView tv_message_received;
    private BluetoothSocket transferSocket;
    private Button btn_connect;
    private Button btn_message_send;

    final UUID uuid = UUID.randomUUID();
    final String TAG = "MainActivity";
    BluetoothDevice server;
    BluetoothAdapter bluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_address = findViewById(R.id.et_address);
        et_message_content = findViewById(R.id.et_message_content);

        tv_message_received = findViewById(R.id.tv_message_received);

        btn_connect = findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(this);

        btn_message_send = findViewById(R.id.btn_message_send);
        btn_message_send.setOnClickListener(this);
        btn_message_send.setEnabled(false);

        bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_message_send){
            //region 发送消息
            String message = et_message_content.getText().toString();
            if (!message.isEmpty()){
                if (transferSocket.isConnected() && transferSocket != null){
                    sendMessage(transferSocket, message);
                }
                else {
                    Log.e(TAG, "transferSocket 未连接");
                }
            }
            //endregion
        }
        else if (v.getId() == R.id.btn_connect){
            //region 连接服务器
            String address = et_address.getText().toString();
            if (!address.isEmpty()){
                server =  bluetooth.getRemoteDevice(address);
                new ConnectToServer().start();
            }
            //endregion
        }
    }


    class ConnectToServer extends Thread {
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public void run(){
            try {
                socket = server.createInsecureRfcommSocketToServiceRecord(uuid);
                socket =(BluetoothSocket) server.getClass().getMethod("createRfcommSocket" , new Class[] {int.class}).invoke(server,29);
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

            try {
                socket.connect();
                transferSocket = socket;

                //更新状态
                mHandler.sendEmptyMessage(1);

                //region 接收服务端发送的消息
                while (true){
                    try {
                        InputStream in = socket.getInputStream();
                        String msg = "";
                        BufferedReader bf = new BufferedReader(new InputStreamReader(in));
                        if((msg = bf.readLine()) != null){
                            msg = "收到服务端消息:" + msg;
                        }
                        Message message = Message.obtain();
                        message.what = 2;
                        message.obj = msg;
                        mHandler.sendMessage(message);
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
                //endregion
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }



    private void sendMessage(BluetoothSocket socket, String message) {
        OutputStream outStream;
        try {
            OutputStream os = socket.getOutputStream();
            PrintStream ps = new PrintStream(os);

            ps.println(message);
            ps.flush();

            Log.i(TAG, message);
        } catch (IOException e) {
            Log.e(TAG, "Message send failed.", e);
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 1){
                tv_message_received.setText("服务器连接成功....");

                btn_connect.setEnabled(false);
                btn_message_send.setEnabled(true);
            }
            else{
                String desc = tv_message_received.getText().toString();
                desc = String.format("%s\n%s", desc, msg.obj);
                tv_message_received.setText(desc);
            }
            return false;
        }
    });

    @SuppressLint("MissingPermission")
    private void getUuids(){
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = bluetooth.getBondedDevices();
        
        if (pairedDevices.size() > 0) {
            int i = 0;
            for (BluetoothDevice device : pairedDevices) {

                String desc ="\nDevices Name:" + device.getName() +
                        "\nDevice MacAddress:" + device.getAddress() +
                        "\nDevices UUID:" + device.getUuids()[0].toString() + "\n";

                System.out.println(desc);
            }
        }
    }
}