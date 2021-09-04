package com.yidont.customview.captcha

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withClip
import androidx.core.view.marginLeft
import com.yidont.customview.R
import kotlin.math.abs
import kotlin.random.Random

/**
 * 滑动拼图验证码
 * Created on 2021/9/4
 * @author zwonb
 */
class CaptchaLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    lateinit var verify: (Boolean) -> Unit
    val imageView: CaptchaImageView = CaptchaImageView(context, attrs)
    private val scrollBar: CaptchaScrollBar = CaptchaScrollBar(context, attrs)
    private val animator = ObjectAnimator.ofFloat(this, "animX", 0f, 4f.dp, 0f, (-4f).dp, 0f)
        .apply {
            duration = 80
            repeatCount = 3
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener {
                it.doOnStart { scrollBar.isEnabled = false }
                it.doOnEnd {
                    imageView.reset()
                    scrollBar.reset()
                }
            }
        }


    init {
//        imageView.setImageResource(R.mipmap.dog1)
//        imageView.refresh()
        imageView.id = View.generateViewId()
        val lp1 = LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.MATCH_CONSTRAINT)
        lp1.dimensionRatio = "16:9"
        lp1.topToTop = LayoutParams.PARENT_ID
        lp1.leftToLeft = LayoutParams.PARENT_ID
        lp1.rightToRight = LayoutParams.PARENT_ID
        addView(imageView, lp1)
        val lp2 = LayoutParams(LayoutParams.MATCH_PARENT, 48.dp)
        lp2.topToBottom = imageView.id
        addView(scrollBar, lp2)

        scrollBar.scrollListener = {
            imageView.scrollGap(it)
        }
        scrollBar.upListener = {
            if (imageView.verify(it)) {
                scrollBar.isEnabled = false
                verify(true)
            } else {
                animator.start()
                verify(false)
            }
        }
    }

    var animX = 0f
        set(x) {
            imageView.verifyFailAnim(x)
        }


    /**
     * 拼图
     * Created on 2021/9/1
     * @author zwonb
     */
    class CaptchaImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        private val gapLeftMargin: Float = 24f.dp,
        defStyleAttr: Int = 0
    ) : AppCompatImageView(context, attrs, defStyleAttr) {

        private val paintGap = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private val pathGap: Path = Path()
        private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private val pathLine = Path()
        private val paintScrollGap = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        var gapSize: Float = 56f.dp
        private var randomX: Float = 0f
        private var randomY: Float = 0f
        private val rect = RectF()
        private var firstBlockIndex = 0
        private var isConcaveFirst = true
        private var secondBlockIndex = 1
        private var isConcaveSecond = false

        private var bitmapGap: Bitmap? = null
        private var drawableBitmap: Bitmap? = null
        private lateinit var gapCanvas: Canvas
        private var gapOffset = 0f

        private var success = false
        private val successBitmap: Bitmap
        private val paintSuccess = Paint()

        init {
            scaleType = ScaleType.CENTER_CROP
            setImageResource(R.mipmap.dog1)
            paintGap.apply {
                style = Paint.Style.FILL
                color = Color.BLACK
                alpha = (0.7f * 255).toInt()
            }
            paintLine.apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f.dp
                color = Color.WHITE
                alpha = (0.65f * 255).toInt()
            }
            successBitmap = ContextCompat.getDrawable(context, R.drawable.ic_success)!!.toBitmap()
            paintSuccess.apply {
                color = Color.parseColor("#CCFFFFFF")
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            randomGapPath()
            gapPathLine()
            post {
                scrollGapBitmap()
                invalidate()
            }
        }

        fun refresh() {
            gapOffset = 0f
            randomGapPath()
            gapPathLine()
            post {
                scrollGapBitmap()
                invalidate()
            }
        }

        fun scrollGap(x: Float) {
            gapOffset = x
            invalidate()
        }

        fun verify(x: Float): Boolean {
            val verify = abs(rect.left - gapLeftMargin - x) < gapSize / 3f * 0.45f
            if (verify) {
                success = true
                invalidate()
            }
            return verify
        }

        fun verifyFailAnim(x: Float) {
            gapOffset += x
            invalidate()
        }

        fun reset() {
            gapOffset = 0f
            invalidate()
        }

        private fun randomGapPath() {
            randomXY()
            randomGapLocation()
            pathGap.reset()
            pathGap.fillType = Path.FillType.EVEN_ODD
            pathGap.moveTo(randomX, randomY)
            pathGap.addRect(
                randomX,
                randomY,
                randomX + gapSize,
                randomY + gapSize,
                Path.Direction.CW
            )

            rect.setEmpty()
            blockRect(rect, randomX, randomY, gapSize, firstBlockIndex)
            if (!rect.isEmpty) {
                pathGap.arcTo(rect, startAngle(isConcaveFirst, firstBlockIndex), 180f, true)
            }
            rect.setEmpty()
            blockRect(rect, randomX, randomY, gapSize, secondBlockIndex)
            if (!rect.isEmpty) {
                pathGap.arcTo(rect, startAngle(isConcaveSecond, secondBlockIndex), 180f, true)
            }
            // 生成随机块的位置
            pathGap.computeBounds(rect, false)
        }

        private fun randomXY() {
            val blockWidth = gapSize / 3f
            randomX = try {
                Random.nextDouble(
                    blockWidth + width * 0.45, width - (gapSize + blockWidth + width * 0.1)
                ).toFloat()
            } catch (e: Exception) {
                blockWidth
            }
            randomY = try {
                Random.nextDouble(
                    blockWidth + height * 0.1, height - (gapSize + blockWidth + height * 0.1)
                ).toFloat()
            } catch (e: Exception) {
                blockWidth
            }
        }

        private fun randomGapLocation() {
            firstBlockIndex = Random.nextInt(0, 3)
            isConcaveFirst = Random.nextBoolean()
            secondBlockIndex = Random.nextInt(firstBlockIndex + 1, 4)
            isConcaveSecond = Random.nextBoolean()
        }

        private fun startAngle(isConcave: Boolean, blockIndex: Int): Float = if (isConcave) {
            if (blockIndex % 2 == 0) 0f else 90f
        } else {
            if (blockIndex % 2 == 0) 180f else 270f
        }

        private fun blockRect(rect: RectF, x: Float, y: Float, gapSize: Float, blockIndex: Int) {
            val blockWidth = gapSize / 3f
            when (blockIndex) {
                0 -> rect.set(
                    x + blockWidth, y - blockWidth / 2f,
                    x + blockWidth * 2f, y + blockWidth / 2f
                )
                1 -> rect.set(
                    x + gapSize - blockWidth / 2f, y + blockWidth,
                    x + gapSize + blockWidth / 2f, y + blockWidth * 2f
                )
                2 -> rect.set(
                    x + blockWidth, y + gapSize - (blockWidth / 2f),
                    x + blockWidth * 2f, y + gapSize + blockWidth / 2f
                )
                3 -> rect.set(
                    x - (blockWidth / 2f), y + blockWidth,
                    x + (blockWidth / 2f), y + blockWidth * 2f
                )
            }
        }

        private fun gapPathLine() {
            pathLine.reset()
            pathLine.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            pathLine.op(pathGap, Path.Op.INTERSECT)
        }

        private fun scrollGapBitmap() {
            bitmapGap?.recycle()
            drawableBitmap = drawable?.toBitmap() ?: return
            bitmapGap = Bitmap.createBitmap(
                drawableBitmap!!.width, drawableBitmap!!.height, Bitmap.Config.ARGB_8888
            )
            gapCanvas = Canvas(bitmapGap!!).apply {
                paintScrollGap.setShadowLayer(5f.dp, 3f.dp, 3f.dp, Color.BLACK)
                drawPath(pathGap, paintScrollGap)
                paintScrollGap.clearShadowLayer()
                withClip(pathGap) {
                    setMatrix(imageMatrix)
                    drawBitmap(drawableBitmap!!, 0f, 0f, paintScrollGap)
                }
                drawPath(pathLine, paintLine)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawPath(pathGap, paintGap)
            canvas.drawPath(pathLine, paintLine)

            bitmapGap?.let {
                canvas.drawBitmap(it, -rect.left + gapLeftMargin + gapOffset, 0f, paintScrollGap)
            }

            if (success) {
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintSuccess)
                canvas.drawBitmap(
                    successBitmap,
                    width / 2f - successBitmap.width / 2f,
                    height / 2f - successBitmap.height / 2f,
                    null
                )
            }
        }
    }

    /**
     * 滑动条
     * Created on 2021/9/3
     * @author zwonb
     */
    class CaptchaScrollBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val paintBar = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        var barHeight = 14f.dp
        private var startY = 0f
        private var startX = 0f
        private var stopY = 0f
        private var stopX = 0f

        private val paintBitmap = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        private var slideBitmap: Bitmap? = null
        var bitmapDefLeft = 0f
        private var bitmapTop = 0f
        private var downX = -1f
        private var bitmapOffset = 0f
        private var leftLimit = bitmapDefLeft
        private var rightLimit = 0f

        lateinit var scrollListener: (Float) -> Unit
        lateinit var upListener: (Float) -> Unit

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
            paintBar.apply {
                style = Paint.Style.FILL
                strokeCap = Paint.Cap.ROUND
                strokeWidth = barHeight
                color = Color.parseColor("#E4E4E4")
            }
            paintBitmap.apply {
                color = Color.TRANSPARENT
                setShadowLayer(2f.dp, 2f.dp, 2f.dp, Color.parseColor("#8D007AFF"))
            }
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            val horizontalOffset = barHeight / 2f
            val verticalOffset = h / 2f
            startX = horizontalOffset
            startY = verticalOffset
            stopX = w - horizontalOffset
            stopY = verticalOffset

            slideBitmap = getBitmap() ?: return
            bitmapDefLeft = 24f.dp
            bitmapTop = (h - slideBitmap!!.height) / 2f
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            slideBitmap ?: return true
            if (!isEnabled) return true
            val x = event.x
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (x in bitmapDefLeft..bitmapDefLeft + slideBitmap!!.width) {
                        downX = x
                        leftLimit = downX - bitmapDefLeft
                        rightLimit = width - (bitmapDefLeft + slideBitmap!!.width - downX)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (downX != -1f) {
                        when {
                            x in leftLimit..rightLimit -> {
                                bitmapOffset = x - downX
                                scrollListener(bitmapOffset)
                                invalidate()
                            }
                            x < leftLimit -> {
                                bitmapOffset = leftLimit - downX
                                scrollListener(bitmapOffset)
                                invalidate()
                            }
                            x > rightLimit -> {
                                bitmapOffset = rightLimit - downX
                                scrollListener(bitmapOffset)
                                invalidate()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (downX != -1f) {
                        upListener(bitmapOffset)
                    }
                    downX = -1f
                }
            }
            return true
        }

        fun reset() {
            isEnabled = true
            bitmapOffset = 0f
            downX = -1f
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawLine(startX, startY, stopX, stopY, paintBar)
            slideBitmap?.let {
                canvas.drawBitmap(
                    it.extractAlpha(),
                    bitmapDefLeft + bitmapOffset,
                    bitmapTop,
                    paintBitmap
                )
                canvas.drawBitmap(it, bitmapDefLeft + bitmapOffset, bitmapTop, null)
            }
        }

        private fun getBitmap(): Bitmap? {
            return ContextCompat.getDrawable(context, R.drawable.ic_slide)?.toBitmap()
        }
    }
}

val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Int.dp get() = this.toFloat().dp.toInt()

