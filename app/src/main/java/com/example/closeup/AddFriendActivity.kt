package com.example.closeup

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddFriendActivity : AppCompatActivity() {

    private lateinit var etFriendEmail: EditText
    private lateinit var etFriendId: EditText
    private lateinit var btnAddFriend: Button
    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        // Inicializa Firebase
        database = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicializa vistas
        etFriendEmail = findViewById(R.id.etFriendEmail)
        etFriendId = findViewById(R.id.etFriendId)
        btnAddFriend = findViewById(R.id.btnAddFriend)

        btnAddFriend.setOnClickListener {
            val friendEmail = etFriendEmail.text.toString().trim()
            val friendId = etFriendId.text.toString().trim()

            if (friendEmail.isNotEmpty() && friendId.isNotEmpty()) {
                addFriendToFirestore(friendEmail, friendId)
            } else {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addFriendToFirestore(friendEmail: String, friendId: String) {
        val currentUser = auth.currentUser ?: return
        val friendData = hashMapOf(
            "amigoid" to friendId,
            "email" to friendEmail,
            "usuarioid" to currentUser.uid
        )

        database.collection("amigos")
            .add(friendData)
            .addOnSuccessListener {
                Toast.makeText(this, "Amigo agregado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar amigo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
