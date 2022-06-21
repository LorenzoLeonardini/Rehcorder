package dev.leonardini.rehcorder

import android.app.Application
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.material.color.DynamicColors
import java.lang.String

class RehcorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        val session: FFmpegSession = FFmpegKit.execute("-version")
        if (ReturnCode.isSuccess(session.returnCode)) {
            // SUCCESS
            Log.i("FFMPEG", session.allLogsAsString)
        } else if (ReturnCode.isCancel(session.returnCode)) {
            // CANCEL
        } else {
            Log.d(
                "FFMPEG",
                String.format(
                    "Command failed with state %s and rc %s.%s",
                    session.state,
                    session.returnCode,
                    session.failStackTrace
                )
            )
        }
    }
}