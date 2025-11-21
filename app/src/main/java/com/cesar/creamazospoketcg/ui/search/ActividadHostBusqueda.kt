package com.cesar.creamazospoketcg.ui.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cesar.creamazospoketcg.R

/**
 * ActividadHostBusqueda
 *
 * Actividad simple que aloja el FragmentoBusquedaCartas para probar la pantalla.
 * Temporalmente puedes marcar esta actividad como LAUNCHER en el AndroidManifest
 * para arrancar directamente la pantalla de búsqueda.
 *
 * Ruta:
 * app/src/main/java/com/cesar/creamazospoketcg/ui/search/ActividadHostBusqueda.kt
 */
class ActividadHostBusqueda : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_host_busqueda)
        // El FragmentContainerView en el layout ya está configurado con el fragment por atributo android:name,
        // por tanto no hace falta código adicional aquí.
    }
}
