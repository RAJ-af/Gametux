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
                            Color(0xFF0F0F13),
                            Color(0xFF161620),
                            Color(0xFF0F0F13)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                // HEADER
                HeaderSection()

                Spacer(modifier = Modifier.height(32.dp))

                // CONNECTION STATUS
                ConnectionStatusCard(status)

                Spacer(modifier = Modifier.height(40.dp))

                // RECENT GAMES PLACEHOLDER
                RecentGamesSection()

                Spacer(modifier = Modifier.weight(1f))

                // PRIMARY ACTION
                PremiumButton(
                    text = "Add ROM",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddRomClick()
                    }
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    @Composable
    fun HeaderSection() {
        Column {
            Text(
                text = "Gametux Console",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                text = "Wireless Retro Console",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }

    @Composable
    fun ConnectionStatusCard(status: String) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(isConnected = status.contains("Connected"))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = status,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }

    @Composable
    fun StatusIndicator(isConnected: Boolean) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

        val color = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(if (!isConnected) 1f + (1f - alpha) else 1f)
                    .background(color.copy(alpha = alpha * 0.4f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
        }
    }

    @Composable
    fun RecentGamesSection() {
        Column {
            Text(
                text = "Recent Games",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .size(100.dp, 140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No ROM",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PremiumButton(text: String, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.96f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF6366F1) // Indigo accent
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
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
