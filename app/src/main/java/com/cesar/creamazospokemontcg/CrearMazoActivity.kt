package com.cesar.creamazospokemontcg

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CrearMazoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_mazo)

        val etNombreMazo: EditText = findViewById(R.id.etNombreMazo)
        val btnGuardarMazo: Button = findViewById(R.id.btnGuardarMazo)

        btnGuardarMazo.setOnClickListener {
            val nombreMazo = etNombreMazo.text.toString()
            if (nombreMazo.isNotEmpty()) {
                guardarMazo(nombreMazo)
            } else {
                Toast.makeText(this, "El nombre del mazo no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarMazo(nombreMazo: String) {
        // Lógica para guardar el mazo
        Toast.makeText(this, "Mazo '$nombreMazo' guardado correctamente", Toast.LENGTH_SHORT).show()
        finish()
    }
}