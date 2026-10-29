    val nodes = remember(displayItems) {
        val list = mutableListOf<StickerNode>()
        val cols = 3
        val rows = 2
        val cellWidth = 350f / cols
        val cellHeight = 220f / rows
        
        val shuffledCells = (0 until (cols * rows)).shuffled()
        
        displayItems.forEachIndexed { index, tea ->
            val cellIndex = shuffledCells[index % (cols * rows)]
            val col = cellIndex % cols
            val row = cellIndex / cols
            
            val random = Random(tea.id.hashCode())
            val jitterX = (random.nextFloat() - 0.5f) * cellWidth * 0.4f
            val jitterY = (random.nextFloat() - 0.5f) * cellHeight * 0.4f
            
            // 问题在这里：x 和 y 的计算是相对于 350f 和 220f 的硬编码，而不是实际容器大小
            val x = col * cellWidth + cellWidth / 2f - cardSizePx / 2f + jitterX
            val y = row * cellHeight + cellHeight / 2f - cardSizePx / 2f + jitterY

            list.add(StickerNode(tea, x, y, random.nextFloat() * 20f - 10f))
        }
        list
    }
