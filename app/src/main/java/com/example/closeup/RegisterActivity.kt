package com.example.closeup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Firebase Auth y Firestore
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Referencias a los elementos del layout
        val nameInput = findViewById<EditText>(R.id.etName)
        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        // Acción para registrarse
        registerButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(name, email, password)
            } else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    // Guardar datos en Firestore
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email
                    )

                    firestore.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show()
                            // Redirigir al inicio de sesión
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error al guardar en Firestore: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Fallo en el registro
                    Toast.makeText(this, "Error de registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
