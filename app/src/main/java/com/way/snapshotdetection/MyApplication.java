package com.way.snapshotdetection;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Created by dliang.wang on 2017/4/12.
 */

public class MyApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
    }
}
