package com.naicha.diary.data

import com.naicha.diary.R
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class DrinkCategory(val label: String, val badge: String, val accent: Long) {
    MilkTea("奶茶", "奶", 0xFFC98A5B),
    FruitTea("果茶", "果", 0xFFE8A33D),
    Coffee("咖啡", "咖", 0xFF8B5E3C),
    LemonTea("柠檬茶", "柠", 0xFFF4B223),
    PureTea("纯茶", "茶", 0xFF3B8C4E),
    Bottled("瓶装", "瓶", 0xFF2E86DE),
    Alcohol("酒类", "酒", 0xFF8E44AD),
    Yogurt("酸奶", "酸", 0xFFE8A0BF);

    companion object {
        fun of(label: String): DrinkCategory = entries.firstOrNull { it.label == label } ?: MilkTea
    }
}

@Serializable
data class Drink(
    val id: String = UUID.randomUUID().toString(),
    val category: String = DrinkCategory.MilkTea.label,
    val brand: String = "",
    val name: String = "",
    val cupSize: String = "中杯",
    val sugar: String = "半糖",
    val ice: String = "少冰",
    val toppings: List<String> = emptyList(),
    val price: Double = 0.0,
    val rating: Float = 5f,
    val mood: String = "😋",
    val note: String = "",
    val photoPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val categoryType: DrinkCategory get() = DrinkCategory.of(category)
    val calories: Int get() = estimateCalories(categoryType, cupSize, sugar, toppings.size)

    companion object {
        private fun estimateCalories(category: DrinkCategory, cupSize: String, sugar: String, toppingCount: Int): Int {
            val base = when (category) {
                DrinkCategory.Coffee, DrinkCategory.PureTea -> when (cupSize) {
                    "小杯" -> 90; "中杯" -> 150; "大杯" -> 210; else -> 260
                }
                else -> when (cupSize) {
                    "小杯" -> 180; "中杯" -> 260; "大杯" -> 340; else -> 420
                }
            }
            val sugarFactor = when (sugar) {
                "全糖" -> 1.30; "七分糖" -> 1.18; "半糖" -> 1.00; "三分糖" -> 0.86; "无糖" -> 0.72; else -> 1.0
            }
            val toppingWeight = if (category == DrinkCategory.Coffee) 38 else 45
            return (base * sugarFactor).toInt() + toppingCount * toppingWeight
        }
    }
}

@Serializable
data class Brand(
    val name: String,
    val category: DrinkCategory,
    val color: Long,
    val badge: String,
    val logo: Int = 0,
    val logoPath: String? = null,
    val isHidden: Boolean = false,
    val isCircle: Boolean = true
)

object Catalog {
    val sugars = listOf("全糖", "七分糖", "半糖", "三分糖", "无糖")
    val moods = listOf("😋", "😌", "🥰", "🥺", "😭", "😡", "🤩", "🤔", "😪", "💪")

