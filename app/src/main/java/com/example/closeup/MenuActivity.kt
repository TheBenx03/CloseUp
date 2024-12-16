@file:Suppress("DEPRECATION")

package com.example.closeup

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap

class MenuActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    // Botón para cerrar sesión
    private lateinit var btnLogout: Button

    // Camara y Imagenes
    private lateinit var btnAbrirCamara: Button
    private lateinit var imgCapturada: ImageView

    //Mapa y ubicacion
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifica si el usuario está autenticado
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

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
        imgCapturada = findViewById(R.id.imgCapturada)
        val compartirUbicacion = findViewById<Button>(R.id.btn_share_location)

        // Configuración del mapa
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl)
            //Simple var para que la primera carga del mapa se salte la fun
            mapBoxMap = map
            markStartingLocation()
            markAllFriends(database)
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

        compartirUbicacion.setOnClickListener {
            //TODO: Lista de amigos, mover camara a amigos
        }
        //listAmigos.setOnClickListener{}
        //Funcion que tome amigos y los dibuje en pantalla
    }

    private fun markAllFriends(db: FirebaseFirestore) {
        db.collection("amigos")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnCompleteListener { doc ->
                val friends = doc.result.data
                for (friend in friends!!.keys){
                    db.collection("coordenadas")
                        .document(friend)
                        .get()
                        .addOnSuccessListener { document ->
                            val lat = document["latitud"]
                            val lon = document["longitud"]
                            val location = LatLng(lat as Double, lon as Double)
                            Log.w("location", location.toString())
                            addMarkerToMap(location, friends[friend].toString())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(baseContext,
                                "Documento no encontrado$e ", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(intent)
        }
    }

    private fun markStartingLocation(){
        // Obtén las coordenadas del Intent
        val hasLatitude = intent.hasExtra("latitude")
        val hasLongitude = intent.hasExtra("longitude")

        if (hasLatitude && hasLongitude) {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)

            val targetLocation = LatLng(latitude, longitude)

            mapBoxMap.cameraPosition = CameraPosition.Builder()
                .target(targetLocation)
                .zoom(14.0)
                .build()

            addMarkerToMap(targetLocation, "Ubicacion Actual")
        }
        else {
            Toast.makeText(
                this,
                "No se proporcionaron coordenadas válidas.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //TODO: MOVER A MENU
    //Busca al usuario en la base de datos y guarda su documento como string
    private fun getCurrentUserDocument(db: FirebaseFirestore){
        db.collection("usuarios")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                val doc = document.data.toString()
                intent.putExtra("currentUserDocument", doc)
                Log.w("MainActivity:getCurrentUserDocument",doc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(baseContext, "Usuario no esta en base de datos$e ", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMarkerToMap(location: LatLng, tag: String) {
        mapBoxMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(tag)
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
        auth.signOut()

        // Redirige al MainActivity
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}