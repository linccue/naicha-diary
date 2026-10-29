package com.naicha.diary.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.naicha.diary.app
import com.naicha.diary.data.AppSettings
import com.naicha.diary.data.Drink
import com.naicha.diary.notify.LiveUpdateNotifier
import com.naicha.diary.ui.components.BouncyBox
import com.naicha.diary.ui.screens.HomeScreen
import com.naicha.diary.ui.screens.RecordSheet
import com.naicha.diary.ui.screens.SettingsContent
import com.naicha.diary.ui.screens.StatsScreen
import com.naicha.diary.ui.screens.DrinkDetailSheet
import com.naicha.diary.ui.screens.WallScreen
import com.naicha.diary.ui.theme.Caramel
import com.naicha.diary.ui.theme.PearlSoft
import com.naicha.diary.util.TimeUtil
import com.naicha.diary.util.rememberHaptics
import kotlinx.coroutines.launch
import java.io.File
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import com.naicha.diary.widget.DiaryWidget
import dev.chrisbanes.haze.HazeState

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("首页", Icons.Filled.Home),
    TabItem("饮品墙", Icons.Filled.Favorite),
    TabItem("统计", Icons.Filled.DateRange),
    TabItem("我的", Icons.Filled.Person),
)

const val dailyGoal = 1

