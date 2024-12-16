package com.example.closeup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth:  FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailInput = findViewById<EditText>(R.id.etEmail)
        val passwordInput = findViewById<EditText>(R.id.etPassword)
        val nameInput = findViewById<EditText>(R.id.etName)
        val aliasInput = findViewById<EditText>(R.id.etAlias)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val name = nameInput.text.toString()
            val alias = aliasInput.text.toString()

            if (email.isNotEmpty() &&
                password.isNotEmpty() &&
                name.isNotEmpty() &&
                alias.isNotEmpty())
            {
                registerUser(email, password, name, alias)
            }
            else {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String, name: String, alias: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()

                    val usuario = hashMapOf(
                        "id" to alias,
                        "nombre" to name,
                        "email" to email,
                    )
                    db.collection("usuarios")
                        .document(auth.currentUser!!.uid)
                        .set(usuario, SetOptions.merge())
                } else {
                    Log.e("RegisterActivity", "Error de registro: ${task.exception?.message}")
                    Toast.makeText(this, "Error de registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}