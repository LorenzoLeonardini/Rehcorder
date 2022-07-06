package dev.leonardini.rehcorder.services

import android.app.Service
import android.content.Intent
import android.os.*
import androidx.annotation.WorkerThread

abstract class ForegroundIntentService(private val mName: String) : Service() {
    @Volatile
    private lateinit var mServiceLooper: Looper

    @Volatile
    private lateinit var mServiceHandler: ServiceHandler
    private var mRedelivery: Boolean = false

    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {
        override fun handleMessage(msg: Message) {
            onHandleIntent(msg.obj as Intent)
            stopSelf(msg.arg1)
        }
    }

    fun setIntentRedelivery(enabled: Boolean) {
        mRedelivery = enabled
    }

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("ForegroundIntentService[$mName]")
        thread.start()

        mServiceLooper = thread.looper
        mServiceHandler = ServiceHandler(mServiceLooper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotification()

        val msg = mServiceHandler.obtainMessage()
        msg.arg1 = startId
        msg.obj = intent
        mServiceHandler.sendMessage(msg)

        return if (mRedelivery) START_REDELIVER_INTENT else START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceLooper.quit()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @WorkerThread
    protected abstract fun onHandleIntent(intent: Intent)

    @WorkerThread
    protected abstract fun startForegroundNotification()
}