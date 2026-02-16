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
import androidx.compose.material.icons.filled.Close
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
            background = Color(0xFF0B0B0F), // Deep Black
            surface = Color(0xFF15151D), // Card Background
            surfaceVariant = Color(0xFF1E1E28), // Lighter surface for variants
            primary = Color(0xFF00E5FF), // Electric Cyan Accent
            secondary = Color(0xFF9AA0A6), // Soft Gray
            onBackground = Color.White,
            onSurface = Color.White,
            onPrimary = Color.Black,
            outline = Color.White.copy(alpha = 0.05f)
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
            // Background console-style gradient/glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0F0F15),
                                Color(0xFF0B0B0F)
                            )
                        )
                    )
            )

            // Subtle accent glow in the corner
            Box(
                modifier = Modifier
                    .size(600.dp)
                    .offset(x = (-200).dp, y = (-200).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp)
            ) {
                // TOP BAR (Blended/Transparent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = {}) // Placeholder for menu
                    ) {
                        Text(
                            text = "GAMETUX",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Connection Status
                    Surface(
                        onClick = onTvIconClick,
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (connectedDisplay != null) Icons.Default.Cast else Icons.Default.Wifi,
                                contentDescription = "TV Connection",
                                tint = if (connectedDisplay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = connectedDisplay?.host ?: "Searching Display...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (connectedDisplay != null) Color.White else MaterialTheme.colorScheme.secondary
                            )
                        }
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

        // Premium Animations
        val scale by animateFloatAsState(
            targetValue = if (isFocused) 1.08f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "scale"
        )
        val glowAlpha by animateFloatAsState(if (isFocused) 0.5f else 0f, label = "glow")
        val borderColor by animateColorAsState(
            if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
            label = "border"
        )

        Column(
            modifier = Modifier
                .width(200.dp)
                .padding(vertical = 12.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f) // Premium portrait ratio
                    .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
                    // Soft Outer Glow on Focus
                    .then(
                        if (isFocused) Modifier.border(
                            4.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            RoundedCornerShape(20.dp)
                        ) else Modifier
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
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
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = rom.system,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    @Composable
    fun AddRomCard(onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "scale")

        Box(
            modifier = Modifier
                .width(200.dp)
                .padding(vertical = 12.dp)
                .scale(scale)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.5.dp, if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
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
                    .fillMaxWidth(0.65f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF15151D).copy(alpha = 0.95f), // Glass-like
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shadowElevation = 32.dp
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "SCANNING LAN...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
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
                .background(Color(0xFF0B0B0F))
                .padding(56.dp)
        ) {
            // Header (Immersive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = rom.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = rom.system,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                // Connection Indicator
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // D-Pad (Refined Tactile Design)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(260.dp)
            ) {
                val buttonSize = 84.dp
                ControllerButton("U", Modifier.align(Alignment.TopCenter).size(buttonSize), haptic)
                ControllerButton("D", Modifier.align(Alignment.BottomCenter).size(buttonSize), haptic)
                ControllerButton("L", Modifier.align(Alignment.CenterStart).size(buttonSize), haptic)
                ControllerButton("R", Modifier.align(Alignment.CenterEnd).size(buttonSize), haptic)
            }

            // Action Buttons (Balanced Premium Design)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(260.dp)
            ) {
                val buttonSize = 100.dp
                ControllerButton(
                    label = "B",
                    modifier = Modifier.align(Alignment.CenterStart).size(buttonSize),
                    haptic = haptic,
                    color = Color.White.copy(alpha = 0.04f)
                )
                ControllerButton(
                    label = "A",
                    modifier = Modifier.align(Alignment.TopEnd).size(buttonSize).offset(y = 20.dp),
                    haptic = haptic,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Start/Select (Minimal Console Style)
            Row(
                modifier = Modifier.align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp, 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SELECT", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp, 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("START", color = Color.White.copy(alpha = 0.3f), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
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
                modifier = Modifier.fillMaxWidth(0.55f),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF15151D).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shadowElevation = 32.dp
            ) {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text("Manual Connection", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("Enter display IP to connect directly", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)

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
