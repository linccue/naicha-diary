package com.naicha.diary.data

import android.content.Context
import com.naicha.diary.util.WebDavClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class DrinkRepository(private val context: Context) {

    private val file = File(context.filesDir, "drinks.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // 用于后台自动同步的协程作用域
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

       // 👇 核心新增：静默自动上传 WebDAV
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
                    val imgFile = brand.logoPath?.let { File(it) }
                    val base64 = if (imgFile != null && imgFile.exists()) {
                        "data:image/jpeg;base64," + android.util.Base64.encodeToString(imgFile.readBytes(), android.util.Base64.NO_WRAP)
                    } else ""
                    brand.copy(logoPath = base64)
                }

                val backup = BackupData(
                    drinks = _items.value,
                    customBrands = brandsWithBase64,
                    hiddenBrands = hiddenStr,
                    brandOrder = orderStr
                )
                
                val jsonStr = json.encodeToString(BackupData.serializer(), backup)

                // 👈 轮询：读取当前索引，上传到对应文件名，索引+1（1,2,3轮询）
                var index = prefs.getInt("auto_backup_index", 1)
                if (index < 1 || index > 3) index = 1
                val fileName = "naicha_backup_${index}.json"
                
                val res = WebDavClient.upload(url, user, pass, jsonStr.toByteArray(), fileName)
                
                // 如果上传成功，才更新索引
                if (res.isSuccess) {
                    val nextIndex = if (index >= 3) 1 else index + 1
                    prefs.edit().putInt("auto_backup_index", nextIndex).apply()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 👇 以下四个方法在原有的基础上，增加了 triggerAutoSync() 调用
    fun add(item: Drink) {
        persist(_items.value + item)
        triggerAutoSync()
    }

    fun update(item: Drink) {
        persist(_items.value.map { if (it.id == item.id) item else it })
        triggerAutoSync()
    }

    fun remove(id: String) {
        persist(_items.value.filterNot { it.id == id })
        triggerAutoSync()
    }

    fun clearAll() {
        persist(emptyList())
        triggerAutoSync()
    }

    // 从云端下载时使用，不触发自动同步，避免死循环
    fun replaceAll(newItems: List<Drink>) {
        persist(newItems)
    }

    fun sorted(): List<Drink> = _items.value.sortedByDescending { it.timestamp }
}
