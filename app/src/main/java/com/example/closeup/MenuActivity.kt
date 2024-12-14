@file:Suppress("DEPRECATION")

package com.example.closeup

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

class MenuActivity : AppCompatActivity() {

    // Botón para cerrar sesión
    private lateinit var btnLogout: Button

    // Camara y Imagenes
    private lateinit var btnAbrirCamara: Button
    private lateinit var imgCapturada: ImageView

    //Mapa y ubicacion
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica si el usuario está autenticado
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        // Configuración de MapTiler
        val key = BuildConfig.MAPTILER_API_KEY
        val mapId = "streets-v2"
        val styleUrl = "https://api.maptiler.com/maps/$mapId/style.json?key=$key"
        Mapbox.getInstance(this)

        setContentView(R.layout.activity_menu)

        // Inicializa vistas
        mapView = findViewById(R.id.mapView)
        btnAbrirCamara = findViewById(R.id.btn_open_camera)
        btnLogout = findViewById(R.id.btnLogout) // Inicializa el botón de logout
        // imgCapturada = findViewById(R.id.imgCapturada)

        mapView.onCreate(savedInstanceState)

        // Configuración del mapa
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl) { style ->
                // Obtén las coordenadas del Intent
                val hasLatitude = intent.hasExtra("latitude")
                val hasLongitude = intent.hasExtra("longitude")

                if (hasLatitude && hasLongitude) {
                    val latitude = intent.getDoubleExtra("latitude", 0.0)
                    val longitude = intent.getDoubleExtra("longitude", 0.0)

                    val targetLocation = LatLng(latitude, longitude)

                    map.cameraPosition = CameraPosition.Builder()
                        .target(targetLocation)
                        .zoom(14.0)
                        .build()

                    addMarkerToMap(map, targetLocation)
                } else {
                    Toast.makeText(
                        this,
                        "No se proporcionaron coordenadas válidas.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Configura el botón para abrir la cámara
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                imgCapturada.setImageBitmap(imageBitmap)
                Toast.makeText(this, "Imagen capturada", Toast.LENGTH_SHORT).show()
            }
        }

        btnAbrirCamara.setOnClickListener {
            abrirCamara()
        }

        // Configura el botón para cerrar sesión
        btnLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(intent)
        }
    }

    private fun addMarkerToMap(map: MapboxMap, location: LatLng) {
        map.addMarker(
            com.mapbox.mapboxsdk.annotations.MarkerOptions()
                .position(location)
                .title("Ubicación actual")
        )
    }

    private fun confirmLogout() {
        // Mostrar un cuadro de diálogo de confirmación para cerrar sesión
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logoutUser()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logoutUser() {
        // Cierra sesión en Firebase
        FirebaseAuth.getInstance().signOut()

        // Redirige al MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
