package io.agora.example.familygame.bean

import androidx.annotation.DrawableRes
import java.io.Serializable

data class RoomInfo(val roomId:Int, val roomName:String, val userId:Int, val userName:String, @DrawableRes val bgdId:Int) :Serializable{
    companion object{
        const val TAG = "RoomInfo"
    }
}