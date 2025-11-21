package com.cesar.creamazospoketcg.data.repository

import android.util.Log
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.network.ModuloRed
import com.cesar.creamazospoketcg.data.network.CartaTCGdexBreve
import com.cesar.creamazospoketcg.data.network.CartaTCGdexCompleta
import com.cesar.creamazospoketcg.data.network.aCarta
import com.cesar.creamazospoketcg.data.network.aCartaCompleta
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * RepositorioCartas consolidado: busca cartas (lista breve) y detalle por id.
 */
class RepositorioCartas {

    private val servicio = ModuloRed.servicioTCGdex
    private val httpClient = OkHttpClient()
    private val gson = Gson()

    suspend fun buscarCartas(consulta: String?): Result<List<Carta>> {
        return withContext(Dispatchers.IO) {
            val filtros = consulta?.takeIf { it.isNotBlank() }?.let { crudo ->
                if (crudo.contains("=") || crudo.contains(":")) mapOf("name" to crudo)
                else mapOf("name" to crudo.trim())
            } ?: emptyMap()

            try {
                val respuesta = servicio.listarCartas(idioma = "en", filtros = filtros)
                if (respuesta.isSuccessful) {
                    val cuerpo: List<CartaTCGdexBreve>? = respuesta.body()
                    val listaCartas = cuerpo?.map { it.aCarta() } ?: emptyList()
                    Result.success(listaCartas)
                } else {
                    Result.failure(Exception("Error API TCGdex: ${respuesta.code()} ${respuesta.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * obtenerCartaPorId (corregido): pedimos al endpoint v2 correcto de TCGdex.
     * Usamos OkHttp y Gson para evitar depender de la versión actual del servicio Retrofit
     * si éste estuviera configurado con la ruta equivocada.
     */
    suspend fun obtenerCartaPorId(id: String): Result<Carta> {
        return withContext(Dispatchers.IO) {
            try {
                // Endpoint correcto (v2, plural "cards")
                val url = "https://api.tcgdex.net/v2/en/cards/$id"
                Log.d("RepositorioCartas", "Pidiendo detalle: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string()

                    if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                        // Intentamos parsear directamente a CartaTCGdexCompleta (DTO que ya usas)
                        return@withContext try {
                            val dto: CartaTCGdexCompleta = gson.fromJson(bodyStr, CartaTCGdexCompleta::class.java)
                            Result.success(dto.aCartaCompleta())
                        } catch (e: Exception) {
                            Log.e("RepositorioCartas", "Error mapeando JSON a CartaTCGdexCompleta", e)
                            Result.failure(Exception("Error parseando detalle carta"))
                        }
                    } else {
                        // Registrar info útil para depuración
                        Log.e("RepositorioCartas", "Error API detalle carta $code : $bodyStr")
                        return@withContext Result.failure(Exception("Error API detalle carta: $code"))
                    }
                }
            } catch (e: Exception) {
                Log.e("RepositorioCartas", "Excepción al pedir detalle carta id=$id", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtener varias cartas por ids (usa detalle para cada id para asegurar datos completos).
     */
    suspend fun obtenerCartasPorIds(ids: List<String>): Result<List<Carta>> {
        return withContext(Dispatchers.IO) {
            try {
                val resultados = mutableListOf<Carta>()
                for (id in ids) {
                    val r = obtenerCartaPorId(id)
                    if (r.isSuccess) r.getOrNull()?.let { resultados.add(it) }
                }
                Result.success(resultados)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
