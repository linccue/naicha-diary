package com.naicha.diary.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import com.naicha.diary.data.AppSettings
import com.naicha.diary.data.Brand
import com.naicha.diary.data.Catalog
import com.naicha.diary.data.Drink
import com.naicha.diary.data.DrinkCategory
import com.naicha.diary.net.DeepSeekClient
import com.naicha.diary.ui.components.*
import com.naicha.diary.ui.theme.*
import com.naicha.diary.util.PhotoStore
import com.naicha.diary.util.U2NetSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordSheet(
    initial: Drink?,
    aiImage: Uri? = null,
    photoImage: Uri? = null,
    onDismiss: () -> Unit,
    onSave: (Drink) -> Unit,
    onDelete: ((Drink) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
    onPick: (forAi: Boolean, fromCamera: Boolean) -> Unit = { _, _ -> },
    onAiImageConsumed: () -> Unit = {},
    onPhotoImageConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var category by remember { mutableStateOf(DrinkCategory.of(initial?.category ?: DrinkCategory.MilkTea.label)) }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var cupSize by remember { mutableStateOf(initial?.cupSize ?: "中杯") }
    var sugar by remember { mutableStateOf(initial?.sugar ?: "七分糖") }
    var ice by remember { mutableStateOf(initial?.ice ?: "") }
    var toppings by remember { mutableStateOf(initial?.toppings ?: emptyList()) }
    var priceText by remember { mutableStateOf(initial?.price?.takeIf { it > 0 }?.let { formatMoney(it) } ?: "") }
    
    // 👇 这里已经彻底改为 Float
    var rating by remember { mutableStateOf(initial?.rating ?: 0f) }
    
    var mood by remember { mutableStateOf(initial?.mood ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var photoName by remember { mutableStateOf(initial?.photoPath) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(initial?.timestamp ?: System.currentTimeMillis()) }

    var isSegmenting by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiMessage by remember { mutableStateOf<String?>(null) }
    var showInAppCamera by remember { mutableStateOf(false) }

    val brandColor = remember(brand, category) {
        if (brand.isNotBlank()) Color(Catalog.colorOf(brand)) else Color(category.accent)
    }

    fun switchCategory(next: DrinkCategory) {
        category = next
        if (brand.isNotBlank() && Catalog.brandOf(brand).category != next) brand = ""
        val sizes = Catalog.sizesFor(next)
        if (cupSize !in sizes) cupSize = sizes.getOrElse(1) { sizes.first() }
        val sugars = Catalog.sugarOptions(next)
        if (sugar !in sugars && sugar.isNotBlank()) sugar = "七分糖"
        val ices = Catalog.iceOptions(next)
        if (ice !in ices && ice.isNotBlank()) ice = ""
        val extras = Catalog.extraOptions(next)
        toppings = toppings.filter { it in extras }
    }

    fun applyRecognized(drink: Drink) {
        category = drink.categoryType
        brand = drink.brand
        name = drink.name
        cupSize = drink.cupSize
        sugar = drink.sugar
        ice = drink.ice
        toppings = drink.toppings
        if (drink.price > 0) priceText = formatMoney(drink.price)
        if (drink.note.isNotBlank()) note = drink.note
    }

    fun runRecognize(uri: Uri) {
        val key = AppSettings.apiKey
        if (key.isBlank()) { aiMessage = "请先到「我的」里填写 DeepSeek API Key"; return }
        aiLoading = true
        aiMessage = "正在识别…"
        scope.launch {
            val saved = withContext(Dispatchers.IO) { PhotoStore.save(context, uri) }
            if (saved != null) photoName = saved
            DeepSeekClient.recognize(context, uri, key)
                .onSuccess { drink ->
                    applyRecognized(drink)
                    aiMessage = "识别完成，已自动填好，记得核对一下"
                }
                .onFailure { error -> aiMessage = error.message ?: "识别失败，请重试" }
            aiLoading = false
        }
    }

    LaunchedEffect(aiImage) {
        if (aiImage != null) { runRecognize(aiImage); onAiImageConsumed() }
    }

    LaunchedEffect(photoImage) {
        if (photoImage != null) {
            isSegmenting = true
            try {
                var bitmap: Bitmap? = null
                try {
                    val options = BitmapFactory.Options().apply { inMutable = true }
                    context.contentResolver.openInputStream(photoImage)?.use { input ->
                        bitmap = BitmapFactory.decodeStream(input, null, options)
                    }
                } catch (e: Exception) { e.printStackTrace() }

                if (bitmap != null) {
                    previewBitmap = bitmap
                    val savedFile = PhotoStore.fileOf("photo_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(savedFile).use { bitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    photoName = savedFile.name

                    withContext(Dispatchers.IO) {
                        try {
                            U2NetSegmenter.init(context)
                            val result = U2NetSegmenter.removeBackground(bitmap!!)
                            if (result != null) {
                                withContext(Dispatchers.Main) { previewBitmap = result }
                                val newFile = PhotoStore.fileOf("seg_${savedFile.nameWithoutExtension}.png")
                                FileOutputStream(newFile).use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                photoName = newFile.name
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() } finally {
                isSegmenting = false
                onPhotoImageConsumed()
            }
        }
    }

    val sheetBackground by animateColorAsState(
        targetValue = lerp(MaterialTheme.colorScheme.background, brandColor, 0.09f),
        animationSpec = tween(420),
        label = "sheetBackground",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = sheetBackground,
            dragHandle = { SheetHandle() },
        ) {
            Column(modifier = Modifier.fillMaxHeight(0.94f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (initial == null) "记一杯" else "编辑记录",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, "关闭", tint = PearlSoft, modifier = Modifier.size(18.dp))
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        PreviewRow(
                            liquid = softLiquid(brandColor), straw = brandColor, style = cupStyleFor(category),
                            toppingCount = (toppings.size + 3).coerceAtMost(10), brand = brand, name = name,
                            cupSize = cupSize, sugar = sugar, rating = rating, mood = mood,
                            aiLoading = aiLoading, onAiCamera = { onPick(true, true) }, onAiGallery = { onPick(true, false) },
                        )
                    }

                    if (aiMessage != null) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(brandColor.copy(alpha = 0.10f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(text = aiMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    item { CategorySwitcher(current = category, onSelect = { switchCategory(it) }) }

                    item {
                        FieldGroup(title = "品牌", subtitle = "${Catalog.brandsOf(category).size} 个") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Catalog.brandsOf(category).forEach { item ->
                                    BrandChip(brand = item, selected = brand == item.name, onClick = { brand = item.name })
                                     Box(
        modifier = Modifier
            .size(74.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF0F0F0))
            .clickable { showBrandManager = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "添加品牌", tint = Color.Gray)
    }
}

// 在 RecordSheet 的最后（底栏外）加上弹窗：
if (showBrandManager) {
    BrandManagerDialog(
        onDismiss = { showBrandManager = false },
        onBrandSelected = { brand ->
            this.brand = brand.name
            this.category = brand.category
        }
    )
}
                                }
                            }
                        }
                    }

                    item {
                        FieldGroup(title = "喝的是什么") {
                            Column {
                                OutlinedTextField(
                                    value = name, onValueChange = { name = it },
                                    placeholder = { Text("例如：多肉葡萄", color = Outline) },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium, colors = drinkFieldColors(),
                                )
                                Spacer(Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Catalog.commonNames[category].orEmpty().forEach { candidate ->
                                        TagChip(text = candidate, selected = name == candidate, accent = brandColor, onClick = { name = candidate })
                                    }
                                }
                            }
                        }
                    }

                    item { FieldGroup(title = "规格") { ChipRow(options = Catalog.sizesFor(category), selected = cupSize, accent = brandColor) { cupSize = it } } }
                    item { FieldGroup(title = Catalog.sugarLabel(category)) { ChipRow(options = Catalog.sugarOptions(category), selected = sugar, accent = brandColor) { sugar = it } } }
                    item { FieldGroup(title = Catalog.iceLabel(category)) { ChipRow(options = Catalog.iceOptions(category), selected = ice, accent = brandColor) { ice = it } } }
                    
                    item {
                        FieldGroup(title = Catalog.extraLabel(category), subtitle = "可多选") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Catalog.extraOptions(category).forEach { item ->
                                    TagChip(
                                        text = item, selected = toppings.contains(item), accent = brandColor,
                                        onClick = { toppings = if (toppings.contains(item)) toppings - item else toppings + item },
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            FieldGroup(title = "价格", modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = priceText,
                                    onValueChange = { input -> priceText = input.filter { it.isDigit() || it == '.' }.take(7) },
                                    placeholder = { Text("0", color = Outline) }, prefix = { Text("¥ ", color = Caramel) },
                                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = MaterialTheme.shapes.medium, colors = drinkFieldColors(), modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            FieldGroup(title = "评分", modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 12.dp, vertical = 15.dp)) {
                                    // 👇 onRate 直接接受 Float
                                    RatingStars(rating = rating, starSize = 24.dp, onRate = { rating = it })
                                }
                            }
                        }
                    }

                    item {
                        FieldGroup(title = "心情") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Catalog.moods.forEach { emoji ->
                                    Box(
                                        modifier = Modifier.size(46.dp).clip(CircleShape)
                                            .background(if (mood == emoji) brandColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow)
                                            .border(width = if (mood == emoji) 2.dp else 0.dp, color = if (mood == emoji) brandColor else Color.Transparent, shape = CircleShape)
                                            .clickable { mood = if (mood == emoji) "" else emoji },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(text = emoji, fontSize = 22.sp) }
                                }
                            }
                        }
                    }

                    item {
                        FieldGroup(title = "照片", subtitle = "可选") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSegmenting) {
                                    Box(modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = brandColor)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(text = "AI 抠图中…", style = MaterialTheme.typography.bodyMedium, color = brandColor)
                                } else if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(), contentDescription = "照片",
                                        contentScale = ContentScale.Crop, modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.medium)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                if (!isSegmenting) {
                                    PhotoButton(text = "相册", emoji = "🖼") { onPick(false, false) }
                                    Spacer(Modifier.width(10.dp))
                                    PhotoButton(text = "拍照", emoji = "📷") { showInAppCamera = true }
                                    if (photoName != null) {
                                        Spacer(Modifier.width(10.dp))
                                        PhotoButton(text = "移除", emoji = "✕") { PhotoStore.delete(photoName!!); photoName = null; previewBitmap = null }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        FieldGroup(title = "备注", subtitle = "可选") {
                            OutlinedTextField(
                                value = note, onValueChange = { note = it.take(120) },
                                placeholder = { Text("今天为什么想喝这杯…", color = Outline) },
                                minLines = 2, modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium, colors = drinkFieldColors(),
                            )
                        }
                    }
                    
                    item {
                        FieldGroup(title = "日期与时间") {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f).clickable { showDatePicker = true }.padding(horizontal = 14.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = formatDrinkDate(selectedDate), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Outline))
                                Box(
                                    modifier = Modifier.weight(1f).clickable { showTimePicker = true }.padding(horizontal = 14.dp, vertical = 14.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(text = formatDrinkTime(selectedDate), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (initial != null && onDelete != null) {
                        Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Strawberry.copy(alpha = 0.16f)).clickable { onDelete(initial) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Delete, "删除", tint = Strawberry, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    GradientButton(
                        text = if (initial == null) "记下这一杯" else "保存修改",
                        onClick = {
                            val item = (initial ?: Drink()).copy(
                                category = category.label, brand = brand.ifBlank { "自制 / 其他" }, name = name.trim(),
                                cupSize = cupSize, sugar = sugar, ice = ice, toppings = toppings,
                                price = priceText.toDoubleOrNull() ?: 0.0, rating = rating, mood = mood,
                                note = note.trim(), photoPath = photoName, timestamp = selectedDate,
                            )
                            onSave(item)
                        },
                        modifier = Modifier.weight(1f), colors = listOf(brandColor, brandColor.copy(alpha = 0.75f)),
                        leading = { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        if (showInAppCamera) {
            Dialog(
                onDismissRequest = { showInAppCamera = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                CameraCapture(
                    onPhotoCaptured = { bitmap ->
                        showInAppCamera = false
                        isSegmenting = true
                        scope.launch {
                            try {
                                val savedFile = PhotoStore.fileOf("photo_${System.currentTimeMillis()}.jpg")
                                withContext(Dispatchers.IO) {
                                    FileOutputStream(savedFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                }
                                photoName = savedFile.name
                                previewBitmap = bitmap

                                withContext(Dispatchers.IO) {
                                    try {
                                        U2NetSegmenter.init(context)
                                        val result = U2NetSegmenter.removeBackground(bitmap)
                                        if (result != null) {
                                            withContext(Dispatchers.Main) { previewBitmap = result }
                                            val newFile = PhotoStore.fileOf("seg_${savedFile.nameWithoutExtension}.png")
                                            FileOutputStream(newFile).use { result.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                            photoName = newFile.name
                                        }
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            } catch (e: Exception) { e.printStackTrace() } finally {
                                isSegmenting = false
                            }
                        }
                    },
                    onCancel = { showInAppCamera = false }
                )
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestampToDatePickerMillis(selectedDate))
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { picked -> selectedDate = datePickerMillisToTimestamp(picked, selectedDate) }
                        showDatePicker = false
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
            ) { DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val timePickerState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE), is24Hour = true)
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        selectedDate = newCal.timeInMillis
                        showTimePicker = false
                    }) { Text("确定") }
                },
                dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } },
                text = { TimePicker(state = timePickerState) }
            )
        }
    }
}

// ================= 辅助组件与函数 =================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySwitcher(
    current: DrinkCategory,
    onSelect: (DrinkCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DrinkCategory.entries.forEach { entry ->
            val selected = current == entry
            val accent = Color(entry.accent)
            val bg by animateColorAsState(if (selected) accent else Color.White, tween(320), label = "catBg")
            val textColor = if (selected) Color.White else Color(0xFF4A4A4A)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .border(1.dp, if (selected) Color.Transparent else Color(0xFFE0E0E0), RoundedCornerShape(50))
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.label,
                    fontSize = 16.sp, 
                    color = textColor, 
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun BrandChip(brand: Brand, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = Color(brand.color)
    Column(modifier = modifier.width(74.dp).clip(RoundedCornerShape(22.dp)).background(if (selected) color.copy(alpha = 0.12f) else Color.Transparent).border(if (selected) 2.dp else 0.dp, if (selected) color else Color.Transparent, RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BrandBadge(badge = brand.badge, color = color, size = 40.dp, logo = brand.logo, squircle = Catalog.isSquircle(brand.name))
        Spacer(Modifier.height(6.dp))
        Text(text = brand.name, style = MaterialTheme.typography.labelMedium, color = if (selected) color else PearlSoft, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun SheetHandle() { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(Outline)) } }

@Composable
private fun PreviewRow(liquid: Color, straw: Color, style: CupStyle, toppingCount: Int, brand: String, name: String, cupSize: String, sugar: String, rating: Float, mood: String, aiLoading: Boolean, onAiCamera: () -> Unit, onAiGallery: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(Brush.horizontalGradient(listOf(straw.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceContainerLow))).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DrinkCup(modifier = Modifier.width(78.dp).height(100.dp), liquidColor = liquid, strawColor = straw, toppingCount = toppingCount, style = style)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = brand.ifBlank { "选个品牌吧" }, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(text = listOf(name.ifBlank { "还没写品名" }, cupSize, sugar).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = PearlSoft)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 👇 改为 > 0f
                    if (rating > 0f) RatingStars(rating = rating, starSize = 14.dp)
                    if (rating > 0f && mood.isNotBlank()) Spacer(Modifier.width(8.dp))
                    if (mood.isNotBlank()) Text(text = mood, fontSize = 16.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (aiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = straw)
                        Spacer(Modifier.width(8.dp))
                        Text(text = "AI 识别中…", style = MaterialTheme.typography.labelMedium, color = PearlSoft)
                    } else {
                        AiChip(text = "📷 拍小票", onClick = onAiCamera)
                        Spacer(Modifier.width(8.dp))
                        AiChip(text = "🖼 选图", onClick = onAiGallery)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiChip(text: String, onClick: () -> Unit) { Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(text = text, style = MaterialTheme.typography.labelMedium, color = PearlSoft, fontWeight = FontWeight.SemiBold) } }
@Composable
private fun FieldGroup(title: String, modifier: Modifier = Modifier, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) { Spacer(Modifier.width(6.dp)); Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(9.dp))
        content()
    }
}
@Composable
private fun ChipRow(options: List<String>, selected: String, accent: Color, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> TagChip(text = option, selected = selected == option, accent = accent, onClick = { onSelect(option) }) }
    }
}
@Composable
private fun PhotoButton(text: String, emoji: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerLow).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 18.sp); Spacer(Modifier.height(2.dp)); Text(text = text, style = MaterialTheme.typography.labelMedium, color = PearlSoft)
    }
}
@Composable
private fun drinkFieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Caramel, unfocusedBorderColor = Outline, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, cursorColor = Caramel, focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface)
private fun softLiquid(color: Color): Color = Color(red = color.red * 0.62f + 0.38f, green = color.green * 0.62f + 0.38f, blue = color.blue * 0.62f + 0.38f, alpha = 1f)

private fun timestampToDatePickerMillis(timestamp: Long): Long = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
private fun datePickerMillisToTimestamp(datePickerMillis: Long, originalTimestamp: Long): Long {
    val date = java.time.Instant.ofEpochMilli(datePickerMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    val originalTime = java.time.Instant.ofEpochMilli(originalTimestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return date.atTime(originalTime).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
private fun formatDrinkDate(timestamp: Long): String = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日"))
private fun formatDrinkTime(timestamp: Long): String = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
