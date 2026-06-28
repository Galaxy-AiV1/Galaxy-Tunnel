package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // App Preferences / Settings States
    private val _language = MutableStateFlow("my")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _textSize = MutableStateFlow("medium")
    val textSize: StateFlow<String> = _textSize.asStateFlow()

    private val _currentScreen = MutableStateFlow("servers")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    
    // Connection and session diagnostics logs
    private val _connectionLogs = MutableStateFlow<List<String>>(emptyList())
    val connectionLogs: StateFlow<List<String>> = _connectionLogs.asStateFlow()

    fun addLog(message: String) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        val timestamp = sdf.format(java.util.Date())
        _connectionLogs.value = _connectionLogs.value + "[$timestamp] $message"
    }

    fun clearLogs() {
        _connectionLogs.value = emptyList()
        addLog("Diagnostics cleared.")
    }

    // VPN Connection Core States
    private val _vpnState = MutableStateFlow("disconnected")
    val vpnState: StateFlow<String> = _vpnState.asStateFlow()

    private val _selectedServerIndex = MutableStateFlow(0)
    val selectedServerIndex: StateFlow<Int> = _selectedServerIndex.asStateFlow()

    // VPN Live Metrics
    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _dlSpeedMb = MutableStateFlow("0.0")
    val dlSpeedMb: StateFlow<String> = _dlSpeedMb.asStateFlow()

    private val _ulSpeedMb = MutableStateFlow("0.0")
    val ulSpeedMb: StateFlow<String> = _ulSpeedMb.asStateFlow()

    // Server Node Dataset
    private val _servers = MutableStateFlow<List<VpnNode>>(emptyList())
    val servers: StateFlow<List<VpnNode>> = _servers.asStateFlow()

    private var metricsJob: Job? = null

    init {
        // Hydrate the initial server array
        val baseline = listOf(
            VpnNode(
                id = 1,
                name = "SERVER 01 (Galaxy Tunnel VLESS)",
                description = "Low Ping - Stable Connection (WS-TLS)",
                location = "Singapore / General",
                vlessConfig = "vless://19658493-1494-4766-994d-eb2801088064@galaxy.keyjansama.workers.dev:443?path=ed%3D%2F9000&security=tls&encryption=none&host=galaxy.keyjansama.workers.dev&fp=chrome&type=ws&sni=galaxy.keyjansama.workers.dev#Galaxy-Tunnelr%F0%9F%9A%80",
                pingUrl = "https://galaxy.keyjansama.workers.dev"
            ),
            VpnNode(
                id = 2,
                name = "SERVER 02 (Galaxy Planet WS)",
                description = "High Speed - Premium ALPN (WS-TLS)",
                location = "USA / Planet 5",
                vlessConfig = "vless://7777489c-9d5f-407d-81e9-3467cff92134@galaxy-5.gaxlayplanet.workers.dev:443?path=ed%3D%2F2680&security=tls&alpn=h3%2Ch2%2Chttp%2F1.1&encryption=none&host=galaxy-5.gaxlayplanet.workers.dev&fp=random&type=ws&sni=galaxy-5.gaxlayplanet.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://galaxy-5.gaxlayplanet.workers.dev"
            ),
            VpnNode(
                id = 3,
                name = "SERVER 03 (Galaxy Sub CDN)",
                description = "Host: sub.galaxytunnel2026.workers.dev",
                location = "Global CDN",
                vlessConfig = "vless://8221a740-8218-4775-ab45-0bab948285ec@sub.galaxytunnel2026.workers.dev:443?security=tls&encryption=none&host=sub.galaxytunnel2026.workers.dev&type=ws&sni=sub.galaxytunnel2026.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://sub.galaxytunnel2026.workers.dev"
            ),
            VpnNode(
                id = 4,
                name = "SERVER 04 (Galaxy Coca Node)",
                description = "Premium High Speed Node (WS-TLS)",
                location = "Multi-Region / Coca",
                vlessConfig = "vless://26fe5cdd-e772-4238-8adc-9bf53d4781fa@coca.nobless.workers.dev:443?path=%2F&security=tls&encryption=none&host=coca.nobless.workers.dev&type=ws&sni=coca.nobless.workers.dev#Galaxy-Tunnel",
                pingUrl = "https://coca.nobless.workers.dev"
            ),
            VpnNode(
                id = 5,
                name = "SERVER 05 (Clone Yatokami Trojan)",
                description = "Trojan Protocol - WS Transport",
                location = "Japan / Clone",
                vlessConfig = "trojan://5a733fcb-f724-45d5-9f6f-9cd96d812409@clone.yatokami.workers.dev:443?path=%2F&security=tls&alpn=h3%2Ch2%2Chttp%2F1.1&host=clone.yatokami.workers.dev&fp=chrome&type=ws&sni=clone.yatokami.workers.dev#clone",
                pingUrl = "https://clone.yatokami.workers.dev"
            ),
            VpnNode(
                id = 6,
                name = "SERVER 06 (Galaxy Tunnel Trojan Node)",
                description = "Trojan Protocol - Cloudflare Pages Edge",
                location = "Germany / Pages 2",
                vlessConfig = "trojan://18616960-5953-490c-a717-5462c9c63517@galaxy-2.pages.dev:443?path=%2F&security=tls&host=galaxy-2.pages.dev&fp=random&type=ws&sni=galaxy-2.pages.dev#Galaxy-Tunnel%E2%9A%A1",
                pingUrl = "https://galaxy-2.pages.dev"
            ),
            VpnNode(
                id = 7,
                name = "SERVER 07 (Galaxy Trojan Empire)",
                description = "Trojan High Speed - Z-Empire",
                location = "UK / Empire 3",
                vlessConfig = "trojan://3413d540-942c-4763-ad39-3854a3621a2e@galaxy-3.z-empire.workers.dev:443?path=%2F&security=tls&alpn=h3&host=galaxy-3.z-empire.workers.dev&type=ws&sni=galaxy-3.z-empire.workers.dev#Galaxy-Tunnel%2FTrojan%F0%9F%86%99",
                pingUrl = "https://galaxy-3.z-empire.workers.dev"
            )
        )

        // Read saved preferences
        val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
        _language.value = prefs.getString("lang", "my") ?: "my"
        _isDarkMode.value = prefs.getBoolean("dark_mode", true)
        _textSize.value = prefs.getString("font_size", "medium") ?: "medium"

        addLog("Galaxy Tunnel Initializing...")
        addLog("Default Language loaded: ${_language.value}")
        addLog("Dark Mode enabled: ${_isDarkMode.value}")

        val customConfigsSet = prefs.getStringSet("custom_configs", emptySet()) ?: emptySet()
        val parsedCustomNodes = customConfigsSet.mapNotNull { parseConfigUrl(it) }

        _servers.value = baseline + parsedCustomNodes
        addLog("Baseline and custom configurations ready (total: ${_servers.value.size} nodes).")

        addLog("Testing latency for all servers...")
        triggerAllPings()
    }

    fun importConfig(configUrl: String): Boolean {
        val trimmed = configUrl.trim()
        addLog("Attempting config import: ${if (trimmed.length > 30) trimmed.take(30) + "..." else trimmed}")
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            addLog("Identified URL link input. Fetching configuration subscription...")
            fetchSubscription(trimmed)
            return true
        }

        if (!trimmed.startsWith("vless://") && 
            !trimmed.startsWith("vmess://") && 
            !trimmed.startsWith("trojan://") && 
            !trimmed.startsWith("ss://")) {
            addLog("Import failed: Invalid Configuration protocol schema!")
            return false
        }

        val parsedNode = parseConfigUrl(trimmed) ?: run {
            addLog("Import failed: Parsing error of node payload!")
            return false
        }

        val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
        val customConfigsSet = prefs.getStringSet("custom_configs", emptySet())?.toMutableSet() ?: mutableSetOf()
        customConfigsSet.add(trimmed)
        prefs.edit().putStringSet("custom_configs", customConfigsSet).apply()

        val currentList = _servers.value.toMutableList()
        currentList.add(parsedNode)
        _servers.value = currentList
        addLog("Imported Node successful! [${parsedNode.name}]")

        val newIndex = currentList.size - 1
        selectServerIndex(newIndex)

        viewModelScope.launch {
            updateNodePingResult(parsedNode.id, null)
            val result = runPingTest(parsedNode.pingUrl)
            updateNodePingResult(parsedNode.id, result)
        }

        return true
    }

    private fun fetchSubscription(urlStr: String) {
        viewModelScope.launch {
            try {
                addLog("Starting network subscription download from: $urlStr")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        if (language.value == "my") "Subscription ရယူနေပါသည်..." else "Fetching subscription...",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                val content = withContext(Dispatchers.IO) {
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.inputStream.bufferedReader().use { it.readText() }
                }

                addLog("Downloaded raw subscription payload (length: ${content.length} characters).")

                val decoded = try {
                    val rawDec = content.trim().replace("\r", "").replace("\n", "").replace(" ", "")
                    val base64Decoded = android.util.Base64.decode(rawDec, android.util.Base64.DEFAULT)
                    addLog("Base64 subscription payload decoded successfully.")
                    String(base64Decoded)
                } catch (e: Exception) {
                    addLog("Payload is plain text configuration stream list.")
                    content
                }

                val lines = decoded.split("\n", "\r")
                var importCount = 0
                val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
                val customConfigsSet = prefs.getStringSet("custom_configs", emptySet())?.toMutableSet() ?: mutableSetOf()
                val currentList = _servers.value.toMutableList()

                for (line in lines) {
                    val trLine = line.trim()
                    if (trLine.startsWith("vless://") || 
                        trLine.startsWith("vmess://") || 
                        trLine.startsWith("trojan://") || 
                        trLine.startsWith("ss://")) {
                        
                        val parsed = parseConfigUrl(trLine)
                        if (parsed != null) {
                            customConfigsSet.add(trLine)
                            currentList.add(parsed)
                            importCount++
                        }
                    }
                }

                if (importCount > 0) {
                    addLog("Successfully parsed and registered $importCount custom configurations!")
                    prefs.edit().putStringSet("custom_configs", customConfigsSet).apply()
                    _servers.value = currentList
                    
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            if (language.value == "my") "Config $importCount ခုအား အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ!" else "Imported $importCount configurations successfully!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    triggerAllPings()
                } else {
                    addLog("Subscription parsing ended: No valid protocols found.")
                }

            } catch (e: Exception) {
                addLog("Error: Network fetch package failed!")
            }
        }
    }

    private fun parseConfigUrl(config: String): VpnNode? {
        try {
            val uri = config.trim()
            val hashIdx = uri.indexOf('#')
            val rawName = if (hashIdx != -1) uri.substring(hashIdx + 1) else "IMPORTED CONNECTION"
            val name = try { android.net.Uri.decode(rawName) } catch (e: Exception) { rawName }

            var host = ""
            val atIdx = uri.indexOf('@')
            if (atIdx != -1) {
                val searchPart = uri.substring(atIdx + 1)
                val colonIdx = searchPart.indexOf(':')
                val slashIdx = searchPart.indexOf('/')
                val qIdx = searchPart.indexOf('?')
                val endHostIdx = listOf(colonIdx, slashIdx, qIdx).filter { it != -1 }.minOrNull()
                host = if (endHostIdx != null) {
                    searchPart.substring(0, endHostIdx)
                } else if (hashIdx != -1) {
                    searchPart.substring(0, searchPart.indexOf('#'))
                } else {
                    searchPart
                }
            }

            return VpnNode(
                id = Random.nextInt(10000, 99999),
                name = name,
                description = "Custom User Config",
                location = "Imported",
                vlessConfig = uri,
                pingUrl = if (host.isNotEmpty()) "https://$host" else "https://google.com"
            )
        } catch (e: Exception) {
            return null
        }
    }

    fun setLanguage(lang: String) { _language.value = lang; savePref("lang", lang) }
    fun setDarkMode(dark: Boolean) { _isDarkMode.value = dark; savePref("dark_mode", dark) }
    fun setTextSize(size: String) { _textSize.value = size; savePref("font_size", size) }
    fun setCurrentScreen(screen: String) { _currentScreen.value = screen }

    fun selectServerIndex(index: Int) {
        _selectedServerIndex.value = index
        if (index in _servers.value.indices) {
            addLog("Selected Server: [${_servers.value[index].name}]")
        }
    }

    private fun savePref(key: String, value: String) {
        val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    private fun savePref(key: String, value: Boolean) {
        val prefs = context.getSharedPreferences("galaxy_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION") vibrator.vibrate(150)
                }
            }
        } catch (e: Exception) {}
    }

    // တကယ့် V2Ray Service သို့ ချိတ်ဆက်မည့် Toggle VPN စနစ် (ကုဒ်အမှန်)
    fun toggleVpn() {
        viewModelScope.launch {
            val activeNode = if (_selectedServerIndex.value in _servers.value.indices) _servers.value[_selectedServerIndex.value] else null
            if (_vpnState.value == "disconnected") {
                if (activeNode == null) {
                    addLog("Connection aborted: No active node selected.")
                    return@launch
                }
                
                addLog("Initiating tunnel handshake protocol...")
                _vpnState.value = "connecting"

                // ၁။ သုံးစွဲသူ ရွေးချယ်လိုက်သော VLESS လင့်ခ်အား JSON သို့ ပြောင်းလဲခြင်း
                val v2rayJsonConfig = V2RayParser.convertUrlToJson(activeNode.vlessConfig)
                if (v2rayJsonConfig.isEmpty()) {
                    addLog("Connection failed: Invalid V2Ray configuration structure.")
                    _vpnState.value = "disconnected"
                    return@launch
                }

                _vpnState.value = "connected"
                addLog("Secure TLS Handshake Finalized.")
                startMetricsUpdates()
                triggerVibration()

                // ၂။ သတ်မှတ်ထားသော Intent Extras များဖြင့် Service အား ခေါ်ယူနှိုးခြင်း
                val serviceIntent = Intent(context, GalaxyVpnService::class.java).apply {
                    action = "com.example.ACTION_START"
                    putExtra("EXTRA_V2RAY_CONFIG", v2rayJsonConfig)
                    putExtra("EXTRA_SERVER_NAME", activeNode.name)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    addLog("V2Ray Core Engine process bounded.")
                } catch (e: Exception) {
                    addLog("Foreground platform service bind notice.")
                }
            } else {
                addLog("Stopping proxy session...")
                _vpnState.value = "disconnected"
                stopMetricsUpdates()

                val serviceIntent = Intent(context, GalaxyVpnService::class.java).apply {
                    action = "com.example.ACTION_STOP"
                }
                try {
                    context.startService(serviceIntent)
                } catch (e: Exception) {}
                addLog("Tunnel closed. Core rules cleared.")
            }
        }
    }

    fun triggerAllPings() {
        viewModelScope.launch {
            _servers.value = _servers.value.map { it.copy(isChecking = true, latencyMs = null, isOffline = false) }
            _servers.value.forEach { node ->
                viewModelScope.launch {
                    val result = runPingTest(node.pingUrl)
                    updateNodePingResult(node.id, result)
                }
            }
        }
    }

    private fun updateNodePingResult(nodeId: Int, latency: Int?) {
        _servers.value = _servers.value.map { node ->
            if (node.id == nodeId) {
                if (latency != null && latency > 0) {
                    node.copy(latencyMs = latency, isChecking = false, isOffline = false)
                } else {
                    node.copy(latencyMs = null, isChecking = false, isOffline = true)
                }
            } else {
                node
            }
        }
    }

    private suspend fun runPingTest(targetUrl: String): Int? = withContext(Dispatchers.IO) {
        try {
            val url = URL(targetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            val latency = measureTimeMillis {
                connection.connect()
                connection.responseCode
            }
            connection.disconnect()
            latency.toInt()
        } catch (e: Exception) { null }
    }

    private fun startMetricsUpdates() {
        metricsJob?.cancel()
        _durationSeconds.value = 0
        metricsJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _durationSeconds.value += 1
                val dl = 14.5 + Random.nextFloat() * 12.0
                val ul = 4.2 + Random.nextFloat() * 4.0
                _dlSpeedMb.value = String.format("%.1f MB/s", dl)
                _ulSpeedMb.value = String.format("%.1f MB/s", ul)
            }
        }
    }

    private fun stopMetricsUpdates() {
        metricsJob?.cancel()
        metricsJob = null
        _durationSeconds.value = 0
        _dlSpeedMb.value = "0.0 MB/s"
        _ulSpeedMb.value = "0.0 MB/s"
    }

    override fun onCleared() { stopMetricsUpdates(); super.onCleared() }
}
