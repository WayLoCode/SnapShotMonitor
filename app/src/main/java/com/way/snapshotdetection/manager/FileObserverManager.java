package com.way.snapshotdetection.manager;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import com.way.snapshotdetection.callback.ISnapShotCallBack;

import java.io.File;

/**
 * FileObserver监听截屏
 * <p>
 * https://www.jianshu.com/p/2e6d52abf115
 * <p>
 * Created by dliang.wang on 2017/4/12.
 */

public class FileObserverManager {

    private final static String TAG = FileObserverManager.class.getSimpleName();

    private static FileObserver fileObserver;
    private static ISnapShotCallBack snapShotCallBack;
    public static String SNAP_SHOT_FOLDER_PATH;
    private static String lastShownSnapshot;
    private static final int MAX_TRYS = 2;

    public static void setSnapShotCallBack(ISnapShotCallBack callBack) {
        snapShotCallBack = callBack;
        initFileObserver();
    }

    private static void initFileObserver() {
        SNAP_SHOT_FOLDER_PATH = Environment.getExternalStorageDirectory()
            + File.separator + Environment.DIRECTORY_PICTURES
            + File.separator + "Screenshots" + File.separator;

        fileObserver = new FileObserver(SNAP_SHOT_FOLDER_PATH, FileObserver.CREATE) {
            @Override
            public void onEvent(int event, String path) {
                if (null != path && event == FileObserver.CREATE && (!path.equals(lastShownSnapshot))) {
                    // 有些手机同一张截图会触发多个CREATE事件，避免重复展示
                    lastShownSnapshot = path;

                    String snapShotFilePath = SNAP_SHOT_FOLDER_PATH + path;

                    int tryTimes = 0;
                    while (true) {
                        try {
                            // 收到CREATE事件后马上获取并不能获取到，需要延迟一段时间
                            Thread.sleep(600);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            BitmapFactory.decodeFile(snapShotFilePath);
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                            tryTimes++;
                            // 尝试MAX_TRYS次失败后，放弃
                            if (tryTimes >= MAX_TRYS) {
                                return;
                            }
                        }
                    }

                    Log.e(TAG, "Find screenshot: path = " + path);
                    snapShotCallBack.snapShotTaken(path);
                }
            }
        };
    }

    public static void startSnapshotWatching() {
        if (null == snapShotCallBack) {
            throw new ExceptionInInitializerError("Call FileObserverUtils.setSnapShotCallBack " +
                "first to setup callback!");
        }

        fileObserver.startWatching();
    }

    public static void stopSnapshotWatching() {
        if (null == snapShotCallBack) {
            throw new ExceptionInInitializerError("Call FileObserverUtils.setSnapShotCallBack " +
                "first to setup callback!");
        }

        fileObserver.stopWatching();
    }
}
