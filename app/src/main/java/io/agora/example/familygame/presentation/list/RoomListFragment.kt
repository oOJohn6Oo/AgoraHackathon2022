package io.agora.example.familygame.presentation.list

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialContainerTransform
import io.agora.example.familygame.GlobalViewModel
import io.agora.example.familygame.R
import io.agora.example.familygame.RTMClientFactory
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.databinding.FragmentRoomListBinding
import io.agora.example.familygame.util.*

class RoomListFragment : Fragment() {

    companion object {
        @JvmField
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }

    private var _mBinding: FragmentRoomListBinding? = null
    private val mBinding get() = _mBinding!!

    private val globalViewModel: GlobalViewModel by activityViewModels()
    private val listViewModel: RoomListViewModel by viewModels { RTMClientFactory(globalViewModel.client) }

    private lateinit var mAdapter: RoomListAdapter
    private lateinit var startAnimation: MaterialContainerTransform
    private lateinit var endAnimation: MaterialContainerTransform
    private var tempPos = -1

    private val lis =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
            var allGranted = true
            for (permission in permissions) {
                if (res[permission] != true) {
                    allGranted = false
                }
            }
            if (allGranted) navToRoomDetail()
            else "PERMISSION REFUSED".toast(requireContext())
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _mBinding = FragmentRoomListBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()

        initListener()

        observe(listViewModel.roomList) { list ->
            mAdapter.submitData(list)
            mBinding.emptyViewFgList.visibility = VISIBLE.takeIf { list.isEmpty() } ?: GONE
        }

        observe(listViewModel.viewStatus) {
            mBinding.swipeFgList.isRefreshing = it is ViewStatus.Loading
        }
    }

    private fun initView() {
        mAdapter = RoomListAdapter()
        mAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onClick(pos: Int) {
                tempPos = pos
                if (GameConstants.localUser.userName.isEmpty()) onFABClicked()
                else
                    permissionCheckBeforeOp(
                        permissions,
                        this@RoomListFragment::navToRoomDetail,
                        lis
                    )
            }
        })

        startAnimation = mBinding.fabFgList.times(mBinding.cardFgList)
        endAnimation = mBinding.cardFgList.times(mBinding.fabFgList)

        mBinding.swipeFgList.setProgressViewEndTarget(true, 200.dp.toInt())
        mBinding.recyclerViewFgList.adapter = mAdapter

        mBinding.editUserViewInput.text =
            Editable.Factory.getInstance().newEditable(GameConstants.localUser.userName)
    }

    private fun initListener() {
        mBinding.cardFgList.setOnClickListener { clearCardFocus() }
        mBinding.btnConfirmViewLayout.setOnClickListener { onBtnConfirmClicked() }
        mBinding.toolbarFgList.setNavigationOnClickListener { showUserInfoDialog() }
        mBinding.swipeFgList.setOnRefreshListener { onRefresh() }

        mBinding.scrimFgList.setOnClickListener { endAnimation.start() }
        mBinding.fabFgList.setOnClickListener {
            tempPos = -1
            onFABClicked()
        }

        mBinding.editRoomViewInput.addTextChangedListener { mBinding.inputRoomViewInput.isErrorEnabled = false }
        mBinding.editUserViewInput.addTextChangedListener { mBinding.inputUserViewInput.isErrorEnabled = false }
    }

    private fun clearCardFocus() {
        mBinding.editRoomViewInput.clearFocus()
        mBinding.editUserViewInput.clearFocus()
        mBinding.root.hideKeyboard()
    }

    private fun showUserInfoDialog() {
        AlertDialog.Builder(requireContext()).setItems(
            arrayOf(
                "UserID: ${GameConstants.localUser.userId}",
                "UserName: ${GameConstants.localUser.userName}"
            ), null
        )
            .show()
    }

    private fun onFABClicked() {
        // 是否为"创建操作"
        "temp:$tempPos".log()
        val createOp = tempPos == -1
        // 标题
        mBinding.titleViewInput.setText(R.string.create_room.takeIf { createOp }
            ?: R.string.join_room)
        // 设置文字
        val roomName = Editable.Factory.getInstance().newEditable("".takeIf { createOp }?:mAdapter.dataList[tempPos].roomName)
        mBinding.editRoomViewInput.text = roomName
        // 是否可编辑
        mBinding.editRoomViewInput.enableInput(createOp)
        // 计数器
        mBinding.inputRoomViewInput.isCounterEnabled = createOp
        mBinding.inputRoomViewInput.isErrorEnabled = false

        val haveName = GameConstants.localUser.userName.isNotBlank()

        // 有名字 ==》 禁止输入 无名字 ==》 可以输入
        mBinding.editUserViewInput.enableInput(!haveName)
        // 有名字 ==》禁止计数器 无名字 ==》开启计数器
        mBinding.inputUserViewInput.isCounterEnabled = !haveName
        mBinding.inputUserViewInput.isErrorEnabled = false
        startAnimation.start()
    }

    private fun onRefresh() {
        listViewModel.fetchRoomList()
    }


    private fun navToRoomDetail() {
        findNavController().navigate(R.id.action_roomListFragment_to_roomFragment, Bundle().apply {
            putSerializable(RoomInfo.TAG, mAdapter.dataList[tempPos])
        })
    }

    /**
     * "确认"按钮点击事件
     */
    private fun onBtnConfirmClicked() {
        val isRoomNameValid = checkInputValid(mBinding.editRoomViewInput)
        val isUserNameValid = checkInputValid(mBinding.editUserViewInput)

        if (!isRoomNameValid)
            showError(mBinding.inputRoomViewInput)
        if (!isUserNameValid)
            showError(mBinding.inputUserViewInput)

        if (isRoomNameValid && isUserNameValid) {
            // Ensure userName
            mBinding.editUserViewInput.text?.toString()?.trim()?.let {
                if (GameConstants.localUser.userName.isBlank()) {
                    GameConstants.localUser.userName = it
                    GameConstants.saveLocalUser(requireContext())
                }
            }
            if (tempPos == -1)
            // CreateRoom
                mBinding.editRoomViewInput.text?.toString()?.let {
                    listViewModel.createRoom(it)
                }
            else navToRoomDetail()
            endAnimation.start()
        }

    }

    private fun checkInputValid(inputEditText: TextInputEditText): Boolean {
        var inputValid = false
        inputEditText.text?.let {
            val roomName = it.toString().trim()
            if (roomName.isNotBlank()) {
                inputValid = true
            }
        }
        return inputValid
    }

    private fun showError(textInputLayout: TextInputLayout) {
        if (textInputLayout.isErrorEnabled)
            textInputLayout.shake(p = 30.dp)
        else
            textInputLayout.error = getString(R.string.input_invalid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mBinding = null
    }

    private fun MaterialContainerTransform.start() {
        TransitionManager.beginDelayedTransition(mBinding.root, this)
        this.startView?.visibility = INVISIBLE
        this.endView?.visibility = VISIBLE
        mBinding.scrimFgList.visibility =
            if (startView == mBinding.fabFgList) VISIBLE else INVISIBLE
        mBinding.root.hideKeyboard()
    }
}