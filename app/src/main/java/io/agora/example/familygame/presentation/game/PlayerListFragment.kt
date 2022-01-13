package io.agora.example.familygame.presentation.game

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.FrameLayout.LayoutParams
import androidx.core.view.get
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.example.familygame.bean.LocalUser
import io.agora.example.familygame.util.dp
import io.agora.example.familygame.util.getAttrResId
import kotlin.math.roundToInt

class PlayerListFragment : Fragment() {
    companion object {
        const val USER_LIST = "userList"
    }

    private lateinit var userList:List<LocalUser>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getParcelableArrayList<LocalUser>(USER_LIST)?.let {
            userList = it
        }

        (view as RecyclerView).adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return object :RecyclerView.ViewHolder(FrameLayout(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setBackgroundResource(requireContext().getAttrResId(android.R.attr.selectableItemBackground))
                    setPadding(16.dp.roundToInt())
                    addView(TextView(this.context), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL })
                    addView(TextView(this.context), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL })
                }){}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as FrameLayout).let {
                    (it[0] as TextView).text = userList[position].userName
                    (it[1] as TextView).text = (position + 1).toString()
                    it.setOnClickListener {  }
                }

            }
            override fun getItemCount() = userList.size
        }
    }

}