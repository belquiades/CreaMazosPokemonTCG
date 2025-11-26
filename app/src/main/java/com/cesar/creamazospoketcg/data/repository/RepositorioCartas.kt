package com.cesar.creamazospoketcg.data.repository

import android.util.Log
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.network.ModuloRed
import com.cesar.creamazospoketcg.data.network.CartaDTO
import com.cesar.creamazospoketcg.data.network.RespuestaListaCartasTCGdex
import com.cesar.creamazospoketcg.data.network.CartaCompletaDTO
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

    /**
     * Busca cartas usando la API TCGdex (vía Retrofit).
     * La consulta se transforma en q="name:consulta" si se pasa texto.
     */
    suspend fun buscarCartas(consulta: String?): Result<List<Carta>> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = "https://api.tcgdex.net/v2/en/cards"
                val url = consulta?.takeIf { it.isNotBlank() }?.let { q ->
                    "$baseUrl?name=${java.net.URLEncoder.encode(q.trim(), "UTF-8")}"
                } ?: baseUrl

                Log.d("RepositorioCartas", "buscarCartas -> URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val code = response.code
                    val bodyStr = response.body?.string()

                    if (!response.isSuccessful) {
                        Log.e("RepositorioCartas", "Error HTTP buscarCartas: $code -> $bodyStr")
                        return@withContext Result.failure(Exception("Error HTTP: $code"))
                    }

                    if (bodyStr.isNullOrBlank()) {
                        Log.w("RepositorioCartas", "Respuesta vacía buscarCartas")
                        return@withContext Result.success(emptyList())
                    }

                    val trimmed = bodyStr.trimStart()
                    val listaMaps: List<Map<String, Any>> = try {
                        if (trimmed.startsWith("{")) {
                            // { "data": [ ... ] } -> extraer data
                            val wrapper = gson.fromJson(bodyStr, Map::class.java) as? Map<*, *>
                            val dataAny = wrapper?.get("data")
                            if (dataAny is List<*>) {
                                // convertir List<*> a List<Map<String,Any>>
                                @Suppress("UNCHECKED_CAST")
                                dataAny.filterIsInstance<Map<String, Any>>()
                            } else emptyList()
                        } else if (trimmed.startsWith("[")) {
                            // Array directo -> parsear como List<Map<String,Any>>
                            @Suppress("UNCHECKED_CAST")
                            gson.fromJson(bodyStr, object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type)
                                    as? List<Map<String, Any>> ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("RepositorioCartas", "Error parseando JSON en buscarCartas", e)
                        return@withContext Result.failure(e)
                    }

                    val listaCartas = mutableListOf<Carta>()

                    for (m in listaMaps) {
                        try {
                            val id = m["id"] as? String ?: continue
                            val name = (m["name"] as? String) ?: (m["title"] as? String) ?: "Sin nombre"

                            // images puede venir en m["images"] como map con small/large
                            val imagesObj = m["images"] as? Map<*, *>
                            val small = imagesObj?.get("small") as? String
                            val large = imagesObj?.get("large") as? String

                            // types y rarity
                            val typesList = (m["types"] as? List<*>)?.mapNotNull { it as? String }
                            val rarity = m["rarity"] as? String

                            // set info si viene
                            val setObj = m["set"] as? Map<*, *>
                            val setInfo = if (setObj != null) {
                                com.cesar.creamazospoketcg.data.model.SetInfo(
                                    id = setObj["id"] as? String,
                                    name = setObj["name"] as? String
                                )
                            } else null

                            val carta = Carta(
                                id = id,
                                name = name,
                                types = typesList,
                                rarity = rarity,
                                set = setInfo,
                                images = com.cesar.creamazospoketcg.data.model.ImagenesCarta(
                                    small = small,
                                    large = large
                                )
                            )

                            listaCartas.add(carta)
                        } catch (e: Exception) {
                            Log.w("RepositorioCartas", "Ignorando elemento inválido en lista de cartas", e)
                            // seguir con los demás
                        }
                    }

                    Result.success(listaCartas)
                }
            } catch (e: Exception) {
                Log.e("RepositorioCartas", "Excepción en buscarCartas", e)
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
                        return@withContext try {
                            // Parseamos al DTO que tienes definido: CartaCompletaDTO
                            val dto: CartaCompletaDTO = gson.fromJson(bodyStr, CartaCompletaDTO::class.java)
                            Result.success(dto.aCartaCompleta())
                        } catch (e: Exception) {
                            Log.e("RepositorioCartas", "Error mapeando JSON a CartaCompletaDTO", e)
                            Result.failure(Exception("Error parseando detalle carta"))
                        }
                    } else {
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
                Log.e("RepositorioCartas", "Excepción en obtenerCartasPorIds", e)
                Result.failure(e)
            }
        }
    }
}
