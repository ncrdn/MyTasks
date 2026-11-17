package com.nyl.mytasks.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.nyl.mytasks.ui.theme.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyl.mytasks.R
import com.nyl.mytasks.notif.Notifier
import com.nyl.mytasks.ui.common.IconCatalog
import com.nyl.mytasks.ui.dashboard.DashboardScreen
import com.nyl.mytasks.ui.detail.TaskDetailScreen
import com.nyl.mytasks.ui.edit.TaskEditScreen
import com.nyl.mytasks.ui.list.TypeTabScreen
import com.nyl.mytasks.ui.settings.RoomsSettingsScreen
import com.nyl.mytasks.ui.settings.SettingsScreen
import com.nyl.mytasks.ui.settings.TypesSettingsScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val TYPE = "type/{typeId}"
    const val TASK = "task/{taskId}"
    const val EDIT = "edit/{typeId}/{taskId}"
    const val SETTINGS = "settings"
    const val SETTINGS_TYPES = "settings/types"
    const val SETTINGS_ROOMS = "settings/rooms"

    fun type(id: String) = "type/$id"
    fun task(id: String) = "task/$id"
    fun edit(typeId: String, taskId: String?) = "edit/$typeId/${taskId ?: "new"}"
}

@Composable
private fun RowScope.NavPill(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    // L'onglet actif prend toute la place restante : son intitulé s'affiche en entier (2 lignes au besoin).
    Row(
        (if (selected) Modifier.weight(1f) else Modifier.width(42.dp))
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (selected) Color.White else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = if (selected) 10.dp else 0.dp, vertical = 4.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Palette.Ink else Color(0xFFB9B7DA), modifier = Modifier.size(22.dp))
        if (selected) {
            Text(
                label, Modifier.padding(start = 6.dp).weight(1f, fill = false), color = Palette.Ink, maxLines = 2,
                overflow = TextOverflow.Ellipsis, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val types by vm.types.collectAsStateWithLifecycle()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val currentTypeId = backStack?.arguments?.getString("typeId")
    val showBar = route == Routes.DASHBOARD || route == Routes.TYPE

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshToday() }

    // Demande unique de la permission d'afficher les rappels (Android 13+).
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.hasPermission(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Tous les onglets de types partagent la route « type/{typeId} » : on ne sauvegarde/restaure donc pas
    // l'état de la pile (il restaurerait toujours le premier type visité), on repart du tableau de bord.
    fun goTab(r: String) = nav.navigate(r) {
        popUpTo(nav.graph.findStartDestination().id)
        launchSingleTop = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                // Barre flottante en pilule : seul l'onglet actif affiche son intitulé.
                Surface(
                    modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
                    shape = RoundedCornerShape(34.dp),
                    color = Palette.Ink,
                    shadowElevation = 12.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NavPill(
                            selected = route == Routes.DASHBOARD,
                            icon = Icons.Filled.Dashboard,
                            label = stringResource(R.string.nav_home),
                            onClick = { goTab(Routes.DASHBOARD) },
                        )
                        // Les onglets de types suivent le catalogue : intitulés, icônes et couleurs personnalisés.
                        types.forEach { type ->
                            NavPill(
                                selected = route == Routes.TYPE && currentTypeId == type.id,
                                icon = IconCatalog.get(type.iconKey),
                                label = type.label,
                                onClick = { goTab(Routes.type(type.id)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(vm, onOpenTask = { nav.navigate(Routes.task(it)) }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.TYPE, arguments = listOf(navArgument("typeId") { type = NavType.StringType })) { entry ->
                TypeTabScreen(
                    vm, typeId = entry.arguments?.getString("typeId").orEmpty(),
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onAdd = { nav.navigate(Routes.edit(it, null)) },
                )
            }
            composable(Routes.TASK, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                TaskDetailScreen(
                    vm, taskId = entry.arguments?.getString("taskId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onEdit = { typeId, taskId -> nav.navigate(Routes.edit(typeId, taskId)) },
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(
                    navArgument("typeId") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.StringType },
                ),
            ) { entry ->
                val taskId = entry.arguments?.getString("taskId")?.takeIf { it != "new" }
                TaskEditScreen(
                    vm, typeId = entry.arguments?.getString("typeId").orEmpty(), taskId = taskId,
                    onClose = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm, onBack = { nav.popBackStack() },
                    onOpenTypes = { nav.navigate(Routes.SETTINGS_TYPES) },
                    onOpenRooms = { nav.navigate(Routes.SETTINGS_ROOMS) },
                )
            }
            composable(Routes.SETTINGS_TYPES) { TypesSettingsScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS_ROOMS) { RoomsSettingsScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
