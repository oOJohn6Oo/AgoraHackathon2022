package io.agora.example.familygame.presentation.room

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import io.agora.example.familygame.GlobalViewModel
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.LocalUser
import io.agora.example.familygame.bean.MoveBean
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.databinding.FragmentRoomBinding
import io.agora.example.familygame.presentation.game.GameFragment
import io.agora.example.familygame.presentation.room.game.GameListDialog
import io.agora.example.familygame.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class RoomFragment : Fragment() {
    private var _mBinding: FragmentRoomBinding? = null
    private val mBinding get() = _mBinding!!

    private lateinit var currentRoom: RoomInfo
    private var amHost: Boolean = false

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private val globalViewModel: GlobalViewModel by activityViewModels()
    private val roomViewModel: RoomViewModel by viewModels {
        RoomViwModelFactory(
            globalViewModel.rtcEngine,
            globalViewModel.client,
            currentRoom
        )
    }

    // Cache my own view
    private var myView: Chip? = null
    private var currentPeopleCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        arguments?.let {
            try {
                currentRoom = it.getSerializable(RoomInfo.TAG) as RoomInfo
                amHost = currentRoom.userId == GameConstants.localUser.userId
                _mBinding = FragmentRoomBinding.inflate(inflater, container, false)
                return mBinding.root
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        "room back".log()
        findNavController().popBackStack()
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
        initObserver()
    }

    private fun initView() {
        bottomSheetBehavior = BottomSheetBehavior.from(mBinding.bottomSheetFgRoom).apply {
            isHideable = false
            peekHeight = 32.dp.toInt()
            state = BottomSheetBehavior.STATE_EXPANDED
        }
        mBinding.titleFgRoom.text = currentRoom.roomName
        mBinding.groupControlFgRoom.visibility = VISIBLE.takeIf { amHost } ?: GONE
        mBinding.peopleCountFgRoom.text = getString(R.string.current_people_count, 0, 24)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {

        if (amHost) {
            // ⬅️ 按钮
            mBinding.toolbarFgRoom.navigationIcon = null
            // 🏆 按钮
            mBinding.btnTrophyFgRoom.setOnClickListener { showGameListDialog() }
            // ❌ 按钮
            mBinding.btnEndFgRoom.setOnClickListener { showExitAlertDialog() }
            // ✅ 按钮
            mBinding.btnStartFgRoom.setOnClickListener { showStartConfirmDialog() }

            // 滚轮
            mBinding.numberPickerFgRoom.apply {
                wrapSelectorWheel = false
                minValue = 1
                maxValue = 5
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                setFormatter { v ->
                    "分 $v 组"
                }
                children.forEach {
                    if (it is EditText) it.filters = arrayOf()
                }
            }
        } else {
            // ⬅️ 按钮
            mBinding.toolbarFgRoom.setNavigationOnClickListener { showExitAlertDialog() }
            // 玩家移动
            mBinding.playgroundFgRoom.setOnTouchListener(GestureTouchListener(this::onSelfPosChanged))
        }

        // 返回键 拦截
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitAlertDialog()
                }
            })

        // 🎤 按钮
        mBinding.btnMicFgRoom.addOnCheckedChangeListener { v, isChecked ->
            if (v.isPressed)
                roomViewModel.enableMic(isChecked)
        }

        // BottomSheet 滑动拦截
        mBinding.bottomSheetFgRoom.setOnTouchListener { _, _ -> true }
    }

    private fun initObserver() {

        observe(roomViewModel.isMicEnabled) {
            mBinding.btnMicFgRoom.isChecked = it
            mBinding.btnMicFgRoom.setIconResource(if (it) R.drawable.ic_mic else R.drawable.ic_mic_off)
        }
        observe(roomViewModel.currentGame) {
            mBinding.btnTrophyFgRoom.text = it.gameName
        }
        observe(roomViewModel.viewStatus) {
            when (it) {
                is ViewStatus.Message -> {
                    it.msg.toast(requireContext())
                }
                is ViewStatus.Error -> {
                    "Error".log()
                    findNavController().popBackStack()
                }
                else -> {

                }
            }
        }
        observe(roomViewModel.gameStatus){
            "gameStatus:$it".log()
            if (it.isEmpty()) "Game Start Error".log()
            else onStartGame(it)
        }

        observe(roomViewModel.moveOp) {
            mBinding.playgroundFgRoom.findViewWithTag<Chip>(it.id)?.let { v ->
                "moveOP".log()
                if (it.name.isEmpty()) {
                    mBinding.playgroundFgRoom.removeView(v)
                    return@let
                }
                if (v.text.isEmpty()) v.text = it.name
                animateName(v, it.x, it.y)
            }
        }

        // 通过用户列表 添加 View
        observe(roomViewModel.userList) {
            currentPeopleCount = it.size
            mBinding.peopleCountFgRoom.text = getString(R.string.current_people_count, currentPeopleCount, 24)
            it.forEach { u ->
                var view: Chip? = mBinding.playgroundFgRoom.findViewWithTag(u.userId)
                if (view == null) {
                    view = createAvatar(u)
                    mBinding.playgroundFgRoom.addView(view)
                }
            }
        }

        observe(globalViewModel.sdkStatus) {
            when (it) {
                is ViewStatus.Message -> {
                    it.msg.toast(requireContext())
                }
                else -> {
                }
            }
        }
    }

    private fun onStartGame(gameInfo :String) {
        roomViewModel.leaveChannel()
        findNavController().popBackStack(R.id.roomFragment, true)
        findNavController().navigate(R.id.gameFragment, Bundle().apply {
            putString(GameFragment.GROUP_KEY, gameInfo)
            putSerializable(RoomInfo.TAG, currentRoom)
        })
    }

    private fun onSelfPosChanged(x: Float, y: Float) = myView?.let {
        val xInRate =
            (MoveBean.RATE * (it.translationX + x) / mBinding.playgroundFgRoom.measuredWidth).roundToInt()
        val yInRate =
            (MoveBean.RATE * (it.translationY + y) / mBinding.playgroundFgRoom.measuredHeight).roundToInt()
        animateName(it, xInRate, yInRate)
        roomViewModel.reportCurrentPos(xInRate, yInRate)
    }

    private fun showGameListDialog() {
        GameListDialog().show(childFragmentManager, GameListDialog.TAG)
    }

    private fun showExitAlertDialog() {
        AlertDialog.Builder(requireContext()).setTitle(R.string.alert)
            .setMessage(if (amHost) R.string.admin_sure_to_exit else R.string.user_sure_to_exit)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                findNavController().popBackStack()
                dialog.dismiss()
            }.show()
    }

    private fun showStartConfirmDialog() {
        val gameName = mBinding.btnTrophyFgRoom.text
        val groupCount = mBinding.numberPickerFgRoom.value
        if (currentPeopleCount < groupCount) "当前分组无法开始游戏".toast(requireContext())
        else
            AlertDialog.Builder(requireContext()).setTitle(R.string.alert)
                .setMessage(
                    getString(
                        R.string.start_alert_message,
                        currentPeopleCount,
                        groupCount,
                        gameName
                    )
                )
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    adminRequestStartGame(groupCount)
//                    dialog.dismiss()
                }.show()
    }

    private fun adminRequestStartGame(groupCount: Int) {
        val players = mutableListOf<LocalUser>()

        // Gather all player
        mBinding.playgroundFgRoom.forEach {
            if (it is Chip)
                players.add(LocalUser(it.text.toString(), it.tag as Int))
        }
        // 10 / 4 = 2 .. 2
        val groupSize = players.size / groupCount
        val perfectGroup = players.size % groupCount == 0
        // Random these players to group
        val tempPlayerWithGroup = Array<ArrayList<LocalUser>>(groupCount){ arrayListOf() }
        for (i in 0 until players.size){
            var whichGroup: Int
            while (true){
                whichGroup = Random.nextInt(groupCount)
                "random:$whichGroup".log()
                val currentSize = tempPlayerWithGroup[whichGroup].size
                if ( currentSize <= groupSize) {
                    if (currentSize < groupSize)
                        break
                    if (!perfectGroup)
                        break
                }
            }
            tempPlayerWithGroup[whichGroup].add(players[i])
        }
        players.clear()
        roomViewModel.adminStartGame(tempPlayerWithGroup)
    }


    private fun animateName(v: View, xInRate: Int, yInRate: Int) {
        val maxX = mBinding.playgroundFgRoom.measuredWidth
        val maxY = mBinding.playgroundFgRoom.measuredHeight

        val finalX = (1f * maxX * xInRate / MoveBean.RATE).coerceIn(0f, 0f + maxX)
        val finalY = (1f * maxY * yInRate / MoveBean.RATE).coerceIn(0f, 0f + maxY)
        v.animate().translationX(finalX).translationY(finalY).start()
    }

    private fun createAvatar(u: LocalUser): Chip = Chip(requireContext()).apply {
        // Make self differ from other
        if (u.userId == GameConstants.localUser.userId) {
            isChecked = true
            myView = this
        }

        tag = u.userId
        text = u.userName

        // can not be touched
        isFocusable = false
        isClickable = false
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            topToTop = ConstraintSet.PARENT_ID
            leftToLeft = ConstraintSet.PARENT_ID
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        "room onDestroyView".log()
        _mBinding = null
        myView = null
    }
}