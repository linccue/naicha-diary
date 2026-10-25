package com.naicha.diary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.util.Collections

object U2NetSegmenter {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    // 初始化模型（必须在后台线程调用，因为加载模型需要时间）
    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("u2netp.onnx").readBytes()
            ortSession = ortEnv?.createSession(modelBytes)
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 执行抠图（要在后台线程调用）
    fun removeBackground(bitmap: Bitmap): Bitmap? {
        if (!isInitialized || ortEnv == null || ortSession == null) return null

        try {
            // 1. 缩小图片到模型要求的 320x320
            val resized = Bitmap.createScaledBitmap(bitmap, 320, 320, true)
            val pixels = IntArray(320 * 320)
            resized.getPixels(pixels, 0, 320, 0, 0, 320, 320)

            // 2. 将像素归一化并转为 AI 认识的数据格式
            val floatBuffer = FloatBuffer.allocate(1 * 3 * 320 * 320)
            val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
            val std = floatArrayOf(0.229f, 0.224f, 0.225f)

            // 分为 R, G, B 三个通道压入
            for (c in 0 until 3) {
                for (i in pixels.indices) {
                    val p = pixels[i]
                    val r = (p shr 16 and 0xFF) / 255.0f
                    val g = (p shr 8 and 0xFF) / 255.0f
                    val b = (p and 0xFF) / 255.0f
                    val valToPut = when (c) {
                        0 -> (r - mean[0]) / std[0]
                        1 -> (g - mean[1]) / std[1]
                        else -> (b - mean[2]) / std[2]
                    }
                    floatBuffer.put(valToPut)
                }
            }
            floatBuffer.rewind()

            // 3. 让 AI 开始计算
            val inputName = ortSession!!.inputNames.iterator().next()
            val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 3, 320, 320))
            val results = ortSession!!.run(Collections.singletonMap(inputName, inputTensor))

            // 4. 获取 AI 画出的“遮罩”（黑白图，白色是奶茶，黑色是背景）
            val outputTensor = results[0] as OnnxTensor
            val outputBuffer = outputTensor.floatBuffer
            val maskData = FloatArray(320 * 320)
            outputBuffer.get(maskData)

            // 5. 把遮罩数据变成 0~255 的灰度图
            var maxVal = -Float.MAX_VALUE
            var minVal = Float.MAX_VALUE
            for (v in maskData) {
                if (v > maxVal) maxVal = v
                if (v < minVal) minVal = v
            }
            val range = maxVal - minVal

            val maskPixels = IntArray(320 * 320)
            for (i in maskData.indices) {
                // 归一化到 0~255
                val alpha = ((maskData[i] - minVal) / range * 255).toInt().coerceIn(0, 255)
                // 生成 ARGB 像素，颜色是黑色，透明度由 AI 决定
                maskPixels[i] = android.graphics.Color.argb(alpha, 0, 0, 0)
            }

            // 6. 生成最终带透明背景的图片
            val maskBitmap = Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)
            maskBitmap.setPixels(maskPixels, 0, 320, 0, 0, 320, 320)

            // 把遮罩放大回原图大小
            val scaledMask = Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, true)

            // 7. 合成原图与遮罩（利用混合模式扣除背景）
            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null) // 画原图

            val paint = Paint()
            paint.isAntiAlias = true
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) // 只保留遮罩里的白色部分
            canvas.drawBitmap(scaledMask, 0f, 0f, paint) // 画遮罩上去

            // 清理内存
            inputTensor.close()
            results.close()
            resized.recycle()
            maskBitmap.recycle()
            scaledMask.recycle()

            return resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