    val brands: List<Brand> = listOf(
        Brand("蜜雪冰城", DrinkCategory.MilkTea, 0xFFE23A2E, "雪", R.drawable.logo_mixue),
        Brand("霸王茶姬", DrinkCategory.MilkTea, 0xFF8E2F3F, "姬", R.drawable.logo_bawangchaji),
        Brand("煲珠公", DrinkCategory.MilkTea, 0xFF3A3EC5, "煲", R.drawable.logo_baozhugong),
        Brand("陈文鼎", DrinkCategory.MilkTea, 0xFF8B4A2B, "陈", R.drawable.logo_chenwending),
        Brand("瑞幸咖啡", DrinkCategory.Coffee, 0xFF1C3F94, "瑞", R.drawable.logo_luckin),
        Brand("古茗", DrinkCategory.MilkTea, 0xFF2B2B2B, "古", R.drawable.logo_guming),
        Brand("沪上阿姨", DrinkCategory.MilkTea, 0xFFC7303E, "沪", R.drawable.logo_hushangayi),
        Brand("茶百道", DrinkCategory.MilkTea, 0xFF2E86DE, "茶", R.drawable.logo_chabaidao),
        Brand("益禾堂", DrinkCategory.MilkTea, 0xFFD7263D, "益", R.drawable.logo_yihetang),
        Brand("喜茶", DrinkCategory.MilkTea, 0xFF1C1C1E, "喜", R.drawable.logo_heytea),
        Brand("奈雪的茶", DrinkCategory.MilkTea, 0xFF7DBE3F, "奈", R.drawable.logo_naixue),
        Brand("茶颜悦色", DrinkCategory.MilkTea, 0xFF7B4A2D, "颜", R.drawable.logo_chayanyuese),
        Brand("CoCo都可", DrinkCategory.MilkTea, 0xFFF26522, "Co", R.drawable.logo_coco),
        Brand("1点点", DrinkCategory.MilkTea, 0xFF3B8C4E, "1", R.drawable.logo_yidiandian),
        Brand("书亦烧仙草", DrinkCategory.MilkTea, 0xFFE23A2E, "书", R.drawable.logo_shuyi),
        Brand("甜啦啦", DrinkCategory.MilkTea, 0xFFFF6B9D, "甜", R.drawable.logo_tianlala),
        Brand("7分甜", DrinkCategory.MilkTea, 0xFFF4B223, "7", R.drawable.logo_qifentian),
        Brand("乐乐茶", DrinkCategory.MilkTea, 0xFFC8A464, "乐", R.drawable.logo_lelecha),
        Brand("阿水大杯茶", DrinkCategory.MilkTea, 0xFF2C7BE5, "水", R.drawable.logo_ashui),
        Brand("茉莉奶白", DrinkCategory.MilkTea, 0xFF6FAE8E, "茉", R.drawable.logo_molinaibai),
        Brand("茶话弄", DrinkCategory.MilkTea, 0xFFB08968, "话", R.drawable.logo_chahualong),
        Brand("快乐柠檬", DrinkCategory.MilkTea, 0xFFE8B93C, "柠", R.drawable.logo_happylemon),
        Brand("爷爷不泡茶", DrinkCategory.MilkTea, 0xFF8D6E63, "爷", R.drawable.logo_yeyebupaocha),
        Brand("桂桂茶", DrinkCategory.MilkTea, 0xFFB08968, "桂", R.drawable.logo_guiguicha),
        Brand("KOI Thé", DrinkCategory.MilkTea, 0xFF2E7D5B, "K", R.drawable.logo_koi),
        Brand("茶理宜世", DrinkCategory.MilkTea, 0xFF5D6D7E, "理", R.drawable.logo_chalishiyi),
        Brand("茶山派", DrinkCategory.MilkTea, 0xFFC0392B, "山", R.drawable.logo_chashanpai),
        Brand("察理王子", DrinkCategory.MilkTea, 0xFFE67E22, "察", R.drawable.logo_chaliwangzi),
        Brand("初茶花月", DrinkCategory.MilkTea, 0xFFE8A0BF, "初", R.drawable.logo_chuchahuayue),
        Brand("丘大叔柠檬茶", DrinkCategory.MilkTea, 0xFFF1C40F, "丘", R.drawable.logo_qiudashu),
        Brand("山茶涧", DrinkCategory.MilkTea, 0xFF7B4A2D, "涧", R.drawable.logo_shanchajian),
        Brand("茶月山", DrinkCategory.MilkTea, 0xFF34495E, "月", R.drawable.logo_chayueshan),
        Brand("淡马茶坊", DrinkCategory.MilkTea, 0xFF8E44AD, "淡", R.drawable.logo_danmachafang),
        Brand("放哈 Fundosa", DrinkCategory.MilkTea, 0xFF27AE60, "放", R.drawable.logo_fundosa),
        Brand("哈茶福", DrinkCategory.MilkTea, 0xFFE74C3C, "哈", R.drawable.logo_hachafu),
        Brand("洪都大拇指", DrinkCategory.MilkTea, 0xFFC0392B, "洪", R.drawable.logo_hongdu),
        Brand("TCROSS 交茶点", DrinkCategory.MilkTea, 0xFF2980B9, "T", R.drawable.logo_tcross),
        Brand("去茶山", DrinkCategory.MilkTea, 0xFF8D9E6E, "去", R.drawable.logo_quchashan),
        Brand("拾叁茶", DrinkCategory.MilkTea, 0xFF7B3F00, "拾", R.drawable.logo_shisancha),
        Brand("唐沫茶兮", DrinkCategory.MilkTea, 0xFF2C3E50, "唐", R.drawable.logo_tangmochaxi),
        Brand("仙雨林", DrinkCategory.MilkTea, 0xFF1E8449, "仙", R.drawable.logo_xianyulin),
        Brand("阿嫲手作", DrinkCategory.MilkTea, 0xFF9C8B7A, "嫲", R.drawable.logo_amashouzuo),
        Brand("茶满方庭", DrinkCategory.MilkTea, 0xFF8B4513, "满", R.drawable.logo_chamanfangting),
        Brand("椿风", DrinkCategory.MilkTea, 0xFF1B4F9C, "椿", R.drawable.logo_chunfeng),
        Brand("放牛斑", DrinkCategory.MilkTea, 0xFFA3C644, "牛", R.drawable.logo_fangniuban),
        Brand("壶见", DrinkCategory.MilkTea, 0xFFD35400, "壶", R.drawable.logo_hujian),
        Brand("眷之茶", DrinkCategory.MilkTea, 0xFF6C3483, "眷", R.drawable.logo_juanzhicha),
        Brand("卡旺卡", DrinkCategory.MilkTea, 0xFF7D3C98, "卡", R.drawable.logo_kawangka),
        Brand("李山山茶事", DrinkCategory.MilkTea, 0xFF4A4A4A, "李", R.drawable.logo_lishanshan),
        Brand("OT 另茶", DrinkCategory.MilkTea, 0xFF95A5A6, "OT", R.drawable.logo_otlingcha),
        Brand("馬伍旺饮料厂", DrinkCategory.MilkTea, 0xFFE85D75, "馬", R.drawable.logo_mawuwang),
        Brand("茉沏", DrinkCategory.MilkTea, 0xFF7F8C8D, "茉", R.drawable.logo_moqi),
        Brand("让茶", DrinkCategory.MilkTea, 0xFF27AE60, "让", R.drawable.logo_rangcha),
        Brand("真茶屋", DrinkCategory.MilkTea, 0xFF2ECC71, "真", R.drawable.logo_zhenchawu),
        Brand("星巴克", DrinkCategory.Coffee, 0xFF00704A, "星", R.drawable.logo_starbucks),
        Brand("库迪咖啡", DrinkCategory.Coffee, 0xFFE2231A, "库", R.drawable.logo_cotti),
        Brand("Manner", DrinkCategory.Coffee, 0xFFC59B5F, "M", R.drawable.logo_manner),
        Brand("M Stand", DrinkCategory.Coffee, 0xFF000000, "M", R.drawable.logo_mstand),
        Brand("Tims", DrinkCategory.Coffee, 0xFFD22630, "T", R.drawable.logo_tims),
        Brand("Peet's", DrinkCategory.Coffee, 0xFF1D4E4F, "P", R.drawable.logo_peets),
        Brand("挪瓦咖啡", DrinkCategory.Coffee, 0xFFEB6B2A, "挪", R.drawable.logo_nowwa),
        Brand("幸运咖", DrinkCategory.Coffee, 0xFFF4B223, "幸", R.drawable.logo_luckycup),
        Brand("自制 / 其他", DrinkCategory.MilkTea, 0xFF8D6E63, "自", R.drawable.logo_mixue)
    )

