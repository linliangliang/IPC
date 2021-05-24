package com.liang.ipc.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.liang.ipc.IConnectService;
import com.liang.ipc.IMessagerListener;
import com.liang.ipc.IMessagerService;
import com.liang.ipc.IServiceManager;
import com.liang.ipc.R;
import com.liang.ipc.entity.MyMessage;
import com.liang.ipc.service.RemoteService;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private Button mConnectButton;
    private Button mDisconnectButton;
    private Button mConnectStatueButton;

    private Button mButtonSendMessage;
    private Button mButtonRegisterListener;
    private Button mButtonUnregisterListener;
    private Button mButtonSendByMessenger;
    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            //接受来着remoteService的消息
            Bundle bundle = msg.getData();
            bundle.setClassLoader(MyMessage.class.getClassLoader());
            final MyMessage message = bundle.getParcelable("message");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message.getContent().toString(),Toast.LENGTH_SHORT).show();
                }
            }, 3000);

        }
    };

    private ServiceConnection mServiceConnection = null;

    private IConnectService mIConnectServiceProxy = null;//binder代理类
    private IMessagerService mIMessagerServiceProxy = null;
    private IServiceManager mIServiceManagerProxy = null;

    private Messenger mMessengerProxy = null;//来自于remountService,
    private Messenger mClientMessengerPorxy = new Messenger(mHandler);//客户端的messenger实现


    private IMessagerListener mIMessagerListener = new IMessagerListener.Stub() {
        @Override
        public void onReceviceMessager(MyMessage message) throws RemoteException {
            Log.i("linliang", "get" + message.getContent().toString());
            //来着remote Service的消息, 该方法在binder的子线程中
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message.getContent().toString(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        initData();
    }

    private void init() {
        mConnectButton = findViewById(R.id.connect_button);
        mDisconnectButton = findViewById(R.id.disconnect_button);
        mConnectStatueButton = findViewById(R.id.connect_status_button);
        mButtonSendMessage = findViewById(R.id.btn_send_message);
        mButtonRegisterListener = findViewById(R.id.btn_register_listener);
        mButtonUnregisterListener = findViewById(R.id.btn_unregister_listener);
        mButtonSendByMessenger = findViewById(R.id.btn_send_by_messager);

    }
    private void initData(){
        mConnectButton.setOnClickListener(this);
        mDisconnectButton.setOnClickListener(this);
        mConnectStatueButton.setOnClickListener(this);
        mButtonSendMessage.setOnClickListener(this);
        mButtonRegisterListener.setOnClickListener(this);
        mButtonUnregisterListener.setOnClickListener(this);
        mButtonSendByMessenger.setOnClickListener(this);
        connectRemoteSevice();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.connect_button:
                try {
                    mIConnectServiceProxy.connect();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
             case R.id.disconnect_button:
                 try {
                     mIConnectServiceProxy.disconnet();
                 } catch (RemoteException e) {
                     e.printStackTrace();
                 }
                 break;
             case R.id.connect_status_button:
                 try {
                     boolean isConnected = mIConnectServiceProxy.isConnected();
                     Toast.makeText(this, "" + isConnected, Toast.LENGTH_SHORT).show();
                 } catch (RemoteException e) {
                     e.printStackTrace();
                 }
                 break;
            case R.id.btn_send_message:
                MyMessage myMessage = new MyMessage();
                myMessage.setContent("main activity发送数据");
                try {
                    mIMessagerServiceProxy.sendMessage(myMessage);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_register_listener:
                try {
                    mIMessagerServiceProxy.registerListener(mIMessagerListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_unregister_listener:
                try {
                    //会执行反序列化
                    mIMessagerServiceProxy.unregisterListener(mIMessagerListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_send_by_messager:
                MyMessage myMessage1 = new MyMessage();
                myMessage1.setContent("send message  by Messenger");
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putParcelable("message", myMessage1);
                message.setData(bundle);
                message.replyTo = mClientMessengerPorxy;
                try {
                    mMessengerProxy.send(message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                };
                break;
        }
    }


    private void connectRemoteSevice() {
        Intent intent = new Intent(MainActivity.this, RemoteService.class);
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                //mIConnectServiceProxy = IConnectService.Stub.asInterface(service);
                mIServiceManagerProxy  = IServiceManager.Stub.asInterface(service);
                try {
                    mIConnectServiceProxy = IConnectService.Stub.asInterface(mIServiceManagerProxy.getService(IConnectService.class.getSimpleName()));
                    mIMessagerServiceProxy = IMessagerService.Stub.asInterface(mIServiceManagerProxy.getService(IMessagerService.class.getSimpleName()));
                    mMessengerProxy = new Messenger(mIServiceManagerProxy.getService(Messenger.class.getSimpleName()));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
    }
}