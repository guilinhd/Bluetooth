package com.guilinhd.bluetoothserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private TextView tv_bluetooth_receive;
    private Button btn_bluetooth_startServer;
    final UUID uuid = UUID.randomUUID();

    private static ArrayList<BluetoothSocket> sockets = new ArrayList<>();

    //Friendly name to match while discovering
    private static final String SEARCH_NAME = "bluetooth.recipe";
    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    BluetoothAdapter mBtAdapter;
    BluetoothSocket mBtSocket;

    // 使用静态变量记住一个线程池对象
    private static ExecutorService pool = new ThreadPoolExecutor(30,
            60, 6, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10)
            , Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_bluetooth_receive = findViewById(R.id.tv_bluetooth_receive);
        btn_bluetooth_startServer = findViewById(R.id.btn_bluetooth_startServer);
        btn_bluetooth_startServer.setOnClickListener(this);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_bluetooth_startServer) {
            tv_bluetooth_receive.setText("");
            new startServer().start();
        }
    }

    @SuppressLint("MissingPermission")
    class startServer extends Thread{
        @Override
        public void run(){
            try {
                BluetoothServerSocket server = mBtAdapter.listenUsingRfcommWithServiceRecord("BluetoothRecipe", uuid);
                server = (BluetoothServerSocket) mBtAdapter.getClass().getMethod("listenUsingRfcommOn", new Class[]{int.class}).invoke(mBtAdapter, new Object[]{ 29});
                //服务器启动成功
                mHandler.sendEmptyMessage(1);

                while (true){
                    BluetoothSocket socket = server.accept();

                    //region 显示客户端上线信息
                    Message message = Message.obtain();
                    message.what = 2;
                    message.obj = socket.getRemoteDevice().getName() + ", 上线了";
                    mHandler.sendMessage(message);
                    //endregion

                    sockets.add(socket);
                    Log.i(TAG, "sockets count:" + String.valueOf(sockets.size()));

                    pool.execute(new SocketRunnable(mHandler, socket, sockets));
                }
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 1){
                tv_bluetooth_receive.setText("服务器端启动成功..");
                btn_bluetooth_startServer.setEnabled(false);
            }
            else {
                String desc = tv_bluetooth_receive.getText().toString();
                desc = String.format("%s\n%s", desc, msg.obj);
                tv_bluetooth_receive.setText(desc);
            }
            return false;
        }
    });

    @SuppressLint("MissingPermission")
    private void getUuids(){
        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

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