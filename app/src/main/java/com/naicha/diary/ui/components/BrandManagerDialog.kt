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
import kotlin.math.min

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
    val allBrands = remember(customBrands) { Catalog.brands + customBrands }

    var selectedBrands by remember { mutableStateOf(setOf<String>()) }
    var showMenu by remember { mutableStateOf(false) }

    // 裁剪状态
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCircleCrop by remember { mutableStateOf(true) }
    
    var showNameInput by remember { mutableStateOf(false) }
    var newBrandName by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropUri = uri
            context.contentResolver.openInputStream(uri)?.use { 
                originalBitmap = BitmapFactory.decodeStream(it)
            }
        }
    }

    // 1. 裁剪界面
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
                        Text("裁切图片", color = Color.White, fontSize = 18.sp)
                        IconButton(onClick = { showNameInput = true }) { Icon(Icons.Default.Check, "确定", tint = Color.Green) }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val bmp = originalBitmap!!
                        val side = min(bmp.width, bmp.height)
                        val x = (bmp.width - side) / 2
                        val y = (bmp.height - side) / 2
                        val squareBmp = Bitmap.createBitmap(bmp, x, y, side, side)
                        
                        Image(
                            bitmap = squareBmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(if (isCircleCrop) CircleShape else RoundedCornerShape(16.dp))
                        )
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

    // 2. 输入名称弹窗
    if (showNameInput && originalBitmap != null) {
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
                        val bmp = originalBitmap!!
                        val side = min(bmp.width, bmp.height)
                        val x = (bmp.width - side) / 2
                        val y = (bmp.height - side) / 2
                        val cropped = Bitmap.createBitmap(bmp, x, y, side, side)
                        
                        val file = File(context.filesDir, "brand_${UUID.randomUUID()}.jpg")
                        FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                        
                        val newBrand = Brand(
                            name = newBrandName.trim(),
                            category =DrinkCategory.MilkTea,
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

    // 3. 主界面
    if (cropUri == null && !showNameInput) {
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f).clip(RoundedCornerShape(24.dp)),
                color = Color(0xFFF4F5F0)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
                        Text("管理品牌", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Default.Add, "添加品牌")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "菜单") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("隐藏选中") }, onClick = {
                                    BrandStore.toggleHide(context, selectedBrands, true)
                                    selectedBrands = emptySet()
                                    showMenu = false
                                })
                                DropdownMenuItem(text = { Text("显示全部") }, onClick = {
                                    BrandStore.toggleHide(context, allBrands.map { it.name }.toSet(), false)
                                    showMenu = false
                                })
                            }
                        }
                    }

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
