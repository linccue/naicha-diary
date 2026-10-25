package com.naicha.diary.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await

object ImageSegmenter {
    // 传入原图 Bitmap，返回抠好图的透明背景 Bitmap
    suspend fun removeBackground(context: Context, originalBitmap: Bitmap): Bitmap? {
        return try {
            // 1. 设置抠图选项
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap() // 要求返回透明背景的图
                .build()

            // 2. 创建抠图客户端
            val segmenter = SubjectSegmentation.getClient(options)

            // 3. 把 Bitmap 转换成 ML Kit 认识的 InputImage
            val image = InputImage.fromBitmap(originalBitmap, 0)

            // 4. 开始抠图（这里会等待模型下载或处理）
            val result = segmenter.process(image).await()

            // 5. 拿到抠好的图并返回
            result.foregroundBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null // 失败就返回空
        }
    }
}