    // 👇 核心新增：让全局能查到你添加的自定义品牌
    var extraBrands: List<Brand> = emptyList()

    val commonNames = mapOf(
        DrinkCategory.MilkTea to listOf("珍珠奶茶", "芋泥啵啵", "杨枝甘露", "布丁奶茶", "烤奶"),
        DrinkCategory.FruitTea to listOf("多肉葡萄", "满杯百香果", "芝芝莓莓", "杨梅吐气", "霸气橙子"),
        DrinkCategory.Coffee to listOf("生椰拿铁", "冰美式", "拿铁", "卡布奇诺", "澳白"),
        DrinkCategory.LemonTea to listOf("鸭屎香柠檬茶", "手打柠檬茶", "泰绿柠檬茶"),
        DrinkCategory.PureTea to listOf("茉莉绿茶", "鸭屎香单丛", "四季春", "大红袍"),
        DrinkCategory.Bottled to listOf("东方树叶", "三得利乌龙茶", "农夫山泉", "元气森林"),
        DrinkCategory.Alcohol to listOf("精酿啤酒", "果酒", "微醺", "鸡尾酒"),
        DrinkCategory.Yogurt to listOf("酸奶牛", "满米酸奶", "草莓酸奶")
    )

    fun brandsOf(category: DrinkCategory): List<Brand> = brands
    
