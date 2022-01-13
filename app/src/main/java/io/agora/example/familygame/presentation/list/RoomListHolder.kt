package io.agora.example.familygame.presentation.list

import androidx.recyclerview.widget.RecyclerView
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.databinding.ItemRoomListBinding

class RoomListHolder(private val mBinding:ItemRoomListBinding):RecyclerView.ViewHolder(mBinding.root) {
    fun bind(position: Int, itemData: RoomInfo, itemClickListener: OnItemClickListener?){
        mBinding.titleItemRoomList.text = mBinding.root.context.getString(R.string.room_list_name, itemData.roomName, itemData.userName)
        mBinding.bgdItemRoomList.setImageResource(itemData.bgdId)
        mBinding.root.setOnClickListener {
            itemClickListener?.onClick(position)
        }
    }
}