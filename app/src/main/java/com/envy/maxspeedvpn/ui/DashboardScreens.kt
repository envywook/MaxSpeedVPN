package com.envy.maxspeedvpn.ui

import android.content.Intent
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Uri
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.envy.maxspeedvpn.R
import com.envy.maxspeedvpn.server.ServerLatencyResult
import com.envy.maxspeedvpn.speed.NetworkSpeedTester
import com.envy.maxspeedvpn.speed.SpeedTestPhase
import com.envy.maxspeedvpn.speed.SpeedTestSnapshot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.envy.maxspeedvpn.BuildConfig
import com.envy.maxspeedvpn.core.EngineKind
import com.envy.maxspeedvpn.core.VpnSessionState
import com.envy.maxspeedvpn.core.VpnTrafficSnapshot
import com.envy.maxspeedvpn.core.VpnTrafficStore
import com.envy.maxspeedvpn.subscription.ServerProfile
import com.envy.maxspeedvpn.subscription.Subscription
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.Locale
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Bg = Color(0xFF080A09)
private val Panel = Color(0xFF151817)
private val PanelHigh = Color(0xFF101211)
private val Mint = Color(0xFFA6F3D1)
private val TextMain = Color(0xFFF4F6F5)
private val TextMuted = Color(0xFF9EA5A1)
private val Border = Color(0xFF343936)
private val Danger = Color(0xFFFF6B78)

internal data class DashboardStrings(
    val subscriptions: String,
    val speed: String,
    val home: String,
    val settings: String,
    val download: String,
    val upload: String,
    val notConnected: String,
    val connected: String,
    val connecting: String,
    val disconnecting: String,
    val connectHint: String,
    val disconnectHint: String,
    val selectServer: String,
    val fastestServer: String,
    val noServers: String,
    val speedTitle: String,
    val speedSubtitle: String,
    val servers: String,
    val baseServers: String,
    val plusServers: String,
    val otherServers: String,
    val manageSubscriptions: String,
    val telegramNews: String,
    val total: String,
    val duration: String,
    val engine: String,
    val protocol: String,
    val endpoint: String,
    val ping: String,
)

@Composable
internal fun dashboardStrings() = DashboardStrings(
    subscriptions = stringResource(com.envy.maxspeedvpn.R.string.nav_subscriptions),
    speed = stringResource(com.envy.maxspeedvpn.R.string.nav_speed),
    home = stringResource(com.envy.maxspeedvpn.R.string.nav_home),
    settings = stringResource(com.envy.maxspeedvpn.R.string.nav_settings),
    download = stringResource(com.envy.maxspeedvpn.R.string.metric_download),
    upload = stringResource(com.envy.maxspeedvpn.R.string.metric_upload),
    notConnected = stringResource(com.envy.maxspeedvpn.R.string.connection_not_connected),
    connected = stringResource(com.envy.maxspeedvpn.R.string.connection_connected),
    connecting = stringResource(com.envy.maxspeedvpn.R.string.connection_connecting),
    disconnecting = stringResource(com.envy.maxspeedvpn.R.string.connection_disconnecting),
    connectHint = stringResource(com.envy.maxspeedvpn.R.string.connection_tap_connect),
    disconnectHint = stringResource(com.envy.maxspeedvpn.R.string.connection_tap_disconnect),
    selectServer = stringResource(com.envy.maxspeedvpn.R.string.server_select),
    fastestServer = stringResource(com.envy.maxspeedvpn.R.string.server_optimal),
    noServers = stringResource(com.envy.maxspeedvpn.R.string.server_empty),
    speedTitle = stringResource(com.envy.maxspeedvpn.R.string.speed_title),
    speedSubtitle = stringResource(com.envy.maxspeedvpn.R.string.speed_current_download),
    servers = stringResource(com.envy.maxspeedvpn.R.string.servers_title),
    baseServers = stringResource(com.envy.maxspeedvpn.R.string.servers_base),
    plusServers = stringResource(com.envy.maxspeedvpn.R.string.servers_plus),
    otherServers = stringResource(com.envy.maxspeedvpn.R.string.servers_other),
    manageSubscriptions = stringResource(com.envy.maxspeedvpn.R.string.subscriptions_manage),
    telegramNews = stringResource(com.envy.maxspeedvpn.R.string.telegram_news),
    total = stringResource(com.envy.maxspeedvpn.R.string.session_total),
    duration = stringResource(com.envy.maxspeedvpn.R.string.session_duration),
    engine = stringResource(com.envy.maxspeedvpn.R.string.session_engine),
    protocol = stringResource(com.envy.maxspeedvpn.R.string.session_protocol),
    endpoint = stringResource(com.envy.maxspeedvpn.R.string.session_endpoint),
    ping = stringResource(com.envy.maxspeedvpn.R.string.session_ping),
)

