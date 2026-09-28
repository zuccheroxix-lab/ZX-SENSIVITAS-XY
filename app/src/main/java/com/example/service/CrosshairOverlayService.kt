package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class CrosshairOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"
        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "zx_crosshair_channel"

        private val _currentConfig = MutableStateFlow(CrosshairConfig())
        val currentConfig = _currentConfig.asStateFlow()

        fun updateLiveConfig(newConfig: CrosshairConfig) {
            _currentConfig.value = newConfig
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: CrosshairView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_STOP -> {
                    stopOverlay()
                    stopSelf()
                    return START_NOT_STICKY
                }
                ACTION_START, ACTION_UPDATE -> {
                    if (!Settings.canDrawOverlays(this)) {
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(
                            NOTIFICATION_ID,
                            buildNotification(),
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, buildNotification())
                    }
                    showOrUpdateOverlay()
                }
            }
        } catch (e: Throwable) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CrosshairOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZX Crosshair Active")
            .setContentText("Precision tactical reticle overlay is running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pOpen)
            .addAction(android.R.drawable.ic_delete, "Stop Overlay", pStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZX Crosshair Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent status while crosshair overlay is active."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showOrUpdateOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val config = _currentConfig.value
        val density = resources.displayMetrics.density
        val totalSizePx = ((config.sizeDp + 40f) * density).roundToInt()

        if (overlayView == null) {
            overlayView = CrosshairView(this).apply {
                updateConfig(config)
            }

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            layoutParams = WindowManager.LayoutParams(
                totalSizePx,
                totalSizePx,
                overlayType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                x = config.offsetX
                y = config.offsetY
            }

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            overlayView?.setOnTouchListener { _, event ->
                val cfg = _currentConfig.value
                if (cfg.isLocked) {
                    return@setOnTouchListener false
                }
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        layoutParams?.x = initialX + dx
                        layoutParams?.y = initialY + dy
                        layoutParams?.let { windowManager?.updateViewLayout(overlayView, it) }
                        _currentConfig.value = cfg.copy(
                            offsetX = layoutParams?.x ?: 0,
                            offsetY = layoutParams?.y ?: 0
                        )
                        true
                    }
                    else -> false
                }
            }

            try {
                windowManager?.addView(overlayView, layoutParams)
            } catch (e: Exception) {
                // handle error gracefully
            }
        } else {
            overlayView?.updateConfig(config)
            layoutParams?.let { lp ->
                lp.width = totalSizePx
                lp.height = totalSizePx
                lp.x = config.offsetX
                lp.y = config.offsetY
                try {
                    windowManager?.updateViewLayout(overlayView, lp)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun stopOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        overlayView = null
        _currentConfig.value = _currentConfig.value.copy(isEnabled = false)
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    class CrosshairView(context: Context) : View(context) {
        private var config: CrosshairConfig = CrosshairConfig()
        private var customBitmap: Bitmap? = null

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(120, 0, 0, 0)
        }

        fun updateConfig(newConfig: CrosshairConfig) {
            this.config = newConfig
            val colorInt = (newConfig.colorHex and 0xFFFFFFFFL).toInt()
            val alpha = (newConfig.opacity * 255).roundToInt().coerceIn(0, 255)

            linePaint.color = colorInt
            linePaint.alpha = alpha
            linePaint.strokeWidth = newConfig.thicknessDp * resources.displayMetrics.density

            shadowPaint.strokeWidth = linePaint.strokeWidth + 2f * resources.displayMetrics.density

            fillPaint.color = colorInt
            fillPaint.alpha = alpha

            if (newConfig.style == CrosshairStyle.CUSTOM_IMAGE && newConfig.customImageUri != null) {
                loadCustomBitmap(newConfig.customImageUri)
            } else {
                customBitmap = null
            }
            invalidate()
        }

        private fun loadCustomBitmap(uriStr: String) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    customBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                customBitmap = null
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val density = resources.displayMetrics.density
            val sizePx = config.sizeDp * density
            val halfSize = sizePx / 2f
            val gapPx = config.gapDp * density
            val thicknessPx = config.thicknessDp * density

            when (config.style) {
                CrosshairStyle.CLASSIC_CROSS -> {
                    // Shadow
                    canvas.drawLine(cx - halfSize, cy, cx + halfSize, cy, shadowPaint)
                    canvas.drawLine(cx, cy - halfSize, cx, cy + halfSize, shadowPaint)
                    // Fore
                    canvas.drawLine(cx - halfSize, cy, cx + halfSize, cy, linePaint)
                    canvas.drawLine(cx, cy - halfSize, cx, cy + halfSize, linePaint)
                    canvas.drawCircle(cx, cy, thicknessPx / 2f + 1f, fillPaint)
                }

                CrosshairStyle.DOT -> {
                    val dotRadius = (config.sizeDp * density) / 3f
                    canvas.drawCircle(cx, cy, dotRadius + 1.5f, shadowPaint)
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                }

                CrosshairStyle.GAP_CROSS -> {
                    val startGap = gapPx.coerceAtLeast(2f)
                    // Left
                    canvas.drawLine(cx - halfSize, cy, cx - startGap, cy, shadowPaint)
                    canvas.drawLine(cx - halfSize, cy, cx - startGap, cy, linePaint)
                    // Right
                    canvas.drawLine(cx + startGap, cy, cx + halfSize, cy, shadowPaint)
                    canvas.drawLine(cx + startGap, cy, cx + halfSize, cy, linePaint)
                    // Top
                    canvas.drawLine(cx, cy - halfSize, cx, cy - startGap, shadowPaint)
                    canvas.drawLine(cx, cy - halfSize, cx, cy - startGap, linePaint)
                    // Bottom
                    canvas.drawLine(cx, cy + startGap, cx, cy + halfSize, shadowPaint)
                    canvas.drawLine(cx, cy + startGap, cx, cy + halfSize, linePaint)
                    // Center dot
                    canvas.drawCircle(cx, cy, thicknessPx / 2f, fillPaint)
                }

                CrosshairStyle.T_SHAPE -> {
                    val startGap = gapPx.coerceAtLeast(2f)
                    // Left
                    canvas.drawLine(cx - halfSize, cy, cx - startGap, cy, shadowPaint)
                    canvas.drawLine(cx - halfSize, cy, cx - startGap, cy, linePaint)
                    // Right
                    canvas.drawLine(cx + startGap, cy, cx + halfSize, cy, shadowPaint)
                    canvas.drawLine(cx + startGap, cy, cx + halfSize, cy, linePaint)
                    // Bottom only
                    canvas.drawLine(cx, cy + startGap, cx, cy + halfSize, shadowPaint)
                    canvas.drawLine(cx, cy + startGap, cx, cy + halfSize, linePaint)
                    // Center dot
                    canvas.drawCircle(cx, cy, thicknessPx / 2f, fillPaint)
                }

                CrosshairStyle.CIRCLE_DOT -> {
                    val radius = halfSize
                    shadowPaint.style = Paint.Style.STROKE
                    canvas.drawCircle(cx, cy, radius, shadowPaint)
                    canvas.drawCircle(cx, cy, radius, linePaint)
                    canvas.drawCircle(cx, cy, thicknessPx * 1.2f, fillPaint)
                }

                CrosshairStyle.BOX_CROSS -> {
                    val r = halfSize
                    val cornerLen = r * 0.45f
                    // Top-Left
                    canvas.drawLine(cx - r, cy - r, cx - r + cornerLen, cy - r, linePaint)
                    canvas.drawLine(cx - r, cy - r, cx - r, cy - r + cornerLen, linePaint)
                    // Top-Right
                    canvas.drawLine(cx + r, cy - r, cx + r - cornerLen, cy - r, linePaint)
                    canvas.drawLine(cx + r, cy - r, cx + r, cy - r + cornerLen, linePaint)
                    // Bottom-Left
                    canvas.drawLine(cx - r, cy + r, cx - r + cornerLen, cy + r, linePaint)
                    canvas.drawLine(cx - r, cy + r, cx - r, cy + r - cornerLen, linePaint)
                    // Bottom-Right
                    canvas.drawLine(cx + r, cy + r, cx + r - cornerLen, cy + r, linePaint)
                    canvas.drawLine(cx + r, cy + r, cx + r, cy + r - cornerLen, linePaint)
                    // Center dot
                    canvas.drawCircle(cx, cy, thicknessPx / 2f, fillPaint)
                }

                CrosshairStyle.CUSTOM_IMAGE -> {
                    val bmp = customBitmap
                    if (bmp != null) {
                        val destRect = RectF(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize)
                        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (config.opacity * 255).roundToInt().coerceIn(0, 255)
                        }
                        canvas.drawBitmap(bmp, null, destRect, paint)
                    } else {
                        // Fallback cross if no image loaded
                        canvas.drawLine(cx - halfSize, cy, cx + halfSize, cy, linePaint)
                        canvas.drawLine(cx, cy - halfSize, cx, cy + halfSize, linePaint)
                        canvas.drawCircle(cx, cy, thicknessPx / 2f, fillPaint)
                    }
                }
            }
        }
    }
}
