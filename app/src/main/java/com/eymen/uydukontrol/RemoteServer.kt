package com.eymen.uydukontrol

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Small HTTP server on the TV's local network. The phone opens the page from the QR code
 * in its browser; no app needs to be installed on the phone. Every API call carries the
 * token from the QR code, so other devices on the network cannot control the TV.
 */
class RemoteServer(private val app: AppState, private val token: String) {
    private var server: ServerSocket? = null
    private val main = Handler(Looper.getMainLooper())
    var port = 0
        private set

    fun start(): Boolean {
        for (p in 8765..8775) {
            try {
                server = ServerSocket(p)
                port = p
                break
            } catch (e: Exception) {
            }
        }
        val s = server ?: return false
        val t = Thread {
            while (!s.isClosed) {
                try {
                    val client = s.accept()
                    Thread { handle(client) }.start()
                } catch (e: Exception) {
                }
            }
        }
        t.isDaemon = true
        t.start()
        return true
    }

    fun stop() {
        try {
            server?.close()
        } catch (e: Exception) {
        }
        server = null
    }

    private fun handle(socket: Socket) {
        try {
            socket.use { s ->
                s.soTimeout = 5000
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                val requestLine = reader.readLine() ?: return
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val parts = requestLine.split(" ")
                val target = if (parts.size > 1) parts[1] else "/"
                val path = target.substringBefore('?')
                val query = parseQuery(target.substringAfter('?', ""))
                val out = s.getOutputStream()
                if (path == "/" || path == "/index.html") {
                    val html = app.ctx.assets.open("remote.html").use { it.readBytes() }
                    send(out, 200, "text/html; charset=utf-8", html)
                    return
                }
                if (query["t"] != token) {
                    send(out, 403, "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"QR kodu yeniden okutun\"}".toByteArray())
                    return
                }
                when (path) {
                    "/api/state" -> send(out, 200, "application/json; charset=utf-8", (onMain { app.remoteState() } ?: "{}").toByteArray())
                    "/api/key" -> {
                        val k = query["k"] ?: ""
                        onMain { app.remoteKey(k) }
                        ok(out)
                    }
                    "/api/tune" -> {
                        val k = query["key"] ?: ""
                        onMain { app.remoteTune(k) }
                        ok(out)
                    }
                    "/api/search" -> {
                        val q = query["q"] ?: ""
                        send(out, 200, "application/json; charset=utf-8", (onMain { app.remoteSearch(q) } ?: "[]").toByteArray())
                    }
                    "/api/text" -> {
                        val v = query["v"] ?: ""
                        onMain { app.remoteText(v) }
                        ok(out)
                    }
                    else -> send(out, 404, "text/plain; charset=utf-8", "Bulunamadı".toByteArray())
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun ok(out: OutputStream) = send(out, 200, "application/json; charset=utf-8", "{\"ok\":true}".toByteArray())

    private fun send(out: OutputStream, code: Int, type: String, body: ByteArray) {
        val status = when (code) {
            200 -> "OK"
            403 -> "Forbidden"
            404 -> "Not Found"
            else -> "Error"
        }
        val header = "HTTP/1.1 $code $status\r\n" +
            "Content-Type: $type\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(body)
        out.flush()
    }

    private fun parseQuery(q: String): Map<String, String> {
        val map = HashMap<String, String>()
        if (q.isEmpty()) return map
        for (pair in q.split('&')) {
            val i = pair.indexOf('=')
            val k = if (i >= 0) pair.substring(0, i) else pair
            val v = if (i >= 0) pair.substring(i + 1) else ""
            try {
                map[URLDecoder.decode(k, "UTF-8")] = URLDecoder.decode(v, "UTF-8")
            } catch (e: Exception) {
            }
        }
        return map
    }

    private fun <T> onMain(block: () -> T): T? {
        val latch = CountDownLatch(1)
        var result: T? = null
        main.post {
            try {
                result = block()
            } catch (e: Exception) {
            } finally {
                latch.countDown()
            }
        }
        latch.await(3, TimeUnit.SECONDS)
        return result
    }

    companion object {
        fun localIp(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
                for (ni in interfaces) {
                    if (!ni.isUp || ni.isLoopback) continue
                    for (a in ni.inetAddresses) {
                        if (a is Inet4Address && !a.isLoopbackAddress && a.isSiteLocalAddress) return a.hostAddress
                    }
                }
            } catch (e: Exception) {
            }
            return null
        }

        fun qr(text: String, size: Int = 360): ImageBitmap? = try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 1
            val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (m.get(x, y)) 0xFF0A1224.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            bmp.setPixels(pixels, 0, size, 0, 0, size, size)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
