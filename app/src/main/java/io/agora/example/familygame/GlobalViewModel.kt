package io.agora.example.familygame

import android.content.Context
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.example.familygame.util.GameConstants
import io.agora.example.familygame.util.ViewStatus
import io.agora.example.familygame.util.log
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtm.*
import java.io.File

@Keep
class GlobalViewModel constructor(fragmentActivity: FragmentActivity) : ViewModel() {

    lateinit var client: RtmClient
    lateinit var rtcEngine: RtcEngine

    private val _rtcInit = MutableLiveData<Boolean>()
    val rtcInit: LiveData<Boolean>
        get() = _rtcInit

    private val _rtmInit = MutableLiveData<Boolean>()
    val rtmInit: LiveData<Boolean>
        get() = _rtmInit

    private val _sdkStatus = MutableLiveData<ViewStatus>()
    val sdkStatus: LiveData<ViewStatus> get() = _sdkStatus

    init {
        val appId = fragmentActivity.getString(R.string.agora_app_id)
        initRTM(fragmentActivity, appId)
        initRTC(fragmentActivity, appId)
    }

    override fun onCleared() {
        super.onCleared()
        RtcEngine.destroy()
        client.release()
    }

    private fun initRTM(context: Context, appId: String) {
        try {
            client = RtmClient.createInstance(context, appId, MyRtmListener())
            val appToken = context.getString(R.string.agora_app_token)
            setLogFile(context, client)
            client.login(
                appToken,
                GameConstants.localUser.userId.toString(),
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        _rtmInit.postValue(true)
                    }

                    override fun onFailure(p0: ErrorInfo?) {
                        _rtmInit.postValue(false)
                    }
                })

        } catch (e: Exception) {
            _rtmInit.postValue(false)
        }
    }

    /**
     * 01xx Success
     * 10xx Fail
     */
    private fun initRTC(context: Context, appId: String) {
        try {
            rtcEngine = RtcEngine.create(context, appId, RtcStatusListener(_sdkStatus))
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

            _rtcInit.postValue(true)

        } catch (e: Exception) {
            _rtcInit.postValue(false)
        }
    }

    private fun setLogFile(context: Context, client: RtmClient) {
        context.externalCacheDir?.let {
            if (!it.exists())
                it.mkdirs()
            if (it.exists()) {
                val logFile = File(it, "rtm.log")
                if (!logFile.exists()) logFile.createNewFile()

                if (logFile.isFile && logFile.exists())
                    client.setLogFile(logFile.absolutePath)
            }
        }
    }

}

class MyRtmListener : RtmClientListener {
    override fun onConnectionStateChanged(p0: Int, p1: Int) {

    }

    override fun onMessageReceived(p0: RtmMessage?, p1: String?) {

    }

    override fun onImageMessageReceivedFromPeer(p0: RtmImageMessage?, p1: String?) {

    }

    override fun onFileMessageReceivedFromPeer(p0: RtmFileMessage?, p1: String?) {

    }

    override fun onMediaUploadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

    }

    override fun onMediaDownloadingProgress(p0: RtmMediaOperationProgress?, p1: Long) {

    }

    override fun onTokenExpired() {

    }

    override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {

    }

}

class RtcStatusListener(private val sdkStatus: MutableLiveData<ViewStatus>) :
    IRtcEngineEventHandler() {
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        sdkStatus.postValue(ViewStatus.Message("Join $channel success"))
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        "uid:$uid joined".log()
    }
}