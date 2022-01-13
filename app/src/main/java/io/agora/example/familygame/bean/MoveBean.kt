package io.agora.example.familygame.bean

import androidx.annotation.IntRange


data class MoveBean(val id: Int, val name: String, @IntRange(from = 0, to = 100000) var x: Int,@IntRange(from = 0, to = 100000) var y: Int){
    companion object{
        const val RATE = 100000
    }
    constructor(localUser:LocalUser,@IntRange(from = 0, to = 100000) x: Int,@IntRange(from = 0, to = 100000) y: Int) : this(localUser.userId, localUser.userName, x, y)
}
