package com.yidont.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 签名版
 * Created on 2021/8/30
 * @author zwonb
 */
class SignedEditionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.BLACK
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private var penX = 0f
    private var penY = 0f

    private lateinit var srcBitmap: Bitmap
    private lateinit var srcBitmapCanvas: Canvas
    private lateinit var cropRect: RectF
    var bitmapOffset = 10 // 清除白边的间距
    var signed = false
    var downOnce: (() -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cropRect = RectF()
        srcBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
        srcBitmapCanvas = Canvas(srcBitmap)
        srcBitmapCanvas.drawColor(Color.WHITE)
        signed = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!signed) downOnce?.invoke()

                penX = event.x
                penY = event.y
                path.moveTo(penX, penY)
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                val offsetX = abs(x - penX)
                val offsetY = abs(y - penY)
                if (offsetX >= 3 || offsetY >= 3) {
                    val x2 = (penX + x) / 2
                    val y2 = (penY + y) / 2
                    path.quadTo(penX, penY, x2, y2)
                    penX = x
                    penY = y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!signed) signed = true
                srcBitmapCanvas.drawPath(path, paint)
                path.computeBounds(cropRect, false)
            }
        }
        return true
    }

    fun reset() {
        signed = false
        path.reset()
        invalidate()
        if (::srcBitmapCanvas.isInitialized) {
            srcBitmapCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR)
            srcBitmapCanvas.drawColor(Color.WHITE)
        }
    }

    fun save(file: File) {
        val rect = cropRect.toRect()
        val bitmapX = max(rect.left - bitmapOffset, 0)
        val bitmapY = max(rect.top - bitmapOffset, 0)
        val bitmapWidth = min(rect.right - bitmapX + bitmapOffset, width - bitmapX)
        val bitmapHeight = min(rect.bottom - bitmapY + bitmapOffset, height - bitmapY)
        val bitmap = Bitmap.createBitmap(srcBitmap, bitmapX, bitmapY, bitmapWidth, bitmapHeight)

        if (file.exists()) file.delete()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, file.outputStream())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

}