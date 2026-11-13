package com.wwdcqxt.wordmemory.ui.common

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wwdcqxt.wordmemory.WordMemoryApp
import com.wwdcqxt.wordmemory.di.AppContainer

/** Fragment 侧拿到 Application 级依赖容器 */
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as WordMemoryApp).container

/**
 * 轻量 ViewModel 工厂：调用方只需要写构造逻辑，不必手写 Factory 类。
 * 用法：`private val viewModel: HomeViewModel by viewModels { viewModelFactory { HomeViewModel(appContainer.wordRepository) } }`
 */
fun <VM : ViewModel> viewModelFactory(create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
