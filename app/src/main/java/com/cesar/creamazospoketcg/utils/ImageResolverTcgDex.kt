package com.cesar.creamazospoketcg.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.core.content.edit
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Resolver de imágenes exclusivo TCGdex (REST -> assets).
 * - Cachea resultados en memoria + SharedPreferences (persistente).
 * - Hace preload con Glide cuando resuelve (ayuda al detalle).
 */
object ImageResolverTcgDex {
    private const val TAG = "ImageResolverTcgDex"
    private const val PREFS = "tcgdex_image_cache"
    private const val PREFS_PREFIX = "resolved_"

    // In-memory mirror of SharedPreferences
    private val memoryCache = mutableMapOf<String, String?>()

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, "OkHttp: $msg") }
        logging.level = HttpLoggingInterceptor.Level.BASIC
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    fun loadInto(imageView: ImageView, carta: Carta) {
        val context = imageView.context.applicationContext
        val lifecycleOwner = imageView.findViewTreeLifecycleOwner()
        val scope = lifecycleOwner?.lifecycleScope

        val runBlock: (suspend () -> Unit) -> Unit = { susp ->
            if (scope != null) {
                scope.launch { susp() }
            } else {
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) { susp() }
            }
        }

        runBlock {
            val key = cacheKeyFor(carta)

            // 1) In-memory
            memoryCache[key]?.let { cached ->
                if (cached.isNotBlank()) {
                    loadWithGlideThumbnail(imageView, cached)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_carta)
                }
                return@runBlock
            }

            // 2) SharedPreferences
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val spVal = prefs.getString(PREFS_PREFIX + key, null)
            if (!spVal.isNullOrBlank()) {
                memoryCache[key] = spVal
                loadWithGlideThumbnail(imageView, spVal)
                return@runBlock
            } else if (spVal != null && spVal.isBlank()) {
                memoryCache[key] = ""
                imageView.setImageResource(R.drawable.placeholder_carta)
                return@runBlock
            }

            // 3) Resolver (REST -> assets)
            var resolved: String? = null
            val lookupId = carta.id ?: carta.localId
            if (!lookupId.isNullOrBlank()) {
                resolved = tryResolveFromTcgDexApi(lookupId)
            }
            if (resolved.isNullOrBlank()) {
                resolved = tryResolveFromTcgDexAssets(carta)
            }

            // 4) Guardar y cargar / fallback
            if (!resolved.isNullOrBlank()) {
                memoryCache[key] = resolved
                prefs.edit { putString(PREFS_PREFIX + key, resolved) }
                loadWithGlideThumbnail(imageView, resolved)
                // PRELOAD para detalle
                preloadFullImage(imageView.context, resolved)
            } else {
                memoryCache[key] = ""
                prefs.edit { putString(PREFS_PREFIX + key, "") }
                imageView.setImageResource(R.drawable.placeholder_carta)
            }
        }
    }

    // Permite que FragmentoDetalleCarta consulte la URL cacheada
    fun getCachedUrl(context: Context, carta: Carta): String? {
        val key = cacheKeyFor(carta)
        memoryCache[key]?.let { return if (it.isNotBlank()) it else null }
        val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = sp.getString(PREFS_PREFIX + key, null)
        return if (v.isNullOrBlank()) null else v
    }

    private fun cacheKeyFor(c: Carta): String {
        return c.id ?: c.localId ?: ((c.set?.id ?: "no_set") + "#" + (c.localId ?: "no_local"))
    }

    private fun preloadFullImage(context: android.content.Context, url: String) {
        try {
            Glide.with(context).load(url).preload()
        } catch (e: Exception) {
            Log.w(TAG, "Preload failed for $url", e)
        }
    }

    private fun loadWithGlideThumbnail(imageView: ImageView, url: String) {
        imageView.post {
            Glide.with(imageView.context)
                .load(url)
                .centerCrop() // thumbnail en lista
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .into(imageView)
        }
    }

    // -------------------------
    // TCGdex REST API: https://api.tcgdex.net/v2/en/cards/{id}
    // -------------------------
    private suspend fun tryResolveFromTcgDexApi(lookupId: String): String? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.tcgdex.net/v2/en/cards/$lookupId"
            val req = Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("User-Agent", "ProyectoDAM-PokeTCG/1.0 (Android)")
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.d(TAG, "TCGdex API ${resp.code} for $lookupId")
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val root = org.json.JSONObject(body)
                val candidateBase = if (root.has("image")) root.optString("image", null) else null
                if (candidateBase.isNullOrBlank()) return@withContext null

                val qualities = listOf("", "high", "low")
                val exts = listOf("", "png", "webp", "jpg")
                for (q in qualities) {
                    for (ext in exts) {
                        val url = when {
                            q.isBlank() && ext.isBlank() -> candidateBase
                            q.isBlank() -> "$candidateBase.$ext"
                            ext.isBlank() -> "$candidateBase/$q"
                            else -> "$candidateBase/$q.$ext"
                        }
                        if (probeUrlProbablyImage(url)) {
                            Log.d(TAG, "TCGdex API-derived ok -> $url")
                            return@withContext url
                        }
                    }
                }
                Log.d(TAG, "TCGdex API returned image base but no variant ok: $candidateBase")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error tcgdex API resolve for $lookupId", e)
            return@withContext null
        }
    }

    // -------------------------
    // TCGdex assets reconstruction probe
    // -------------------------
    private suspend fun tryResolveFromTcgDexAssets(carta: Carta): String? = withContext(Dispatchers.IO) {
        try {
            val setId = carta.set?.id?.trim() ?: run {
                val compound = carta.id?.trim()
                if (!compound.isNullOrBlank() && compound.contains("-")) compound.split("-", limit = 2)[0] else null
            }
            var local = carta.localId?.trim() ?: run {
                val compound = carta.id?.trim()
                if (!compound.isNullOrBlank() && compound.contains("-")) compound.split("-", limit = 2)[1] else null
            }

            if (setId.isNullOrBlank() || local.isNullOrBlank()) {
                Log.d(TAG, "TCGdex: setId/local insuficientes (set='$setId', local='$local')")
                return@withContext null
            }

            local = local.trimStart('#').trim()
            val localNoLeading = local.trimStart('0').ifEmpty { local }
            val serie = setId.replace(Regex("\\d+$"), "")
            val qualities = listOf("high", "low", "")
            val exts = listOf("png", "webp", "jpg", "")

            val tried = mutableListOf<String>()
            for (q in qualities) {
                for (ext in exts) {
                    val url = when {
                        q.isBlank() && ext.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$local"
                        q.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$local.$ext"
                        ext.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$local/$q"
                        else -> "https://assets.tcgdex.net/en/$serie/$setId/$local/$q.$ext"
                    }
                    tried.add(url)
                    try {
                        if (probeUrlProbablyImage(url)) {
                            Log.d(TAG, "TCGdex assets ok -> $url")
                            return@withContext url
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error probing tcgdex asset $url", e)
                    }
                }
            }

            if (localNoLeading != local) {
                for (q in qualities) {
                    for (ext in exts) {
                        val url = when {
                            q.isBlank() && ext.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$localNoLeading"
                            q.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$localNoLeading.$ext"
                            ext.isBlank() -> "https://assets.tcgdex.net/en/$serie/$setId/$localNoLeading/$q"
                            else -> "https://assets.tcgdex.net/en/$serie/$setId/$localNoLeading/$q.$ext"
                        }
                        tried.add(url)
                        try {
                            if (probeUrlProbablyImage(url)) {
                                Log.d(TAG, "TCGdex assets ok (noLeading) -> $url")
                                return@withContext url
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error probing tcgdex asset $url", e)
                        }
                    }
                }
            }

            Log.d(TAG, "TCGdex tried urls: ${tried.joinToString(", ")}")
            return@withContext null
        } catch (e: Exception) {
            Log.w(TAG, "fetchFromTcgDexAssets unexpected error", e)
            return@withContext null
        }
    }

    // HEAD then GET probe
    private fun probeUrlProbablyImage(url: String): Boolean {
        return try {
            val reqHead = Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", "ProyectoDAM-PokeTCG/1.0 (Android)")
                .build()
            httpClient.newCall(reqHead).execute().use { resp ->
                if (resp.isSuccessful) {
                    val ct = resp.header("Content-Type") ?: ""
                    if (ct.lowercase().contains("image") || ct.lowercase().contains("png") || ct.lowercase().contains("jpeg") || ct.lowercase().contains("webp")) {
                        return true
                    }
                }
            }

            val reqGet = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "ProyectoDAM-PokeTCG/1.0 (Android)")
                .build()
            httpClient.newCall(reqGet).execute().use { resp2 ->
                if (resp2.isSuccessful) {
                    val ct2 = resp2.header("Content-Type") ?: ""
                    return ct2.lowercase().contains("image") || ct2.lowercase().contains("png") || ct2.lowercase().contains("jpeg") || ct2.lowercase().contains("webp")
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "probeUrlProbablyImage exception for $url", e)
            false
        }
    }
}
