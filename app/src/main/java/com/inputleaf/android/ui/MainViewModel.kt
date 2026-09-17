package com.inputleaf.android.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inputleaf.android.model.ConnectionState
import com.inputleaf.android.model.ServerInfo
import com.inputleaf.android.network.ClientCertificateSummary
import com.inputleaf.android.network.ClientCertificateValidationResult
import com.inputleaf.android.network.ConnectResult
import com.inputleaf.android.network.ConnectionTransportPolicy
import com.inputleaf.android.network.ServerScanner
import com.inputleaf.android.service.ConnectionService
import com.inputleaf.android.storage.AppPreferences
import com.inputleaf.android.storage.ClientCertificateStore
import com.inputleaf.android.update.ChangelogProvider
import com.inputleaf.android.update.UpdateCheckResult
import com.inputleaf.android.update.UpdateService
import com.inputleaf.android.update.VersionChangelog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.NetworkInterface

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private const val MAX_CLIENT_CERTIFICATE_BYTES = 16 * 1024 * 1024

internal fun connectionFailureMessage(
    reason: ConnectResult.FailureReason,
    detail: String? = null,
): String = when (reason) {
    ConnectResult.FailureReason.NETWORK -> "无法连接到 Deskflow 服务器"
    ConnectResult.FailureReason.TLS_AGAINST_PLAIN_SERVER ->
        "服务器未使用 TLS。请仅选择自动或明文，或在 Deskflow 中启用 TLS。"
    ConnectResult.FailureReason.CERTIFICATE_MISMATCH ->
        "Deskflow 的 TLS 证书已更改。仅在你确实预期如此时才移除受信任的服务器。"
    ConnectResult.FailureReason.CLIENT_CERT_REQUIRED ->
        "Deskflow 正在请求信任此手机。请打开设置，比对指纹，并在 Deskflow 中接受。"
    ConnectResult.FailureReason.HANDSHAKE ->
        "Deskflow 在所选的传输方式上握手失败"
    ConnectResult.FailureReason.INCOMPATIBLE ->
        detail ?: "Deskflow 拒绝了此客户端的协议版本"
    ConnectResult.FailureReason.BUSY ->
        "此屏幕名称已连接到 Deskflow"
}

