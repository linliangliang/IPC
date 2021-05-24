// IMessagerListener.aidl
package com.liang.ipc;
import com.liang.ipc.entity.MyMessage;
// Declare any non-default types here with import statements

interface IMessagerListener {
    void onReceviceMessager(in MyMessage message);
}