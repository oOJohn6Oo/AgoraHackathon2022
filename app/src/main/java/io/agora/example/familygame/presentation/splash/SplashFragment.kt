package io.agora.example.familygame.presentation.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.agora.example.familygame.GlobalViewModel
import io.agora.example.familygame.R
import io.agora.example.familygame.databinding.FragmentSplashBinding
import io.agora.example.familygame.util.log
import io.agora.example.familygame.util.observe

class SplashFragment : Fragment() {
    private var _mBinding: FragmentSplashBinding? = null
    private val mBinding get() = _mBinding!!

    private val globalViewModel :GlobalViewModel by activityViewModels()

    private var rtcInit :Boolean? = null
    private var rtmInit :Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _mBinding = FragmentSplashBinding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initObserver()
    }

    private fun initObserver() {

        observe(globalViewModel.rtcInit){
            rtcInit = it
            checkInitStatus()
        }
        observe(globalViewModel.rtmInit){
            rtmInit = it
            checkInitStatus()
        }
    }

    private fun checkInitStatus() {
        rtcInit?.let { rtc ->
            rtmInit?.let { rtm ->
                if (rtc && rtm) {
                    findNavController().popBackStack(R.id.splashFragment, true)
                    findNavController().navigate(R.id.roomListFragment)
                }
                else if (rtc && !rtm) showErrorView("RTM")
                else if (!rtc && rtm) showErrorView("RTC")
                else showErrorView("RTM,RTC")
            }
        }
    }


    private fun showLoadingView() {
        mBinding.errorStatus.visibility = GONE
        mBinding.loadingStatus.visibility = VISIBLE
    }

    private fun showErrorView(errMsg: String) {
        mBinding.errorStatus.visibility = VISIBLE
        mBinding.loadingStatus.visibility = GONE
        mBinding.btnFgSplash.text = getString(R.string.error_happened, errMsg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mBinding = null
    }
}