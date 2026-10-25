package com.naicha.diary.util

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.util.Collections

object U2NetSegmenter {
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            val modelBytes = context.assets.open("u2netp.onnx").readBytes()
            
            // 配置多线程加速
            val options = OrtSession.SessionOptions()
            options.setIntraOpNumThreads(4)
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            ortSession = ortEnv?.createSession(modelBytes, options)
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeBackground(bitmap: Bitmap): Bitmap? {
        if (!isInitialized || ortEnv == null || ortSession == null) return null

        try {
            // 1. 缩放至模型要求的 320x320
            val resized = Bitmap.createScaledBitmap(bitmap, 320, 320, true)
            val pixels = IntArray(320 * 320)
            resized.getPixels(pixels, 0, 320, 0, 0, 320, 320)

            // 2. 构造 NCHW 数据格式
            val floatBuffer = FloatBuffer.allocate(1 * 3 * 320 * 320)
            val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
            val std = floatArrayOf(0.229f, 0.224f, 0.225f)

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

            // 3. 运行推理
            val inputName = ortSession!!.inputNames.iterator().next()
            val inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(1, 3, 320, 320))
            val results = ortSession!!.run(Collections.singletonMap(inputName, inputTensor))

            // 4. 读取输出遮罩（直接读内存值，避免变量名冲突）
            val outputTensor = results[0] as OnnxTensor
            val maskArray = FloatArray(320 * 320)
            val outputBuffer = outputTensor.floatBuffer
            outputBuffer.get(maskArray)
            outputBuffer.rewind()

            // 5. 归一化 Alpha 通道
            var maxVal = -Float.MAX_VALUE
            var minVal = Float.MAX_VALUE
            for (v in maskArray) {
                if (v > maxVal) maxVal = v
                if (v < minVal) minVal = v
            }
            val range = maxVal - minVal
            if (range == 0f) return null

            // 6. 生成最终图像并逐像素合成
            val width = bitmap.width
            val height = bitmap.height
            val resultPixels = IntArray(width * height)
            bitmap.getPixels(resultPixels, 0, width, 0, 0, width, height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val maskX = (x * 320 / width).coerceIn(0, 319)
                    val maskY = (y * 320 / height).coerceIn(0, 319)
                    val maskIndex = maskY * 320 + maskX
                    
                    val alpha = (((maskArray[maskIndex] - minVal) / range) * 255).toInt().coerceIn(0, 255)
                    
                    val originalColor = resultPixels[y * width + x]
                    val r = (originalColor shr 16) and 0xFF
                    val g = (originalColor shr 8) and 0xFF
                    val b = originalColor and 0xFF
                    
                    resultPixels[y * width + x] = (alpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            // 7. 输出透明背景图
            val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            finalBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)

            // 清理
            inputTensor.close()
            results.close()
            resized.recycle()

            return finalBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
