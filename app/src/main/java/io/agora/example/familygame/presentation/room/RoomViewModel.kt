package io.agora.example.familygame.presentation.room

import androidx.annotation.IntRange
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.reflect.TypeToken
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.*
import io.agora.example.familygame.repo.GameRepo
import io.agora.example.familygame.util.*
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.internal.RtcEngineImpl
import io.agora.rtm.*

class RoomViewModel(
    private val rtcEngine: RtcEngine,
    private val rtmClient: RtmClient,
    private val currentRoom: RoomInfo
) : ViewModel() {


    private val currentPos = MoveBean(GameConstants.localUser, 0, 0)
    private val currentChannel: RtmChannel
    private val amHost: Boolean = currentRoom.userId == GameConstants.localUser.userId

    private val _userList = MutableLiveData(mutableListOf<LocalUser>())
    val userList: LiveData<MutableList<LocalUser>> get() = _userList

    private val _moveOp = MutableLiveData<MoveBean>()
    val moveOp: LiveData<MoveBean> get() = _moveOp

    private val _gameStatus = MutableLiveData<String>()
    val gameStatus: LiveData<String> get() = _gameStatus

    private val _currentGame = MutableLiveData<AgoraGame>()
    val currentGame: LiveData<AgoraGame> get() = _currentGame

    private val _isMicEnabled = MutableLiveData(true)
    val isMicEnabled: LiveData<Boolean> get() = _isMicEnabled

    val viewStatus = MutableLiveData<ViewStatus>()

    init {
        joinRoom()

        currentChannel =
            rtmClient.createChannel(currentRoom.roomId.toString(), MyRTMChannelListener())
        currentChannel.join(object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                if (!amHost) {
                    onSelfJoined()
                }
            }

            override fun onFailure(p0: ErrorInfo?) {
                "Join RTM failed".log()
                viewStatus.postValue(ViewStatus.Message("Join RTM failed"))
                viewStatus.postValue(ViewStatus.Error)
            }
        })
        // Update Agora Game
        if (amHost)
            _currentGame.postValue(GameRepo.getGameById("0"))
        else
            rtmClient.getChannelAttributes(
                currentRoom.roomId.toString(),
                object : ResultCallback<List<RtmChannelAttribute>> {
                    override fun onSuccess(p0: List<RtmChannelAttribute>?) {
                        p0?.let {
                            for (rtmChannelAttribute in it) {
                                if (rtmChannelAttribute.key == AgoraGame.TAG)
                                    _currentGame.postValue(GameRepo.getGameById(rtmChannelAttribute.value))
                            }
                        }
                    }

                    override fun onFailure(p0: ErrorInfo?) {
                        _currentGame.postValue(GameRepo.getGameById("0"))
                    }
                })
    }

    override fun onCleared() {
        super.onCleared()
        "room Clear".log()
        // 管理员离开 删除房间
        if (amHost) rtmClient.deleteAttr(
            GameConstants.globalChannelName,
            currentRoom.roomId.toString(),
            null
        )
        if (gameStatus.value.isNullOrEmpty())
            leaveChannel()
    }

    fun leaveChannel(){
        currentChannel.leave(null)
        currentChannel.release()
        rtcEngine.leaveChannel()
    }

    fun reportCurrentPos() {
        reportCurrentPos(currentPos.x, currentPos.y)
    }

    fun reportCurrentPos(
        @IntRange(from = 0, to = 100000) percentX: Int,
        @IntRange(from = 0, to = 100000) percentY: Int
    ) {
        if (amHost) return
        currentPos.x = percentX
        currentPos.y = percentY
        val msg = GameConstants.gson.toJson(currentPos)
        currentChannel.sendMessage(MyRTMMessage(msg), null)
    }

    fun adminStartGame(playerWithGroup: Array<ArrayList<LocalUser>>) {
        val playInfo = GameConstants.gson.toJson(playerWithGroup, GameConstants.groupType)
        currentChannel.sendMessage(MyRTMMessage(playInfo, 1), object : ResultCallback<Void> {
            override fun onSuccess(p0: Void?) {
                _gameStatus.postValue(playInfo)
            }

            override fun onFailure(p0: ErrorInfo?) {
                _gameStatus.postValue("")
            }
        })
    }

    private fun onMemberJoined(id: String, name: String) {
        var list = _userList.value
        if (list == null) list = mutableListOf()
        list.add(LocalUser(name, Integer.parseInt(id)))
        _userList.postValue(list)
    }

    fun onSelfJoined() {
        onMemberJoined(GameConstants.localUser.userId.toString(), GameConstants.localUser.userName)
        reportCurrentPos()
    }

    fun requestGame(gameId: String) {
        rtmClient.addOrUpdateAttr(currentChannel.id, AgoraGame.TAG, gameId, null)
    }

    private fun ensureUserExist(moveBean: MoveBean) {
        var exist = false
        _userList.value?.forEach {
            if (it.userId == moveBean.id)
                exist = true
        }
        if (!exist) {
            _userList.value?.let {
                it.add(LocalUser(moveBean.name, moveBean.id))
                _userList.postValue(it)
            }
        }
    }

    private fun joinRoom() {
        val options = ChannelMediaOptions().apply {
            autoSubscribeAudio = true
            autoSubscribeVideo = false
            publishCameraTrack = false
            publishAudioTrack = true
        }

        rtcEngine.apply {
            val token = (this as RtcEngineImpl).context.getString(R.string.agora_app_token)
            enableAudio()
            disableVideo()
            joinChannel(
                token,
                currentRoom.roomId.toString(),
                GameConstants.localUser.userId,
                options
            )
        }

    }

    /**
     * checked ==> mute = false
     */
    fun enableMic(checked: Boolean) {
        _isMicEnabled.value = checked
        rtcEngine.muteLocalAudioStream(!checked)
    }

    inner class MyRTMChannelListener : RtmChannelListener {
        override fun onMemberCountUpdated(p0: Int) {
        }

        /**
         * Only Sync AgoraGameId
         */
        override fun onAttributesUpdated(attrs: MutableList<RtmChannelAttribute>?) {
            attrs?.forEach {
                if (it.key == AgoraGame.TAG)
                    _currentGame.postValue(GameRepo.getGameById(it.value))
            }
        }

        /**
         * 约定 id + name + x + y
         */
        override fun onMessageReceived(message: RtmMessage?, member: RtmChannelMember?) {
            member?.let { m ->
                // 忽略自己消息
                if (m.userId == GameConstants.localUser.userId.toString()) return@let
                message?.let { msg ->
                    "received: ${msg.text}".log()
                    when (msg.text.startsWith("{")) {
                        true -> GameConstants.gson.fromJson(msg.text, MoveBean::class.java)?.let {
                            try {
                                val tempId = m.userId.toInt()
                                // 确认发消息方
                                if (tempId == it.id) {
                                    ensureUserExist(it)
                                    _moveOp.postValue(it)
                                }
                            } catch (e: Exception) {
                            }
                        }
                        false -> {
                            _gameStatus.postValue(msg.text)
                        }
                    }
                }
            }
        }

        override fun onImageMessageReceived(p0: RtmImageMessage?, p1: RtmChannelMember?) {

        }

        override fun onFileMessageReceived(p0: RtmFileMessage?, p1: RtmChannelMember?) {

        }

        override fun onMemberJoined(member: RtmChannelMember?) {
            reportCurrentPos()
            member?.let {
                if (it.userId != currentRoom.userId.toString())
                    this@RoomViewModel.onMemberJoined(it.userId, "")
            }
        }

        override fun onMemberLeft(member: RtmChannelMember?) {
            member?.let { m ->
                if (m.userId == currentRoom.userId.toString()) {
//                    val msg = "房间已解散"
//                    msg.log()
//                    viewStatus.postValue(ViewStatus.Message(msg))
//                    viewStatus.postValue(ViewStatus.Error)
                    return@let
                }
                _userList.value?.let { list ->
                    for (i in list.lastIndex downTo 0)
                        if (list[i].userId.toString() == m.userId)
                            list.removeAt(i)
                    _userList.postValue(list)
                }
                // name 为空 删除
                _moveOp.postValue(MoveBean(m.userId.toInt(), "", 0, 0))
            }
        }

    }
}