package com.naicha.diary.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.naicha.diary.data.Brand
import com.naicha.diary.data.BrandStore
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.ui.theme.Caramel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

private const val ORDER_PREFS_NAME = "naicha_brand_order_prefs"
private const val ORDER_KEY = "brand_order"
private const val ORDER_SEPARATOR = "\u0001"

private fun loadSavedBrandOrder(context: Context): List<String> {
    val raw = context.getSharedPreferences(ORDER_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(ORDER_KEY, null) ?: return emptyList()
    if (raw.isEmpty()) return emptyList()
    return raw.split(ORDER_SEPARATOR).filter { it.isNotEmpty() }
}

private fun saveBrandOrder(context: Context, order: List<String>) {
    context.getSharedPreferences(ORDER_PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(ORDER_KEY, order.joinToString(ORDER_SEPARATOR))
        .apply()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrandManagerDialog(
    onDismiss: () -> Unit,
    onBrandSelected: (Brand) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    LaunchedEffect(Unit) { BrandStore.init(context) }

    val customBrands by BrandStore.customBrands.collectAsState()
    val hiddenBrands by BrandStore.hiddenBrands.collectAsState()

    val allBrands: List<Brand> = remember(customBrands) { Catalog.brands + customBrands }

    var savedOrder: List<String> by remember { mutableStateOf(loadSavedBrandOrder(context)) }

    val orderedAllBrands: List<Brand> = remember(allBrands, savedOrder) {
        if (savedOrder.isEmpty()) {
            allBrands
        } else {
            val map = allBrands.associateBy { brand -> brand.name }
            val result = ArrayList<Brand>(savedOrder.size)
            savedOrder.forEach { name -> map[name]?.let { result.add(it) } }
            allBrands.forEach { b -> if (result.none { it.name == b.name }) result.add(b) }
            result
        }
    }

    val currentOrder: List<Brand> by rememberUpdatedState(orderedAllBrands)
    val currentSaved: List<String> by rememberUpdatedState(savedOrder)

    var selectedBrands by remember { mutableStateOf(setOf<String>()) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var isSortMode by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // ★ 边缘自动滚动速度（> 0 = 向下滚，< 0 = 向上滚，0 = 停止）
    var autoScrollSpeed by remember { mutableStateOf(0f) }

    // ★ 拖拽中的边缘自动滚动协程（每 16ms 检查一次）
    LaunchedEffect(isSortMode) {
        if (!isSortMode) return@LaunchedEffect
        while (true) {
            val speed = autoScrollSpeed
            if (speed != 0f && draggingIndex != null) {
                                val consumed = gridState.dispatchRawDelta(speed)
                if (consumed != 0f) {
                    // 补偿网格滚动导致的 item 位置变化，让被拖 item 保持跟手
                    dragOffset += Offset(0f, consumed)
                }
            }
            delay(16)
        }
    }

    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCircleCrop by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 3f)
        offset += panChange
    }

    var showNameInput by remember { mutableStateOf(false) }
    var newBrandName by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropUri = uri
            context.contentResolver.openInputStream(uri)?.use {
                originalBitmap = BitmapFactory.decodeStream(it)
            }
            scale = 1f
            offset = Offset.Zero
        }
    }

    if (cropUri != null && originalBitmap != null) {
        Dialog(onDismissRequest = { cropUri = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { cropUri = null }) { Icon(Icons.Default.Close, "取消", tint = Color.White) }
                        Text("双指缩放 / 拖拽移动", color = Color.White, fontSize = 16.sp)
                        IconButton(onClick = { showNameInput = true }) { Icon(Icons.Default.Check, "确定", tint = Color.Green) }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(280.dp).clip(if (isCircleCrop) CircleShape else RoundedCornerShape(16.dp))) {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                                    .transformable(transformableState)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        Button(
                            onClick = { isCircleCrop = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isCircleCrop) Caramel else Color.Gray)
                        ) { Text("圆形") }
                        Spacer(Modifier.width(16.dp))
                        Button(
                            onClick = { isCircleCrop = false },
                            colors = ButtonDefaults.buttonColors(containerColor = if (!isCircleCrop) Caramel else Color.Gray)
                        ) { Text("方形") }
                    }
                }
            }
        }
    }

    if (showNameInput) {
        AlertDialog(
            onDismissRequest = { showNameInput = false },
            title = { Text("输入品牌名称") },
            text = {
                OutlinedTextField(
                    value = newBrandName,
                    onValueChange = { newBrandName = it },
                    placeholder = { Text("例如：星巴克") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newBrandName.isNotBlank()) {
                        scope.launch {
                            val frameSizePx = with(density) { 280.dp.toPx() }
                            val finalBitmap = cropImageToCenter(originalBitmap!!, scale, offset, frameSizePx)
                            val file = File(context.filesDir, "brand_${UUID.randomUUID()}.jpg")
                            FileOutputStream(file).use { finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                            val newBrand = Brand(
                                name = newBrandName.trim(),
                                category = DrinkCategory.MilkTea,
                                color = 0xFFC98A5B,
                                badge = newBrandName.trim().take(1),
                                logoPath = file.absolutePath,
                                isCircle = isCircleCrop
                            )
                                                        BrandStore.addBrand(context, newBrand)
                            
                            // 👇 新增：让新添加的品牌排在第四行第一个
                            val currentNames = orderedAllBrands.map { it.name }.toMutableList()
                            currentNames.remove(newBrand.name) // 防重复
                            // 第四行第一个的索引是 15 (0-4第一行, 5-9第二行, 10-14第三行, 15是第四行第一个)
                            val targetIndex = 15
                            if (currentNames.size >= targetIndex) {
                                currentNames.add(targetIndex, newBrand.name)
                            } else {
                                // 如果总数还不到第四行，就直接追加到末尾
                                currentNames.add(newBrand.name)
                            }
                            savedOrder = currentNames
                            saveBrandOrder(context, currentNames)
                            cropUri = null
                            showNameInput = false
                            newBrandName = ""
                            onDismiss()
                        }
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showNameInput = false }) { Text("取消") }
            }
        )
    }

    if (cropUri == null && !showNameInput) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f).clip(RoundedCornerShape(24.dp)),
                color = Color(0xFFF4F5F0)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }

                        if (showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("搜索品牌", fontSize = 14.sp) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        } else {
                            Text(
                                text = if (isSortMode) "拖拽排序" else "管理品牌",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        IconButton(onClick = {
                            showSearch = !showSearch
                            if (!showSearch) searchQuery = ""
                            if (showSearch) isSortMode = false
                        }) {
                            Icon(Icons.Default.Search, "搜索", tint = if (showSearch) Caramel else Color.Black)
                        }

                        IconButton(onClick = {
                            if (isSortMode) {
                                isSortMode = false
                                draggingIndex = null
                                dragOffset = Offset.Zero
                                autoScrollSpeed = 0f
                                saveBrandOrder(context, savedOrder)
                            } else {
                                isSortMode = true
                                showSearch = false
                                searchQuery = ""
                            }
                        }) {
                            Icon(
                                imageVector = if (isSortMode) Icons.Default.Check else Icons.Default.List,
                                contentDescription = if (isSortMode) "完成排序" else "排序",
                                tint = if (isSortMode) Caramel else Color.Black
                            )
                        }

                        IconButton(onClick = {
                            if (!isSortMode) {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        }) {
                            Icon(Icons.Default.Add, "添加品牌", tint = if (isSortMode) Color.Gray else Color.Black)
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "菜单") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("隐藏选中") }, onClick = {
                                    if (selectedBrands.isNotEmpty()) {
                                        BrandStore.toggleHide(context, selectedBrands, true)
                                        selectedBrands = emptySet()
                                    }
                                    showMenu = false
                                })
                                DropdownMenuItem(text = { Text("显示选中") }, onClick = {
                                    if (selectedBrands.isNotEmpty()) {
                                        BrandStore.toggleHide(context, selectedBrands, false)
                                        selectedBrands = emptySet()
                                    }
                                    showMenu = false
                                })
                                DropdownMenuItem(text = { Text("显示全部") }, onClick = {
                                    BrandStore.toggleHide(context, allBrands.map { b -> b.name }.toSet(), false)
                                    showMenu = false
                                })
                                                                HorizontalDivider(color = Color.LightGray) // 加一条分割线
                                DropdownMenuItem(
                                    text = { Text("删除选中", color = Color.Red) },
                                    onClick = {
                                        if (selectedBrands.isNotEmpty()) {
                                            scope.launch {
                                                // 💡 你的 BrandStore 里删除方法可能不叫 deleteBrands
                                                // 如果这行报错，请把 deleteBrands 换成你 BrandStore 里实际的方法名
                                                BrandStore.deleteBrands(context, selectedBrands)
                                                selectedBrands = emptySet()
                                            }
                                        }
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    val filteredBrands: List<Brand> = if (isSortMode) {
                        orderedAllBrands
                    } else {
                        orderedAllBrands.filter { brand ->
                            searchQuery.isBlank() || brand.name.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().pointerInput(isSortMode) {
                            if (!isSortMode) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { touchOffset ->
                                    val item = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        touchOffset.x in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                                        touchOffset.y in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
                                    }
                                    if (item != null) {
                                        draggingIndex = item.index
                                        dragOffset = Offset.Zero
                                        autoScrollSpeed = 0f
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val fromIndex: Int = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                    dragOffset += dragAmount

                                    val currentItem = gridState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { info -> info.index == fromIndex }
                                        ?: return@detectDragGesturesAfterLongPress

                                    val centerX = currentItem.offset.x + currentItem.size.width / 2f + dragOffset.x
                                    val centerY = currentItem.offset.y + currentItem.size.height / 2f + dragOffset.y

                                    // ★ 边缘自动滚动检测
                                    val gridHeight = size.height.toFloat()
                                    val edgeZone = 80.dp.toPx()
                                    val maxSpeed = 18f
                                    autoScrollSpeed = when {
                                        centerY < edgeZone -> {
                                            val ratio = (1f - centerY / edgeZone).coerceIn(0f, 1f)
                                            -maxSpeed * ratio
                                        }
                                        centerY > gridHeight - edgeZone -> {
                                            val ratio = (1f - (gridHeight - centerY) / edgeZone).coerceIn(0f, 1f)
                                            maxSpeed * ratio
                                        }
                                        else -> 0f
                                    }

                                    val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                        info.index != fromIndex &&
                                        centerX in info.offset.x.toFloat()..(info.offset.x + info.size.width).toFloat() &&
                                        centerY in info.offset.y.toFloat()..(info.offset.y + info.size.height).toFloat()
                                    } ?: return@detectDragGesturesAfterLongPress

                                    val toIndex: Int = targetItem.index
                                    val newList: ArrayList<Brand> = ArrayList<Brand>(currentOrder)
                                    if (fromIndex in newList.indices && toIndex in newList.indices) {
                                        val moved: Brand = newList.removeAt(fromIndex)
                                        newList.add(toIndex, moved)
                                        val newOrder: List<String> = newList.map { brand -> brand.name }
                                        savedOrder = newOrder
                                        draggingIndex = toIndex
                                        dragOffset -= Offset(
                                            (targetItem.offset.x - currentItem.offset.x).toFloat(),
                                            (targetItem.offset.y - currentItem.offset.y).toFloat()
                                        )
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffset = Offset.Zero
                                    autoScrollSpeed = 0f
                                    saveBrandOrder(context, currentSaved)
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffset = Offset.Zero
                                    autoScrollSpeed = 0f
                                    saveBrandOrder(context, currentSaved)
                                }
                            )
                        }
                    ) {
                        itemsIndexed(filteredBrands, key = { _, brand: Brand -> brand.name }) { index, brand ->
                            val isSelected = selectedBrands.contains(brand.name)
                            val isHidden = hiddenBrands.contains(brand.name)
                            val isDragging = index == draggingIndex

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer {
                                        if (isDragging) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            shadowElevation = 12.dp.toPx()
                                            scaleX = 1.08f
                                            scaleY = 1.08f
                                        }
                                    }
                                    .alpha(if (isHidden) 0.3f else 1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isDragging -> Color.White
                                            isSelected -> Color.LightGray
                                            else -> Color.Transparent
                                        }
                                    )
                                    .combinedClickable(
                                        enabled = !isSortMode,
                                        onClick = {
                                            if (selectedBrands.isNotEmpty()) {
                                                selectedBrands = if (isSelected) selectedBrands - brand.name else selectedBrands + brand.name
                                            } else {
                                                onBrandSelected(brand)
                                                onDismiss()
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSortMode) {
                                                selectedBrands = if (isSelected) selectedBrands - brand.name else selectedBrands + brand.name
                                            }
                                        }
                                    )
                                    .padding(6.dp)
                            ) {
                                BrandBadge(
                                    badge = brand.badge,
                                    color = Color(brand.color),
                                    size = 44.dp,
                                    logo = brand.logo,
                                    logoPath = brand.logoPath,
                                    isCircle = brand.isCircle,
                                    squircle = Catalog.isSquircle(brand.name)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = brand.name,
                                    fontSize = 11.sp,
                                    color = if (isHidden) Color.Gray else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cropImageToCenter(bitmap: Bitmap, scale: Float, offset: Offset, frameSizePx: Float): Bitmap {
    val output = Bitmap.createBitmap(frameSizePx.toInt(), frameSizePx.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val ratio = min(frameSizePx / bitmap.width, frameSizePx / bitmap.height)
    val baseWidth = bitmap.width * ratio
    val baseHeight = bitmap.height * ratio
    canvas.save()
    canvas.translate(frameSizePx / 2 + offset.x, frameSizePx / 2 + offset.y)
    canvas.scale(scale, scale)
    canvas.translate(-frameSizePx / 2, -frameSizePx / 2)
    val left = (frameSizePx - baseWidth) / 2
    val top = (frameSizePx - baseHeight) / 2
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + baseWidth, top + baseHeight), paint)
    canvas.restore()
    return output
}
