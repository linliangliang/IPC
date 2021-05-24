// IServiceManager.aidl
package com.liang.ipc;

// Declare any non-default types here with import statements

interface IServiceManager {
    IBinder getService(String serviceName);
}