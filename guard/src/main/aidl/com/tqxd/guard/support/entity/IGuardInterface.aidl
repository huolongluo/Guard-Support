// IGuardInterface.aidl
package com.tqxd.guard.support.entity;

// Declare any non-default types here with import statements
import com.tqxd.guard.support.entity.GuardConfig;

interface IGuardInterface {
    void wakeup(in GuardConfig config);
    void connectionTimes(in int time);
}