// IMessagerService.aidl
package com.liang.ipc;
import com.liang.ipc.entity.MyMessage;
import com.liang.ipc.IMessagerListener;

// Declare any non-default types here with import statements

interface IMessagerService {

    void sendMessage(inout MyMessage message);//主进程调用 发送数据给 子进程

    void registerListener(IMessagerListener listeener);//主进程注册 让子进程获取到 主进程activity的引用 用户反悔数据，实现双向通讯

    void unregisterListener(IMessagerListener listeener);//这里需要在定义一个Listener接口 ，被传入的对象需要实现该接口
}