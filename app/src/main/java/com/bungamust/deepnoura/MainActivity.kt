package com.bungamust.deepnoura

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.bungamust.deepnoura.ui.AppScaffold
import com.bungamust.deepnoura.ui.theme.DeepNouraTheme
import com.bungamust.deepnoura.viewmodel.ProjectViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProjectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            DeepNouraTheme(darkTheme = viewModel.uiState.value.isDarkMode) {
                AppScaffold(viewModel = viewModel)
            }
        }
    }
}
