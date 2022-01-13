package io.agora.example.familygame

import android.app.Application
import io.agora.example.familygame.util.GameConstants

class GameApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        GameConstants.checkLocalUser(this.applicationContext)
    }
}