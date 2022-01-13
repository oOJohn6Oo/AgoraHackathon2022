package io.agora.example.familygame.presentation.game

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.agora.example.familygame.bean.LocalUser
import io.agora.example.familygame.bean.RoomInfo
import io.agora.rtc2.RtcEngine
import io.agora.rtm.RtmClient

@Keep
class GameViwModelFactory(
    private val rtcEngine: RtcEngine,
    private val rtmClient: RtmClient,
    private val groups: Array<ArrayList<LocalUser>>,
    private val previousRoom: RoomInfo
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getDeclaredConstructor(RtcEngine::class.java, RtmClient::class.java, groups::class.java,previousRoom::class.java).newInstance(rtcEngine, rtmClient, groups, previousRoom)
    }

}