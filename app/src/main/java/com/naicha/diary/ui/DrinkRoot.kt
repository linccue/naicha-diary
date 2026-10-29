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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeTint
import androidx.core.content.FileProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
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

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("首页", Icons.Filled.Home),
    TabItem("饮品墙", Icons.Filled.Favorite),
    TabItem("统计", Icons.Filled.DateRange),
    TabItem("我的", Icons.Filled.Person),
)

private const val RECORD_SLOT = 2

const val dailyGoal = 1

@Composable
fun DrinkRoot() {
    val repository = app.repository
    val items by repository.items.collectAsState()
    val context = LocalContext.current
    val hazeState = remember { HazeState() }

    var tab by remember { mutableStateOf(0) }
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
                // 趁系统相机还没收回权限，立刻把照片复制到自己的地盘
                context.contentResolver.openInputStream(target!!)?.use { input ->
                    safeFile.outputStream().use { output -> input.copyTo(output) }
                }
                // 生成一个新的绝对安全的 Uri 传进去
                val safeUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    safeFile
                )
                if (pickForAi) aiImage = safeUri else photoImage = safeUri
            } catch (e: Exception) {
                e.printStackTrace()
                // 如果复制失败，退而求其次传原 Uri
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F0))) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
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
                    onOpenWall = { scope.launch { pagerState.animateScrollToPage(1) } },
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
            hazeState = hazeState,
            pagerState = pagerState,
            onSelect = { index ->
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
                .padding(horizontal = 14.dp, vertical = 16.dp),
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
                scope.launch { pagerState.animateScrollToPage(3) }
            },
            onPick = { forAi, fromCamera -> requestPick(forAi, fromCamera, openSheet = false) },
            onAiImageConsumed = { aiImage = null },
            onPhotoImageConsumed = { photoImage = null },
            onSave = { drink ->
                // 通知桌面小部件刷新数据
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

@Composable
private fun RecordFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fab")
    transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "fabPulse",
    )

    BouncyBox(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Caramel.copy(alpha = 0.65f),
                    spotColor = Caramel.copy(alpha = 0.65f),
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFD89B69), Caramel, Color(0xFFB0724A))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "记一杯",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun MilkBottomBar(
    hazeState: HazeState, // 保留参数防止报错，但不再使用毛玻璃
    pagerState: PagerState,
    onSelect: (Int) -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 整体的纯白悬浮胶囊
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧 2 个图标
        BottomBarIcon(tabs[0].icon, pagerState.currentPage == 0) { onSelect(0) }
        BottomBarIcon(tabs[1].icon, pagerState.currentPage == 1) { onSelect(1) }

        // 👇 正中间的纯黑加号按钮
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f)
                )
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

        // 右侧 2 个图标
        BottomBarIcon(tabs[2].icon, pagerState.currentPage == 2) { onSelect(2) }
        BottomBarIcon(tabs[3].icon, pagerState.currentPage == 3) { onSelect(3) }
    }
}

// 提取出来的单个图标组件（带选中动画）
@Composable
private fun BottomBarIcon(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 选中时变黑，未选中时是浅灰色
    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1C1C1E) else Color(0xFFB0B0B0),
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "iconTint"
    )
    // 选中时背后浮现极浅的灰色圆角背景
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) Color(0xFFF4F5F0) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "bgColor"
    )

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

// ================= 保留的相机 URI 生成工具函数 =================
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
