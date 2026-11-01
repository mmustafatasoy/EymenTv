package com.eymen.uydukontrol

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    lateinit var app: AppState
    private var pickCallback: ((Uri?) -> Unit)? = null
    private val consumedKeys = HashSet<Int>()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (::app.isInitialized) app.onPermissionResult(granted)
    }

    private val documentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cb = pickCallback
        pickCallback = null
        cb?.invoke(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        Fonts.init(this)
        app = AppState(this)
        setContent { EymenRoot(app) }
        app.start()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    fun requestTvPermission() {
        try {
            permissionLauncher.launch(AppState.PERM_TV)
        } catch (e: Exception) {
            app.onPermissionResult(false)
        }
    }

    fun pickDocument(types: Array<String>, callback: (Uri?) -> Unit) {
        pickCallback = callback
        try {
            documentLauncher.launch(types)
        } catch (e: ActivityNotFoundException) {
            pickCallback = null
            app.toast("Bu TV'de dosya seçici yok. Bir USB dosya yöneticisi gerekli.")
        } catch (e: Exception) {
            pickCallback = null
            app.toast("Dosya seçici açılamadı")
        }
    }

    fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            app.toast("Uygulama ayarları açılamadı")
        }
    }

    private fun isSystemKey(code: Int): Boolean = when (code) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MUTE,
        KeyEvent.KEYCODE_POWER,
        KeyEvent.KEYCODE_TV_POWER,
        KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_SLEEP,
        KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_SETTINGS,
        KeyEvent.KEYCODE_TV_INPUT,
        KeyEvent.KEYCODE_ASSIST,
        KeyEvent.KEYCODE_VOICE_ASSIST -> true
        else -> false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!::app.isInitialized) return super.dispatchKeyEvent(event)
        val code = event.keyCode
        if (isSystemKey(code)) return super.dispatchKeyEvent(event)
        if (app.textInputActive() && code != KeyEvent.KEYCODE_BACK && code != KeyEvent.KEYCODE_ESCAPE) {
            return super.dispatchKeyEvent(event)
        }
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (app.onKey(code)) {
                    consumedKeys.add(code)
                    true
                } else super.dispatchKeyEvent(event)
            }
            KeyEvent.ACTION_UP -> {
                if (consumedKeys.remove(code)) true else super.dispatchKeyEvent(event)
            }
            else -> super.dispatchKeyEvent(event)
        }
    }

    override fun onStart() {
        super.onStart()
        if (::app.isInitialized) app.onStart()
    }

    override fun onStop() {
        if (::app.isInitialized) app.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::app.isInitialized) app.onDestroy()
        super.onDestroy()
    }
}
