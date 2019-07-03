package com.way.snapshotdetection.manager

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import com.way.snapshotdetection.MyApplication
import com.way.snapshotdetection.utils.DeviceUtil
import java.util.*

/**
 * Fuction:ContentObserver监听截屏
 *
 *
 * https://www.jianshu.com/p/d7aba5a03b0f
 *
 * @author Way Lo
 * @date 2019/7/2
 */
class ContentObserverManager private constructor(private val mContext: Context) {

    /**
     * 已回调过的路径
     */
    private val sHasCallbackPaths = LinkedList<String>()

    private var mListener: OnScreenShotListener? = null

    private var mStartListenTime: Long = 0

    private var mInternalObserver: MediaContentObserver? = null
    private var mExternalObserver: MediaContentObserver? = null

    private var mDataIndex = -1
    private var mDateTakenIndex = -1
    private var mDateAddIndex = -1

    /**
     * 启动监听
     */
    fun startListener() {
        sHasCallbackPaths.clear()
        // 记录开始监听的时间戳
        mStartListenTime = System.currentTimeMillis()

        if (mInternalObserver == null) {
            mInternalObserver = MediaContentObserver(null)
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                false, mInternalObserver!!
            )
        }

        if (mExternalObserver == null) {
            mExternalObserver = MediaContentObserver(null)
            mContext.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                false, mExternalObserver!!
            )
        }
    }

    /**
     * 停止监听
     */
    fun stopListener() {
        if (mInternalObserver != null) {
            mContext.contentResolver.unregisterContentObserver(mInternalObserver!!)
            mInternalObserver = null
        }
        if (mExternalObserver != null) {
            mContext.contentResolver.unregisterContentObserver(mExternalObserver!!)
            mExternalObserver = null
        }
        // 清空数据
        mStartListenTime = 0
        sHasCallbackPaths.clear()
    }

    /**
     * 处理媒体数据库的内容改变
     */
    private fun handleContentChange(contentUri: Uri) {
        var cursor: Cursor? = null
        try {
            // 数据改变时查询数据库中最后加入的一条数据
            cursor = mContext.contentResolver.query(
                contentUri, MEDIA_PROJECTIONS, null, null,
                MediaStore.Images.ImageColumns.DATE_ADDED + " DESC LIMIT 1"
            )

            if (cursor == null) {
                Log.e(TAG, "Deviant logic.")
                return
            }
            if (!cursor.moveToFirst()) {
                Log.d(TAG, "Cursor no data.")
                return
            }

            if (mDataIndex == -1 || mDateTakenIndex == -1 || mDateAddIndex == -1) {
                // 获取各列的索引
                mDataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                mDateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
                mDateAddIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_ADDED)
            }
            // 获取行数据
            val data = cursor.getString(mDataIndex)
            val dateTaken = cursor.getLong(mDateTakenIndex)
            val dateAdd = cursor.getLong(mDateAddIndex)

            // 处理获取到的第一行数据
            handleMediaRowData(data, dateTaken, dateAdd)
            Log.d(TAG, "Find picture : path =  $data date = $dateTaken")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    /**
     * 处理获取到的一行数据
     */
    private fun handleMediaRowData(data: String, dateTaken: Long, dateAdd: Long) {
        // 个别手机会自己修改截图文件夹的文件， 截屏功能会误以为是用户在截屏操作
        if (!isTimeValidate(dateAdd)) {
            Log.e(TAG, "Invalidate time")
            return
        }

        // 设置最大等待时间500ms（因为某些魅族时间保持有延迟）
        var duration = 0
        val step = 100
        while (!checkScreenShot(data, dateTaken) && duration <= 500) {
            duration += step
            try {
                Thread.sleep(step.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        if (checkScreenShot(data, dateTaken)) {
            if (mListener != null && !checkCallback(data)) {
                Log.e(TAG, "Find screenshot: path =  $data date = $dateTaken")
                mListener!!.onShot(data)
            }
        } else {
            // 如果在观察区间媒体数据库有数据改变，又不符合截屏规则，则输出到 log 待分析
            Log.d(TAG, "Media content changed, but not screenshot: path = " + data
                + " date = " + dateTaken)
        }
    }

    /**
     * 判断时间是否正确，图片插入时间小于1秒才有效
     *
     * @param dateAdd
     * @return
     */
    private fun isTimeValidate(dateAdd: Long): Boolean {
        return Math.abs(System.currentTimeMillis() / 1000 - dateAdd) < 1
    }

    /**
     * 判断指定的数据行是否符合截屏条件
     */
    private fun checkScreenShot(data: String, dateTaken: Long): Boolean {
        var path = data
        // 时间
        if (dateTaken < mStartListenTime /*|| (System.currentTimeMillis() - dateTaken) / 1000
        >10*/) {
            return false
        }

        // 路径
        if (TextUtils.isEmpty(path)) {
            return false
        }

        path = path.toLowerCase()
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (keyWork in KEYWORDS) {
            if (path.contains(keyWork)) {
                return true
            }
        }

        return false
    }

    /**
     * 判断是否已回调过, 某些手机ROM截屏一次会发出多次内容改变的通知;
     * 删除一个图片也会发通知, 同时防止删除图片时误将上一张符合截屏规则
     * 的图片当做是当前截屏.
     */
    private fun checkCallback(imagePath: String): Boolean {
        if (sHasCallbackPaths.contains(imagePath)) {
            return true
        }
        // 大概缓存15~20条记录便可
        if (sHasCallbackPaths.size >= 20) {
            for (i in 0..4) {
                sHasCallbackPaths.removeAt(0)
            }
        }
        sHasCallbackPaths.add(imagePath)
        return false
    }

    fun setListener(listener: OnScreenShotListener) {
        mListener = listener
    }

    interface OnScreenShotListener {
        fun onShot(imagePath: String)
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private inner class MediaContentObserver internal constructor(handler: Handler?) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri) {
            super.onChange(selfChange, uri)
            if (DeviceUtil.isRunningForeground(MyApplication.mContext)) {
                handleContentChange(uri)
            }
        }
    }

    companion object {

        private val TAG = ContentObserverManager::class.java.simpleName

        @SuppressLint("StaticFieldLeak")
        private var sInstance: ContentObserverManager? = null

        private val MEDIA_PROJECTIONS = arrayOf(MediaStore.Images.ImageColumns.DATA,
            MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.DATE_ADDED)

        /**
         * 截屏依据中的路径判断关键字
         */
        private val KEYWORDS = arrayOf("screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture", "screencap",
            "screen_cap", "screen-cap", "screen cap", "截屏")

        fun instance(context: Context): ContentObserverManager {
            if (null == sInstance) {
                sInstance = ContentObserverManager(context)
            }
            return sInstance!!
        }
    }
}
