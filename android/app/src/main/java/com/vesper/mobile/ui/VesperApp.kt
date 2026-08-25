package com.vesper.mobile.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vesper.mobile.VesperApplication
import com.vesper.mobile.ui.components.Hairline
import com.vesper.mobile.ui.screens.ActivityScreen
import com.vesper.mobile.ui.screens.ApplicationsScreen
import com.vesper.mobile.ui.screens.ApplicationsViewModel
import com.vesper.mobile.ui.screens.AuditScreen
import com.vesper.mobile.ui.screens.AuditViewModel
import com.vesper.mobile.ui.screens.ChatScreen
import com.vesper.mobile.ui.screens.ChatViewModel
import com.vesper.mobile.ui.screens.DiscoveryScreen
import com.vesper.mobile.ui.screens.DiscoveryViewModel
import com.vesper.mobile.ui.screens.DriveIntakeScreen
import com.vesper.mobile.ui.screens.DriveIntakeViewModel
import com.vesper.mobile.ui.screens.HomeScreen
import com.vesper.mobile.ui.screens.InboxScreen
import com.vesper.mobile.ui.screens.InboxViewModel
import com.vesper.mobile.ui.screens.MoreScreen
import com.vesper.mobile.ui.screens.NotificationsScreen
import com.vesper.mobile.ui.screens.OperatorHomeScreen
import com.vesper.mobile.ui.screens.OperatorHomeViewModel
import com.vesper.mobile.ui.screens.ProposalDetailScreen
import com.vesper.mobile.ui.screens.ProposalViewModel
import com.vesper.mobile.ui.screens.ReleaseCenterScreen
import com.vesper.mobile.ui.screens.ReleaseViewModel
import com.vesper.mobile.ui.screens.ScheduleScreen
import com.vesper.mobile.ui.screens.ScheduleViewModel
import com.vesper.mobile.ui.screens.SettingsScreen
import com.vesper.mobile.ui.screens.SettingsViewModel
import com.vesper.mobile.ui.screens.SplashScreen
import com.vesper.mobile.ui.screens.UnlockScreen
import com.vesper.mobile.ui.screens.UnlockViewModel
import com.vesper.mobile.ui.screens.VoiceScreen
import com.vesper.mobile.ui.theme.LabelStyle
import com.vesper.mobile.ui.theme.Muted
import com.vesper.mobile.ui.theme.NearBlack
import com.vesper.mobile.ui.theme.Parchment

object Routes {
    const val Home = "home"
    const val Chat = "chat"
    const val Operator = "operator"
    const val Inbox = "operator/inbox"
    const val Proposal = "operator/proposal/{id}"
    const val Release = "operator/release"
    const val Applications = "operator/applications"
    const val Audit = "operator/audit"
    const val Schedule = "operator/schedule"
    const val Discovery = "operator/discovery"
    const val Intake = "operator/intake"
    const val Settings = "settings"
    const val More = "more"
    const val Notifications = "notifications"
    const val Activity = "activity"
    const val Voice = "voice"

    fun proposal(id: String): String = "operator/proposal/${Uri.encode(id)}"
}

private data class TabSpec(val route: String, val label: String)

private val Tabs = listOf(
    TabSpec(Routes.Home, "HOME"),
    TabSpec(Routes.Chat, "CHAT"),
    TabSpec(Routes.Operator, "OPERATOR"),
    TabSpec(Routes.More, "MORE"),
)

