package io.agora.example.familygame.presentation.room.game

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.example.familygame.bean.AgoraGame
import io.agora.example.familygame.databinding.ItemGameBinding
import io.agora.example.familygame.presentation.list.OnItemClickListener

class GameListAdapter:RecyclerView.Adapter<GameHolder>() {
    var itemClickListener:OnItemClickListener? = null
    val dataList = mutableListOf<AgoraGame>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(newData :List<AgoraGame>){
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameHolder {
        return GameHolder(ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: GameHolder, position: Int) {
        holder.bind(position, dataList[position], itemClickListener)
    }

    override fun getItemCount() = dataList.size
}