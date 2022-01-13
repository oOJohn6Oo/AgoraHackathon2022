package io.agora.example.familygame.presentation.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.databinding.ItemRoomListBinding

class RoomListAdapter: RecyclerView.Adapter<RoomListHolder>() {

    val dataList: MutableList<RoomInfo> = mutableListOf()

    private var itemClickListener: OnItemClickListener? = null

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(newData:List<RoomInfo>){
        this.dataList.clear()
        this.dataList.addAll(newData)
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(itemClickListener: OnItemClickListener?){
        this.itemClickListener = itemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomListHolder {
        return RoomListHolder(
            ItemRoomListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RoomListHolder, position: Int) {
        holder.bind(position, dataList[position], itemClickListener)
    }

    override fun getItemCount() = dataList.size

}

interface OnItemClickListener{
    fun onClick(pos:Int)
}