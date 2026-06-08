package com.jishiyong

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jishiyong.ui.screens.AddItemScreen
import com.jishiyong.ui.screens.HomeScreen
import com.jishiyong.ui.screens.InspectionScreen
import com.jishiyong.ui.screens.ItemDetailScreen
import com.jishiyong.ui.screens.SettingsScreen
import com.jishiyong.ui.screens.StatisticsScreen
import com.jishiyong.ui.components.FreshBackdropStyle
import com.jishiyong.ui.theme.JiShiYongTheme
import com.jishiyong.viewmodel.MainViewModel
import com.jishiyong.viewmodel.StatisticsViewModel

class MainActivity : ComponentActivity() {

    // 通知权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            JiShiYongTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JiShiYongNavigation()
                }
            }
        }
    }
}

private const val UI_PREFS_NAME = "jishiyong_ui_settings"
private const val KEY_BACKDROP_STYLE = "backdrop_style"

@Composable
fun JiShiYongNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()
    val operationError by mainViewModel.operationError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uiPreferences = remember(context) {
        context.applicationContext.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var backdropStyleName by rememberSaveable {
        mutableStateOf(
            uiPreferences.getString(KEY_BACKDROP_STYLE, FreshBackdropStyle.ColdMist.name)
                ?: FreshBackdropStyle.ColdMist.name
        )
    }
    val backdropStyle = FreshBackdropStyle.entries.find { it.name == backdropStyleName }
        ?: FreshBackdropStyle.ColdMist
    val updateBackdropStyle: (FreshBackdropStyle) -> Unit = { style ->
        backdropStyleName = style.name
        uiPreferences.edit().putString(KEY_BACKDROP_STYLE, style.name).apply()
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // 首页
        composable("home") {
            HomeScreen(
                viewModel = mainViewModel,
                onItemClick = { itemId ->
                    navController.navigate("item/$itemId")
                },
                onAddClick = {
                    navController.navigate("add")
                },
                onStatsClick = {
                    navController.navigate("statistics")
                },
                onInspectClick = {
                    navController.navigate("inspection")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                },
                backdropStyle = backdropStyle
            )
        }

        // 添加物品
        composable("add") {
            AddItemScreen(
                onSave = { item ->
                    mainViewModel.addItem(item) {
                        navController.popBackStack()
                    }
                },
                onCancel = {
                    navController.popBackStack()
                },
                backdropStyle = backdropStyle
            )
        }

        // 物品详情
        composable(
            route = "item/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            ItemDetailScreen(
                itemId = itemId,
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() },
                backdropStyle = backdropStyle
            )
        }

        // 统计页面
        composable("statistics") {
            val statisticsViewModel: StatisticsViewModel = viewModel()
            StatisticsScreen(
                viewModel = statisticsViewModel,
                onBack = { navController.popBackStack() },
                backdropStyle = backdropStyle
            )
        }

        // 小用巡视
        composable("inspection") {
            InspectionScreen(
                viewModel = mainViewModel,
                onBack = { navController.popBackStack() },
                backdropStyle = backdropStyle
            )
        }

        composable("settings") {
            SettingsScreen(
                selectedBackdropStyle = backdropStyle,
                onBackdropStyleSelected = updateBackdropStyle,
                onBack = { navController.popBackStack() }
            )
        }
    }

    operationError?.let { message ->
        AlertDialog(
            onDismissRequest = mainViewModel::dismissOperationError,
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = mainViewModel::dismissOperationError) {
                    Text("知道了")
                }
            }
        )
    }
}
