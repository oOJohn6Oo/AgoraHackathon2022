package io.agora.example.familygame.presentation.game

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.agora.example.familygame.bean.LocalUser

class GameFragmentAdapter(val fragment: Fragment, private val userList: Array<ArrayList<LocalUser>>):FragmentStateAdapter(fragment) {

    override fun getItemCount() = userList.size

    override fun createFragment(position: Int) = PlayerListFragment().apply {
        arguments = Bundle().apply {
            putParcelableArrayList(PlayerListFragment.USER_LIST, userList[position])
        }
    }
}