@Composable
fun DrinkRoot() {
    val repository = app.repository
    val items by repository.items.collectAsState()
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    var showRecord by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Drink?>(null) }
    var detail by remember { mutableStateOf<Drink?>(null) }

    var aiImage by remember { mutableStateOf<Uri?>(null) }
    var photoImage by remember { mutableStateOf<Uri?>(null) }
    var pickForAi by remember { mutableStateOf(true) }
    var openSheetAfterPick by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val target = pendingCameraUri
        pendingCameraUri = null
        if (success) {
            val safeFile = java.io.File(context.filesDir, "cam_safe_${System.currentTimeMillis()}.jpg")
            try {
                context.contentResolver.openInputStream(target!!)?.use { input ->
                    safeFile.outputStream().use { output -> input.copyTo(output) }
                }
                val safeUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
                if (pickForAi) aiImage = safeUri else photoImage = safeUri
            } catch (e: Exception) {
                e.printStackTrace()
                if (pickForAi) aiImage = target else photoImage = target
            }
            
            if (openSheetAfterPick) {
                editing = null
                showRecord = true
            }
        } else {
            if (pickForAi) aiImage = null else photoImage = null
            target?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
        openSheetAfterPick = false
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            if (pickForAi) aiImage = uri else photoImage = uri
            if (openSheetAfterPick) {
                editing = null
                showRecord = true
            }
        }
        openSheetAfterPick = false
    }

    fun requestPick(forAi: Boolean, fromCamera: Boolean, openSheet: Boolean = false) {
        pickForAi = forAi
        openSheetAfterPick = openSheet
        if (fromCamera) {
            val uri = createCameraUri(context)
            if (uri == null) {
                openSheetAfterPick = false
                return
            }
            pendingCameraUri = uri
            if (forAi) aiImage = uri else photoImage = uri
            runCatching { cameraLauncher.launch(uri) }
        } else {
            runCatching {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val todayCount = items.count { TimeUtil.isToday(it.timestamp) }
    val latest = items.maxByOrNull { it.timestamp }

    LaunchedEffect(todayCount, latest?.id) {
        if (todayCount > 0) {
            LiveUpdateNotifier.show(context, todayCount, dailyGoal, latest)
        } else {
            LiveUpdateNotifier.dismiss(context)
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // 👇 核心改动 1：用一个独立的本地状态记录当前选中的 Tab
    var currentTab by remember { mutableIntStateOf(0) }

    // 监听滑动，如果通过手势滑动，同步更新 currentTab
    LaunchedEffect(pagerState.currentPage) {
        if (currentTab != pagerState.currentPage) {
            currentTab = pagerState.currentPage
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0))) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> HomeScreen(
                    items = items,
                    onRecord = {
                        editing = null
                        aiImage = null
                        showRecord = true
                    },
                    onAiRecord = {
                        if (AppSettings.hasApiKey) {
                            requestPick(forAi = true, fromCamera = true, openSheet = true)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        }
                    },
                    onOpenWall = { 
                        currentTab = 1 // 👈 立即切换状态
                        scope.launch { pagerState.animateScrollToPage(1) } 
                    },
                    onOpenDetail = { detail = it },
                )
                1 -> WallScreen(
                    items = items,
                    onOpenDetail = { detail = it },
                )
                2 -> StatsScreen(items = items)
                else -> SettingsContent(
                    items = items,
                    onClearAll = { repository.clearAll() },
                )
            }
        }

        MilkBottomBar(
            currentTab = currentTab, // 👈 传入当前 Tab
            onSelect = { index ->
                currentTab = index // 👈 点击瞬间直接变色，不等滑动
                scope.launch { pagerState.animateScrollToPage(index) }
            },
            onRecord = {
                editing = null
                aiImage = null
                showRecord = true
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }

    if (showRecord) {
        RecordSheet(
            initial = editing,
            aiImage = aiImage,
            photoImage = photoImage,
            onDismiss = {
                showRecord = false
                editing = null
                aiImage = null
                photoImage = null
            },
            onOpenSettings = {
                showRecord = false
                editing = null
                aiImage = null
                currentTab = 3 // 👈 立即切换
                scope.launch { pagerState.animateScrollToPage(3) }
            },
            onPick = { forAi, fromCamera -> requestPick(forAi, fromCamera, openSheet = false) },
            onAiImageConsumed = { aiImage = null },
            onPhotoImageConsumed = { photoImage = null },
            onSave = { drink ->
                val manager = AppWidgetManager.getInstance(context.applicationContext)
                val component = ComponentName(context, DiaryWidget::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isNotEmpty()) {
                    val intent = android.content.Intent(context, DiaryWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                    context.sendBroadcast(intent)
                }
                if (editing == null) repository.add(drink) else repository.update(drink)
                showRecord = false
                editing = null
                aiImage = null
                photoImage = null
            },
            onDelete = { drink ->
                repository.remove(drink.id)
                showRecord = false
                editing = null
                aiImage = null
                photoImage = null
            },
        )
    }

    val current = detail
    if (current != null) {
        DrinkDetailSheet(
            tea = current,
            onDismiss = { detail = null },
            onEdit = {
                editing = current
                detail = null
                showRecord = true
            },
            onDelete = {
                repository.remove(current.id)
                detail = null
            },
        )
    }
}

// ================= 新版底栏（无阴影 + 瞬间变色） =================
@Composable
private fun MilkBottomBar(
    currentTab: Int, // 👈 接收传入的状态
    onSelect: (Int) -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarIcon(tabs[0].icon, currentTab == 0) { onSelect(0) }
        BottomBarIcon(tabs[1].icon, currentTab == 1) { onSelect(1) }

        // 正中间的纯黑加号按钮
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .clickable { onRecord() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "记一杯",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        BottomBarIcon(tabs[2].icon, currentTab == 2) { onSelect(2) }
        BottomBarIcon(tabs[3].icon, currentTab == 3) { onSelect(3) }
    }
}

@Composable
private fun BottomBarIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 👇 核心改动 2：去掉了 animateColorAsState，直接瞬间变色，绝对不会闪烁
    val iconTint = if (isSelected) Color(0xFF1C1C1E) else Color(0xFFB0B0B0)
    val bgColor = if (isSelected) Color(0xFFF4F5F0) else Color.Transparent

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
    }
}

// ================= 相机 URI 生成工具函数 =================
private fun createCameraUri(context: Context): Uri? = runCatching {
    val name = "drink_${System.currentTimeMillis()}.jpg"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/饮品日记",
            )
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } else {
        val dir = File(context.cacheDir, "capture").apply { mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, name))
    }
}.getOrNull()
