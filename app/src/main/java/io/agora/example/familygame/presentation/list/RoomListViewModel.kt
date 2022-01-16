package io.agora.example.familygame.presentation.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.util.GameConstants
import io.agora.example.familygame.util.ViewStatus
import io.agora.example.familygame.util.log
import io.agora.rtm.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class RoomListViewModel(private val client: RtmClient) : ViewModel() {

    private val _viewStatus = MutableLiveData<ViewStatus>()
    val viewStatus: LiveData<ViewStatus> = _viewStatus

    private val _roomList = MutableLiveData<List<RoomInfo>>()
    val roomList: LiveData<List<RoomInfo>> = _roomList

    init {
        fetchRoomList()
    }

    fun fetchRoomList() {
        _viewStatus.postValue(ViewStatus.Loading())
        client.getChannelAttributes(
            GameConstants.globalChannelName,
            object : ResultCallback<List<RtmChannelAttribute>> {
                override fun onSuccess(res: List<RtmChannelAttribute>?) {
                    _viewStatus.postValue(ViewStatus.Done)
                    val resList = mutableListOf<RoomInfo>()
                    res?.let { attrList ->
                        attrList.forEach { attr ->
                            var tempRoomInfo: RoomInfo? = null
                            try {
                                tempRoomInfo =
                                    GameConstants.gson.fromJson(attr.value, RoomInfo::class.java)
                            } catch (e: Exception) {
                            }
                            tempRoomInfo?.let { resList.add(it) }
                        }
                    }
                    _roomList.postValue(resList)
                }

                override fun onFailure(p0: ErrorInfo?) {
                    _viewStatus.postValue(ViewStatus.Error)
                }
            })
    }

    /**
     * 创建房间，成功后拉取房间列表
     * 失败则提示用户
     */
    fun createRoom(roomName: String) {
        GameConstants.localUser.let {

            val randomRoomId = Random.nextInt(10000)
            val roomInfo =
                RoomInfo(
                    randomRoomId,
                    roomName,
                    it.userId,
                    it.userName,
                    GameConstants.randomBgdId()
                )
            val list = listOf(
                RtmChannelAttribute(
                    randomRoomId.toString(),
                    GameConstants.gson.toJson(roomInfo)
                )
            )
            //        channelId: string
            //        该指定频道的 ID。
            //
            //        attributes: AttributesMap
            //        待增加或更新的属性列表。
            //
            //        Optional options: ChannelAttributeOptions
            //                频道属性操作选项。详见 ChannelAttributeOptions。
            client.addOrUpdateChannelAttributes(
                GameConstants.globalChannelName,
                list,
                ChannelAttributeOptions(true),
                object : ResultCallback<Void> {
                    override fun onSuccess(p0: Void?) {
                        viewModelScope.launch {
                            delay(300)
                            fetchRoomList()
                        }
                    }

                    override fun onFailure(p0: ErrorInfo?) {
                        _viewStatus.postValue(ViewStatus.Message("Create Fail"))
                    }
                })
        }
    }
}