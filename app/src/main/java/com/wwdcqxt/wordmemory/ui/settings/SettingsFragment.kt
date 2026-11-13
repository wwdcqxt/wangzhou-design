package com.wwdcqxt.wordmemory.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wwdcqxt.wordmemory.R
import com.wwdcqxt.wordmemory.databinding.FragmentSettingsBinding
import com.wwdcqxt.wordmemory.notification.NotificationHelper
import com.wwdcqxt.wordmemory.notification.ReminderScheduler
import com.wwdcqxt.wordmemory.ui.common.appContainer
import com.wwdcqxt.wordmemory.ui.common.viewModelFactory
import com.wwdcqxt.wordmemory.util.TimeFormats
import kotlinx.coroutines.launch

/**
 * “我的”页：每日目标、每日提醒（WorkManager）、通知权限降级提示、看板入口。
 *
 * 权限降级策略：Android 13+ 若用户拒绝 POST_NOTIFICATIONS，
 * 提醒开关仍可用（任务照常到期检查），但只弹应用内横幅，不发系统通知。
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory {
            SettingsViewModel(appContainer.settingsRepository, appContainer.statsRepository)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(requireContext(), R.string.settings_notification_denied, Toast.LENGTH_LONG).show()
            }
            refreshNotificationUi()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sliderGoal.addOnChangeListener { _, value, fromUser ->
            if (fromUser) viewModel.setDailyGoal(value.toInt())
        }

        binding.switchReminder.setOnCheckedChangeListener { _, checked ->
            viewModel.setReminderEnabled(checked)
            if (checked) {
                requestNotificationIfNeeded()
                ReminderScheduler.schedule(requireContext(), currentHour(), currentMinute())
            } else {
                ReminderScheduler.cancel(requireContext())
            }
            refreshNotificationUi()
        }

        binding.rowReminderTime.setOnClickListener { showTimePicker() }

        binding.btnOpenNotification.setOnClickListener {
            openNotificationSettings()
        }

        binding.btnOpenDashboard.setOnClickListener {
            openDashboard()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationUi()
        viewModel.loadOverview()
    }

    private fun render(state: SettingsViewModel.UiState) {
        if (_binding == null) return

        val settings = state.settings
        binding.tvGoalValue.text = "${settings.dailyGoal} 词 / 天"
        if (binding.sliderGoal.value != settings.dailyGoal.toFloat()) {
            binding.sliderGoal.value = settings.dailyGoal.toFloat()
        }

        if (binding.switchReminder.isChecked != settings.reminderEnabled) {
            binding.switchReminder.isChecked = settings.reminderEnabled
        }
        binding.tvReminderTime.text = TimeFormats.timeOfDay(settings.reminderHour, settings.reminderMinute)
        binding.tvOverview.text = state.overview
    }

    private fun refreshNotificationUi() {
        if (_binding == null) return
        val granted = NotificationHelper.canPost(requireContext())
        binding.tvNotificationStatus.text = getString(
            if (granted) R.string.settings_notification_granted else R.string.settings_notification_denied
        )
        binding.btnOpenNotification.isVisible = !granted
    }

    private fun requestNotificationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canPost(requireContext())) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun showTimePicker() {
        val settings = viewModel.state.value.settings
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                viewModel.setReminderTime(hour, minute)
                if (viewModel.state.value.settings.reminderEnabled) {
                    ReminderScheduler.schedule(requireContext(), hour, minute)
                }
            },
            settings.reminderHour,
            settings.reminderMinute,
            true
        ).show()
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", requireContext().packageName, null))
        }
        runCatching { startActivity(intent) }
    }

    /** 伴生模块是独立 APK，用包名显式启动；未安装时给出提示（降级） */
    private fun openDashboard() {
        val intent = Intent().apply {
            setClassName(
                DASHBOARD_PACKAGE,
                "$DASHBOARD_PACKAGE.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(
                    requireContext(),
                    R.string.settings_dashboard_missing,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun currentHour(): Int = viewModel.state.value.settings.reminderHour

    private fun currentMinute(): Int = viewModel.state.value.settings.reminderMinute

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val DASHBOARD_PACKAGE = "com.wwdcqxt.wordmemory.dashboard"
    }
}
