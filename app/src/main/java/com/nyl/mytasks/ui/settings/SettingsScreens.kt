package com.nyl.mytasks.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nyl.mytasks.R
import com.nyl.mytasks.domain.HouseRoom
import com.nyl.mytasks.domain.TaskType
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.common.ConfirmDialog
import com.nyl.mytasks.ui.common.IconCatalog
import com.nyl.mytasks.ui.common.SectionTitle
import com.nyl.mytasks.ui.common.TypeBadge
import com.nyl.mytasks.ui.common.toColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    fab: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        floatingActionButton = fab,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { content() }
    }
}

// --- Écran principal des réglages ---

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenTypes: () -> Unit,
    onOpenRooms: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val msgExported = stringResource(R.string.msg_exported)
    val msgImported = stringResource(R.string.msg_imported)
    val msgError = stringResource(R.string.msg_error)

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            snackbar.showSnackbar(if (vm.export(context, uri).isSuccess) msgExported else msgError)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    SettingsScaffold(stringResource(R.string.settings), onBack, snackbar) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.settings_customize), modifier = Modifier.padding(horizontal = 16.dp))
            NavRow(stringResource(R.string.settings_types), stringResource(R.string.settings_types_help), onOpenTypes)
            NavRow(stringResource(R.string.settings_rooms), stringResource(R.string.settings_rooms_help), onOpenRooms)

            SectionTitle(stringResource(R.string.settings_display), modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.font_size, (settings.fontScale * 100).toInt()))
                Slider(
                    value = settings.fontScale, valueRange = 0.85f..1.5f, steps = 12,
                    onValueChange = { v -> vm.setFontScale(Math.round(v * 20) / 20f) },
                )
                Text(stringResource(R.string.font_size_sample), style = MaterialTheme.typography.bodyLarge)
            }

            SectionTitle(stringResource(R.string.settings_notifications), modifier = Modifier.padding(horizontal = 16.dp))
            SwitchRow(stringResource(R.string.notif_enable), settings.notificationsEnabled) { on ->
                if (on) ensureNotificationPermission()
                vm.updateSettings(context) { it.copy(notificationsEnabled = on) }
            }
            if (settings.notificationsEnabled) {
                TimeRow(stringResource(R.string.notif_daily_time), settings.dailyHour, settings.dailyMinute) { h, m ->
                    vm.updateSettings(context) { it.copy(dailyHour = h, dailyMinute = m) }
                }
                SwitchRow(stringResource(R.string.notif_due_eve), settings.dueEveEnabled) { on ->
                    vm.updateSettings(context) { it.copy(dueEveEnabled = on) }
                }
                SwitchRow(stringResource(R.string.notif_weekly), settings.weeklyEnabled) { on ->
                    vm.updateSettings(context) { it.copy(weeklyEnabled = on) }
                }
                if (settings.weeklyEnabled) {
                    TimeRow(stringResource(R.string.notif_weekly_time), settings.weeklyHour, settings.weeklyMinute) { h, m ->
                        vm.updateSettings(context) { it.copy(weeklyHour = h, weeklyMinute = m) }
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_data), modifier = Modifier.padding(horizontal = 16.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.settings_data_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = { exportLauncher.launch("mytasks-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.export_json)) }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.import_json)) }
            }
            Box(Modifier.size(32.dp))
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.import_json), message = stringResource(R.string.import_warning),
            confirmText = stringResource(R.string.import_confirm),
            onConfirm = {
                scope.launch { snackbar.showSnackbar(if (vm.import(context, uri).isSuccess) msgImported else msgError) }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TimeRow(label: String, hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable {
            TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, true).show()
        }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

// --- Intitulés, icônes et couleurs des types (section 3.6) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypesSettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val types by vm.types.collectAsStateWithLifecycle()
    SettingsScaffold(stringResource(R.string.settings_types), onBack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            types.forEach { type -> TypeEditor(type, onChange = vm::saveType) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeEditor(type: TaskType, onChange: (TaskType) -> Unit) {
    var label by remember(type.id) { mutableStateOf(type.label) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(type, 48)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it; if (it.isNotBlank()) onChange(type.copy(label = it.trim())) },
                    label = { Text(stringResource(R.string.type_name)) }, singleLine = true,
                    isError = label.isBlank(),
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
            }
            Text(stringResource(R.string.type_icon), style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconCatalog.icons.forEach { (key, icon) ->
                    val selected = key == type.iconKey
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(if (selected) type.color.toColor().copy(alpha = 0.2f) else Color.Transparent)
                            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) type.color.toColor() else MaterialTheme.colorScheme.outlineVariant), CircleShape)
                            .clickable { onChange(type.copy(iconKey = key)) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, contentDescription = key, tint = type.color.toColor()) }
                }
            }
            Text(stringResource(R.string.type_color), style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconCatalog.colors.forEach { c ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(c.toColor())
                            .border(BorderStroke(if (c == type.color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                            .clickable { onChange(type.copy(color = c)) },
                    )
                }
            }
        }
    }
}

// --- Pièces de la maison ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsSettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val rooms by vm.rooms.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<HouseRoom?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<HouseRoom?>(null) }

    SettingsScaffold(
        title = stringResource(R.string.settings_rooms), onBack = onBack,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) },
            )
        },
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            rooms.forEachIndexed { i, room ->
                if (i > 0) HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(room.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { editing = room }) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit)) }
                    IconButton(onClick = { deleting = room }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete)) }
                }
            }
        }
    }

    if (adding) {
        RoomNameDialog(initial = "", onDismiss = { adding = false }) { name ->
            vm.saveRoom(HouseRoom(UUID.randomUUID().toString(), name, (rooms.maxOfOrNull { it.sortOrder } ?: -1) + 1))
        }
    }
    editing?.let { room ->
        RoomNameDialog(initial = room.name, onDismiss = { editing = null }) { name -> vm.saveRoom(room.copy(name = name)) }
    }
    deleting?.let { room ->
        ConfirmDialog(
            title = stringResource(R.string.delete_room_title, room.name), message = stringResource(R.string.delete_room_message),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = { vm.deleteRoom(room.id) }, onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun RoomNameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.room_name)) },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()); onDismiss() }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
