package com.yidont.customview.captcha

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.yidont.customview.R

class CaptchaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_captcha)

        findViewById<CaptchaLayout>(R.id.captchaLayout).apply {
//            imageView.setImageResource(R.mipmap.dog1)
//            imageView.refresh()
            verify = {
                if (it) {
                    Toast.makeText(context, "验证成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "验证失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}