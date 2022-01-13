package io.agora.example.familygame.util

sealed class ViewStatus {
    object Done : ViewStatus()
    class Loading(val showProgress : Boolean = true) : ViewStatus()
    object Error : ViewStatus()
    class Message(val msg: String) : ViewStatus()
}