@Composable
fun VesperApp(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as VesperApplication
    val factory = remember(app) { VesperViewModelFactory(app) }
    val appVm: AppStateViewModel = viewModel(factory = factory)
    var splash by remember { mutableStateOf(true) }

    if (splash) {
        SplashScreen(appVm) { splash = false }
        return
    }

    val nav = rememberNavController()
    val appState by appVm.state.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val unlocked = appState.surface.operator.unlocked

    LaunchedEffect(unlocked, current) {
        if (!unlocked && current != null && current.startsWith("operator/")) {
            nav.navigate(Routes.Operator) {
                popUpTo(Routes.Operator) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlack),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = nav,
                startDestination = Routes.Home,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.Home) {
                    HomeScreen(
                        appVm = appVm,
                        onChat = { nav.goTab(Routes.Chat) },
                        onOperator = { nav.goTab(Routes.Operator) },
                        onUnlock = { nav.goTab(Routes.Operator) },
                        onSettings = { nav.navigate(Routes.Settings) },
                    )
                }
                composable(Routes.Chat) {
                    val vm: ChatViewModel = viewModel(factory = factory)
                    ChatScreen(vm = vm, appVm = appVm, onBack = null)
                }
                composable(Routes.Operator) {
                    val live by appVm.state.collectAsStateWithLifecycle()
                    if (!live.surface.operator.unlocked) {
                        val vm: UnlockViewModel = viewModel(factory = factory)
                        UnlockScreen(
                            vm = vm,
                            appVm = appVm,
                            onBack = { nav.goTab(Routes.Home) },
                            onUnlocked = { appVm.refresh() },
                        )
                    } else {
                        val vm: OperatorHomeViewModel = viewModel(factory = factory)
                        OperatorHomeScreen(
                            vm = vm,
                            appVm = appVm,
                            onBack = null,
                            onInbox = { nav.navigate(Routes.Inbox) },
                            onRelease = { nav.navigate(Routes.Release) },
                            onApps = { nav.navigate(Routes.Applications) },
                            onAudit = { nav.navigate(Routes.Audit) },
                            onSchedule = { nav.navigate(Routes.Schedule) },
                            onDiscovery = { nav.navigate(Routes.Discovery) },
                            onDrive = { nav.navigate(Routes.Intake) },
                            onLogout = { appVm.logout() },
                            onTile = { hint -> tileRoute(hint)?.let { nav.navigate(it) } },
                        )
                    }
                }
                composable(Routes.Inbox) {
                    val vm: InboxViewModel = viewModel(factory = factory)
                    InboxScreen(
                        vm = vm,
                        appVm = appVm,
                        onBack = { nav.popBackStack() },
                        onOpen = { id -> nav.navigate(Routes.proposal(id)) },
                    )
                }
                composable(
                    route = Routes.Proposal,
                    arguments = listOf(navArgument("id") { type = NavType.StringType }),
                ) { back ->
                    val id = Uri.decode(back.arguments?.getString("id").orEmpty())
                    val vm: ProposalViewModel = viewModel(factory = factory)
                    ProposalDetailScreen(
                        id = id,
                        vm = vm,
                        appVm = appVm,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(Routes.Release) {
                    val vm: ReleaseViewModel = viewModel(factory = factory)
                    ReleaseCenterScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Applications) {
                    val vm: ApplicationsViewModel = viewModel(factory = factory)
                    ApplicationsScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Audit) {
                    val vm: AuditViewModel = viewModel(factory = factory)
                    AuditScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Schedule) {
                    val vm: ScheduleViewModel = viewModel(factory = factory)
                    ScheduleScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Discovery) {
                    val vm: DiscoveryViewModel = viewModel(factory = factory)
                    DiscoveryScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Intake) {
                    val vm: DriveIntakeViewModel = viewModel(factory = factory)
                    DriveIntakeScreen(vm = vm, appVm = appVm, onBack = { nav.popBackStack() })
                }
                composable(Routes.More) {
                    MoreScreen(
                        appVm = appVm,
                        onBack = null,
                        onSettings = { nav.navigate(Routes.Settings) },
                        onNotifications = { nav.navigate(Routes.Notifications) },
                        onActivity = { nav.navigate(Routes.Activity) },
                        onVoice = { nav.navigate(Routes.Voice) },
                        onAudit = { nav.navigate(Routes.Audit) },
                        onLogout = { appVm.logout(); nav.goTab(Routes.Home) },
                    )
                }
                composable(Routes.Settings) {
                    val vm: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(
                        vm = vm,
                        onBack = { nav.popBackStack() },
                        onNotifications = { nav.navigate(Routes.Notifications) },
                    )
                }
                composable(Routes.Notifications) {
                    val vm: SettingsViewModel = viewModel(factory = factory)
                    NotificationsScreen(vm = vm, onBack = { nav.popBackStack() })
                }
                composable(Routes.Activity) {
                    val vm: ChatViewModel = viewModel(factory = factory)
                    ActivityScreen(
                        vm = vm,
                        appVm = appVm,
                        onBack = { nav.popBackStack() },
                        onChat = { nav.goTab(Routes.Chat) },
                        onInbox = { nav.navigate(Routes.Inbox) },
                        onRelease = { nav.navigate(Routes.Release) },
                        onApps = { nav.navigate(Routes.Applications) },
                    )
                }
                composable(Routes.Voice) {
                    VoiceScreen(onBack = { nav.popBackStack() })
                }
            }
        }
        BottomTabs(
            currentRoute = current,
            onTab = { nav.goTab(it) },
        )
    }
}

@Composable
private fun BottomTabs(currentRoute: String?, onTab: (String) -> Unit) {
    val selected = selectedTab(currentRoute)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NearBlack)
            .navigationBarsPadding(),
    ) {
        Hairline()
        Row(Modifier.fillMaxWidth()) {
            Tabs.forEach { tab ->
                val active = selected == tab.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clickable { onTab(tab.route) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = tab.label,
                            style = LabelStyle.copy(
                                color = if (active) Parchment else Muted,
                                letterSpacing = 1.8.sp,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun selectedTab(route: String?): String = when {
    route == null -> Routes.Home
    route == Routes.Chat -> Routes.Chat
    route.startsWith("operator") -> Routes.Operator
    route == Routes.Home -> Routes.Home
    else -> Routes.More
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
    }
}

internal fun tileRoute(hint: String?): String? {
    if (hint.isNullOrBlank()) return null
    return when (hint.trim().lowercase()) {
        "inbox", "staging", "review", "attention", "pipeline", "approved", "prepared", "sealed" -> Routes.Inbox
        "release", "rel", "candidate" -> Routes.Release
        "applications", "apps" -> Routes.Applications
        "schedule", "sched" -> Routes.Schedule
        "discovery", "fragments", "tease" -> Routes.Discovery
        "intake", "drive", "drive files" -> Routes.Intake
        "audit" -> Routes.Audit
        else -> null
    }
}
