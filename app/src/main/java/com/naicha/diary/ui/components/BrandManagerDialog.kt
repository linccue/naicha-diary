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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    // 初始化 BrandStore
    LaunchedEffect(Unit) { BrandStore.init(context) }
    
    val customBrands by BrandStore.customBrands.collectAsState()
    val hiddenBrands by BrandStore.hiddenBrands.collectAsState()
    
    // 所有品牌合并
    val allBrands = remember(customBrands) { Catalog.brands + customBrands }

    // 状态
    var selectedBrands by remember { mutableStateOf(setOf<String>()) }
    var showMenu by remember { mutableStateOf(false) }

    // 选图和裁剪状态
    var pickingImage by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var isCircleCrop by remember { mutableStateOf(true) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showNameInput by remember { mutableStateOf(false) }
    var newBrandName by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropUri = uri
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            context.contentResolver.openInputStream(uri)?.use { 
                croppedBitmap = BitmapFactory.decodeStream(it, null, options) 
            }
        }
    }

    if (pickingImage) {
        LaunchedEffect(Unit) {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            pickingImage = false
        }
    }

    // === 1. 裁剪预览界面 ===
    if (cropUri != null && croppedBitmap != null) {
        Dialog(onDismissRequest = { cropUri = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 顶部栏
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { cropUri = null }) {
                            Icon(Icons.Default.Close, "取消", tint = Color.White)
                        }
                        Text("裁切图片", color = Color.White, fontSize = 18.sp)
                        IconButton(onClick = { showNameInput = true }) {
                            Icon(Icons.Default.Check, "确定", tint = Color.Green)
                        }
                    }

                    // 预览区域
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = croppedBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(if (isCircleCrop) CircleShape else RoundedCornerShape(16.dp))
                        )
                    }

                    // 底部切换形状
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
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

    // === 2. 输入品牌名弹窗 ===
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
                    if (newBrandName.isNotBlank() && croppedBitmap != null) {
                        // 保存图片到本地
                        val file = File(context.filesDir, "brand_${UUID.randomUUID()}.jpg")
                        FileOutputStream(file).use { croppedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                        val newBrand = Brand(
                            name = newBrandName.trim(),
                            category = DrinkCategory.Tea, // 默认放茶饮
                            color = 0xFFC98A5B,
                            badge = newBrandName.trim().take(1),
                            logoPath = file.absolutePath,
                            isCircle = isCircleCrop
                        )
                        BrandStore.addBrand(context, newBrand)
                        
                        // 关闭所有状态
                        cropUri = null
                        showNameInput = false
                        newBrandName = ""
                        onDismiss() // 关闭主弹窗
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showNameInput = false }) { Text("取消") }
            }
        )
    }

    // === 3. 主管理弹窗 ===
    if (cropUri == null && !showNameInput) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f).clip(RoundedCornerShape(24.dp)),
                color = Color(0xFFF4F5F0)
            ) {
                Column {
                    // 顶部工具栏
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
                        Text("管理品牌", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        
                        // 加号（选图）
                        IconButton(onClick = { pickingImage = true }) {
                            Icon(Icons.Default.Add, "添加品牌")
                        }
                        
                        // 三点菜单
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "菜单") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("隐藏选中") },
                                    onClick = {
                                        BrandStore.toggleHide(context, selectedBrands, true)
                                        selectedBrands = emptySet()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("显示全部") },
                                    onClick = {
                                        BrandStore.toggleHide(context, allBrands.map { it.name }.toSet(), false)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // 品牌网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allBrands) { brand ->
                            if (!hiddenBrands.contains(brand.name)) {
                                val isSelected = selectedBrands.contains(brand.name)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
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
                                        .padding(8.dp)
                                ) {
                                    BrandBadge(
                                        badge = brand.badge,
                                        color = Color(brand.color),
                                        size = 52.dp,
                                        logo = brand.logo,
                                        logoPath = brand.logoPath,
                                        isCircle = brand.isCircle,
                                        squircle = Catalog.isSquircle(brand.name)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = brand.name,
                                        fontSize = 12.sp,
                                        color = Color.Black,
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
}
