package com.gametux.console

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gametux.console.service.ConsoleService

data class RomItem(val id: String, val title: String, val system: String, val boxArt: String? = null)

class MainActivity : ComponentActivity() {
    private var consoleService: ConsoleService? = null
    private var isBound by mutableStateOf(false)
    private var discoveredDisplays = mutableStateListOf<ConsoleService.DiscoveredDisplay>()
    private var connectedDisplay by mutableStateOf<ConsoleService.DiscoveredDisplay?>(null)

    private val mockRoms = listOf(
        RomItem("1", "Super Mario Bros.", "NES"),
        RomItem("2", "The Legend of Zelda", "NES"),
        RomItem("3", "Pokemon Emerald", "GBA"),
        RomItem("4", "Metroid Fusion", "GBA"),
        RomItem("5", "Sonic Advance", "GBA")
    )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConsoleService.ConsoleBinder
            val serviceInstance = binder.getService()
            consoleService = serviceInstance
            isBound = true

            serviceInstance.startDiscovery {
                discoveredDisplays.clear()
                discoveredDisplays.addAll(serviceInstance.discoveredDisplays)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        startAndBindService()

        setContent {
            GametuxTheme {
                var showTvDialog by remember { mutableStateOf(false) }
                var showManualIpDialog by remember { mutableStateOf(false) }
                var activeRom by remember { mutableStateOf<RomItem?>(null) }

                if (activeRom == null) {
                    ConsoleDashboard(
                        connectedDisplay = connectedDisplay,
                        roms = mockRoms,
                        onTvIconClick = { showTvDialog = true },
                        onRomClick = { rom ->
                            activeRom = rom
                            consoleService?.startGame(rom.title)
                        },
                        onAddRomClick = {
                            // ROM Picker
                        }
                    )
                } else {
                    ControllerScreen(
                        rom = activeRom!!,
                        onExit = { activeRom = null }
                    )
                }

                if (showTvDialog) {
                    TvSelectionDialog(
                        displays = discoveredDisplays,
                        onDismiss = { showTvDialog = false },
                        onDisplaySelected = { display ->
                            connectedDisplay = display
                            consoleService?.connectToDisplay(display.host, display.port)
                            showTvDialog = false
                        },
                        onManualIpClick = {
                            showTvDialog = false
                            showManualIpDialog = true
                        }
                    )
                }

                if (showManualIpDialog) {
                    ManualIpDialog(
                        onDismiss = { showManualIpDialog = false },
                        onConnect = { ip, port ->
                            val display = ConsoleService.DiscoveredDisplay(ip, port)
                            connectedDisplay = display
                            consoleService?.connectToDisplay(ip, port)
                            showManualIpDialog = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun GametuxTheme(content: @Composable () -> Unit) {
        val darkColors = darkColorScheme(
            background = Color(0xFF000000), // Jet Black
            surface = Color(0xFF111111), // Subtle Grey Surface
            surfaceVariant = Color(0xFF1A1A1A), // Focus Surface
            primary = Color(0xFF00F5FF), // Electric Cyan
            secondary = Color(0xFF00D1FF), // Deeper Cyan
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.Black,
            outline = Color.White.copy(alpha = 0.1f)
        )
        MaterialTheme(colorScheme = darkColors, content = content)
    }

    @Composable
    fun ConsoleDashboard(
        connectedDisplay: ConsoleService.DiscoveredDisplay?,
        roms: List<RomItem>,
        onTvIconClick: () -> Unit,
        onRomClick: (RomItem) -> Unit,
        onAddRomClick: () -> Unit
    ) {
        val haptic = LocalHapticFeedback.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Background subtle gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 2000f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp)
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GAMETUX",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTvIconClick) {
                            Icon(
                                imageVector = if (connectedDisplay != null) Icons.Default.Cast else Icons.Default.Wifi,
                                contentDescription = "TV Connection",
                                tint = if (connectedDisplay != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectedDisplay?.host ?: "No TV Connected",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ROM CAROUSEL
                Text(
                    text = "My Games",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(end = 64.dp)
                ) {
                    items(roms) { rom ->
                        RomCard(rom = rom, onClick = { onRomClick(rom) })
                    }

                    item {
                        AddRomCard(onClick = onAddRomClick)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // BOTTOM INDICATORS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Press A to Start",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    @Composable
    fun RomCard(rom: RomItem, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "scale")
        val borderColor by animateColorAsState(
            if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "border"
        )
        val shadowAlpha by animateFloatAsState(if (isFocused) 0.6f else 0f, label = "shadow")

        Column(
            modifier = Modifier
                .width(220.dp)
                .scale(scale)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f) // Portrait box art style
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            ) {
                // Background Placeholder with Blur/Vignette effect
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                // Vignette overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                radius = 500f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = rom.title.take(1).uppercase(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.1f)
                    )
                }

                // System Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = rom.system,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = rom.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = "RETRO ADVENTURE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    @Composable
    fun AddRomCard(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "IMPORT ROM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
            }
        }
    }

    @Composable
    fun TvSelectionDialog(
        displays: List<ConsoleService.DiscoveredDisplay>,
        onDismiss: () -> Unit,
        onDisplaySelected: (ConsoleService.DiscoveredDisplay) -> Unit,
        onManualIpClick: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0F0F0F),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shadowElevation = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Connect to Display",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a Gametux display on your network",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (displays.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        displays.forEach { display ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onDisplaySelected(display) },
                                color = Color.White.copy(alpha = 0.03f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Gametux Display", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(display.host, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onManualIpClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("MANUAL IP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ControllerScreen(rom: RomItem, onExit: () -> Unit) {
        val haptic = LocalHapticFeedback.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(48.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Default.Add, contentDescription = "Exit", modifier = Modifier.scale(1.5f).background(Color.White.copy(0.1f), CircleShape), tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(rom.title, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(rom.system, color = Color.White.copy(0.5f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            // D-Pad (Left Side)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(240.dp)
            ) {
                // Cross Shape
                val buttonSize = 80.dp
                ControllerButton("U", Modifier.align(Alignment.TopCenter).size(buttonSize), haptic)
                ControllerButton("D", Modifier.align(Alignment.BottomCenter).size(buttonSize), haptic)
                ControllerButton("L", Modifier.align(Alignment.CenterStart).size(buttonSize), haptic)
                ControllerButton("R", Modifier.align(Alignment.CenterEnd).size(buttonSize), haptic)
            }

            // Action Buttons (Right Side)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ControllerButton("B", Modifier.size(96.dp), haptic, color = Color.White.copy(0.05f))
                ControllerButton("A", Modifier.size(96.dp).offset(y = (-24).dp), haptic, color = MaterialTheme.colorScheme.primary)
            }

            // Start/Select (Bottom Center)
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Text("SELECT", color = Color.White.copy(0.5f), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Text("START", color = Color.White.copy(0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    fun ControllerButton(
        label: String,
        modifier: Modifier,
        haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
        color: Color = Color.White.copy(0.05f)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "press_scale")
        val opacity by animateFloatAsState(if (isPressed) 0.6f else 1f, label = "press_opacity")

        LaunchedEffect(isPressed) {
            if (isPressed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        Box(
            modifier = modifier
                .scale(scale)
                .alpha(opacity)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = if (isPressed)
                            listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f))
                        else
                            listOf(color.copy(alpha = 0.15f), color)
                    )
                )
                .border(1.dp, Color.White.copy(0.1f), CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) {},
            contentAlignment = Alignment.Center
        ) {
            // Subtle inner shadow effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            radius = 100f
                        )
                    )
            )

            Text(
                text = label,
                color = if (label == "A" && color != Color.White.copy(0.05f)) Color.Black else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
    }

    @Composable
    fun ManualIpDialog(onDismiss: () -> Unit, onConnect: (String, Int) -> Unit) {
        var ip by remember { mutableStateOf("") }
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF0F0F0F),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                shadowElevation = 24.dp
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text("Manual IP", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Enter display's IP address manually", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        placeholder = { Text("e.g. 192.168.1.5", color = Color.White.copy(alpha = 0.2f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text("CANCEL", color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = { onConnect(ip, 9999) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("CONNECT", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }


    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun startAndBindService() {
        val intent = Intent(this, ConsoleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
