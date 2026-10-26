package com.naicha.diary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.naicha.diary.data.DrinkRepository
import com.naicha.diary.util.WebDavClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.naicha.diary.DrinkApp
import com.naicha.diary.data.AppSettings
import com.naicha.diary.data.Drink
import com.naicha.diary.net.DeepSeekClient
import com.naicha.diary.ui.components.AbstinenceTracker
import com.naicha.diary.ui.components.GradientButton
import com.naicha.diary.ui.components.SectionHeader
import com.naicha.diary.ui.components.SoftCard
import com.naicha.diary.ui.theme.Caramel
import com.naicha.diary.ui.theme.Matcha
import com.naicha.diary.ui.theme.Outline
import com.naicha.diary.ui.theme.PearlSoft
import com.naicha.diary.ui.theme.Strawberry
import com.naicha.diary.ui.theme.Taro
import com.naicha.diary.util.rememberHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    items: List<Drink>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(Outline))
            }
        },
    ) {
        SettingsContent(items = items, onClearAll = onClearAll, showHeader = false)
    }
}

@Composable
fun SettingsContent(
    items: List<Drink>,
    onClearAll: () -> Unit,
    showHeader: Boolean = true,
) {
    // 控制是否进入第二层页面
    var showDetail by remember { mutableStateOf(false) }
    
    // 拦截系统返回键，如果在第二层，则返回第一层
    BackHandler(enabled = showDetail) {
        showDetail = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeader) {
            item {
                Column {
                    Text("我的", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text("设置与数据", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!showDetail) {
            // ================= 第一层：打卡 + 设置入口 =================
            item {
                AbstinenceTracker(
                    modifier = Modifier.fillMaxWidth(),
                    onOpenSettings = { showDetail = true } // 点击齿轮进入第二层
                )
            }
            
            // 如果你在第一层想放点别的东西，可以放这

        } else {
            // ================= 第二层：设置详情页 =================
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    IconButton(onClick = { showDetail = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // === AI 识图 ===
            item {
                SettingsAiCard()
            }

            // === 我的数据 ===
            item {
                SettingsDataCard(items, onClearAll)
            }

            // === 关于 ===
            item {
                SettingsAboutCard()
            }
        }
    }
}

// ============== 下面是拆分出的卡片组件，保持原本逻辑 ==============

@Composable
private fun SettingsAiCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    var apiKey by remember { mutableStateOf(AppSettings.apiKey) }
    var reveal by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var autoRecognize by remember { mutableStateOf(AppSettings.autoRecognize) }

    SoftCard {
        SectionHeader(title = "AI 识图录入", subtitle = "拍小票自动填表")
        Spacer(Modifier.height(12.dp))
        Text("填入 DeepSeek API Key 后，在记录页拍照或选图，会自动识别品牌、品名、规格和价格。Key 只保存在本机。", style = MaterialTheme.typography.bodyMedium, color = PearlSoft)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it.trim() },
            placeholder = { Text("sk-...", color = Outline) },
            singleLine = true,
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Caramel,
                unfocusedBorderColor = Outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                cursorColor = Caramel,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallButton(text = if (reveal) "隐藏" else "显示") { haptics.tick(); reveal = !reveal }
            Spacer(Modifier.width(8.dp))
            SmallButton(text = "保存") { haptics.confirm(); AppSettings.apiKey = apiKey; testResult = "已保存" }
            Spacer(Modifier.width(8.dp))
            SmallButton(text = if (testing) "测试中…" else "测试连接", enabled = !testing) {
                haptics.tick(); AppSettings.apiKey = apiKey; testing = true; testResult = null
                scope.launch {
                    testResult = DeepSeekClient.testConnection(apiKey).fold(
                        onSuccess = { "连接正常：$it" },
                        onFailure = { it.message ?: "连接失败" },
                    )
                    testing = false
                }
            }
            if (testing) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Caramel)
            }
        }
        if (testResult != null) {
            Spacer(Modifier.height(10.dp))
            Text(testResult!!, style = MaterialTheme.typography.labelMedium, color = if (testResult!!.contains("正常") || testResult == "已保存") Matcha else Strawberry)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("记录时优先用 AI 填表", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("开启后，选完图片直接进识别流程", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = autoRecognize,
                onCheckedChange = { haptics.tick(); autoRecognize = it; AppSettings.autoRecognize = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Caramel),
            )
        }
    }
}

@Composable
private fun SettingsDataCard(items: List<Drink>, onClearAll: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    
    // 清空数据状态
    var confirmClear by remember { mutableStateOf(false) }
    
    // WebDAV 状态
    val prefs = remember { context.getSharedPreferences("webdav_prefs", android.content.Context.MODE_PRIVATE) }
    var url by remember { mutableStateOf(prefs.getString("url", "") ?: "") }
    var user by remember { mutableStateOf(prefs.getString("user", "") ?: "") }
    var pass by remember { mutableStateOf(prefs.getString("pass", "") ?: "") }
    var isWorking by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    SoftCard {
        SectionHeader(title = "我的数据", subtitle = "全部存在本机")
        Spacer(Modifier.height(12.dp))
        
        // === 数据统计 ===
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox("记录", "${items.size}", Caramel, Modifier.weight(1f))
            StatBox("照片", "${items.count { it.photoPath != null }}", Taro, Modifier.weight(1f))
            StatBox("品牌", "${items.map { it.brand }.distinct().size}", Matcha, Modifier.weight(1f))
        }
        
        Spacer(Modifier.height(16.dp))
        
        // === WebDAV 云端同步 ===
        Text(
            text = "云端同步 (WebDAV)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim(); prefs.edit().putString("url", url).apply() },
            placeholder = { Text("WebDAV 地址 (如 https://dav.jianguoyun.com/dav/)", color = Outline) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Caramel, unfocusedBorderColor = Outline),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = user,
                onValueChange = { user = it.trim(); prefs.edit().putString("user", user).apply() },
                placeholder = { Text("账号", color = Outline) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Caramel, unfocusedBorderColor = Outline),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it.trim(); prefs.edit().putString("pass", pass).apply() },
                placeholder = { Text("密码/应用密码", color = Outline) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Caramel, unfocusedBorderColor = Outline),
                modifier = Modifier.weight(1f),
            )
        }
        
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // 备份按钮
            Box(modifier = Modifier.weight(1f)) {
                GradientButton(
                    text = if (isWorking) "处理中…" else "立即备份",
                    onClick = {
                        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                            statusMsg = "请先填写完整的 WebDAV 配置"
                            return@GradientButton
                        }
                        haptics.tick()
                        isWorking = true
                        statusMsg = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val json = Json { encodeDefaults = true }
                                    val jsonStr = json.encodeToString(items)
                                    val res = WebDavClient.upload(url, user, pass, jsonStr.toByteArray())
                                    withContext(Dispatchers.Main) {
                                        if (res.isSuccess) {
                                            statusMsg = "✅ 备份成功！"
                                            haptics.confirm()
                                        } else {
                                            statusMsg = "❌ ${res.exceptionOrNull()?.message}"
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { statusMsg = "❌ 序列化失败" }
                                }
                            }
                            isWorking = false
                        }
                    },
                    colors = listOf(Caramel, Color(0xFFE2A776))
                )
            }
            
            // 还原按钮
            Box(modifier = Modifier.weight(1f)) {
                GradientButton(
                    text = if (isWorking) "处理中…" else "从云端还原",
                    onClick = {
                        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
                            statusMsg = "请先填写完整的 WebDAV 配置"
                            return@GradientButton
                        }
                        haptics.tick()
                        isWorking = true
                        statusMsg = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val res = WebDavClient.download(url, user, pass)
                                withContext(Dispatchers.Main) {
                                    if (res.isSuccess) {
                                        try {
                                            val jsonStr = res.getOrThrow()
                                            val json = Json { ignoreUnknownKeys = true }
                                            val restoredList = json.decodeFromString<List<Drink>>(jsonStr)
                                            
                                            // 覆盖本地数据
                                            DrinkRepository(context).replaceAll(restoredList)
                                            statusMsg = "✅ 还原成功！请重新打开 App 查看"
                                            haptics.confirm()
                                        } catch (e: Exception) {
                                            statusMsg = "❌ 数据解析失败"
                                        }
                                    } else {
                                        statusMsg = "❌ ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            }
                            isWorking = false
                        }
                    },
                    colors = listOf(Taro, Color(0xFFCBBAF5))
                )
            }
        }
        
        if (statusMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = statusMsg!!, 
                style = MaterialTheme.typography.labelMedium, 
                color = if (statusMsg!!.startsWith("✅")) Matcha else Strawberry
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 浅色分割线
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Outline.copy(alpha = 0.2f)))
        
        Spacer(Modifier.height(16.dp))
        
        // === 清空数据按钮 ===
        GradientButton(
            text = if (confirmClear) "再点一次确认清空" else "清空所有本地记录",
            onClick = {
                if (confirmClear) {
                    haptics.confirm()
                    onClearAll()
                    confirmClear = false
                } else {
                    haptics.tick()
                    confirmClear = true
                }
            },
            colors = if (confirmClear) {
                listOf(Color(0xFFE53935), Color(0xFFEF5350))
            } else {
                listOf(Strawberry, Color(0xFFFFB3C6))
            },
        )
    }
}
@Composable
private fun SettingsAboutCard() {
    val context = LocalContext.current
    val crash = remember { DrinkApp.lastCrash(context) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SoftCard {
            SectionHeader(title = "关于", subtitle = "饮品日记")
            Spacer(Modifier.height(10.dp))
            InfoLine("版本", "1.1.0")
            InfoLine("AI 模型", "deepseek-flash（视觉）")
            InfoLine("数据存储", "本机文件，不上传")
            InfoLine("玻璃材质", "chrisbanes/haze")
        }
        
        if (crash != null) {
            SoftCard(shadowColor = Strawberry) {
                SectionHeader(title = "上次出错记录", subtitle = "反馈问题时可以复制这段")
                Spacer(Modifier.height(10.dp))
                Text(crash.take(1200), style = MaterialTheme.typography.labelMedium, color = PearlSoft)
                Spacer(Modifier.height(12.dp))
                SmallButton(text = "清除") { DrinkApp.clearCrash(context) }
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = if (enabled) PearlSoft else Outline)
    }
}

@Composable
private fun StatBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
