package com.example.movieapplication.ui.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.raywenderlich.android.movieapp.R
import com.raywenderlich.android.movieapp.databinding.ActivityDialogBinding

class AboutDialogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialog)

        binding = ActivityDialogBinding.inflate(layoutInflater)
        binding.okButton.setOnClickListener { finish() }
    }

}
