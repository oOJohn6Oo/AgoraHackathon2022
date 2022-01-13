package io.agora.example.familygame.bean;

import androidx.annotation.NonNull;

public class AgoraGame {
    public static final String TAG = "GAME";

    @NonNull
    private final String gameId;
    @NonNull
    private final String appId;
    @NonNull
    private final String gameName;
    @NonNull
    private final String gameStartUrl;
    @NonNull
    private final String gameEndUrl;
    @NonNull
    private final String gameGiftUrl;
    @NonNull
    private final String gameCommentUrl;

    public AgoraGame(@NonNull String gameId, @NonNull String appId, @NonNull String gameName, @NonNull String gameStartUrl, @NonNull String gameEndUrl, @NonNull String gameGiftUrl, @NonNull String gameCommentUrl) {
        this.gameId = gameId;
        this.appId = appId;
        this.gameName = gameName;
        this.gameStartUrl = gameStartUrl;
        this.gameEndUrl = gameEndUrl;
        this.gameGiftUrl = gameGiftUrl;
        this.gameCommentUrl = gameCommentUrl;
    }

    @NonNull
    public String getAppId() {
        return appId;
    }

    @NonNull
    public String getGameId() {
        return gameId;
    }

    @NonNull
    public String getGameName() {
        return gameName;
    }

    @NonNull
    public String getGameStartUrl() {
        return gameStartUrl;
    }

    @NonNull
    public String getGameEndUrl() {
        return gameEndUrl;
    }

    @NonNull
    public String getGameGiftUrl() {
        return gameGiftUrl;
    }

    @NonNull
    public String getGameCommentUrl() {
        return gameCommentUrl;
    }
}
