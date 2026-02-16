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
            background = Color(0xFF000000), // Pure Black
            surface = Color(0xFF121212), // Very Dark Grey
            primary = Color(0xFF00F5FF), // Electric Cyan
            secondary = Color(0xFFFFFFFF), // White
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.Black
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
                    .systemBarsPadding()
                    .padding(horizontal = 32.dp)
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
        val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "scale")
        val borderColor by animateColorAsState(
            if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
            label = "border"
        )

        Column(
            modifier = Modifier
                .width(180.dp)
                .scale(scale)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                )

                // Placeholder for Title in the box
                Text(
                    text = rom.title.take(1),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.05f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = rom.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1
            )
            Text(
                text = rom.system,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }

    @Composable
    fun AddRomCard(onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .width(180.dp)
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add ROM",
                    fontSize = 14.sp,
                    color = Color.White
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
                    .fillMaxWidth(0.8f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF121212),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Select Display",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (displays.isEmpty()) {
                        Text(
                            text = "Searching for Gametux displays...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        displays.forEach { display ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onDisplaySelected(display) }
                                    .padding(vertical = 8.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Gametux TV", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(display.host, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = onManualIpClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("MANUAL IP", color = MaterialTheme.colorScheme.primary)
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
                .padding(32.dp)
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
                    .size(200.dp)
            ) {
                // Cross Shape
                val buttonSize = 64.dp
                ControllerButton("UP", Modifier.align(Alignment.TopCenter).size(buttonSize), haptic)
                ControllerButton("DOWN", Modifier.align(Alignment.BottomCenter).size(buttonSize), haptic)
                ControllerButton("LEFT", Modifier.align(Alignment.CenterStart).size(buttonSize), haptic)
                ControllerButton("RIGHT", Modifier.align(Alignment.CenterEnd).size(buttonSize), haptic)
            }

            // Action Buttons (Right Side)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ControllerButton("B", Modifier.size(80.dp), haptic, color = Color.White.copy(0.1f))
                ControllerButton("A", Modifier.size(80.dp).offset(y = (-20).dp), haptic, color = MaterialTheme.colorScheme.primary)
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
        color: Color = Color.White.copy(0.1f)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (isPressed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(if (isPressed) color.copy(alpha = 0.4f) else color)
                .border(1.dp, Color.White.copy(0.1f), CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) {},
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
    }

    @Composable
    fun ManualIpDialog(onDismiss: () -> Unit, onConnect: (String, Int) -> Unit) {
        var ip by remember { mutableStateOf("") }
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF121212)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Manual Connection", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = ip,
                        onValueChange = { ip = it },
                        placeholder = { Text("192.168.1.XX") },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White.copy(alpha = 0.6f)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onConnect(ip, 9999) }, // Default signaling port
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("CONNECT", color = Color.Black)
                        }
                    }
                }
            }
        }
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
