package com.jishiyong

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jishiyong.ui.screens.AddItemScreen
import com.jishiyong.ui.screens.HomeScreen
import com.jishiyong.ui.screens.ItemDetailScreen
import com.jishiyong.ui.screens.StatisticsScreen
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

@Composable
fun JiShiYongNavigation() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = viewModel()

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
                }
            )
        }

        // 添加物品
        composable("add") {
            AddItemScreen(
                onSave = { item ->
                    mainViewModel.addItem(item)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
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
                onBack = { navController.popBackStack() }
            )
        }

        // 统计页面
        composable("statistics") {
            val statisticsViewModel: StatisticsViewModel = viewModel()
            StatisticsScreen(
                viewModel = statisticsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
