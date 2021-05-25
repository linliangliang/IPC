package com.liang.ipc.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

        initStateBar();
        init();
        initData();
    }

    private void initStateBar() {
        Window window = getWindow();
        /*//设置状态栏为透明，并不占据空间，需要添加一个占位控件，HUAWEI7.0显示为半透明状态栏，11.0显示为全透明
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //添加站位控件*/

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //取消设置Window半透明的Flag
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //添加Flag把状态栏设为可绘制模式
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            //设置状态栏颜色
            window.setStatusBarColor(getResources().getColor(R.color.main));
            //设置系统状态栏处于可见状态
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            //让view不根据系统窗口来调整自己的布局
            ViewGroup mContentView = (ViewGroup) window.findViewById(Window.ID_ANDROID_CONTENT);
            View mChildView = mContentView.getChildAt(0);
            if (mChildView != null) {
                ViewCompat.setFitsSystemWindows(mChildView, false);
                ViewCompat.requestApplyInsets(mChildView);
            }
        } else {

        }
        initStateBarTextColor();
    }

    private void initStateBarTextColor() {
        //在Android 6.0的Api中提供了SYSTEM_UI_FLAG_LIGHT_STATUS_BAR这么一个常量，可以使状态栏文字设置为黑色，但对6.0以下是不起作用的。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//19 4.4
            //判断是否为小米或魅族手机，如果是则将状态栏文字改为黑色
            /*if (MIUISetStatusBarLightMode(activity, true) || FlymeSetStatusBarLightMode(activity, true)) {
                //设置状态栏为指定颜色
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0
                    activity.getWindow().setStatusBarColor(color);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4
                    //调用修改状态栏颜色的方法
                    setStatusBarColor(activity, color);
                }
            } else*/ if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //如果是6.0以上将状态栏文字改为黑色
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                
                //fitsSystemWindow 为 false, 不预留系统栏位置.
                ViewGroup mContentView = (ViewGroup) getWindow().findViewById(Window.ID_ANDROID_CONTENT);
                View mChildView = mContentView.getChildAt(0);
                if (mChildView != null) {
                    ViewCompat.setFitsSystemWindows(mChildView, true);
                    ViewCompat.requestApplyInsets(mChildView);
                }
            }
        }
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