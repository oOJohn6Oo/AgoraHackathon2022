package io.agora.example.familygame.presentation.game

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.tabs.TabLayoutMediator
import io.agora.example.familygame.GlobalViewModel
import io.agora.example.familygame.R
import io.agora.example.familygame.bean.LocalUser
import io.agora.example.familygame.bean.MoveBean
import io.agora.example.familygame.bean.RoomInfo
import io.agora.example.familygame.databinding.FragmentGameBinding
import io.agora.example.familygame.util.*
import io.agora.rtc2.Constants
import io.agora.rtc2.video.VideoCanvas
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * 游戏主界面
 *
 * argument 传递所有玩家信息，包括分组
 * Intent 大小限制 为 512KB
 * RTM Message 大小限制为 2KB
 */
class GameFragment : Fragment() {

    companion object {
        const val GROUP_KEY = "GROUP"
        const val avatarSize = 72
    }

    private var _mBinding: FragmentGameBinding? = null
    private val mBinding get() = _mBinding!!

    private lateinit var playerWithGroup: Array<ArrayList<LocalUser>>
    private lateinit var previousRoom: RoomInfo

    private val globalViewModel: GlobalViewModel by activityViewModels()
    private val gameViewModel: GameViewModel by viewModels {
        GameViwModelFactory(
            globalViewModel.rtcEngine,
            globalViewModel.client,
            playerWithGroup,
            previousRoom
        )
    }

    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>

    private var myView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        playerWithGroup = arguments?.getString(GROUP_KEY)?.let {
            GameConstants.gson.fromJson(it, GameConstants.groupType)
        } ?: return null

        previousRoom =
            arguments?.getSerializable(RoomInfo.TAG)?.let { it as RoomInfo } ?: return null

        _mBinding = FragmentGameBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListener()
        initObserver()

