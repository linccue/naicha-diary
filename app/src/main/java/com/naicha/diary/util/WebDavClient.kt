package com.naicha.diary.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object WebDavClient {

    private fun getBase64Auth(user: String, pass: String): String {
        val auth = "$user:$pass"
        // 使用 Android 原生的 Base64，完美兼容所有机型
        return "Basic " + android.util.Base64.encodeToString(auth.toByteArray(), android.util.Base64.NO_WRAP)
    }

    // 上传文件
    fun upload(urlStr: String, user: String, pass: String, fileBytes: ByteArray): Result<Unit> {
        return runCatching {
            var finalUrl = urlStr
            if (!finalUrl.endsWith("/")) finalUrl += "/"
            val fileName = "naicha_backup.json"
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

    // 下载文件
    fun download(urlStr: String, user: String, pass: String): Result<String> {
        return runCatching {
            var finalUrl = urlStr
            if (!finalUrl.endsWith("/")) finalUrl += "/"
            val fileName = "naicha_backup.json"
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
                throw Exception("下载失败，HTTP状态码: $responseCode。请检查路径或账号密码")
            }
        }
    }
}
