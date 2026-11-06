package com.ollie.chesscoach

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var projection: MediaProjection
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val engine by lazy { StockfishEngine(this) }
    private var overlayView: OverlayView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        val data: Intent? = intent?.getParcelableExtra("data")
        if (data == null) { stopSelf(); return START_NOT_STICKY }

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        imageReader = ImageReader.newInstance(
            metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2
        )
        virtualDisplay = projection.createVirtualDisplay(
            "coach", metrics.widthPixels, metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface, null, handler
        )

        overlayView = OverlayView(this).also { it.attach() }
        engine.start()

        imageReader!!.setOnImageAvailableListener({ reader ->
            if (busy) { reader.acquireLatestImage()?.close(); return@setOnImageAvailableListener }
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            busy = true
            val bmp = toBitmap(image)
            image.close()
            handler.post {
                val pos = BoardRecognizer.recognize(bmp)
                if (pos != null) {
                    engine.analyze(pos.toFen()) { best ->
                        overlayView?.update(best)
                        busy = false
                    }
                } else {
                    overlayView?.clear()
                    busy = false
                }
            }
        }, handler)

        return START_STICKY
    }

    private fun toBitmap(image: android.media.Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bmp = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
    }

    private fun buildNotification(): Notification {
        val ch = NotificationChannel("coach", "Chess Coach",
            NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(ch)
        return NotificationCompat.Builder(this, "coach")
            .setContentTitle("Chess Coach running")
            .setContentText("Analysing your board")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()
    }

    override fun onDestroy() {
        engine.stop()
        virtualDisplay?.release()
        imageReader?.close()
        overlayView?.detach()
        super.onDestroy()
    }
}