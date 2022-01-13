package io.agora.example.familygame

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.agora.rtm.RtmClient

@Keep
class RTMClientFactory(private val client: RtmClient) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getDeclaredConstructor(RtmClient::class.java).newInstance(client)
    }

}