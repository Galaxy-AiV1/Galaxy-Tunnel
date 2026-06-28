package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.result.contract.ActivityResultContracts
import android.net.VpnService
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.toggleVpn()
        } else {
            Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            
            // Custom MaterialTheme to apply the dynamic dark/light slate theme directly
            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) Color(0xFF14171B) else Color(0xFFF9F9FB)
                ) {
                    GalaxyTunnelApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyTunnelApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentScreen by viewModel.currentScreen.collectAsState()
    val language by viewModel.language.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val textSize by viewModel.textSize.collectAsState()

    // Determine font scaling multipliers
    val fontScale = when (textSize) {
        "small" -> 0.85f
        "medium" -> 1.0f
        "large" -> 1.2f
        else -> 1.0f
    }

    val serverTitle = Translation.get("headerTitle", language)

    // Palette tokens (Silver Matte slate dark colors and blended card borders)
    val bgColor = if (isDark) Color(0xFF14171B) else Color(0xFFF9F9FB)
    val cardColor = if (isDark) Color(0xFF1A1D22) else Color(0xFFF0F1F5)
    val borderColor = if (isDark) Color(0x0AFFFFFF) else Color(0x11000000)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)
    val textSecondary = if (isDark) Color(0xFFA3A39E) else Color(0xFF555555)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                color = cardColor,
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .border(1.dp, borderColor, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardColor)
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        GalaxyTunnelBrandText(fontSize = 18f * fontScale, isDark = isDark)
                    }
                    HorizontalDivider(color = borderColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    DrawerItem(
                        label = Translation.get("navServers", language),
                        icon = Icons.Default.Dns,
                        isActive = currentScreen == "servers",
                        isDark = isDark,
                        onClick = {
                            viewModel.setCurrentScreen("servers")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        label = Translation.get("navSettings", language),
                        icon = Icons.Default.Settings,
                        isActive = currentScreen == "settings",
                        isDark = isDark,
                        onClick = {
                            viewModel.setCurrentScreen("settings")
                            scope.launch { drawerState.close() }
                        }
                    )

                    DrawerItem(
                        label = Translation.get("navContact", language),
                        icon = Icons.Default.Info,
                        isActive = currentScreen == "contact",
                        isDark = isDark,
                        onClick = {
                            viewModel.setCurrentScreen("contact")
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = bgColor,
                        titleContentColor = textPrimary
                    ),
                    modifier = Modifier.border(1.dp, borderColor),
                    title = {
                        GalaxyTunnelBrandText(fontSize = 16f * fontScale, isDark = isDark)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_drawer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sidebar Menu",
                                tint = textPrimary
                            )
                        }
                    },
                    actions = {
                        // Dynamic Language Toggle Row (EN / MY)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            LanguageButton(
                                label = "MY",
                                isActive = language == "my",
                                isDark = isDark,
                                onClick = { viewModel.setLanguage("my") }
                            )
                            LanguageButton(
                                label = "EN",
                                isActive = language == "en",
                                isDark = isDark,
                                onClick = { viewModel.setLanguage("en") }
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    "servers" -> ServersScreen(viewModel, fontScale, cardColor, borderColor, textPrimary, textSecondary, isDark)
                    "settings" -> SettingsScreen(viewModel, fontScale, cardColor, borderColor, textPrimary, textSecondary)
                    "contact" -> ContactScreen(viewModel, fontScale, cardColor, borderColor, textPrimary, textSecondary)
                }
            }
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val highlightColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val textSecondary = if (isDark) Color(0xFFA3A39E) else Color(0xFF666666)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isActive) (if (isDark) Color(0x1F2A2A28) else Color(0x0F000000)) else Color.Transparent)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(highlightColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) textPrimary else textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) textPrimary else textSecondary
        )
    }
}

