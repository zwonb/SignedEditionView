package com.yidont.customview

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun reset(view: View) {
        findViewById<SignedEditionView>(R.id.signedEditionView).reset()
    }

    fun save(view: View) {
        val file = File(externalCacheDir, "signed.jpg")
        findViewById<SignedEditionView>(R.id.signedEditionView).save(file)
    }
}