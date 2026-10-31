package com.naicha.diary.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object WebDavClient {

    private fun getBase64Auth(user: String, pass: String): String {
        val auth = "$user:$pass"
        return "Basic " + android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
    }

    // 👈 新增了 fileName 参数，默认值为 naicha_backup.json（兼容旧逻辑）
    fun upload(urlStr: String, user: String, pass: String, fileBytes: ByteArray, fileName: String = "naicha_backup.json"): Result<Unit> {
        return runCatching {
            var finalUrl = urlStr
            if (!finalUrl.endsWith("/")) finalUrl += "/"
            val fullUrl = "$finalUrl$fileName"

            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.setRequestProperty("Authorization", getBase64Auth(user, pass))
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            conn.outputStream.use { os: OutputStream ->
                os.write(fileBytes)
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                return Result.success(Unit)
            } else {
                throw Exception("上传失败，HTTP状态码: $responseCode")
            }
        }
    }

    // 👈 新增了 fileName 参数
    fun download(urlStr: String, user: String, pass: String, fileName: String = "naicha_backup.json"): Result<String> {
        return runCatching {
            var finalUrl = urlStr
            if (!finalUrl.endsWith("/")) finalUrl += "/"
            val fullUrl = "$finalUrl$fileName"

            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", getBase64Auth(user, pass))

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                return Result.success(response.toString())
            } else {
                throw Exception("下载失败，HTTP状态码: $responseCode。请检查是否存在该备份")
            }
        }
    }
        // 👈 新增：获取文件最后修改时间
    fun getLastModified(urlStr: String, user: String, pass: String, fileName: String): Result<Long> {
        return runCatching {
            var finalUrl = urlStr
            if (!finalUrl.endsWith("/")) finalUrl += "/"
            val fullUrl = "$finalUrl$fileName"

            val url = URL(fullUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.setRequestProperty("Authorization", getBase64Auth(user, pass))

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val lastModifiedStr = conn.getHeaderField("Last-Modified")
                if (lastModifiedStr != null) {
                    // 标准 HTTP 时间格式转换
                    val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US)
                    val date = sdf.parse(lastModifiedStr)
                    return Result.success(date?.time ?: 0L)
                }
            }
            throw Exception("获取时间失败")
        }
    }
}
