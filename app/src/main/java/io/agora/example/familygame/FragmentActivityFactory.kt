package io.agora.example.familygame

import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.agora.rtm.RtmClient

@Keep
class FragmentActivityFactory(private val fragmentActivity: FragmentActivity) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getDeclaredConstructor(FragmentActivity::class.java).newInstance(fragmentActivity)
    }

}