package com.naicha.diary.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.naicha.diary.data.Brand
import com.naicha.diary.data.BrandStore
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.ui.theme.Caramel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrandManagerDialog(
    onDismiss: () -> Unit,
    onBrandSelected: (Brand) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { BrandStore.init(context) }
    
    val customBrands by BrandStore.customBrands.collectAsState()
    val hiddenBrands by BrandStore.hiddenBrands.collectAsState()
    
    // 合并所有品牌
    val allBrands = remember(customBrands) { Catalog.brands + customBrands }

    // 状态管理
    var selectedBrands by remember { mutableStateOf(setOf<String>()) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // 裁剪状态
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCircleCrop by remember { mutableStateOf(true) }
    
    // 缩放、移动状态，以及捕获画面的 GraphicsLayer
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val graphicsLayer = rememberGraphicsLayer()
    
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

    // ================= 1. 裁剪预览界面 =================
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

                    // 预览区域
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(if (isCircleCrop) CircleShape else RoundedCornerShape(16.dp))
                                // 👇 将当前 Box 内的画面捕获到 graphicsLayer
                                .drawWithContent {
                                    graphicsLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(graphicsLayer)
                                }
                        ) {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
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

    // ================= 2. 输入品牌名称 =================
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
                        // 👇 直接截取预览框里的画面（所见即所得）
                        val finalBitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        
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
                        
                        cropUri = null
                        showNameInput = false
                        newBrandName = ""
                        onDismiss()
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showNameInput = false }) { Text("取消") }
            }
        )
    }

    // ================= 3. 主管理界面 =================
    if (cropUri == null && !showNameInput) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f).clip(RoundedCornerShape(24.dp)),
                color = Color(0xFFF4F5F0)
            ) {
                Column {
                    // 顶部工具栏
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
                            Text("管理品牌", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                        
                        // 搜索按钮
                        IconButton(onClick = { 
                            showSearch = !showSearch 
                            if (!showSearch) searchQuery = ""
                        }) {
                            Icon(Icons.Default.Search, "搜索", tint = if (showSearch) Caramel else Color.Black)
                        }

                        // 加号（添加品牌）
                        IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.Add, "添加品牌")
                        }
                        
                        // 三点菜单
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "菜单") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("隐藏选中") },
                                    onClick = {
                                        if (selectedBrands.isNotEmpty()) {
                                            BrandStore.toggleHide(context, selectedBrands, true)
                                            selectedBrands = emptySet()
                                        }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("显示选中") },
                                    onClick = {
                                        if (selectedBrands.isNotEmpty()) {
                                            BrandStore.toggleHide(context, selectedBrands, false)
                                            selectedBrands = emptySet()
                                        }
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("显示全部") }, // 👈 恢复显示全部
                                    onClick = {
                                        BrandStore.toggleHide(context, allBrands.map { it.name }.toSet(), false)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 品牌网格（5列，支持搜索，隐藏的变透明）
                    val filteredBrands = allBrands.filter { brand ->
                        searchQuery.isBlank() || brand.name.contains(searchQuery, ignoreCase = true)
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBrands) { brand ->
                            val isSelected = selectedBrands.contains(brand.name)
                            val isHidden = hiddenBrands.contains(brand.name) // 👈 判断是否隐藏

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .alpha(if (isHidden) 0.3f else 1f) // 👈 隐藏的透明度变低
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color.LightGray else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedBrands.isNotEmpty()) {
                                                selectedBrands = if (isSelected) selectedBrands - brand.name else selectedBrands + brand.name
                                            } else {
                                                onBrandSelected(brand)
                                                onDismiss()
                                            }
                                        },
                                        onLongClick = {
                                            selectedBrands = if (isSelected) selectedBrands - brand.name else selectedBrands + brand.name
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
                                    color = if (isHidden) Color.Gray else Color.Black, // 隐藏文字也变灰
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
