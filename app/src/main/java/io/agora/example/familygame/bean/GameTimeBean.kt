package io.agora.example.familygame.bean

data class GameTimeBean(
    var startMs: Long,
    var currentMs: Long,
    var endMs: Long,
    var gameEnd: Boolean,
    var winnerGroup: Int,
    var id: Int

){
    companion object{
        const val TAG = "GameTimeBean"
    }

    fun offsetMs(offset:Long){
        startMs += offset
        currentMs += offset
    }
}