internal fun clientCertificateImportError(
    result: ClientCertificateValidationResult,
): String? = when (result) {
    is ClientCertificateValidationResult.Success -> null
    ClientCertificateValidationResult.IncorrectPassword -> "PKCS12 密码错误"
    ClientCertificateValidationResult.InvalidFormat ->
        "文件不是有效的 PKCS12（.p12 或 .pfx）文件包"
    ClientCertificateValidationResult.NoPrivateKey ->
        "证书文件包不包含私钥"
    ClientCertificateValidationResult.KeyMismatch ->
        "私钥与客户端证书不匹配"
    ClientCertificateValidationResult.Expired -> "客户端证书已过期"
    ClientCertificateValidationResult.NotYetValid ->
        "客户端证书尚未生效"
    ClientCertificateValidationResult.UnsupportedKey ->
        "客户端证书使用了不受支持的密钥类型"
    ClientCertificateValidationResult.StorageError ->
        "无法安全存储客户端证书"
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val clientCertificateStore = ClientCertificateStore(app)
    private val scanner = ServerScanner()
    private var service: ConnectionService? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredServers = MutableStateFlow<List<ServerInfo>>(emptyList())
    val discoveredServers: StateFlow<List<ServerInfo>> = _discoveredServers

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private val _clientCertificateSummary = MutableStateFlow<ClientCertificateSummary?>(null)
    val clientCertificateSummary: StateFlow<ClientCertificateSummary?> =
        _clientCertificateSummary

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    private var scanJob: Job? = null
    private val permissionProvider = PermissionStatusProvider(app)
    
    // Shizuku status
    val shizukuStatus: StateFlow<ShizukuStatus> = permissionProvider.shizukuStatus
    
    // Cursor overlay status
    val canDrawOverlays: StateFlow<Boolean> = permissionProvider.canDrawOverlays
    
    // Battery optimization status
    val batteryOptimizationExempt: StateFlow<Boolean> = permissionProvider.batteryOptimizationExempt
    
    val showCursor: Flow<Boolean> = prefs.showCursor

    val screenName: Flow<String> = prefs.screenName
    val autoConnect: Flow<Boolean> = prefs.autoConnect
    val fingerprints: Flow<Map<String, String>> = prefs.allFingerprints()
    val themeMode: Flow<String> = prefs.themeMode
    val cursorStyle: Flow<String> = prefs.cursorStyle
    val leafOnboardingComplete: Flow<Boolean> = prefs.leafOnboardingComplete
    val onboardingComplete: Flow<Boolean> = prefs.leafOnboardingComplete

    val mouseEnabled: Flow<Boolean> = prefs.mouseEnabled
    val keyboardEnabled: Flow<Boolean> = prefs.keyboardEnabled
    val favoriteServers: Flow<Set<String>> = prefs.favoriteServers
    val inputMethod: Flow<String> = prefs.inputMethod
    val connectionTransportPolicy: Flow<ConnectionTransportPolicy> =
        prefs.connectionTransportPolicy

    val shizukuAvailable: Flow<Boolean> = permissionProvider.shizukuAvailable
    val accessibilityAvailable: Flow<Boolean> = permissionProvider.accessibilityAvailable
    val imeEnabledAndSelected: Flow<Boolean> = permissionProvider.imeEnabledAndSelected

    // TOFU: suspending channel — UI collects this and shows FingerprintDialog
    private val _fingerprintRequest = Channel<FingerprintRequest>(1)
    val fingerprintRequest = _fingerprintRequest.receiveAsFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate

    private val _whatsNewChangelog = MutableStateFlow<VersionChangelog?>(null)
    val whatsNewChangelog: StateFlow<VersionChangelog?> = _whatsNewChangelog

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = UpdateService.checkUpdate(getApplication())
            _isCheckingUpdate.value = false
            if (manual || result is UpdateCheckResult.UpdateAvailable) {
                _updateCheckResult.value = result
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateCheckResult.value = null
    }

    fun dismissWhatsNew() {
        _whatsNewChangelog.value = null
    }

    data class FingerprintRequest(
        val ip: String,
        val newFp: String,
        val oldFp: String?,
        val response: kotlinx.coroutines.CompletableDeferred<Boolean>
    )


    fun saveScreenName(name: String) { 
        viewModelScope.launch { 
            prefs.saveScreenName(name)
            // Reconnect with new screen name if currently connected
            val currentState = _connectionState.value
            if (currentState is ConnectionState.Idle || 
                currentState is ConnectionState.Active ||
                currentState is ConnectionState.Connecting ||
                currentState is ConnectionState.Handshaking) {
                // Get the current server IP and reconnect
                val serverIp = when (currentState) {
                    is ConnectionState.Idle -> currentState.serverIp
                    is ConnectionState.Active -> currentState.serverIp
                    is ConnectionState.Connecting -> currentState.serverIp
                    is ConnectionState.Handshaking -> currentState.serverIp
                    else -> null
                }
                if (serverIp != null) {
                    Log.d("InputLeaf", "Screen name changed, reconnecting with new name: $name")
                    service?.reconnect(serverIp, name.trim())
                }
            }
        } 
    }
    fun saveAutoConnect(v: Boolean) { viewModelScope.launch { prefs.saveAutoConnect(v) } }
    fun deleteFingerprint(ip: String) { viewModelScope.launch { prefs.removeFingerprint(ip) } }
    fun saveThemeMode(mode: String) { viewModelScope.launch { prefs.saveThemeMode(mode) } }
    fun completeOnboarding() { viewModelScope.launch { prefs.saveLeafOnboardingComplete() } }
    fun toggleMouseEnabled(enabled: Boolean) { viewModelScope.launch { prefs.saveMouseEnabled(enabled) } }
    fun toggleKeyboardEnabled(enabled: Boolean) { viewModelScope.launch { prefs.saveKeyboardEnabled(enabled) } }
    fun toggleFavoriteServer(ip: String) { viewModelScope.launch { prefs.toggleFavoriteServer(ip) } }
    fun saveCursorStyle(style: String) { viewModelScope.launch { prefs.saveCursorStyle(style) } }
    fun saveInputMethod(method: String) {
        viewModelScope.launch {
            val oldMethod = prefs.inputMethod.first()
            prefs.saveInputMethod(method)

            if (oldMethod == method) return@launch

            val currentIp = when (val state = _connectionState.value) {
                is ConnectionState.Connecting -> state.serverIp
                is ConnectionState.Handshaking -> state.serverIp
                is ConnectionState.Idle -> state.serverIp
                is ConnectionState.Active -> state.serverIp
                ConnectionState.Disconnected -> null
            } ?: return@launch

            Log.i("InputLeaf", "Input method changed from $oldMethod to $method while connected to $currentIp — auto-reconnecting")

            val injector = resolveInjector(preferredMethod = method)
            if (injector == null) {
                _errorState.value = "所选的输入方式不可用。请启用 Shizuku 或无障碍服务。"
                disconnect()
                return@launch
            }

            val connected = injector.connect()
            if (!connected) {
                _errorState.value = "连接输入方式失败：${injector.name}"
                disconnect()
                return@launch
            }

            val name = prefs.screenName.first()
            service?.setInjector(injector)
            service?.reconnect(currentIp, name)
        }
    }
    fun saveConnectionTransportPolicy(policy: ConnectionTransportPolicy) {
        viewModelScope.launch { prefs.saveConnectionTransportPolicy(policy) }
    }
    fun importClientCertificate(uri: Uri, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pkcs12 = try {
                readClientCertificate(uri)
            } catch (_: IllegalArgumentException) {
                _errorState.value = "客户端证书必须小于 16 MB"
                return@launch
            } catch (error: Exception) {
                Log.e("InputLeaf", "Failed to read client certificate", error)
                _errorState.value = "无法读取所选的证书文件"
                return@launch
            }
            val passwordChars = password.toCharArray()
            val result = try {
                clientCertificateStore.importCertificate(pkcs12, passwordChars)
            } finally {
                pkcs12.fill(0)
                passwordChars.fill('\u0000')
            }
            Log.i("InputLeaf", "Client certificate import result=$result")
            if (result is ClientCertificateValidationResult.Success) {
                _clientCertificateSummary.value = result.summary
            }
            clientCertificateImportError(result)?.let { _errorState.value = it }
        }
    }

    fun regenerateClientCertificate() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = clientCertificateStore.regenerate()
            Log.i("InputLeaf", "Client certificate regenerate result=$result")
            if (result is ClientCertificateValidationResult.Success) {
                _clientCertificateSummary.value = result.summary
            }
            clientCertificateImportError(result)?.let { _errorState.value = it }
        }
    }

    fun clearClientCertificate() {
        viewModelScope.launch(Dispatchers.IO) {
            clientCertificateStore.clear()
            _clientCertificateSummary.value = null
        }
    }

    private fun readClientCertificate(uri: Uri): ByteArray {
        val input = checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri))
        return input.use {
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                try {
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_CLIENT_CERTIFICATE_BYTES)
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                } finally {
                    buffer.fill(0)
                }
            }
        }
    }

    // Called by UI after user taps Trust/Cancel in FingerprintDialog
    fun respondToFingerprint(request: FingerprintRequest, trusted: Boolean) {
        request.response.complete(trusted)
    }

    private var hasAutoConnected = false
    private var userRequestedDisconnect = false

    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localService = (binder as ConnectionService.LocalBinder).getService()
            service = localService
            
            // Push current service state immediately to avoid race conditions
            _connectionState.value = localService.state.value

            viewModelScope.launch {
                localService.state.collect { _connectionState.value = it }
            }
            // Wire TOFU callback: bridge service's suspend callback → UI Channel
            service!!.onFingerprintConfirmationRequired = { ip, newFp, oldFp ->
                val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
                _fingerprintRequest.send(FingerprintRequest(ip, newFp, oldFp, deferred))
                deferred.await()
            }
            service!!.onConnectionRejected = {
                _errorState.value = "连接未受信任"
            }
            service!!.onConnectionFailed = { reason, detail ->
                _errorState.value = connectionFailureMessage(reason, detail)
            }
            
            // Auto-connect to last server if enabled
            if (!hasAutoConnected) {
                hasAutoConnected = true
                viewModelScope.launch {
                    val auto = prefs.autoConnect.first()
                    val lastIp = prefs.lastServerIp.first()
                    val currentState = service?.state?.value ?: ConnectionState.Disconnected
                    if (auto && !lastIp.isNullOrBlank() && currentState !is ConnectionState.Idle && currentState !is ConnectionState.Active) {
                        Log.i("InputLeaf", "Auto-connecting to last server: $lastIp")
                        connect(ServerInfo(ip = lastIp))
                    }
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) { service = null }
    }
    init {
        bindService()

        // Auto-connect when Shizuku becomes available (e.g. started after app launch or restarted)
        viewModelScope.launch {
            permissionProvider.shizukuAvailable.collect { available ->
                if (available && !userRequestedDisconnect) {
                    val auto = prefs.autoConnect.first()
                    val lastIp = prefs.lastServerIp.first()
                    val currentState = service?.state?.value ?: _connectionState.value
                    if (auto && !lastIp.isNullOrBlank() && currentState is ConnectionState.Disconnected) {
                        Log.i("InputLeaf", "Shizuku became available — auto-connecting to last server: $lastIp")
                        if (_errorState.value?.contains("输入方式", ignoreCase = true) == true ||
                            _errorState.value?.contains("Shizuku", ignoreCase = true) == true
                        ) {
                            _errorState.value = null
                        }
                        connect(ServerInfo(ip = lastIp))
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = clientCertificateStore.ensureGenerated()) {
                is ClientCertificateValidationResult.Success ->
                    _clientCertificateSummary.value = result.summary
                else -> {
                    _clientCertificateSummary.value = null
                    clientCertificateImportError(result)?.let { _errorState.value = it }
                }
            }
        }

        // Observe showCursor preference and update service
        viewModelScope.launch {
            prefs.showCursor.collect { enabled ->
                service?.setCursorOverlayEnabled(enabled)
            }
        }

        // Check for version upgrade (What's New changelog)
        viewModelScope.launch {
            val lastSeenCode = prefs.lastSeenVersionCode.first()
            val currentCode = UpdateService.getCurrentVersionCode(app).toInt()
            val currentVersion = UpdateService.getCurrentVersion(app)

            if (lastSeenCode != null && lastSeenCode < currentCode) {
                _whatsNewChangelog.value = ChangelogProvider.getChangelog(currentVersion)
            }
            prefs.saveLastSeenVersionCode(currentCode)

            // Silent update check in background
            checkForUpdates(manual = false)
        }
    }
    fun checkShizukuStatus() = permissionProvider.checkShizukuStatus()
    fun requestShizukuPermission() = permissionProvider.requestShizukuPermission()
    fun checkOverlayPermission() = permissionProvider.checkOverlayPermission()
    fun checkBatteryOptimization() = permissionProvider.checkBatteryOptimization()
    
    fun saveShowCursor(enabled: Boolean) {
        viewModelScope.launch { 
            prefs.saveShowCursor(enabled)
            service?.setCursorOverlayEnabled(enabled)
        }
    }

    private fun bindService() {
        if (serviceBound) return
        val intent = Intent(getApplication(), ConnectionService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    fun scan() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            val ip = com.inputleaf.android.network.NetworkUtils.getLocalIpAddress(getApplication())
            Log.d("InputLeaf", "Scanning from IP: $ip")
            try {
                if (ip != null) {
                    scanner.scan(ip) { server ->
                        _discoveredServers.update { currentServers ->
                            val existingMap = currentServers.associateBy { it.ip }.toMutableMap()
                            existingMap[server.ip] = server
                            existingMap.values.toList()
                        }
                    }
                } else {
                    Log.e("InputLeaf", "Could not determine local IP address")
                    _discoveredServers.value = emptyList()
                }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    private suspend fun resolveInjector(preferredMethod: String? = null): com.inputleaf.android.inject.InputInjector? {
        val method = preferredMethod ?: prefs.inputMethod.first()
        val wm = getApplication<Application>().getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val bounds = wm.currentWindowMetrics.bounds
        val shizukuInjector = com.inputleaf.android.shizuku.ShizukuInputInjector(bounds.width(), bounds.height())
        val accessibilityInjector = com.inputleaf.android.inject.AccessibilityInputInjector(getApplication(), bounds.width(), bounds.height())

        val resolved = com.inputleaf.android.inject.InputMethodResolver.resolve(
            preferredMethod = method,
            isShizukuAvailable = shizukuInjector.isAvailable(),
            isAccessibilityAvailable = accessibilityInjector.isAvailable()
        )

        return when (resolved) {
            com.inputleaf.android.inject.ResolvedMethod.SHIZUKU -> shizukuInjector
            com.inputleaf.android.inject.ResolvedMethod.ACCESSIBILITY -> accessibilityInjector
            com.inputleaf.android.inject.ResolvedMethod.NONE -> null
        }
    }

    fun connect(server: ServerInfo) {
        userRequestedDisconnect = false
        scanJob?.cancel()
        viewModelScope.launch {
            val state = _connectionState.value
            if (state is ConnectionState.Connecting || state is ConnectionState.Handshaking) {
                Log.d("InputLeaf", "Ignoring connect — already connecting to ${server.ip}")
                return@launch
            }
            val name = prefs.screenName.first()
            prefs.saveLastServer(server.ip)
            
            val injector = resolveInjector()
            if (injector == null) {
                _errorState.value = "没有可用的输入方式。请启用 Shizuku 或无障碍服务。"
                return@launch
            }
            
            val connected = injector.connect()
            if (!connected) {
                _errorState.value = "连接输入方式失败：${injector.name}"
                return@launch
            }
            
            service?.setInjector(injector)
            service?.connect(server.ip, name)
        }
    }

    fun disconnect() {
        userRequestedDisconnect = true
        service?.disconnect()
    }

    fun addManualServer(ip: String) {
        val trimmed = ip.trim()
        if (isValidServerAddress(trimmed)) {
            _discoveredServers.update { currentServers ->
                val existing = currentServers.filter { it.ip != trimmed }
                existing + ServerInfo(ip = trimmed)
            }
        }
    }

    private fun isValidServerAddress(address: String): Boolean {
        if (address.isBlank()) return false
        val parts = address.split(".")
        if (parts.size == 4 && parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } == true }) {
            return true
        }
        return address.matches(Regex("^[a-zA-Z0-9.-]+$"))
    }

    override fun onCleared() {
        scanJob?.cancel()
        permissionProvider.cleanup()
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }
    }
}