@Composable
internal fun DashboardHeader(onAdd: (() -> Unit)? = null, addEnabled: Boolean = true) {
    val context = LocalContext.current
    val strings = dashboardStrings()
    val telegramUri = remember {
        BuildConfig.MAXSPEED_TELEGRAM_URL.takeIf(String::isNotBlank)?.let(Uri::parse)
            ?.takeIf { it.scheme.equals("https", ignoreCase = true) && it.host.equals("t.me", ignoreCase = true) }
    }
    val telegramIntent = telegramUri?.let { Intent(Intent.ACTION_VIEW, it) }
    val telegramEnabled = telegramIntent?.resolveActivity(context.packageManager) != null
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("MaxSpeedVPN", color = TextMain, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (onAdd != null) {
            IconButton(
                enabled = addEnabled,
                onClick = onAdd,
                modifier = Modifier.semantics { contentDescription = strings.manageSubscriptions },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = if (addEnabled) Mint else TextMuted,
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            IconButton(
                enabled = telegramEnabled,
                onClick = { telegramIntent?.let(context::startActivity) },
                modifier = Modifier.semantics { contentDescription = strings.telegramNews },
            ) { TelegramMark(Modifier.size(26.dp), if (telegramEnabled) TextMain else TextMuted) }
        }
    }
}

@Composable
internal fun HomeDashboard(
    state: VpnSessionState,
    selected: ServerProfile?,
    servers: List<ServerProfile>,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onSelect: (ServerProfile) -> Unit,
    latencyResults: Map<String, ServerLatencyResult>,
    latencyTesting: Boolean,
    latencyTestingIds: Set<String>,
    onTestLatency: () -> Unit,
    onTestServerLatency: (ServerProfile) -> Unit,
    onManageSubscriptions: () -> Unit,
) {
    val strings = dashboardStrings()
    val connected = state is VpnSessionState.Connected
    val busy = state is VpnSessionState.Connecting || state is VpnSessionState.Disconnecting
    val rates by VpnTrafficStore.state.collectAsState()
    var speedSnapshot by remember { mutableStateOf(SpeedTestSnapshot()) }
    var speedError by remember { mutableStateOf<String?>(null) }
    var speedConfirm by remember { mutableStateOf(false) }
    val speedScope = rememberCoroutineScope()
    val speedContext = LocalContext.current
    val speedRunning = speedSnapshot.phase == SpeedTestPhase.DOWNLOAD || speedSnapshot.phase == SpeedTestPhase.UPLOAD
    val startSpeedTest = {
        speedConfirm = false
        speedError = null
        if (state !is VpnSessionState.Connected) {
            speedError = "Сначала подключите VPN"
        } else {
            val socksProxy = Proxy(
                Proxy.Type.SOCKS,
                InetSocketAddress("127.0.0.1", 10808),
            )
            speedScope.launch {
                runCatching { NetworkSpeedTester(proxy = socksProxy).run { value -> speedScope.launch { speedSnapshot = value } } }
                    .onSuccess {
                        delay(120_000)
                        speedSnapshot = SpeedTestSnapshot()
                    }
                    .onFailure { speedError = it.message ?: it.javaClass.simpleName; speedSnapshot = SpeedTestSnapshot() }
            }
        }
        Unit
    }
    Column(Modifier.fillMaxSize().background(Bg)) {
        DashboardHeader()
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard(
                        label = strings.download,
                        bytesPerSecond = rates.downloadBytesPerSecond,
                        modifier = Modifier.weight(1f),
                        testSnapshot = speedSnapshot,
                        testError = speedError,
                        enabled = !speedRunning,
                        onClick = { speedConfirm = true },
                    )
                    MetricCard(
                        label = strings.upload,
                        bytesPerSecond = rates.uploadBytesPerSecond,
                        modifier = Modifier.weight(1f),
                        testSnapshot = speedSnapshot,
                        testError = speedError,
                        enabled = !speedRunning,
                        onClick = { speedConfirm = true },
                    )
                }
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ConnectionControl(
                        state = state,
                        enabled = selected != null || connected || busy,
                        onClick = { if (connected || busy) onDisconnect() else selected?.let { onConnect(it.config) } },
                    )
                }
            }
            item { SectionTitle(strings.servers, latencyTesting, onTestLatency) }
            item {
                if (servers.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onManageSubscriptions),
                        shape = RoundedCornerShape(18.dp), color = Panel, border = BorderStroke(1.dp, Mint.copy(alpha = .62f)),
                    ) { Text(strings.noServers, color = Mint, modifier = Modifier.padding(18.dp), fontWeight = FontWeight.SemiBold) }
                } else {
                    ServerListCard(servers, selected, latencyResults, latencyTestingIds, onSelect, onTestServerLatency, false)
                }
            }
        }
    }
    if (speedConfirm) {
        val metered = speedContext.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true
        AlertDialog(
            onDismissRequest = { speedConfirm = false },
            title = { Text(stringResource(R.string.speed_test_confirm_title)) },
            text = { Text(stringResource(R.string.speed_test_confirm_body, if (metered) stringResource(R.string.speed_test_metered_note) else "")) },
            confirmButton = { TextButton(onClick = startSpeedTest) { Text(stringResource(R.string.speed_test_start)) } },
            dismissButton = { TextButton(onClick = { speedConfirm = false }) { Text(stringResource(R.string.speed_test_cancel)) } },
        )
    }
}

