package com.example.closeup

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("MainActivity", "Usuario autenticado: ${currentUser.email}")
            redirectToMenu()
            return
        } else {
            Log.d("MainActivity", "No hay usuario autenticado.")
        }

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si el usuario denegó permisos anteriormente, explica por qué los necesitas
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    "La aplicación necesita acceso a tu ubicación para mostrar el mapa correctamente.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Solicita los permisos
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            // Si los permisos ya están concedidos, obtén la ubicación
            getLastKnownLocation()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido
                getLastKnownLocation()
            } else {
                // Permiso denegado
                Toast.makeText(
                    this,
                    "Permiso de ubicación denegado. No se puede mostrar el mapa.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val intent = Intent(this, MenuActivity::class.java)
                    intent.putExtra("latitude", location.latitude)
                    intent.putExtra("longitude", location.longitude)
                    updateCurrentUserCoordinates(location)
                    startActivity(intent)
                    finish()
                } else {
                    // Manejo estricto: no se redirige si no hay ubicación
                    Toast.makeText(this, "No se pudo obtener la ubicación actual. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al obtener la ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            checkLocationPermission()
        }
    }



    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Inicio de sesión exitoso para: $email")
                    redirectToMenu()
                } else {
                    Log.e("MainActivity", "Error de autenticación: ${task.exception?.message}")
                    Toast.makeText(this, "Error de autenticación: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    loginUser(email, password) // Inicia sesión automáticamente
                } else {
                    Log.e("MainActivity", "Error de registro: ${task.exception?.message}")
                    Toast.makeText(this, "Error de registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    //Busca al usuario en la base de datos y guarda su documento como string
    private fun getCurrentUserDocument(){
        db.collection("usuarios")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                val doc = document.data.toString()
                val intent = Intent(this, MenuActivity::class.java)
                intent.putExtra("currentUserDocument", doc)
                Log.w("UserDocument",doc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(baseContext, "Usuario no esta en base de datos$e ", Toast.LENGTH_SHORT).show()

            }
    }

    //Busca al usuario en las coordenadas y guarda su documento como string
    private fun getCurrentUserCoordinates(){
        db.collection("coordenadas")
            .document(auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener { document ->
                val doc = document.data.toString()
                val intent = Intent(this, MenuActivity::class.java)
                intent.putExtra("currentUserCoords", doc)
                Log.w("UserDocument",doc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(baseContext, "Usuario no esta en base de datos$e ", Toast.LENGTH_SHORT).show()
                //TODO: Crear usuario al registrarse
                //A la hora de crear un registro, tomar esa uid y crear un documento con el usuario
                val coordenadas = hashMapOf(
                    "id" to "nick?",
                    "nombre" to "nombre",
                    "email" to "email@email.cl",
                    "amigos" to arrayOf(String)
                )
                db.collection("coordenadas")
                    .document(auth.currentUser!!.uid)
                    .set(coordenadas, SetOptions.merge())
            }

    }

    //Actualiza las coordenadas del usuario a las actuales
    private fun updateCurrentUserCoordinates(location: Location){
        val coordenadas = hashMapOf(
            "latitud" to location.latitude,
            "longitud" to location.longitude,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("coordenadas")
            .document(auth.currentUser!!.uid)
            .set(coordenadas, SetOptions.merge())
    }

    private fun redirectToMenu() {
        getCurrentUserDocument()
        getLastKnownLocation()
    }

    //TODO: Compartir ubicacion actual a tus amigos
    //TODO: Enviar ubicacion a amigo, añade marcador y mueve camara(?)
        //Simplemente dibujar en el mapa todas las ultimas ubicaciones de tus amigos
}