// IConnectService.aidl
package com.liang.ipc;

// 连接服务
interface IConnectService {

    //oneway 关键字 调用后主进程不会阻塞，特点是该方法不能有返回值
    oneway void connect();

    void disconnet();

    boolean isConnected();
}