package tw.idv.niugnep.pwamaker.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import tw.idv.niugnep.pwamaker.MainActivity
import tw.idv.niugnep.pwamaker.R
import tw.idv.niugnep.pwamaker.model.PwaConfig

object ShortcutUtils {

    const val EXTRA_LAUNCH_VIEWER = "extra_launch_viewer"
    const val EXTRA_PWA_ID = "extra_pwa_id"
    const val EXTRA_PWA_NAME = "extra_pwa_name"
    const val EXTRA_PWA_URL = "extra_pwa_url"
    const val EXTRA_PWA_ICON_URI = "extra_pwa_icon_uri"
    const val EXTRA_PWA_UA_MODE = "extra_pwa_ua_mode"
    const val EXTRA_PWA_CUSTOM_UA = "extra_pwa_custom_ua"

    fun createShortcut(context: Context, config: PwaConfig) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, context.getString(R.string.toast_shortcut_not_supported), Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("pwamaker://pwa/${config.id}")
            putExtra(EXTRA_LAUNCH_VIEWER, true)
            putExtra(EXTRA_PWA_ID, config.id)
            putExtra(EXTRA_PWA_NAME, config.name)
            putExtra(EXTRA_PWA_URL, config.getFormattedUrl())
            putExtra(EXTRA_PWA_ICON_URI, config.iconUri)
            putExtra(EXTRA_PWA_UA_MODE, config.uaMode.name)
            putExtra(EXTRA_PWA_CUSTOM_UA, config.customUa)
            flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }

        val bitmap = loadOrGenerateIcon(context, config)
        val icon = IconCompat.createWithBitmap(bitmap)

        val shortcut = ShortcutInfoCompat.Builder(context, config.id)
            .setShortLabel(config.name.ifBlank { context.getString(R.string.app_name) })
            .setLongLabel(config.name.ifBlank { context.getString(R.string.app_name) })
            .setIcon(icon)
            .setIntent(intent)
            .build()

        val pinnedShortcutCallbackIntent = ShortcutManagerCompat.createShortcutResultIntent(context, shortcut)
        val successPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            pinnedShortcutCallbackIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, successPendingIntent.intentSender)
        Toast.makeText(context, context.getString(R.string.toast_shortcut_requested, config.name), Toast.LENGTH_SHORT).show()
    }

    fun launchPwaInNewWindow(context: Context, config: PwaConfig) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("pwamaker://pwa/${config.id}")
            putExtra(EXTRA_LAUNCH_VIEWER, true)
            putExtra(EXTRA_PWA_ID, config.id)
            putExtra(EXTRA_PWA_NAME, config.name)
            putExtra(EXTRA_PWA_URL, config.getFormattedUrl())
            putExtra(EXTRA_PWA_ICON_URI, config.iconUri)
            putExtra(EXTRA_PWA_UA_MODE, config.uaMode.name)
            putExtra(EXTRA_PWA_CUSTOM_UA, config.customUa)
            flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        context.startActivity(intent)
    }

    fun loadOrGenerateIcon(context: Context, config: PwaConfig): Bitmap {
        val uriStr = config.iconUri
        if (!uriStr.isNull_or_empty_uri()) {
            try {
                val uri = Uri.parse(uriStr)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val original = BitmapFactory.decodeStream(inputStream)
                    if (original != null) {
                        return scaleCenterSquare(original, 192)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return generateDefaultIcon(config.name)
    }

    private fun String?.isNull_or_empty_uri(): Boolean = this.isNull_or_blank_str()
    private fun String?.isNull_or_blank_str(): Boolean = this == null || this.isBlank()

    private fun scaleCenterSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)
        val x = (width - size) / 2
        val y = (height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
        return Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    }

    private fun generateDefaultIcon(title: String): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6750A4") // Material M3 Primary color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        // Draw letter
        val text = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 96f
            textAlign = Paint.Align.CENTER
        }
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, size / 2f, yPos, textPaint)

        return bitmap
    }
}