@Composable
private fun SessionDetails(
    state: VpnSessionState,
    sessionProfile: ServerProfile?,
    rates: VpnTrafficSnapshot,
    latency: ServerLatencyResult?,
) {
    if (state !is VpnSessionState.Connected) return
    val sessionServer = state.server
    val protocol = sessionServer?.protocol ?: sessionProfile?.protocol ?: "—"
    val endpoint = sessionServer?.let { safeEndpointLabel(it.address, it.port) }
        ?: sessionProfile?.let { safeEndpointLabel(it.address, it.port) }
        ?: "—"
    val strings = dashboardStrings()
    var now by remember(state.startedAtElapsedRealtimeMillis) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.startedAtElapsedRealtimeMillis) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val total = rates.downloadedBytes + rates.uploadedBytes
    val layout = sessionDetailsLayout(
        total = formatBytes(total),
        duration = formatSessionDuration(now - state.startedAtElapsedRealtimeMillis),
        ping = latency?.latencyMillis?.let { "${it}мс" } ?: "—",
        engine = engineLabel(state.engine),
        protocol = protocol.uppercase(Locale.ROOT),
        endpoint = endpoint,
        totalLabel = strings.total,
        durationLabel = strings.duration,
        pingLabel = strings.ping,
        engineLabel = strings.engine,
        protocolLabel = strings.protocol,
        endpointLabel = strings.endpoint,
    )
    Card(
        modifier = Modifier.fillMaxWidth().height(layout.cardHeightDp.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = PanelHigh),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                layout.items.take(3).forEach { item -> SessionDatum(item.label, item.value) }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                layout.items.drop(3).forEach { item ->
                    SessionDatum(item.label, item.value, Modifier.weight(item.weight))
                }
            }
        }
    }
}

@Composable
private fun SessionDatum(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = TextMuted, fontSize = 10.sp, maxLines = 1)
        Text(
            value,
            color = TextMain,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun engineLabel(engine: EngineKind): String = when (engine) {
    EngineKind.XRAY -> "Xray"
    EngineKind.SING_BOX -> "sing-box"
}

private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = safe.toDouble()
    var index = 0
    while (value >= 1_000.0 && index < units.lastIndex) {
        value /= 1_000.0
        index++
    }
    val displayed = when {
        index == 0 -> safe.toString()
        value >= 100.0 -> "%.0f".format(Locale.ROOT, value)
        else -> "%.1f".format(Locale.ROOT, value)
    }
    return "$displayed ${units[index]}"
}

