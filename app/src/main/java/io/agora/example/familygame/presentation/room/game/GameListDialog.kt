package io.agora.example.familygame.presentation.room.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.agora.example.familygame.databinding.DialogGameListBinding
import io.agora.example.familygame.presentation.list.OnItemClickListener
import io.agora.example.familygame.presentation.room.RoomViewModel
import io.agora.example.familygame.repo.GameRepo

class GameListDialog:BottomSheetDialogFragment() {

    companion object{
        const val TAG = "GameListDialog"
    }

    private var _mBinding: DialogGameListBinding? = null
    private val mBinding get() = _mBinding!!

    private lateinit var roomViewModel:RoomViewModel

    private lateinit var mAdapter : GameListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _mBinding = DialogGameListBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomViewModel = ViewModelProvider(requireParentFragment()).get(RoomViewModel::class.java)
        mAdapter = GameListAdapter()
        mBinding.recyclerViewDialogGameList.adapter = mAdapter
        mAdapter.itemClickListener = object :OnItemClickListener{
            override fun onClick(pos: Int) {
                requestGame(pos)
            }
        }
        fetchGameList()
    }

    private fun fetchGameList() {
        mAdapter.submitData(GameRepo.fetchGameList())
    }

    private fun requestGame(pos: Int) {
        roomViewModel.requestGame(mAdapter.dataList[pos].gameId)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mBinding = null
    }
}