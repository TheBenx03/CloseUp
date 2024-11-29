package com.example.closeup

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MenuActivity : AppCompatActivity() {
    private lateinit var btnAbrirCamara: Button
    private lateinit var imgCapturada: ImageView
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        btnAbrirCamara = findViewById(R.id.btn_open_camera)
        // imgCapturada = findViewById(R.id.imgCapturada)

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK){
                val imageBitmap = result.data?.extras?.get("data")as Bitmap
                imgCapturada.setImageBitmap(imageBitmap)
            }
        }

        btnAbrirCamara.setOnClickListener {
            abrirCamara()
        }
    }

    private fun abrirCamara(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intent.resolveActivity(packageManager) != null){
            takePictureLauncher.launch(intent)
        }
    }
}