package io.agora.example.familygame.presentation.room.game

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.AgoraGame
import io.agora.example.familygame.databinding.ItemGameBinding
import io.agora.example.familygame.presentation.list.OnItemClickListener

class GameHolder(private val mBinding: ItemGameBinding) : RecyclerView.ViewHolder(mBinding.root) {
    fun bind(position: Int, agoraGame: AgoraGame, itemClickListener: OnItemClickListener?) {
        mBinding.root.apply {
            text = agoraGame.gameName
            icon = ContextCompat.getDrawable(context, R.drawable.slice_game_1)
            setOnClickListener { itemClickListener?.onClick(position) }
        }
    }
}