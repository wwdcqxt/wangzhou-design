package com.wwdcqxt.wordmemory.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.ActivityMainBinding

/**
 * 单 Activity + Navigation 架构。
 * 只在 4 个一级页面显示底部导航，进入复习 / 详情 / 导入等二级页面时隐藏。
 *
 * Android 15+ 强制 edge-to-edge，这里手动处理状态栏内边距，
 * 并把状态栏图标设为深色（浅色背景上才看得清）。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = statusBar.top)
            insets
        }

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
