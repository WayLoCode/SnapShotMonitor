package com.way.snapshotdetection.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Fuction:
 *
 * @author Way Lo
 * @date 2019/7/2
 */
public class DeviceUtil {

    /**
     * App 是否在前台运行
     * <p>
     * https://www.jianshu.com/p/e1d385bf8a03
     *
     * @param context
     * @return
     */
    public static boolean isRunningForeground(Context context) {
        ActivityManager activityManager = (ActivityManager)
            context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (null == activityManager) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
            .getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        String packageName = context.getApplicationContext().getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName) && appProcess.importance == ActivityManager
                .RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

}
