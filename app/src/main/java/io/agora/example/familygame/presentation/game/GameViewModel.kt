package io.agora.example.familygame.presentation.game

import androidx.annotation.IntRange
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.*
import io.agora.example.familygame.util.GameConstants
import io.agora.example.familygame.util.addOrUpdateAttr
import io.agora.example.familygame.util.log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.internal.RtcEngineImpl
import io.agora.rtm.*
import kotlinx.coroutines.*

/**
 * 所有人加入同一 RTC 房间
 * 所有人加入不同 RTM 房间 主持人默认进入第一组，后续可以切换组
 *
 */
class GameViewModel(
    private val rtcEngine: RtcEngine,
    private val rtmClient: RtmClient,
    val userList: Array<ArrayList<LocalUser>>,
    private val previousRoom: RoomInfo
) : ViewModel() {

    companion object {
        const val Hanoi = "Hanoi"
    }

    private lateinit var currentRTMChannel: RtmChannel
    private lateinit var adminRTMChannel: List<RtmChannel>
    private var myGroup = -1
    val amHost: Boolean = previousRoom.userId == GameConstants.localUser.userId
    private val currentPos = MoveBean(GameConstants.localUser, 0, 0)

    private val _moveOp = MutableLiveData<MoveBean>()
    val moveOp: LiveData<MoveBean> get() = _moveOp

    private val _isMicEnabled = MutableLiveData(true)
    val isMicEnabled: LiveData<Boolean> get() = _isMicEnabled

    private val _isCameraEnabled = MutableLiveData(true)
    val isCameraEnabled: LiveData<Boolean> get() = _isCameraEnabled

    private val _rtcUserList = MutableLiveData<List<LocalUser>>()
    val rtcUserList: LiveData<List<LocalUser>> get() = _rtcUserList

    private val _timeBean = MutableLiveData<GameTimeBean>()
    val timeBean: LiveData<GameTimeBean> get() = _timeBean


    private val _hanoi = MutableLiveData<Int>()
    val hanoi: LiveData<Int> get() = _hanoi

    @Volatile
    private var gameEnd = false

    init {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                if (amHost) myGroup = 0
                else
                    userList.forEachIndexed { index, arrayList ->
                        for (localUser in arrayList) {
                            if (localUser.userId == GameConstants.localUser.userId) {
                                myGroup = index
                                return@forEachIndexed
                            }
                        }
                    }

                if (myGroup != -1) {
                    joinRTCChannel()
                    joinRTMChannel()
                    if (amHost)
                        _rtcUserList.postValue(userList[0])
                    else
                        _rtcUserList.postValue(userList[myGroup])
                }
                if (amHost) {
                    val current = System.currentTimeMillis()
                    adminRTMChannel.forEachIndexed { index, channel ->
                        val timeBean = GameTimeBean(current + 3000, current, -1, false, -1, index)
                        rtmClient.addOrUpdateAttr(
                            channel.id,
                            GameTimeBean.TAG,
                            GameConstants.gson.toJson(timeBean),
                            null
                        )
                    }
                }
            }
        }
        startSyncTime()
    }

    override fun onCleared() {
        super.onCleared()
        if (amHost) adminRTMChannel.forEach {
            it.leave(null)
            it.release()
        } else {
            currentRTMChannel.leave(null)
            currentRTMChannel.release()
        }
        rtcEngine.leaveChannel()
    }

    fun reportCurrentPos(
        @IntRange(from = 0, to = 100000) percentX: Int,
        @IntRange(from = 0, to = 100000) percentY: Int
    ) {
        if (amHost) return
        currentPos.x = percentX
        currentPos.y = percentY
        val msg = GameConstants.gson.toJson(currentPos)
        currentRTMChannel.sendMessage(MyRTMMessage(msg), null)
    }

    private fun joinRTMChannel() {
        if (amHost) {
            adminRTMChannel = List(userList.size) {
                rtmClient.createChannel("$it-${previousRoom.roomId}", GameRTMChannelListener())
            }
            currentRTMChannel = adminRTMChannel[myGroup]
            adminRTMChannel.forEach {
                it.join(null)
            }
        } else {
            currentRTMChannel =
                rtmClient.createChannel("$myGroup-${previousRoom.roomId}", GameRTMChannelListener())
            currentRTMChannel.join(null)
            "joinRTMChannel:sdklf".log()
        }
    }

    private fun joinRTCChannel() {
        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            autoSubscribeVideo = true
            publishCameraTrack = true
            publishAudioTrack = true
        }

        updateMuteRule()

        val rtcChannelId = "${previousRoom.roomId}_START"
        rtcEngine.apply {
            val token = (this as RtcEngineImpl).context.getString(R.string.agora_app_token)
            enableAudio()
            enableVideo()
            startPreview()
            joinChannel(
                token,
                rtcChannelId,
                GameConstants.localUser.userId,
                options
            )
        }
    }

    private fun updateMuteRule() {
        userList.forEachIndexed { index, arrayList ->
            arrayList.forEach {
                rtcEngine.muteRemoteAudioStream(it.userId, index == myGroup)
            }
        }
    }

    fun switchGroup(pageIndex: Int) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                updateMuteRule()
                myGroup = pageIndex
                _rtcUserList.postValue(userList[pageIndex])
            }
        }
    }

    fun enableMic(enable: Boolean) {
        _isMicEnabled.value = enable
        rtcEngine.muteLocalAudioStream(!enable)
    }

    fun enableCamera(enable: Boolean) {
        _isCameraEnabled.value = enable
        rtcEngine.muteLocalVideoStream(!enable)
    }

    private fun startSyncTime() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (!gameEnd) {
                    _timeBean.value?.let {
                        delay(500)
                        if (!gameEnd)
                            _timeBean.postValue(it)
                    }
                }
            }
        }
    }

    fun ensureHanoi() {
        if (_hanoi.value == null)
            _hanoi.value = 0x1c0
    }

    fun reportHanoi(hanoiValue: Int) {
        rtmClient.addOrUpdateAttr(currentRTMChannel.id, Hanoi, "${myGroup}_${hanoiValue}", null)
    }

    inner class GameRTMChannelListener : RtmChannelListener {

        override fun onMemberCountUpdated(p0: Int) {

        }

        override fun onAttributesUpdated(attrs: MutableList<RtmChannelAttribute>?) {
            attrs?.let {
                it.forEach { attr ->
                    attr.toString().log()
                    when (attr.key) {
                        GameTimeBean.TAG -> GameConstants.gson.fromJson(
                            attr.value,
                            GameTimeBean::class.java
                        )?.let { timeBean ->
//                            if (gameEnd) return@forEach
                            if (timeBean.id == myGroup) {
                                // first Time ==> initialize TimeBean
                                if (_timeBean.value == null) {
                                    timeBean.offsetMs(System.currentTimeMillis() - timeBean.currentMs)
                                    _timeBean.postValue(timeBean)
                                } else if (timeBean.gameEnd) {
                                    gameEnd = true
                                    _timeBean.postValue(timeBean)
                                }
                            }
                        }
                        Hanoi -> {
                            if (gameEnd) return@forEach
                            // 结束
                            val value = attr.value.split("_")
                            if (value[1] == "7" && amHost) {
                                gameEnd = true
                                _timeBean.value?.let { time ->
                                    this@GameViewModel.gameEnd = true
                                    time.gameEnd = true
                                    time.endMs = System.currentTimeMillis()
                                    time.winnerGroup = value[0].toInt()
                                    adminRTMChannel.forEachIndexed { index, rtmChannel ->
                                        time.id = index
                                        rtmClient.addOrUpdateAttr(
                                            rtmChannel.id,
                                            GameTimeBean.TAG,
                                            GameConstants.gson.toJson(time),
                                            null
                                        )
                                    }
                                }
                            }
                            if (value[0] == myGroup.toString()) {
                                _hanoi.postValue(value[1].toInt())
                            }
                        }
                    }
                }
            }
        }

        override fun onMessageReceived(message: RtmMessage?, member: RtmChannelMember?) {
            member?.let { m ->
                // 忽略自己消息
                if (m.userId == GameConstants.localUser.userId.toString()) return@let
                message?.let { msg ->
                    when (msg.text.startsWith("{")) {
                        // 其他人的移动信息
                        true -> GameConstants.gson.fromJson(msg.text, MoveBean::class.java)?.let {
                            try {
                                val tempId = m.userId.toInt()
                                // 确认发消息方
                                if (tempId == it.id) {
                                    _moveOp.postValue(it)
                                }
                            } catch (e: Exception) {
                            }
                        }
                        // 游戏开始信息
                        false -> {
//                            _gameStatus.postValue(msg.text)
                        }
                    }
                }
            }
        }

        override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {

        }

        override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {

        }

        override fun onMemberJoined(p0: RtmChannelMember?) {

        }

        override fun onMemberLeft(p0: RtmChannelMember?) {

        }

    }
}