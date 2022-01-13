package io.agora.example.familygame.repo

import io.agora.example.familygame.bean.AgoraGame

object GameRepo {
    private val gameList = mutableListOf<AgoraGame>(
        AgoraGame(
            "0", "10020", "汉诺塔",
            "https://imgsecond.yuanqiyouxi.com/test/DrawAndGuess/index.html",
            "https://testgame.yuanqihuyu.com/guess/leave",
            "https://testgame.yuanqihuyu.com/guess/gift",
            "https://testgame.yuanqihuyu.com/guess/barrage"
        )
    )

    fun fetchGameList() = gameList

    fun getGameById(gameId: String):AgoraGame{
        for (agoraGame in gameList) {
            if (agoraGame.gameId == gameId)
                return agoraGame
        }
        return gameList[0]
    }
}