@Composable
fun LanguageButton(
    label: String,
    isActive: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val activeBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val activeText = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val inactiveBg = if (isDark) Color(0xFF24272C) else Color(0xFFF5F5F5)
    val inactiveText = if (isDark) Color(0xFFA3A39E) else Color(0xFF666666)
    val borderColor = if (isDark) Color(0x0CFFFFFF) else Color(0xFFE5E5E5)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) activeBg else inactiveBg)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) activeText else inactiveText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ServersScreen(
    viewModel: MainViewModel,
    fontScale: Float,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    val selectedIndex by viewModel.selectedServerIndex.collectAsState()
    val servers by viewModel.servers.collectAsState()

    val duration by viewModel.durationSeconds.collectAsState()
    val dlSpeed by viewModel.dlSpeedMb.collectAsState()
    val ulSpeed by viewModel.ulSpeedMb.collectAsState()

    val activeNode = if (servers.isNotEmpty()) servers[selectedIndex] else null
    var isListExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GalaxyTunnelBrandText(fontSize = 24f * fontScale, isDark = isDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translation.get("serversSubtitle", language),
                    fontSize = (12 * fontScale).sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // VPN Control Dashboard Engine
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Translation.get("engineTitle", language).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Sliding Switch/Toggle Control capsule
                    ConnectionToggle(
                        vpnState = vpnState,
                        onToggle = {
                            val activity = context as? MainActivity
                            if (activity != null) {
                                val prepareIntent = VpnService.prepare(activity)
                                if (prepareIntent != null) {
                                    activity.vpnPrepareLauncher.launch(prepareIntent)
                                } else {
                                    viewModel.toggleVpn()
                                }
                            } else {
                                viewModel.toggleVpn()
                            }
                        },
                        isDark = isDark,
                        borderColor = borderColor,
                        textSecondary = textSecondary
                    )

                    // Selected server info
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = Translation.get("selectedServer", language).uppercase(),
                        fontSize = 9.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeNode?.name ?: "No Node Selected",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Connected dynamic telemetry HUD metrics
                    AnimatedVisibility(
                        visible = vpnState == "connected",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = borderColor)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // Duration timer
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = Translation.get("duration", language).uppercase(),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = formatDuration(duration),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Download
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Download Speed",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "DL",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Text(
                                        text = dlSpeed,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Upload
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Upload Speed",
                                            tint = Color(0xFF06B6D4),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "UL",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF06B6D4)
                                        )
                                    }
                                    Text(
                                        text = ulSpeed,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section header with collapsible Accordion Card "Tag"
        item {
            var showImportDialog by remember { mutableStateOf(false) }
            var importText by remember { mutableStateOf("") }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The Collapsible Accordion Card "Tag"
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { isListExpanded = !isListExpanded },
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isListExpanded) Icons.Default.ExpandMore else Icons.Default.NavigateNext,
                            contentDescription = "Toggle List",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translation.get("serverToggleLabel", language),
                            fontSize = (12 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "(${servers.size})",
                            fontSize = (11 * fontScale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textSecondary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(
                            onClick = { 
                                viewModel.triggerAllPings() 
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // "Import Config" Dialog launcher Button
                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cardColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = "Import Config",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Dialog for importing user configurations
            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    containerColor = cardColor,
                    title = {
                        Text(
                            text = Translation.get("importTitle", language),
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = importText,
                                onValueChange = { importText = it },
                                placeholder = {
                                    Text(
                                        text = Translation.get("importPlaceholder", language),
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 12.sp,
                                    color = textPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = textPrimary,
                                    unfocusedBorderColor = borderColor,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (importText.isNotEmpty()) {
                                    val success = viewModel.importConfig(importText)
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            Translation.get("importSuccess", language),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showImportDialog = false
                                        importText = ""
                                        isListExpanded = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            Translation.get("importError", language),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = Translation.get("importBtn", language),
                                color = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text(
                                text = Translation.get("cancel", language),
                                color = textSecondary
                            )
                        }
                    }
                )
            }
        }

        // Expanded Server items
        if (isListExpanded) {
            itemsIndexed(servers) { idx, node ->
                val isActive = idx == selectedIndex
                val cardBorderColor = if (isActive) textPrimary.copy(alpha = 0.4f) else borderColor

                var animateEntry by remember { mutableStateOf(false) }
                LaunchedEffect(isListExpanded) {
                    animateEntry = true
                }

                AnimatedVisibility(
                    visible = animateEntry,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = idx * 25)) +
                            slideInVertically(initialOffsetY = { 25 }, animationSpec = tween(durationMillis = 300, delayMillis = idx * 25)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 150))
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .border(
                                1.dp,
                                cardBorderColor,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.selectServerIndex(idx) },
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) textPrimary else Color.Transparent)
                                        .border(1.2.dp, if (isActive) textPrimary else textSecondary, CircleShape)
                                )

                                Text(
                                    text = node.name,
                                    fontSize = (13 * fontScale).sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            node.isChecking -> Color.Transparent
                                            node.isOffline -> Color.Transparent
                                            else -> Color(0x2210B981)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            node.isChecking -> borderColor
                                            node.isOffline -> Color(0xFFDC2626)
                                            else -> Color(0xFF10B981)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (node.isChecking) {
                                        CircularProgressIndicator(
                                            color = textSecondary,
                                            strokeWidth = 1.5.dp,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = Translation.get("checking", language),
                                            fontSize = 9.sp,
                                            color = textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else if (node.isOffline) {
                                        Text(
                                            text = Translation.get("offline", language).uppercase(),
                                            fontSize = 9.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "${node.latencyMs ?: 0} ms",
                                            fontSize = 9.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    fontScale: Float,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val textSize by viewModel.textSize.collectAsState()
    val logs by viewModel.connectionLogs.collectAsState()

    var isLogsExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Translation.get("settingsTitle", language),
                fontSize = (24 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Translation.get("settingsSubtitle", language),
                fontSize = (12 * fontScale).sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Card 1: Preferences (Theme, Font Size)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Palette",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Translation.get("themeLabel", language),
                            fontSize = (14 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val selectLight = !isDark
                        val lightBg = if (selectLight) textPrimary else (if (isDark) Color(0xFF1E1E1C) else Color(0xFFE5E5E5))
                        val lightText = if (selectLight) (if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)) else textPrimary

                        Button(
                            onClick = { viewModel.setDarkMode(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = lightBg),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = "Light Theme icon",
                                tint = lightText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Translation.get("lightMode", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = lightText
                            )
                        }

                        val darkBg = if (isDark) textPrimary else (if (isDark) Color(0xFF24272C) else Color(0xFFE5E5E5))
                        val darkText = if (isDark) (if (isDark) Color(0xFF14171B) else Color(0xFFFFFFFF)) else textPrimary

                        Button(
                            onClick = { viewModel.setDarkMode(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = darkBg),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = "Dark Theme icon",
                                tint = darkText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = Translation.get("darkMode", language),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = darkText
                            )
                        }
                    }
                }

                HorizontalDivider(color = borderColor)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Format Text Size",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = Translation.get("textSizeLabel", language),
                            fontSize = (14 * fontScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("small", "medium", "large").forEach { size ->
                            val isSel = textSize == size
                            val szLabel = when (size) {
                                "small" -> Translation.get("textSmall", language)
                                "medium" -> Translation.get("textMedium", language)
                                else -> Translation.get("textLarge", language)
                            }
                            val szBg = if (isSel) textPrimary else (if (isDark) Color(0xFF1E1E1C) else Color(0xFFE5E5E5))
                            val szText = if (isSel) (if (isDark) Color(0xFF14171B) else Color(0xFFFFFFFF)) else textPrimary

                            Button(
                                onClick = { viewModel.setTextSize(size) },
                                colors = ButtonDefaults.buttonColors(containerColor = szBg),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(
                                    text = szLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = szText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: App Sharing & Local APK Distribution
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = Translation.get("directShareApk", language),
                        fontSize = (13 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Text(
                    text = if (language == "my") "သင့်မိတ်ဆွေများထံ VPN application အား လွယ်ကူစွာ တိုက်ရိုက်မျှဝေပေးပို့နိုင်ပါသည်။" else "Perfect for sharing your private, high-speed tunnel with family and nearby friends.",
                    fontSize = (11 * fontScale).sp,
                    color = textSecondary
                )

                Button(
                    onClick = {
                        val shareText = "${Translation.get("shareMessage", language)} https://galaxy-tunnel.pages.dev"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Galaxy Tunnel"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2A2B30) else Color(0xFFE5E5E5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "QR Share Icon",
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translation.get("directShareApk", language),
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Card 3: Expandable System Diagnostics & Session Logs
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { isLogsExpanded = !isLogsExpanded },
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLogsExpanded) Icons.Default.ExpandMore else Icons.Default.NavigateNext,
                        contentDescription = "Toggle Logs",
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Translation.get("diagnosticsLabel", language),
                        fontSize = (13 * fontScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "[${logs.size}]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(
                    visible = isLogsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable(enabled = false) { }
                    ) {
                        Surface(
                            color = if (isDark) Color(0xFF0F1115) else Color(0xFFF9F9FA),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                if (logs.isEmpty()) {
                                    Text(
                                        text = "Empty logs session.",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = textSecondary,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                } else {
                                    val terminalScrollState = rememberScrollState()
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(terminalScrollState)
                                    ) {
                                        logs.reversed().forEach { logLine ->
                                            Text(
                                                text = logLine,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (logLine.contains("failed") || logLine.contains("Error")) Color(0xFFF87171) else (if (logLine.contains("success") || logLine.contains("established")) Color(0xFF34D399) else textSecondary),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.clearLogs() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2A2B30) else Color(0xFFE5E5E5)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Clear logs icon",
                                    tint = textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Translation.get("clearLogsBtn", language),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    val compiled = logs.joinToString("\n")
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, compiled)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Galaxy Tunnel Log"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF2A2B30) else Color(0xFFE5E5E5)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy logs icon",
                                    tint = textPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Translation.get("shareLogsBtn", language),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactScreen(
    viewModel: MainViewModel,
    fontScale: Float,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Translation.get("contactTitle", language),
                fontSize = (24 * fontScale).sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Translation.get("contactSubtitle", language),
                fontSize = (12 * fontScale).sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )
        }

        ContactRowItem(
            label = "Telegram",
            value = "@Swnt7771",
            icon = Icons.Default.Send,
            cardColor = cardColor,
            borderColor = borderColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Swnt7771"))
                context.startActivity(intent)
            }
        )

        ContactRowItem(
            label = "Facebook",
            value = "Sal W Tun",
            icon = Icons.Default.Share,
            cardColor = cardColor,
            borderColor = borderColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/share/14bd1ThErYb/"))
                context.startActivity(intent)
            }
        )

        ContactRowItem(
            label = Translation.get("phoneLabel", language),
            value = "09674688300",
            icon = Icons.Default.Phone,
            cardColor = cardColor,
            borderColor = borderColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09674688300"))
                context.startActivity(intent)
            }
        )

        ContactRowItem(
            label = Translation.get("addressLabel", language),
            value = Translation.get("addressValue", language),
            icon = Icons.Default.Place,
            cardColor = cardColor,
            borderColor = borderColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = {}
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:16.4839,98.6186?q=Thin+Gun+Nyi+Naung,+Myawaddy"))
                mapIntent.setPackage("com.google.android.apps.maps")
                try {
                    context.startActivity(mapIntent)
                } catch (e: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=Thin+Gun+Nyi+Naung,+Myawaddy"))
                    context.startActivity(browserIntent)
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = textPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Map view direction launcher",
                tint = if (isDark(textPrimary)) Color.White else Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = Translation.get("mapBtn", language),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark(textPrimary)) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun ContactRowItem(
    label: String,
    value: String,
    icon: ImageVector,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x0CFFFFFF))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format("%02d:%02d:%02d", h, m, s)
}

private fun isDark(color: Color): Boolean {
    return (color.red * 0.299 + color.green * 0.587 + color.blue * 0.114) < 0.5
}

@Composable
fun GalaxyTunnelBrandText(fontSize: Float = 18f, isDark: Boolean = true) {
    val primaryColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF090909)
    val accentColor = Color(0xFF00E5FF)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GALAXY ",
            color = primaryColor,
            fontSize = fontSize.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Text(
            text = "TUNNEL",
            color = accentColor,
            fontSize = fontSize.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun ConnectionToggle(
    vpnState: String,
    onToggle: () -> Unit,
    isDark: Boolean,
    borderColor: Color,
    textSecondary: Color
) {
    val transition = updateTransition(targetState = vpnState, label = "ToggleState")
    val isConnected = vpnState == "connected"
    val isConnecting = vpnState == "connecting"

    val alignmentProgress by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "alignment"
    ) { state ->
        when (state) {
            "connected" -> 1f
            "connecting" -> 0.5f
            else -> 0f
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF14161B) else Color(0xFFECECEC)
        ),
        modifier = Modifier
            .width(140.dp)
            .height(56.dp)
            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
            .clickable { onToggle() }
            .testTag("vpn_connect_button"),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OFF",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textSecondary.copy(alpha = if (!isConnected) 1.0f else 0.3f),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "ON",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981).copy(alpha = if (isConnected) 1.0f else 0.3f),
                    letterSpacing = 0.5.sp
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxOffset = maxWidth - 48.dp
                val thumbOffset = maxOffset * alignmentProgress

                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (vpnState) {
                                "connected" -> Color(0xFF10B981)
                                "connecting" -> Color(0xFFD97706)
                                else -> if (isDark) Color(0xFF2E3238) else Color(0xFFC5C5C2)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color(0x33FFFFFF) else Color(0x33000000),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                            contentDescription = "Status Thumb",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()
