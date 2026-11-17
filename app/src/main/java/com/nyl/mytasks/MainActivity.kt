package com.nyl.mytasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nyl.mytasks.ui.AppRoot
import com.nyl.mytasks.ui.AppViewModel
import com.nyl.mytasks.ui.theme.MyTasksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel(
                factory = viewModelFactory { initializer { AppViewModel(application.container) } },
            )
            val settings by vm.settings.collectAsStateWithLifecycle()
            MyTasksTheme(fontScale = settings.fontScale) { AppRoot(vm) }
        }
    }
}
