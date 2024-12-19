package com.example.closeup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()
    private val database = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        recyclerView = findViewById(R.id.recyclerViewUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Cargar usuarios desde Firestore
        database.collection("usuarios")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val nombre = document.getString("nombre") ?: "Desconocido"
                    val email = document.getString("email") ?: ""
                    val id = document.id
                    userList.add(User(id, nombre, email))
                }
                userAdapter = UserAdapter(userList) { user ->
                    addFriend(user)
                }
                recyclerView.adapter = userAdapter
            }
    }

    private fun addFriend(user: User) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val friendData = mapOf(
            "amigoid" to user.id,
            "usuarioid" to currentUserId
        )

        // Agregar amigo a Firestore
        database.collection("amigos")
            .add(friendData)
            .addOnSuccessListener {
                Toast.makeText(this, "${user.nombre} agregado como amigo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al agregar amigo", Toast.LENGTH_SHORT).show()
            }
    }
}