        // 首次视图渲染
        if (gameViewModel.amHost)
            showLocalVideo(mBinding.hostCameraFgGame)
        else
            showRemoteVideo(mBinding.hostCameraFgGame, previousRoom.userId)
    }

    private fun initView() {
        behavior = BottomSheetBehavior.from(mBinding.bottomSheetFgGame).apply {
            isHideable = false
            peekHeight = 42.dp.toInt()
            state = STATE_EXPANDED
        }

        mBinding.viewPagerFgGame.offscreenPageLimit = 3
        mBinding.viewPagerFgGame.adapter = GameFragmentAdapter(this, gameViewModel.userList)

        TabLayoutMediator(mBinding.tabFgGame, mBinding.viewPagerFgGame) { tab, pos ->
            tab.text = "${previousRoom.roomName}-${pos + 1}组"
        }.attach()

        mBinding.gameFgGame.isEnabled = !gameViewModel.amHost

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        mBinding.dragFgGame.setOnClickListener {
            behavior.state =
                if (behavior.state == STATE_EXPANDED) STATE_COLLAPSED else STATE_EXPANDED
        }
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                mBinding.dragFgGame.rotation = 180 * (1 - slideOffset)
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }
        })

        mBinding.btnMicFgGame.addOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) gameViewModel.enableMic(isChecked)
        }
        mBinding.btnCameraFgGame.addOnCheckedChangeListener { button, isChecked ->
            if (button.isPressed) gameViewModel.enableCamera(isChecked)
        }
        mBinding.viewPagerFgGame.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (gameViewModel.amHost)
                    gameViewModel.switchGroup(position)
            }
        })
        mBinding.playgroundFgGame.setOnTouchListener(GestureTouchListener(this::selfMoved))
        mBinding.gameFgGame.finishListener = this::onUserOpFinished
    }

    private fun initObserver() {
        observe(gameViewModel.isMicEnabled) { enabled ->
            mBinding.btnMicFgGame.isChecked = enabled
            mBinding.btnMicFgGame.setIconResource(if (enabled) R.drawable.ic_mic else R.drawable.ic_mic_off)
        }
        observe(gameViewModel.isCameraEnabled) { enabled ->
            mBinding.btnCameraFgGame.isChecked = enabled
            mBinding.btnCameraFgGame.setIconResource(if (enabled) R.drawable.ic_camera else R.drawable.ic_camera_off)
        }
        observe(gameViewModel.rtcUserList) {
            mBinding.playgroundFgGame.removeAllViews()
            it.forEach { u ->
                val cameraView = createCameraView(u.userId)
                mBinding.playgroundFgGame.addView(cameraView)
                // 本人
                if (u.userId == GameConstants.localUser.userId) {
                    myView = cameraView
                    showLocalVideo(cameraView[0] as TextureView)
                } else {
                    showRemoteVideo(cameraView[0] as TextureView, u.userId)
                }
            }
        }

        observe(gameViewModel.moveOp) {
            mBinding.playgroundFgGame.findViewWithTag<CardView>(it.id)?.let { v ->
                animateName(v, it.x, it.y)
            }
        }

        observe(gameViewModel.timeBean) {
            val time = it.startMs - System.currentTimeMillis()
            when {
                it.gameEnd -> {
                    showEndDialog(it.winnerGroup, (it.endMs - it.startMs).toInt())
                }
                time >= 0L -> {
                    mBinding.alertMessageFgGame.text = requireContext().getString(
                        R.string.game_start_alert,
                        time / 1000
                    )
                }
                time < 0L -> {
                    if (!gameViewModel.amHost) mBinding.gameFgGame.isEnabled = true
                    gameViewModel.ensureHanoi()
                    mBinding.alertMessageFgGame.visibility = GONE
                }
            }
        }
        observe(gameViewModel.hanoi) {
            mBinding.gameFgGame.hanoiValue = it
        }
    }

    private fun onUserOpFinished(succeed:Boolean){
        if (succeed) gameViewModel.reportHanoi(mBinding.gameFgGame.hanoiValue)
    }

    private fun selfMoved(x: Float, y: Float) = myView?.let {
        val xInRate =
            (MoveBean.RATE * (it.translationX + x) / mBinding.playgroundFgGame.measuredWidth).roundToInt()
        val yInRate =
            (MoveBean.RATE * (it.translationY + y) / mBinding.playgroundFgGame.measuredHeight).roundToInt()
        animateName(it, xInRate, yInRate)
        gameViewModel.reportCurrentPos(xInRate, yInRate)
    }

    private fun animateName(v: View, xInRate: Int, yInRate: Int) {
        val maxX = mBinding.playgroundFgGame.measuredWidth
        val maxY = mBinding.playgroundFgGame.measuredHeight

        val finalX = (1f * maxX * xInRate / MoveBean.RATE).coerceIn(0f, 0f + maxX - v.measuredWidth)
        val finalY = (1f * maxY * yInRate / MoveBean.RATE).coerceIn(0f, 0f + maxY - v.measuredHeight)
        v.animate().translationX(finalX).translationY(finalY).start()
    }

    private fun createCameraView(id: Int): CardView = CardView(requireContext()).apply {
        tag = id
        layoutParams = FrameLayout.LayoutParams(avatarSize.dp.toInt(), avatarSize.dp.toInt())
        translationX = Random.nextInt(mBinding.playgroundFgGame.measuredWidth).toFloat()
        val h =mBinding.waitingAreaFgGame.measuredHeight
        translationY = 0f + Random.nextInt(h - avatarSize.dp.toInt()) + mBinding.playgroundFgGame.measuredHeight - h

        setCardBackgroundColor(Color.WHITE)
        radius = avatarSize.shr(1).dp
        addView(TextureView(requireContext()))
    }

    private fun showLocalVideo(view: TextureView) {
        globalViewModel.rtcEngine.setupLocalVideo(
            VideoCanvas(
                view,
                Constants.RENDER_MODE_HIDDEN,
                GameConstants.localUser.userId
            )
        )
    }

    private fun showRemoteVideo(view: TextureView, id: Int) {
        globalViewModel.rtcEngine.setupRemoteVideo(
            VideoCanvas(
                view,
                Constants.RENDER_MODE_HIDDEN,
                id
            )
        )
    }

    private fun showEndDialog(winnerGroup: Int, usedTime: Int) {
        AlertDialog.Builder(requireContext())
            .setMessage(requireContext().getString(R.string.game_end_alert, winnerGroup + 1, usedTime))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { d, _ ->
                d.dismiss()
                findNavController().popBackStack()
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mBinding = null
    }
}