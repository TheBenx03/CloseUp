package com.example.closeup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class User(val id: String, val nombre: String, val email: String)

class UserAdapter(
    private val userList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.txtName.text = user.nombre
        holder.txtEmail.text = user.email
        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    override fun getItemCount() = userList.size
}
