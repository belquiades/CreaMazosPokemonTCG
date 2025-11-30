package com.cesar.creamazospokemontcg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etCorreo: EditText = findViewById(R.id.etCorreo)
        val etContraseña: EditText = findViewById(R.id.etContraseña)
        val btnIniciarSesion: Button = findViewById(R.id.btnIniciarSesion)

        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text.toString()
            val contraseña = etContraseña.text.toString()

            if (correo.isNotEmpty() && contraseña.isNotEmpty()) {
                auth.signInWithEmailAndPassword(correo, contraseña)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Inicio de sesión exitoso, actualizar UI con la información del usuario autenticado
                            val user = auth.currentUser
                            actualizarUI(user)
                        } else {
                            // Si el inicio de sesión falla, mostrar un mensaje al usuario
                            Toast.makeText(baseContext, "Autenticación fallida.",
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}