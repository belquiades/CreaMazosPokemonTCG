package com.cesar.creamazospoketcg.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cesar.creamazospoketcg.R

/**
 * MainActivity
 * -----------------------
 * Activity principal que actúa como contenedor de navegación.
 * Dentro aloja el NavHostFragment indicado en activity_main.xml.
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
