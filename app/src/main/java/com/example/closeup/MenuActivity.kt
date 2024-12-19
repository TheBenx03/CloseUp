@file:Suppress("DEPRECATION")

package com.example.closeup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.io.ByteArrayOutputStream

class MenuActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    // Ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Botones y Vistas
    private lateinit var btnLogout: Button
    private lateinit var btnAbrirCamara: Button
    private lateinit var btnshareLocation: Button
    private lateinit var btnAddFriendMenu: Button
    private lateinit var imgCapturada: ImageView

    // Mapa
    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap

    // Cámara
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de Firebase
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Verifica autenticación
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

        // Inicializar Vistas
        initializeViews()

        // Configuración del Mapa
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl)
            mapBoxMap = map
            startMarkAllFriends(database)
            startMarkLocation()
        }

        // Escuchar mensajes en tiempo real
        listenForIncomingMessages()
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        btnAbrirCamara = findViewById(R.id.btn_open_camera)
        btnLogout = findViewById(R.id.btnLogout)
        imgCapturada = findViewById(R.id.imgCapturada)
        btnshareLocation = findViewById(R.id.btn_share_location)
        btnAddFriendMenu = findViewById(R.id.btnAddFriendMenu)

        // Inicializar servicios de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar botones
        btnAbrirCamara.setOnClickListener { abrirCamara() }
        btnAddFriendMenu.setOnClickListener {
            val intent = Intent(this, AddFriendActivity::class.java)
            startActivity(intent)
        }
        btnshareLocation.setOnClickListener { shareLocationWithImage() }
        btnLogout.setOnClickListener { confirmLogout() }

        // Inicializar cámara
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                imgCapturada.setImageBitmap(imageBitmap)
                Toast.makeText(this, "Imagen capturada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(intent)
        }
    }

    private fun shareLocationWithImage() {
        if (imgCapturada.drawable == null) {
            Toast.makeText(this, "Primero captura una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                selectFriend { friendId ->
                    uploadImageAndLocation(friendId, location.latitude, location.longitude)
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    private fun selectFriend(onFriendSelected: (String) -> Unit) {
        val friendsList = mutableListOf<String>()
        val friendIds = mutableListOf<String>()

        database.collection("amigos")
            .whereEqualTo("usuarioid", auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val email = doc.getString("email") ?: "Desconocido"
                    val id = doc.getString("amigoid") ?: ""
                    friendsList.add(email)
                    friendIds.add(id)
                }

                AlertDialog.Builder(this)
                    .setTitle("Selecciona un amigo")
                    .setItems(friendsList.toTypedArray()) { _, which ->
                        onFriendSelected(friendIds[which])
                    }
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar amigos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageAndLocation(friendId: String, latitude: Double, longitude: Double) {
        val bitmap = (imgCapturada.drawable as BitmapDrawable).bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)

        val data = hashMapOf(
            "senderId" to auth.currentUser!!.uid,
            "receiverId" to friendId,
            "latitude" to latitude,
            "longitude" to longitude,
            "image" to base64Image,
            "timestamp" to System.currentTimeMillis()
        )

        database.collection("imagenes")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Ubicación e imagen enviadas correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> logoutUser() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun addMarkerWithImage(location: LatLng, senderId: String, imageBitmap: Bitmap) {
        val marker = mapBoxMap.addMarker(
            MarkerOptions()
                .position(location)
                .title("Enviado por: $senderId")
                .snippet("Toca aquí para ver la imagen")
        )

        mapBoxMap.setOnInfoWindowClickListener { clickedMarker ->
            if (clickedMarker == marker) {
                showImagePreview(imageBitmap)
                true
            } else {
                false
            }
        }
    }

    private fun showImagePreview(imageBitmap: Bitmap?) {
        val dialog = AlertDialog.Builder(this)
        val imageView = ImageView(this)

        if (imageBitmap != null) {
            Log.d("ImagePreview", "Bitmap Width: ${imageBitmap.width}, Height: ${imageBitmap.height}")
            imageView.setImageBitmap(imageBitmap)
        } else {
            Log.e("ImagePreview", "El Bitmap está vacío o nulo")
            Toast.makeText(this, "Error: No se pudo cargar la imagen", Toast.LENGTH_SHORT).show()
            return
        }

        dialog.setView(imageView)
        dialog.setPositiveButton("Cerrar") { dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        dialog.show()
    }


    private fun listenForIncomingMessages() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        database.collection("imagenes")
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "Error al escuchar mensajes: $e")
                    return@addSnapshotListener
                }

                for (doc in snapshots!!.documentChanges) {
                    when (doc.type) {
                        DocumentChange.Type.ADDED -> {
                            val senderId = doc.document.getString("senderId") ?: "Desconocido"
                            val latitude = doc.document.getDouble("latitude") ?: 0.0
                            val longitude = doc.document.getDouble("longitude") ?: 0.0
                            val base64Image = doc.document.getString("image") ?: ""
                            if (base64Image.isBlank()) {
                                Log.e("Firestore", "La cadena Base64 está vacía o inválida")
                                continue
                            }

                            val imageBitmap = decodeBase64ToBitmap(base64Image)
                            if (imageBitmap == null) {
                                Log.e("Firestore", "Error al decodificar la imagen para el documento ${doc.document.id}")
                                continue
                            }

                            val location = LatLng(latitude, longitude)
                            if (imageBitmap != null) {
                                addMarkerWithImage(location, senderId, imageBitmap)
                            }
                        }

                        DocumentChange.Type.MODIFIED -> TODO()
                        DocumentChange.Type.REMOVED -> TODO()
                    }
                }
            }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("DecodeBase64", "Error al decodificar la imagen: ${e.message}")
            null
        }
    }


    private fun startMarkAllFriends(db: FirebaseFirestore) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("amigos")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val friends = doc.data as? Map<String, Any> ?: return@addOnSuccessListener
                for (friendId in friends.keys) {
                    db.collection("coordenadas")
                        .document(friendId)
                        .get()
                        .addOnSuccessListener { document ->
                            val lat = document.getDouble("latitud")
                            val lon = document.getDouble("longitud")
                            if (lat != null && lon != null) {
                                val location = LatLng(lat, lon)
                                addMarkerWithImage(location, "Amigo", Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
                            }
                        }
                }
            }
    }

    private fun startMarkLocation() {
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val targetLocation = LatLng(latitude, longitude)

        mapBoxMap.cameraPosition = CameraPosition.Builder()
            .target(targetLocation)
            .zoom(14.0)
            .build()

        addMarkerWithImage(targetLocation, "Ubicación Actual", Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }
}
