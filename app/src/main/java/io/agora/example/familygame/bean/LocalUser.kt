package io.agora.example.familygame.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class LocalUser(var userName: String = "", val userId: Int = Random.nextInt(Int.MAX_VALUE)) :
    Parcelable {
    companion object {
        const val USERID = "userId"
        const val USERNAME = "userName"
    }
}