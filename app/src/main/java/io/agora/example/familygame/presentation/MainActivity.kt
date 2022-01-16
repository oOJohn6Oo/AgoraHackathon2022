package io.agora.example.familygame.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import io.agora.example.familygame.FragmentActivityFactory
import io.agora.example.familygame.GlobalViewModel
import io.agora.example.familygame.R


class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewModelProvider(this, FragmentActivityFactory(this)).get(GlobalViewModel::class.java)
    }
}