    // 👇 核心修改：优先查自定义品牌
        fun brandOf(name: String): Brand {
        return brands.firstOrNull { it.name == name }
            ?: BrandStore.customBrands.value.firstOrNull { it.name == name }
            ?: brands.last()
    }
        
    fun colorOf(name: String): Long = brandOf(name).color
    
    // 👇 核心修改：让自定义的方形图标也能生效
    fun isSquircle(name: String): Boolean {
        if (name in setOf("喜茶", "奈雪的茶", "霸王茶姬", "陈文鼎", "煲珠公")) return true
        val custom = BrandStore.customBrands.value.firstOrNull { it.name == name }
        return custom?.let { !it.isCircle } ?: false
    }
    
    fun sizesFor(category: DrinkCategory): List<String> = listOf("小杯", "中杯", "大杯", "超大杯")
    fun sugarOptions(category: DrinkCategory): List<String> = sugars
    fun iceOptions(category: DrinkCategory): List<String> = listOf("热饮", "常温", "去冰", "少冰", "正常冰")
    fun extraOptions(category: DrinkCategory): List<String> = if (category == DrinkCategory.Coffee) listOf("浓缩", "燕麦奶", "厚乳", "奶油顶") else listOf("珍珠", "椰果", "芋泥", "奶盖", "布丁", "西米")
    fun sugarLabel(category: DrinkCategory): String = "糖度"
    fun iceLabel(category: DrinkCategory): String = "冰量 / 温度"
    fun extraLabel(category: DrinkCategory): String = "加料"
}

object BrandStore {
    private var prefs: android.content.SharedPreferences? = null
    private val _customBrands = MutableStateFlow<List<Brand>>(emptyList())
    val customBrands: StateFlow<List<Brand>> = _customBrands.asStateFlow()
    private val _hiddenBrands = MutableStateFlow<Set<String>>(emptySet())
    val hiddenBrands: StateFlow<Set<String>> = _hiddenBrands.asStateFlow()

    fun init(context: android.content.Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences("brand_store", android.content.Context.MODE_PRIVATE)
        val gson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        
        val customStr = prefs?.getString("custom", "") ?: ""
        if (customStr.isNotEmpty()) {
            try { 
                _customBrands.value = gson.decodeFromString(customStr) 
                Catalog.extraBrands = _customBrands.value // 👈 同步给 Catalog
            } catch (e: Exception) {}
        }
        
        val hiddenStr = prefs?.getString("hidden", "") ?: ""
        if (hiddenStr.isNotEmpty()) {
            _hiddenBrands.value = hiddenStr.split(",").toSet()
        }
    }
  fun reload(context: android.content.Context) {
        prefs = null
        init(context)
        // 同时更新排序
        val orderPrefs = context.getSharedPreferences("naicha_brand_order_prefs", android.content.Context.MODE_PRIVATE)
        // 此处仅作触发，具体在还原时重新读取即可
    }
    fun addBrand(context: android.content.Context, brand: Brand) {
        _customBrands.value = _customBrands.value + brand
        Catalog.extraBrands = _customBrands.value // 👈 同步给 Catalog
        prefs?.edit()?.putString("custom", kotlinx.serialization.json.Json.encodeToString(_customBrands.value))?.apply()
    }

    fun deleteBrands(context: android.content.Context, names: Set<String>) {
        _customBrands.value = _customBrands.value.filterNot { names.contains(it.name) }
        Catalog.extraBrands = _customBrands.value // 👈 同步给 Catalog
        prefs?.edit()?.putString("custom", kotlinx.serialization.json.Json.encodeToString(_customBrands.value))?.apply()
        
        val catalogNames = Catalog.brands.map { it.name }.toSet()
        val toHide = names.intersect(catalogNames)
        if (toHide.isNotEmpty()) {
            toggleHide(context, toHide, true)
        }
    }

    fun toggleHide(context: android.content.Context, names: Set<String>, hide: Boolean) {
        val current = _hiddenBrands.value.toMutableSet()
        if (hide) current.addAll(names) else current.removeAll(names)
        _hiddenBrands.value = current
        prefs?.edit()?.putString("hidden", current.joinToString(","))?.apply()
    }
}
@Serializable
data class BackupData(
    val drinks: List<Drink> = emptyList(),
    val customBrands: List<Brand> = emptyList(),
    val hiddenBrands: String = "",
    val brandOrder: String = ""
)
