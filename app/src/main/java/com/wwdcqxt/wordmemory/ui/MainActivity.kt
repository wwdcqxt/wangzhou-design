package com.wwdcqxt.wordmemory.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.ActivityMainBinding

/**
 * 单 Activity + Navigation 架构。
 * 只在 4 个一级页面显示底部导航，进入复习 / 详情 / 导入等二级页面时隐藏。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        val topLevel = setOf(R.id.nav_home, R.id.nav_wordbooks, R.id.nav_wrong, R.id.nav_mine)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomBar = destination.id in topLevel
            binding.bottomNav.isVisible = showBottomBar
            binding.navDivider.isVisible = showBottomBar
        }
    }
}