@Composable
private fun MetricCard(
    label: String,
    bytesPerSecond: Long,
    modifier: Modifier,
    testSnapshot: SpeedTestSnapshot,
    testError: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val testValue = when (label) {
        dashboardStrings().download -> testSnapshot.downloadMbps
        else -> testSnapshot.uploadMbps
    }
    val testingThis = when (label) {
        dashboardStrings().download -> testSnapshot.phase == SpeedTestPhase.DOWNLOAD
        else -> testSnapshot.phase == SpeedTestPhase.UPLOAD
    }
    val (value, unit) = if (testingThis || testValue != null) {
        (testValue ?: testSnapshot.megabitsPerSecond).roundToInt().toString() to "Mbps"
    } else {
        formatRate(bytesPerSecond)
    }
    var history by remember { mutableStateOf(List(12) { 0L }) }
    LaunchedEffect(bytesPerSecond) { history = (history + bytesPerSecond).takeLast(12) }
    Card(
        modifier = modifier.height(132.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF1B1F1D), PanelHigh)))
                .padding(horizontal = 15.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (testingThis) CircularProgressIndicator(Modifier.size(14.dp), color = Mint, strokeWidth = 2.dp)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 2.dp)) {
                Text(value, color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(unit, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp, bottom = 4.dp))
            }
            if (testError != null) {
                Text(testError, color = Danger, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(3.dp))
            MiniWave(history, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun MiniWave(history: List<Long>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path()
        val max = history.maxOrNull()?.coerceAtLeast(1L)?.toFloat() ?: 1f
        history.forEachIndexed { index, sample ->
            val x = size.width * index / history.lastIndex.coerceAtLeast(1)
            val fraction = if (max <= 1f) {
                .58f + sin(index * 1.7).toFloat() * .12f
            } else {
                1f - sample / max * .78f
            }
            val y = size.height * fraction.coerceIn(.12f, .88f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Mint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
@Composable
private fun ConnectionControl(state: VpnSessionState, enabled: Boolean, onClick: () -> Unit) {
    val strings = dashboardStrings()
    val connected = state is VpnSessionState.Connected
    val busy = state is VpnSessionState.Connecting || state is VpnSessionState.Disconnecting
    val title = when (state) {
        VpnSessionState.Disconnected -> strings.notConnected
        is VpnSessionState.Connecting -> strings.connecting
        is VpnSessionState.Connected -> strings.connected
        is VpnSessionState.Disconnecting -> strings.disconnecting
        is VpnSessionState.Error -> state.message
    }
    val buttonScale by animateFloatAsState(if (connected) 1.035f else 1f, tween(220), label = "connectionScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(178.dp)
                .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale }
                .background(Brush.radialGradient(listOf(Color(0xFF1B201E), PanelHigh)), CircleShape)
                .clickable(enabled = enabled, onClick = onClick)
                .semantics {
                    contentDescription = if (connected || busy) strings.disconnectHint else strings.connectHint
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF0D0F0E), radius = size.minDimension / 2 - 4.dp.toPx(), style = Stroke(7.dp.toPx()))
                drawCircle(if (enabled) Mint.copy(alpha = .62f) else Border, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                drawCircle(Color(0xFF3B423F), radius = size.minDimension / 2 - 9.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            PowerMark(Modifier.size(65.dp), if (enabled) Mint else TextMuted)
        }
        Spacer(Modifier.height(18.dp))
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "connectionStatus",
        ) { value ->
            Text(value, color = if (state is VpnSessionState.Error) Danger else TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(if (connected || busy) strings.disconnectHint else strings.connectHint, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

internal data class ServerSections(
    val base: List<ServerProfile>,
    val plus: List<ServerProfile>,
    val others: List<ServerProfile>,
    val managed: Boolean,
)

internal fun planServerSections(servers: List<ServerProfile>, subscriptions: List<Subscription>, managedHosts: String): ServerSections {
    val hosts = managedHosts.split(',').map { it.trim().lowercase() }.filter(String::isNotBlank).toSet()
    val managedIds = subscriptions.filter { subscription ->
        runCatching { URI(subscription.url).host?.lowercase() in hosts }.getOrDefault(false)
    }.mapTo(mutableSetOf()) { it.id }
    if (managedIds.isEmpty()) return ServerSections(servers, emptyList(), emptyList(), false)
    val owned = servers.filter { it.subscriptionId in managedIds }
    val plus = owned.filter { it.name.startsWith("[plus]", true) || it.name.startsWith("plus:", true) }
    return ServerSections(owned - plus.toSet(), plus, servers.filterNot { it.subscriptionId in managedIds }, true)
}

@Composable
private fun SectionTitle(text: String, loading: Boolean = false, onTestLatency: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(top = 4.dp))
        if (onTestLatency != null) IconButton(onClick = onTestLatency, enabled = !loading) {
            AnimatedContent(
                targetState = loading,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "pingAll",
            ) { active ->
                if (active) CircularProgressIndicator(Modifier.size(22.dp), color = Mint, strokeWidth = 2.dp)
                else Icon(painterResource(R.drawable.ic_network_check), contentDescription = stringResource(R.string.server_ping_all), tint = Mint, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerListCard(
    items: List<ServerProfile>,
    selected: ServerProfile?,
    latencyResults: Map<String, ServerLatencyResult>,
    latencyTestingIds: Set<String>,
    onSelect: (ServerProfile) -> Unit,
    onTestLatency: (ServerProfile) -> Unit,
    locked: Boolean,
) {
    var menuServer by remember { mutableStateOf<ServerProfile?>(null) }
    var configServer by remember { mutableStateOf<ServerProfile?>(null) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val pingLabel = stringResource(R.string.server_ping)
    val viewConfigLabel = stringResource(R.string.server_view_config)
    val shareLabel = stringResource(R.string.server_share_config)
    val closeLabel = stringResource(R.string.server_close_config)
    val shareChooserLabel = stringResource(R.string.server_share_config_chooser)
    Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Border), colors = CardDefaults.cardColors(containerColor = Panel)) {
        if (items.isEmpty()) Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { Text("—", color = TextMuted) }
        items.forEach { server ->
            Box {
            val isSelected = server.id == selected?.id
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (isSelected) Mint.copy(alpha = .12f) else Color.Transparent)
                    .combinedClickable(
                        onClick = { onSelect(server) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuServer = server
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = PanelHigh, modifier = Modifier.size(38.dp)) { Box(contentAlignment = Alignment.Center) { Text(serverFlag(server), fontSize = 19.sp) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(localizedServerName(cleanServerName(server.name)), color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (server.id == selected?.id) FontWeight.Bold else FontWeight.Medium)
                    Text("${server.protocol.uppercase()} · ${server.address}", color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (server.id in latencyTestingIds) CircularProgressIndicator(Modifier.size(18.dp), color = Mint, strokeWidth = 2.dp)
                else latencyResults[server.id]?.let { result ->
                    Text(result.latencyMillis?.let { "${it}мс" } ?: "!", color = if (result.latencyMillis != null) Mint else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                if (locked) Text("♙", color = Mint, fontSize = 18.sp)
                Text("›", color = if (server.id == selected?.id) Mint else TextMuted, fontSize = 24.sp)
            }
            DropdownMenu(expanded = menuServer?.id == server.id, onDismissRequest = { menuServer = null }) {
                DropdownMenuItem(
                    text = { Text(pingLabel) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_network_check), null, Modifier.size(22.dp)) },
                    onClick = { menuServer = null; onTestLatency(server) },
                )
                DropdownMenuItem(
                    text = { Text(viewConfigLabel) },
                    leadingIcon = { Text("ⓘ", fontSize = 20.sp) },
                    onClick = { menuServer = null; configServer = server },
                )
            }
            }
        }
    }
    configServer?.let { server ->
        AlertDialog(
            onDismissRequest = { configServer = null },
            title = { Text(localizedServerName(cleanServerName(server.name))) },
            text = { Text(server.config, maxLines = 18, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                TextButton(onClick = {
                    // Return to the server list after handing off to Android's chooser; otherwise
                    // the configuration dialog unexpectedly reappears when the chooser closes.
                    configServer = null
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, server.config)
                    }, shareChooserLabel))
                }) { Text(shareLabel) }
            },
            dismissButton = { TextButton(onClick = { configServer = null }) { Text(closeLabel) } },
        )
    }
}

private fun cleanServerName(name: String): String = name.replace(Regex("^(\\[plus]|plus:)\\s*", RegexOption.IGNORE_CASE), "")

@Composable
private fun formatRate(bytesPerSecond: Long): Pair<String, String> {
    val bits = bytesPerSecond * 8.0
    return when {
        bits >= 1_000_000 -> "%.1f".format(Locale.US, bits / 1_000_000) to stringResource(com.envy.maxspeedvpn.R.string.unit_mbps)
        bits >= 1_000 -> "%.0f".format(Locale.US, bits / 1_000) to stringResource(com.envy.maxspeedvpn.R.string.unit_kbps)
        else -> bits.roundToInt().toString() to stringResource(com.envy.maxspeedvpn.R.string.unit_bps)
    }
}

@Composable
private fun localizedServerName(name: String): String = when (name.trim().lowercase()) {
    "canada" -> stringResource(com.envy.maxspeedvpn.R.string.country_canada)
    "germany" -> stringResource(com.envy.maxspeedvpn.R.string.country_germany)
    "ireland" -> stringResource(com.envy.maxspeedvpn.R.string.country_ireland)
    "china" -> stringResource(com.envy.maxspeedvpn.R.string.country_china)
    "united states", "usa" -> stringResource(com.envy.maxspeedvpn.R.string.country_united_states)
    "russia" -> stringResource(com.envy.maxspeedvpn.R.string.country_russia)
    else -> name
}

private fun serverFlag(server: ServerProfile): String = serverFlagFromName(server.name)

private val countryFlagRegex = Regex("[\\x{1F1E6}-\\x{1F1FF}]{2}")

internal fun serverFlagFromName(name: String): String {
    countryFlagRegex.find(name)?.value?.let { return it }
    val value = name.lowercase()
    fun matches(vararg aliases: String): Boolean = aliases.any { alias ->
        if (alias.any(Char::isLetter)) {
            Regex("(^|[^\\p{L}])${Regex.escape(alias)}([^\\p{L}]|$)").containsMatchIn(value)
        } else {
            value.contains(alias)
        }
    }
    return when {
        matches("🇦🇹", "austria", "австрия", "österreich", "vienna", "вена", "at") -> "🇦🇹"
        matches("🇨🇦", "canada", "канада", "toronto", "торонто", "ca") -> "🇨🇦"
        matches("🇩🇪", "germany", "германия", "deutschland", "берлин", "berlin", "de") -> "🇩🇪"
        matches("🇮🇪", "ireland", "ирландия", "dublin", "дублин", "ie") -> "🇮🇪"
        matches("🇨🇳", "china", "китай", "beijing", "пекин", "cn") -> "🇨🇳"
        matches("🇺🇸", "united states", "сша", "usa", "new york", "нью-йорк", "us") -> "🇺🇸"
        matches("🇷🇺", "russia", "россия", "moscow", "москва", "ru") -> "🇷🇺"
        matches("🇬🇧", "united kingdom", "великобритания", "англия", "london", "лондон", "uk", "gb") -> "🇬🇧"
        matches("🇳🇱", "netherlands", "нидерланды", "holland", "голландия", "amsterdam", "амстердам", "nl") -> "🇳🇱"
        matches("🇫🇷", "france", "франция", "paris", "париж", "fr") -> "🇫🇷"
        matches("🇫🇮", "finland", "финляндия", "helsinki", "хельсинки", "fi") -> "🇫🇮"
        matches("🇸🇪", "sweden", "швеция", "stockholm", "стокгольм", "se") -> "🇸🇪"
        matches("🇵🇱", "poland", "польша", "warsaw", "варшава", "pl") -> "🇵🇱"
        matches("🇨🇭", "switzerland", "швейцария", "zurich", "zürich", "цюрих", "ch") -> "🇨🇭"
        matches("🇯🇵", "japan", "япония", "tokyo", "токио", "jp") -> "🇯🇵"
        matches("🇸🇬", "singapore", "сингапур", "sg") -> "🇸🇬"
        else -> "🌐"
    }
}

@Composable
private fun MiniWave(modifier: Modifier) { Canvas(modifier) { val p = Path().apply { moveTo(0f, size.height * .7f); cubicTo(size.width * .2f, size.height * .1f, size.width * .32f, size.height, size.width * .5f, size.height * .45f); cubicTo(size.width * .68f, 0f, size.width * .82f, size.height * .8f, size.width, size.height * .25f) }; drawPath(p, Mint, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round)) } }

@Composable
private fun PowerMark(modifier: Modifier, color: Color) { Canvas(modifier) { val stroke = 5.dp.toPx(); drawArc(color, -45f, 270f, false, style = Stroke(stroke, cap = StrokeCap.Round)); drawLine(color, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height * .48f), stroke, StrokeCap.Round) } }

@Composable
private fun TelegramMark(modifier: Modifier, color: Color) { Canvas(modifier) { val p = Path().apply { moveTo(size.width * .08f, size.height * .47f); lineTo(size.width * .9f, size.height * .1f); lineTo(size.width * .7f, size.height * .9f); lineTo(size.width * .43f, size.height * .68f); lineTo(size.width * .27f, size.height * .83f); lineTo(size.width * .3f, size.height * .61f); close() }; drawPath(p, color); drawLine(Bg, Offset(size.width * .31f, size.height * .6f), Offset(size.width * .72f, size.height * .31f), 2.dp.toPx()) } }
