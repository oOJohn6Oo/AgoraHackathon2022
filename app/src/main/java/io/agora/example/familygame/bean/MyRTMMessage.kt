package io.agora.example.familygame.bean

import io.agora.rtm.RtmMessage
import io.agora.rtm.RtmMessageType

class MyRTMMessage(private val msg:String, private val type:Int = 0):RtmMessage() {
    override fun setText(p0: String?) {
    }

    override fun getText() = msg

    override fun setRawMessage(p0: ByteArray?) {
    }

    override fun setRawMessage(p0: ByteArray?, p1: String?) {

    }

    override fun getRawMessage() = msg.toByteArray()

    override fun getMessageType() = RtmMessageType.TEXT

    override fun getServerReceivedTs(): Long {
        return 0
    }

    override fun isOfflineMessage(): Boolean {
        return false
    }
}