@file:Suppress("DEPRECATION")

package com.example.closeup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.coroutines.CoroutineStart
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64

class MenuActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore
    private lateinit var friendList: Map<String, Any>

    // Botón para cerrar sesión
    private lateinit var btnLogout: Button

    // Camara y Imagenes
    private lateinit var btnAbrirCamara: Button
    private lateinit var imgCapturada: ImageView

    //Mapa y ubicacion
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var mapView: MapView
    private lateinit var mapBoxMap: MapboxMap
    private lateinit var btnshareLocation: Button

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
        btnLogout = findViewById(R.id.btnLogout)
        imgCapturada = findViewById(R.id.imgCapturada)
        btnshareLocation = findViewById(R.id.btn_share_location)

        // **Nuevo Botón para Agregar Amigos**
        val btnAddFriendMenu: Button = findViewById(R.id.btnAddFriendMenu)
        btnAddFriendMenu.setOnClickListener {
            val intent = Intent(this, AddFriendActivity::class.java)
            startActivity(intent)
        }

        // Configuración del mapa
        mapView.getMapAsync { map ->
            map.setStyle(styleUrl)
            mapBoxMap = map
            startMarkAllFriends(database)
            startMarkLocation()
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

        btnshareLocation.setOnClickListener {
            // TODO: Lista de amigos, mover cámara a amigos
        }
    }
    private fun shareLocationWithImage() {
        // Verificar si hay una imagen capturada
        if (imgCapturada.drawable == null) {
            Toast.makeText(this, "Primero captura una imagen", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener la ubicación actual
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Mostrar lista de amigos
                selectFriend { friendId ->
                    uploadImageAndLocation(friendId, location.latitude, location.longitude)
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }
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

                // Mostrar diálogo de selección
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Selecciona un amigo")
                builder.setItems(friendsList.toTypedArray()) { _, which ->
                    onFriendSelected(friendIds[which])
                }
                builder.show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar amigos", Toast.LENGTH_SHORT).show()
            }
    }
    private fun uploadImageAndLocation(friendId: String, latitude: Double, longitude: Double) {
        // Convertir imagen a Base64
        val bitmap = (imgCapturada.drawable as BitmapDrawable).bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)


        // Crear un mapa con los datos
        val data = hashMapOf(
            "senderId" to auth.currentUser!!.uid,
            "receiverId" to friendId,
            "latitude" to latitude,
            "longitude" to longitude,
            "image" to base64Image,
            "timestamp" to System.currentTimeMillis()
        )

        // Subir a Firestore
        database.collection("imagenes")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Ubicación e imagen enviadas correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al enviar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun startMarkAllFriends(db: FirebaseFirestore) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("Firestore", "El usuario no está autenticado")
            return
        }

        val userId = currentUser.uid

        // Obtener el documento de "amigos"
        db.collection("amigos")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val friends = doc.data as? Map<String, Any> ?: run {
                    Log.e("Firestore", "No se encontraron amigos o el formato es incorrecto")
                    return@addOnSuccessListener
                }

                // Iterar sobre las claves (IDs de amigos)
                for (friendId in friends.keys) {
                    // Obtener las coordenadas de cada amigo
                    db.collection("coordenadas")
                        .document(friendId)
                        .get()
                        .addOnSuccessListener { document ->
                            val lat = document.getDouble("latitud")
                            val lon = document.getDouble("longitud")

                            if (lat != null && lon != null) {
                                val location = LatLng(lat, lon)
                                Log.d("location", location.toString())
                                addMarkerToMap(location, friends[friendId].toString())
                            } else {
                                Log.e("Firestore", "Latitud o longitud nula para el amigo $friendId")
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                baseContext,
                                "Error al obtener coordenadas: $e",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    // Escuchar cambios en las coordenadas de cada amigo
                    db.collection("coordenadas")
                        .document(friendId)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                Log.e("Firestore", "Error al escuchar actualizaciones", e)
                                return@addSnapshotListener
                            }

                            if (snapshot != null && snapshot.exists()) {
                                val latitude = snapshot.getDouble("latitude")
                                val longitude = snapshot.getDouble("longitude")

                                if (latitude != null && longitude != null) {
                                    Log.d("Firestore", "Ubicación actualizada: Lat: $latitude, Lng: $longitude")
                                    val updatedLocation = LatLng(latitude, longitude)
                                    addMarkerToMap(updatedLocation, friends[friendId].toString())
                                } else {
                                    Log.e("Firestore", "Latitud o longitud nula en la actualización")
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error al obtener el documento de amigos", e)
            }
    }


    private fun markAllFriends(database: FirebaseFirestore) {
        database.collection("amigos")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnCompleteListener { doc ->
                val friends = doc.result.data
                friendList = doc.result.data as Map<String, Any>
                for (friend in friends!!.keys){
                    database.collection("coordenadas")
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

    private fun startMarkLocation(){
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

    private fun markLocation(){
        // Obtén las coordenadas del Intent
        val hasLatitude = intent.hasExtra("latitude")
        val hasLongitude = intent.hasExtra("longitude")

        if (hasLatitude && hasLongitude) {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)

            val targetLocation = LatLng(latitude, longitude)

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

