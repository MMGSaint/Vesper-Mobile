package com.vesper.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vesper.mobile.AppContainer
import com.vesper.mobile.VesperApplication
import com.vesper.mobile.ui.screens.ApplicationsViewModel
import com.vesper.mobile.ui.screens.AuditViewModel
import com.vesper.mobile.ui.screens.ChatViewModel
import com.vesper.mobile.ui.screens.DiscoveryViewModel
import com.vesper.mobile.ui.screens.DriveIntakeViewModel
import com.vesper.mobile.ui.screens.InboxViewModel
import com.vesper.mobile.ui.screens.OperatorHomeViewModel
import com.vesper.mobile.ui.screens.ProposalViewModel
import com.vesper.mobile.ui.screens.ReleaseViewModel
import com.vesper.mobile.ui.screens.ScheduleViewModel
import com.vesper.mobile.ui.screens.SettingsViewModel
import com.vesper.mobile.ui.screens.UnlockViewModel

class VesperViewModelFactory(
    private val app: VesperApplication,
) : ViewModelProvider.Factory {
    private val c: AppContainer get() = app.container

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(AppStateViewModel::class.java) -> AppStateViewModel(c)
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(c)
            modelClass.isAssignableFrom(UnlockViewModel::class.java) -> UnlockViewModel(c)
            modelClass.isAssignableFrom(OperatorHomeViewModel::class.java) -> OperatorHomeViewModel(c)
            modelClass.isAssignableFrom(InboxViewModel::class.java) -> InboxViewModel(c)
            modelClass.isAssignableFrom(ProposalViewModel::class.java) -> ProposalViewModel(c)
            modelClass.isAssignableFrom(ReleaseViewModel::class.java) -> ReleaseViewModel(c)
            modelClass.isAssignableFrom(ApplicationsViewModel::class.java) -> ApplicationsViewModel(c)
            modelClass.isAssignableFrom(AuditViewModel::class.java) -> AuditViewModel(c)
            modelClass.isAssignableFrom(ScheduleViewModel::class.java) -> ScheduleViewModel(c)
            modelClass.isAssignableFrom(DiscoveryViewModel::class.java) -> DiscoveryViewModel(c)
            modelClass.isAssignableFrom(DriveIntakeViewModel::class.java) -> DriveIntakeViewModel(c)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(app, c)
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
        return vm as T
    }
}
