package com.naicha.diary.data

import android.content.Context
import com.naicha.diary.util.PhotoStore
import com.naicha.diary.util.WebDavClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File

class DrinkRepository(private val context: Context) {

    private val file = File(context.filesDir, "drinks.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _items = MutableStateFlow<List<Drink>>(emptyList())
    val items: StateFlow<List<Drink>> = _items.asStateFlow()

    init {
        _items.value = read()
    }

    private fun read(): List<Drink> = runCatching {
        if (!file.exists()) return emptyList()
        json.decodeFromString<List<Drink>>(file.readText())
    }.getOrDefault(emptyList())

    private fun persist(list: List<Drink>) {
        _items.value = list
        runCatching { file.writeText(json.encodeToString(list)) }
    }

    private fun getActualFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val cleanPath = path.trim()
        val directFile = File(cleanPath)
        if (directFile.exists() && directFile.isFile) return directFile

        val fileName = if (cleanPath.contains("/")) cleanPath.substringAfterLast("/") else cleanPath
        return try {
            val storeFile = PhotoStore.fileOf(fileName)
            if (storeFile.exists()) storeFile else null
        } catch (e: Exception) {
            val fallback = File(context.filesDir, fileName)
            if (fallback.exists()) fallback else null
        }
    }

    private fun triggerAutoSync() {
        val prefs = context.getSharedPreferences("webdav_prefs", Context.MODE_PRIVATE)
        val url = prefs.getString("url", "") ?: ""
        val user = prefs.getString("user", "") ?: ""
        val pass = prefs.getString("pass", "") ?: ""
        
        if (url.isBlank() || user.isBlank() || pass.isBlank()) return

        syncScope.launch {
            try {
                val brandPrefs = context.getSharedPreferences("brand_store", Context.MODE_PRIVATE)
                val orderPrefs = context.getSharedPreferences("naicha_brand_order_prefs", Context.MODE_PRIVATE)
                val hiddenStr = brandPrefs.getString("hidden", "") ?: ""
                val orderStr = orderPrefs.getString("brand_order", "") ?: ""
                
                val customBrandsStr = brandPrefs.getString("custom", "") ?: ""
                val jsonIgnore = Json { ignoreUnknownKeys = true }
                val customBrands = try { jsonIgnore.decodeFromString<List<Brand>>(customBrandsStr) } catch (e: Exception) { emptyList() }
                
                val brandsWithBase64 = customBrands.map { brand ->
                    val imgFile = getActualFile(brand.logoPath)
                    val base64 = if (imgFile != null) {
                        "data:image/jpeg;base64," + android.util.Base64.encodeToString(imgFile.readBytes(), android.util.Base64.NO_WRAP)
                    } else ""
                    brand.copy(logoPath = base64)
                }

                val photoMap = mutableMapOf<String, String>()
                _items.value.take(50).forEach { drink ->
                    val imgFile = getActualFile(drink.photoPath)
                    if (imgFile != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                        if (bitmap != null) {
                            // 👈 修改 1：将尺寸限制缩小到 360f
                            val scale = 360f / maxOf(bitmap.width, bitmap.height)
                            val finalBitmap = if (scale < 1f) {
                                android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
                            } else { bitmap }
                            
                            val stream = ByteArrayOutputStream()
                            // 👈 修改 2：将 PNG 改为 WEBP_LOSSY，质量 60
                            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 60, stream)
                            photoMap[imgFile.name] = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                        }
                    }
                }

                val backup = BackupData(
                    drinks = _items.value,
                    customBrands = brandsWithBase64,
                    hiddenBrands = hiddenStr,
                    brandOrder = orderStr,
                    photoFiles = photoMap
                )
                
                val jsonStr = json.encodeToString(BackupData.serializer(), backup)

                var index = prefs.getInt("auto_backup_index", 1)
                if (index < 1 || index > 3) index = 1
                val fileName = "naicha_backup_${index}.json"
                
                val res = WebDavClient.upload(url, user, pass, jsonStr.toByteArray(), fileName)
                
                if (res.isSuccess) {
                    val nextIndex = if (index >= 3) 1 else index + 1
                    prefs.edit().putInt("auto_backup_index", nextIndex).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun add(item: Drink) { persist(_items.value + item); triggerAutoSync() }
    fun update(item: Drink) { persist(_items.value.map { if (it.id == item.id) item else it }); triggerAutoSync() }
    fun remove(id: String) { persist(_items.value.filterNot { it.id == id }); triggerAutoSync() }
    fun clearAll() { persist(emptyList()); triggerAutoSync() }
    fun replaceAll(newItems: List<Drink>) { persist(newItems) }
    fun sorted(): List<Drink> = _items.value.sortedByDescending { it.timestamp }
}
