package com.gametux.console

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.gametux.console.service.ConsoleService

class MainActivity : ComponentActivity() {
    private var consoleService: ConsoleService? = null
    private var isBound by mutableStateOf(false)
    private var tvStatus by mutableStateOf("Scanning for TV...")

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ConsoleService.ConsoleBinder
            consoleService = binder.getService()
            isBound = true

            consoleService?.startDiscovery {
                tvStatus = "Connected to TV ✅"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            tvStatus = "Scanning for TV..."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        startAndBindService()

        setContent {
            GametuxTheme {
                ConsoleDashboard(
                    status = tvStatus,
                    onAddRomClick = {
                        // TODO: Implement actual ROM picker
                    }
                )
            }
        }
    }

    @Composable
    fun GametuxTheme(content: @Composable () -> Unit) {
        val darkColors = darkColorScheme(
            background = Color(0xFF0F0F13),
            surface = Color(0xFF1C1C24),
            primary = Color(0xFF8B5CF6), // Premium Purple
            secondary = Color(0xFF06B6D4), // Cyan accent
            onBackground = Color.White,
            onSurface = Color.White
        )
        MaterialTheme(colorScheme = darkColors, content = content)
    }

    @Composable
    fun ConsoleDashboard(status: String, onAddRomClick: () -> Unit) {
        val haptic = LocalHapticFeedback.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B0B0E),
                            Color(0xFF161622),
                            Color(0xFF0B0B0E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // HEADER SECTION
                Column {
                    Text(
                        text = "Gametux Console",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "Wireless Retro Console",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // CONNECTION STATUS CARD
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPulseIndicator(isConnected = status.contains("Connected"))
                        Spacer(modifier = Modifier.width(20.dp))
                        Text(
                            text = status,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // RECENT GAMES (UI ONLY)
                Text(
                    text = "Recent Games",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(20.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(110.dp, 150.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ROM",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // PRIMARY ACTION BUTTON
                PremiumButton(
                    text = "Add ROM",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddRomClick()
                    }
                )

                Spacer(modifier = Modifier.height(56.dp))
            }
        }
    }

    @Composable
    fun StatusPulseIndicator(isConnected: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        val color = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .scale(if (!isConnected) 1.5f - (alpha * 0.5f) else 1f)
                    .background(color.copy(alpha = alpha * 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(color, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
    }

    @Composable
    fun PremiumButton(text: String, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .scale(scale)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF6366F1)
                        )
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
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
