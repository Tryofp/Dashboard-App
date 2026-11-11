package com.tryofp.pluginnotifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TryoMessagingService.ensureChannel(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 40, 32, 40)
            setBackgroundColor(android.graphics.Color.rgb(16, 24, 39))
        }
        root.addView(TextView(this).apply {
            text = "Tryo Plugin Notifications"
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        })
        statusView = TextView(this).apply {
            text = "Menghubungkan ke Firebase…"
            textSize = 15f
            setTextColor(android.graphics.Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        root.addView(statusView)
        setContentView(root)
        requestNotificationPermissionIfNeeded()
        refreshAndRegisterToken()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
    }

    private fun refreshAndRegisterToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FcmRegistration.register(applicationContext, token) { success, message ->
                    runOnUiThread {
                        statusView.text = if (success) {
                            "Perangkat terhubung. Notifikasi siap diterima."
                        } else {
                            "Token Firebase tersedia, tetapi pendaftaran gagal: $message"
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                statusView.text = "Gagal mengambil token Firebase: ${error.localizedMessage}"
            }
    }
}
