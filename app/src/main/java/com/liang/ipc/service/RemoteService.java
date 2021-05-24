package com.liang.ipc.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.liang.ipc.IConnectService;
import com.liang.ipc.IMessagerListener;
import com.liang.ipc.IMessagerService;
import com.liang.ipc.IServiceManager;
import com.liang.ipc.entity.MyMessage;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  实现远程的链接服务和消息发生服务
 *  连接服务
 *  发生消息服务
 */
public class RemoteService extends Service {

    private boolean isConnected = false;
    private RemoteCallbackList<IMessagerListener> mListenersArray = new RemoteCallbackList<IMessagerListener>();
    private ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private Messenger mClientMessenger = null;

    private ScheduledFuture mScheduledFuture;
    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            //接受 activity发送过来的消息
            mClientMessenger = msg.replyTo;
            Bundle bundle = msg.getData();
            bundle.setClassLoader(MyMessage.class.getClassLoader());//主要bundle反序列化的时候 设置这个对象的classLoader 否则肯出现序列号异常的问题
            MyMessage myMessage = bundle.getParcelable("message");

            Toast.makeText(RemoteService.this, myMessage.getContent(),Toast.LENGTH_SHORT).show();


            try {
                //返回数据给Activitu
                MyMessage myMessage1 = new MyMessage();
                myMessage1.setContent("reply from remote");
                Bundle bundle1 = new Bundle();
                bundle1.putParcelable("message" ,myMessage1);
                Message message = new Message();
                message.setData(bundle1);
                mClientMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    private Messenger mMessenger = new Messenger(mHandler);

    private IConnectService mIConnectService = new IConnectService.Stub() {
        @Override
        public void connect() throws RemoteException {
            try {
                Thread.sleep(5000);
                isConnected = true;
                mHandler.post(new Runnable() {//在主线程中toast
                    @Override
                    public void run() {
                        Toast.makeText(RemoteService.this,"connect", Toast.LENGTH_SHORT).show();
                    }
                });


                //启动一个线程不停的向主进程发送数据
                mScheduledFuture = mScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        int size = mListenersArray.beginBroadcast();
                        for(int i = 0; i < size; i++){
                            MyMessage myMessage = new MyMessage();
                            myMessage.setContent("remoteService发送消息给activity");
                            try {
                                IMessagerListener messagerListener = mListenersArray.getBroadcastItem(i);
                                messagerListener.onReceviceMessager(myMessage);
                                Log.i("linliang","send");
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        mListenersArray.finishBroadcast();
                    }
                },5000,5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void disconnet() throws RemoteException {
            isConnected = false;
            mScheduledFuture.cancel(true);
            mHandler.post(new Runnable() {//在主线程中toast
                @Override
                public void run() {
                    Toast.makeText(RemoteService.this,"disconnet", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public boolean isConnected() throws RemoteException {
            return isConnected;
        }
    };

    IMessagerService mIMessagerService = new IMessagerService.Stub(){
        @Override
        public void sendMessage(MyMessage message) throws RemoteException {
            //接受 主进程发送过来的消息
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(RemoteService.this,message.getContent(),Toast.LENGTH_SHORT).show();
                }
            });

            if(isConnected == true){
                message.setSendSuccess(true);
            } else {
                message.setSendSuccess(false);
            }

        }

        @Override
        public void registerListener(IMessagerListener listener) throws RemoteException {
            //获取主进程的activity用于向activity发送数据
            if(listener != null){
                mListenersArray.register(listener);
            }
        }

        @Override
        public void unregisterListener(IMessagerListener listeener) throws RemoteException {
            if(listeener != null){
                mListenersArray.unregister(listeener);//注册和取消注册 的不是用一个对象 反序列化导致的
            }
        }
    };


    IServiceManager mIServiceManager = new IServiceManager.Stub() {
        @Override
        public IBinder getService(String serviceName) throws RemoteException {
            if(IConnectService.class.getSimpleName().equals(serviceName)){
                return mIConnectService.asBinder();
            } else if (IMessagerService.class.getSimpleName().equals(serviceName)){
                return mIMessagerService.asBinder();
            } else if(Messenger.class.getSimpleName().equals(serviceName)){
                return mMessenger.getBinder();
            }
            return null;
        }
    };

    public RemoteService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {

        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mIServiceManager.asBinder();
    }
}