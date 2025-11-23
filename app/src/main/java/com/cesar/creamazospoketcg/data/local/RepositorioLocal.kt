package com.cesar.creamazospoketcg.data.local

import android.content.Context
import android.util.Log
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.CartaTCGdexBreve
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * RepositorioLocal
 *
 * Maneja el almacenamiento de la colección de cartas del usuario.
 * Guarda datos en SharedPreferences como JSON usando Gson.
 *
 * Ventajas:
 *  - Código centralizado, no duplicado.
 *  - Cualquier fragmento puede añadir, eliminar o leer cartas.
 *  - Fácil de migrar a Firestore en el futuro.
 */
class RepositorioLocal(context: Context) {

    private val PREFS = "prefs_mis_cartas"
    private val KEY_COLECCION = "coleccion_cartas"

    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Cargar lista de CartaTCGdexBreve desde SharedPreferences.
     */
    fun cargarColeccion(): List<CartaTCGdexBreve> {
        val json = prefs.getString(KEY_COLECCION, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CartaTCGdexBreve>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("RepositorioLocal", "Error parseando JSON", e)
            emptyList()
        }
    }

    /**
     * Guardar lista de CartaTCGdexBreve en SharedPreferences.
     */
    private fun salvarColeccion(lista: List<CartaTCGdexBreve>) {
        val json = gson.toJson(lista)
        prefs.edit().putString(KEY_COLECCION, json).apply()
    }

    /**
     * Añadir una carta a la colección.
     * Si ya existe, no la duplica.
     */
    fun guardarCarta(carta: Carta): Boolean {
        val imagen = carta.images?.small ?: carta.images?.large ?: return false

        val breve = CartaTCGdexBreve(
            id = carta.id,
            localId = null,
            name = carta.name,
            image = imagen
        )

        val lista = cargarColeccion().toMutableList()

        if (lista.any { it.id == breve.id }) {
            return false // ya existe
        }

        lista.add(0, breve)
        salvarColeccion(lista)
        return true
    }

    /**
     * Eliminar por ID.
     */
    fun eliminarCarta(idCarta: String): Boolean {
        val lista = cargarColeccion().toMutableList()
        val borrado = lista.removeAll { it.id == idCarta }
        if (borrado) salvarColeccion(lista)
        return borrado
    }

    /**
     * Buscar una carta por su ID.
     */
    fun obtenerCarta(idCarta: String): CartaTCGdexBreve? {
        return cargarColeccion().find { it.id == idCarta }
    }
}
