package com.cesar.creamazospokemontcg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        val etCorreoRegistro: EditText = findViewById(R.id.etCorreoRegistro)
        val etContraseñaRegistro: EditText = findViewById(R.id.etContraseñaRegistro)
        val btnRegistrar: Button = findViewById(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val correo = etCorreoRegistro.text.toString()
            val contraseña = etContraseñaRegistro.text.toString()

            if (correo.isNotEmpty() && contraseña.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(correo, contraseña)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Registro exitoso, actualizar UI con la información del usuario autenticado
                            val user = auth.currentUser
                            Toast.makeText(baseContext, "Registro exitoso.",
                                Toast.LENGTH_SHORT).show()
                            actualizarUI(user)
                        } else {
                            // Si el registro falla, mostrar un mensaje al usuario
                            Toast.makeText(baseContext, "Registro fallido.",
                                Toast.LENGTH_SHORT).show()
                            actualizarUI(null)
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Correo y contraseña son requeridos.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarUI(user: FirebaseAuth.User?) {
        if (user != null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}