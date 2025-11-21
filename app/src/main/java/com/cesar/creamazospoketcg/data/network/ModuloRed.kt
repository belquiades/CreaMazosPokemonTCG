package com.cesar.creamazospoketcg.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ModuloRed
 *
 * Configuración de Retrofit/OkHttp para usar la API de TCGdex.
 * - BASE_URL se puede ajustar según la instancia que uses.
 * - Logging a BODY durante desarrollo para ver peticiones/respuestas en Logcat.
 */

object ModuloRed {

    // URL base para la API TCGdex (ajusta si usas tcgdex.dev u otra instancia)
    private const val BASE_URL = "https://api.tcgdex.net/"

    // Interceptor de logging (ver peticiones/respuestas)
    private val registro = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Interceptor para añadir cabeceras comunes
    private val interceptorCabeceras = Interceptor { chain ->
        val original: Request = chain.request()
        val peticion = original.newBuilder()
            .header("Accept", "application/json")
            .method(original.method, original.body)
            .build()
        chain.proceed(peticion)
    }

    // Cliente OkHttp con timeouts razonables para desarrollo
    private val cliente: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptorCabeceras)
        .addInterceptor(registro)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Servicio Retrofit para TCGdex (se crea perezosamente)
    val servicioTCGdex: ServicioTCGdex by lazy {
        retrofit.create(ServicioTCGdex::class.java)
    }
}
