package io.agora.example.familygame.presentation.room

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.agora.example.familygame.bean.RoomInfo
import io.agora.rtc2.RtcEngine
import io.agora.rtm.RtmClient

@Keep
class RoomViwModelFactory(private val rtcEngine: RtcEngine, private val rtmClient: RtmClient, private val currentRoom: RoomInfo) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getDeclaredConstructor(RtcEngine::class.java, RtmClient::class.java, RoomInfo::class.java).newInstance(rtcEngine, rtmClient, currentRoom)
    }

}