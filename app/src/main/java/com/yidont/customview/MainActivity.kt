package com.yidont.customview

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yidont.customview.captcha.CaptchaActivity
import com.yidont.customview.signededition.SignedEditionActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun signed(view: View) {
        startActivity(Intent(this, SignedEditionActivity::class.java))
    }

    fun captcha(view: View) {
        startActivity(Intent(this, CaptchaActivity::class.java))
    }
}