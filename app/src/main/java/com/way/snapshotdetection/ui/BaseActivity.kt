package com.way.snapshotdetection.ui

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity

import com.way.snapshotdetection.callback.SnapShotTakeCallBack
import com.way.snapshotdetection.manager.ContentObserverManager
import com.way.snapshotdetection.manager.FileObserverManager

/**
 * Fuction:
 *
 * @author Way Lo
 * @date 2019/7/2
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()

        FileObserverManager.setSnapShotCallBack(SnapShotTakeCallBack(this))
        FileObserverManager.startSnapshotWatching()

        ContentObserverManager.instance(this).startListener()
        ContentObserverManager.instance(this).setListener(object : ContentObserverManager.OnScreenShotListener {
            override fun onShot(imagePath: String) {

            }

        })
    }

    override fun onPause() {
        super.onPause()

        FileObserverManager.stopSnapshotWatching()

        ContentObserverManager.instance(this).stopListener()
    }
}
