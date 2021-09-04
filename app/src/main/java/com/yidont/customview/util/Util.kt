package com.yidont.customview.util

import android.content.res.Resources
import android.graphics.*
import android.util.TypedValue
import com.yidont.customview.R
import kotlin.random.Random

/**
 * Created on 2021/9/1
 * @author zwonb
 */

val Float.dp
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Int.dp get() = this.toFloat().dp.toInt()
