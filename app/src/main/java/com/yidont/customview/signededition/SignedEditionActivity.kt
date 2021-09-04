package com.yidont.customview.signededition

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.yidont.customview.R
import com.yidont.customview.SignedEditionView
import java.io.File

class SignedEditionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signed)
    }

    fun reset(view: View) {
        findViewById<SignedEditionView>(R.id.signedEditionView).reset()
    }

    fun save(view: View) {
        val file = File(externalCacheDir, "signed.jpg")
        findViewById<SignedEditionView>(R.id.signedEditionView).save(file)
    }
}