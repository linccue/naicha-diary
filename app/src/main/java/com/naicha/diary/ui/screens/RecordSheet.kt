Catalog.brandsOf(category).forEach { item ->
    BrandChip(...)
     Box(
        modifier = Modifier...
        .clickable { showBrandManager = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "添加品牌", tint = Color.Gray)
    }
} // 这里多了一个大括号，导致原本的 FlowRow 闭合错误，直接插入了弹窗代码
// 在 RecordSheet 的最后（底栏外）加上弹窗：
if (showBrandManager) {
    BrandManagerDialog(...)
}
