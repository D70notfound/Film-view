package net.nepuview.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import net.nepuview.data.Film
import java.io.File
import java.io.FileOutputStream

object ShareCardGenerator {

    fun share(context: Context, film: Film, poster: Bitmap?) {
        val bitmap = generateCard(context, film, poster)
        val file = File(context.cacheDir, "share_${film.id}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Schau dir ${film.title} auf NepuView an!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Teilen via"))
    }

    private fun generateCard(context: Context, film: Film, poster: Bitmap?): Bitmap {
        val width = 1080
        val height = 600
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        canvas.drawColor(Color.parseColor("#040608"))

        // Poster
        poster?.let {
            val scaled = Bitmap.createScaledBitmap(it, 280, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
        }

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 52f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(film.title.take(30), 320f, 120f, titlePaint)

        // Subtitle info
        val subPaint = Paint().apply {
            color = Color.parseColor("#6e7b9a")
            textSize = 36f
            isAntiAlias = true
        }
        if (film.year.isNotBlank()) canvas.drawText(film.year, 320f, 180f, subPaint)
        if (film.genre.isNotEmpty()) canvas.drawText(film.genre.take(3).joinToString(" · "), 320f, 230f, subPaint)

        // Rating
        val ratingPaint = Paint().apply {
            color = Color.parseColor("#f4b942")
            textSize = 44f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        if (film.rating > 0) canvas.drawText("★ %.1f".format(film.rating), 320f, 300f, ratingPaint)

        // Brand
        val brandPaint = Paint().apply {
            color = Color.parseColor("#e63946")
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("NepuView", 320f, height - 40f, brandPaint)

        return bmp
    }
}
