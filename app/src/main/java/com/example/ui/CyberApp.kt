package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.CommandScreen
import com.example.ui.screens.CopilotScreen
import com.example.ui.screens.ThreatScanScreen
import com.example.ui.screens.ToolkitScreen
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

const val ROUTE_COMMAND = "command"
const val ROUTE_SCANNER = "scanner"
const val ROUTE_COPILOT = "copilot"
const val ROUTE_TOOLKIT = "toolkit"

private data class CyberDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val CYBER_DESTINATIONS = listOf(
    CyberDestination(
        route = ROUTE_COMMAND,
        labelRes = R.string.nav_command,
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_command"
    ),
    CyberDestination(
        route = ROUTE_SCANNER,
        labelRes = R.string.nav_scanner,
        selectedIcon = Icons.Filled.DocumentScanner,
        unselectedIcon = Icons.Outlined.DocumentScanner,
        testTag = "nav_scanner"
    ),
    CyberDestination(
        route = ROUTE_COPILOT,
        labelRes = R.string.nav_copilot,
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        testTag = "nav_copilot"
    ),
    CyberDestination(
        route = ROUTE_TOOLKIT,
        labelRes = R.string.nav_toolkit,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build,
        testTag = "nav_toolkit"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberApp(
    viewModel: CyberViewModel,
    themeMode: AppThemeMode
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_COMMAND

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var toolkitInitialTab by rememberSaveable { mutableIntStateOf(0) }

    val postureScore by viewModel.postureScore.collectAsStateWithLifecycle()
    val hardeningItems by viewModel.hardeningItems.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()

    val scannerInput by viewModel.scannerInput.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val attachedBitmap by viewModel.attachedBitmap.collectAsStateWithLifecycle()
    val scanUiState by viewModel.scanUiState.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()

    val passwordInput by viewModel.passwordInput.collectAsStateWithLifecycle()
    val passwordAnalysis by viewModel.passwordAnalysis.collectAsStateWithLifecycle()
    val cryptoInput by viewModel.cryptoInput.collectAsStateWithLifecycle()
    val selectedHashAlgorithm by viewModel.selectedHashAlgorithm.collectAsStateWithLifecycle()
    val hashOutput by viewModel.hashOutput.collectAsStateWithLifecycle()
    val base64Output by viewModel.base64Output.collectAsStateWithLifecycle()
    val base64Error by viewModel.base64Error.collectAsStateWithLifecycle()

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun showToastMessage(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        FilledTonalIconButton(
                            onClick = { viewModel.cycleThemeMode() },
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("theme_toggle_button")
                        ) {
                            val icon = when (themeMode) {
                                AppThemeMode.TACTICAL_DARK -> Icons.Filled.DarkMode
                                AppThemeMode.CRISP_LIGHT -> Icons.Filled.LightMode
                                AppThemeMode.SYSTEM_DYNAMIC -> Icons.Filled.Tonality
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "${stringResource(R.string.cd_theme_toggle)} (${themeMode.label})"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        CYBER_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.size(12.dp))
                        CYBER_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_COMMAND,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable(ROUTE_COMMAND) {
                            CommandScreen(
                                postureScore = postureScore,
                                hardeningItems = hardeningItems,
                                recentScans = recentScans,
                                onQuickScanCategory = { category ->
                                    viewModel.selectScanCategory(category)
                                    navigateTopLevel(ROUTE_SCANNER)
                                },
                                onOpenCopilot = {
                                    navigateTopLevel(ROUTE_COPILOT)
                                },
                                onOpenHardening = {
                                    toolkitInitialTab = 2
                                    navigateTopLevel(ROUTE_TOOLKIT)
                                },
                                onOpenReport = { report ->
                                    viewModel.openReportInScanner(report)
                                    navigateTopLevel(ROUTE_SCANNER)
                                }
                            )
                        }

                        composable(ROUTE_SCANNER) {
                            ThreatScanScreen(
                                inputText = scannerInput,
                                selectedCategory = selectedCategory,
                                attachedBitmap = attachedBitmap,
                                scanUiState = scanUiState,
                                onInputChange = viewModel::updateScannerInput,
                                onSelectCategory = viewModel::selectScanCategory,
                                onAttachImageUri = viewModel::attachScreenshotUri,
                                onRemoveImage = viewModel::clearAttachedScreenshot,
                                onRunScan = viewModel::runThreatScan,
                                onAskCopilot = { report ->
                                    viewModel.askCopilotAboutReport(report)
                                    navigateTopLevel(ROUTE_COPILOT)
                                },
                                onShowMessage = ::showToastMessage
                            )
                        }

                        composable(ROUTE_COPILOT) {
                            CopilotScreen(
                                messages = chatMessages,
                                isStreaming = isChatStreaming,
                                onSendMessage = viewModel::sendCopilotPrompt,
                                onClearSession = viewModel::clearCopilotSession,
                                onShowMessage = ::showToastMessage
                            )
                        }

                        composable(ROUTE_TOOLKIT) {
                            ToolkitScreen(
                                initialTab = toolkitInitialTab,
                                passwordInput = passwordInput,
                                passwordAnalysis = passwordAnalysis,
                                cryptoInput = cryptoInput,
                                selectedHashAlgorithm = selectedHashAlgorithm,
                                hashOutput = hashOutput,
                                base64Output = base64Output,
                                base64Error = base64Error,
                                hardeningItems = hardeningItems,
                                onPasswordChange = viewModel::updatePasswordInput,
                                onGeneratePassword = { viewModel.generateStrongPassword() },
                                onCryptoInputChange = viewModel::updateCryptoInput,
                                onSelectHashAlgorithm = viewModel::selectHashAlgorithm,
                                onEncodeBase64 = viewModel::encodeInputToBase64,
                                onDecodeBase64 = viewModel::decodeInputFromBase64,
                                onToggleHardeningItem = viewModel::toggleHardeningItem,
                                onShowMessage = ::showToastMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
