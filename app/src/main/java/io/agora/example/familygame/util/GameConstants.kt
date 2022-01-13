package io.agora.example.familygame.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.LocalUser
import java.lang.reflect.Type

object GameConstants {
    const val globalChannelName = "GLOBAL_ROOM"

    lateinit var localUser: LocalUser

    val gson = Gson()

    val groupType: Type = object :TypeToken<Array<ArrayList<LocalUser>>>(){}.type

    fun randomBgdId(): Int {
        return R.drawable.ic_account
    }

    private fun isUserInitialized() = this::localUser.isInitialized

    fun checkLocalUser(context: Context) {
        if (!isUserInitialized()) {
            val sp = context.getSharedPreferences("GF", Context.MODE_PRIVATE)
            sp.getInt(LocalUser.USERID, -1).let {
                sp.getString(LocalUser.USERNAME, "")?.let { name ->
                    if (it != -1)
                        localUser = LocalUser(name, it)
                }
            }
        }

        if (!isUserInitialized())
            localUser = LocalUser()
    }

    fun saveLocalUser(context: Context) {
        if (isUserInitialized()) {
            val sp = context.getSharedPreferences("GF", Context.MODE_PRIVATE)
            sp.edit().putInt(LocalUser.USERID, localUser.userId)
                .putString(LocalUser.USERNAME, localUser.userName)
                .apply()
        }
    }
}