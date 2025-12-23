package com.cesar.creamazospokemontcg

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        val etCorreo: EditText = findViewById(R.id.etCorreo)
        val etContraseña: EditText = findViewById(R.id.etContraseña)
        val btnIniciarSesion: Button = findViewById(R.id.btnIniciarSesion)
        val tvOlvidasteContraseña: TextView = findViewById(R.id.tvOlvidasteContraseña)
        val btnIniciarSesionGoogle: Button = findViewById(R.id.btnIniciarSesionGoogle)
        val tvRegistrarse: TextView = findViewById(R.id.tvRegistrarse)

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

        tvOlvidasteContraseña.setOnClickListener {
            val correo = etCorreo.text.toString()
            if (correo.isNotEmpty()) {
                auth.sendPasswordResetEmail(correo)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(baseContext, "Correo de recuperación enviado.",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(baseContext, "Correo no encontrado.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(baseContext, "Ingresa tu correo electrónico.",
                    Toast.LENGTH_SHORT).show()
            }
        }
// Lógica para que el botón de google funcione
        private lateinit var googleSignInClient: GoogleSignInClient

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)

            // Configurar Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

            btnIniciarSesionGoogle.setOnClickListener {
                startActivityForResult(googleSignInClient.signInIntent, RC_SIGN_IN)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

            if (requestCode == RC_SIGN_IN) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account!!)
                } catch (e: ApiException) {
                    Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        actualizarUI(user)
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        actualizarUI(null)
                    }
                }
        }

        companion object {
            private const val RC_SIGN_IN = 9001
        }
        btnIniciarSesionGoogle.setOnClickListener {
            // Aquí implementarás la lógica para iniciar sesión con Google
            // Esto requerirá configurar Google Sign-In en Firebase y en tu aplicación
        }

        tvRegistrarse.setOnClickListener {
            // Aquí implementarás la lógica para navegar a la pantalla de registro
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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