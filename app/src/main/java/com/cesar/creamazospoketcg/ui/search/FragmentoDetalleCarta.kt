package com.cesar.creamazospoketcg.ui.search

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cesar.creamazospoketcg.BuildConfig
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Ataque
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaBinding
import com.cesar.creamazospoketcg.utils.ImageResolverTcgDex
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

class FragmentoDetalleCarta : Fragment() {

    private var _binding: FragmentDetalleCartaBinding? = null
    private val binding get() = _binding!!

    private val TAG = "DetalleCarta"

    private val repo by lazy { RepositorioCartas() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var cartaIdArg: String? = null
    private var imageBaseArg: String? = null

    // Cache + prefs (local a fragment, se usa para otras resoluciones internas)
    private val PREFS_NAME = "img_cache_prefs"
    private val PREFS_KEY_MAP = "img_cache_map"
    private val memoryCache = mutableMapOf<String, String?>()

    // OkHttp client
    private val httpClient by lazy {
        val logging = HttpLoggingInterceptor { msg -> Log.d(TAG, "OkHttp: $msg") }
        logging.level = HttpLoggingInterceptor.Level.BASIC
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(70, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            cartaIdArg = args.getString("arg_id_carta")
            imageBaseArg = args.getString("arg_image_base")
        }
        // cargar cache persistente (tu cache local previa)
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
            val json = prefs.getString(PREFS_KEY_MAP, null)
            if (!json.isNullOrBlank()) {
                val obj = JSONObject(json)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k, null)
                    if (!v.isNullOrBlank()) memoryCache[k] = v
                }
                Log.d(TAG, "Cache prefs cargada: ${memoryCache.size} entradas")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cargando cache prefs", e)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleCartaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.pbCargando.visibility = View.GONE
        binding.groupDetalle.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        binding.btnVolverDetalle.setOnClickListener {
            val popOk = findNavController().popBackStack()
            if (!popOk) {
                findNavController().navigate(R.id.perfilFragment)
            }
        }

        binding.btnAnadirMiColeccion.setOnClickListener {
            val id = cartaIdArg
            if (id == null || id.isBlank()) {
                Toast.makeText(requireContext(), "No hay id de carta para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nombreRaw = binding.tvNombreCarta.text
            val nombre: String
            if (nombreRaw != null && nombreRaw.isNotBlank()) {
                nombre = nombreRaw.toString()
            } else {
                nombre = ""
            }
            val tag = binding.ivCarta.tag
            val imagen: String?
            if (tag is String && tag.isNotBlank()) {
                imagen = tag
            } else {
                imagen = imageBaseArg
            }
            guardarCartaEnFirestore(id, nombre, imagen)
        }

        if (cartaIdArg != null && cartaIdArg!!.isNotBlank()) {
            cargarDetalleCarta(cartaIdArg!!)
        } else {
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = getString(R.string.error_carta_no_encontrada)
        }
    }

    private fun cargarDetalleCarta(id: String) {
        binding.pbCargando.visibility = View.VISIBLE
        binding.groupDetalle.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    repo.obtenerCartaPorId(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción pidiendo detalle carta", e)
                    Result.failure<Carta>(e)
                }
            }

            binding.pbCargando.visibility = View.GONE

            if (resultado.isSuccess) {
                val carta = resultado.getOrNull()
                if (carta != null) {
                    mostrarCartaEnUI(carta)
                } else {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = getString(R.string.error_carta_no_encontrada)
                }
            } else {
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = getString(R.string.error_carta_no_encontrada)
            }
        }
    }

    private fun mostrarCartaEnUI(carta: Carta) {
        Log.d(TAG, "Mostrar carta: $carta")

        // calcular imageCandidate (tu DTO)
        var computedLarge: String? = null
        if (!carta.images?.large.isNullOrBlank()) computedLarge = carta.images?.large

        var computedSmall: String? = null
        if (!carta.images?.small.isNullOrBlank()) computedSmall = carta.images?.small

        var imageCandidate: String? = null
        if (computedLarge != null) {
            imageCandidate = computedLarge
        } else {
            if (computedSmall != null) {
                imageCandidate = computedSmall
            } else {
                imageCandidate = imageBaseArg
            }
        }

        Log.d(TAG, "Computed imageCandidate = '$imageCandidate' (large='$computedLarge', small='$computedSmall', arg='$imageBaseArg')")

        binding.groupDetalle.visibility = View.VISIBLE

        if (!carta.name.isNullOrBlank()) binding.tvNombreCarta.text = carta.name else binding.tvNombreCarta.text = "—"

        val tipoParts = mutableListOf<String>()
        if (!carta.types.isNullOrEmpty()) tipoParts.add(carta.types.joinToString(", "))
        if (!carta.rarity.isNullOrBlank()) tipoParts.add(carta.rarity)
        if (tipoParts.isEmpty()) binding.tvTipoRarity.text = "—" else binding.tvTipoRarity.text = tipoParts.joinToString(" — ")

        // --------------- NUEVO: usar cache de TCGdex si existe ---------------
        val cachedFromTcgDex = ImageResolverTcgDex.getCachedUrl(requireContext(), carta)
        if (!cachedFromTcgDex.isNullOrBlank()) {
            // Cargar imagen cached en modo detalle (fitCenter -> mostrar completa)
            Glide.with(requireContext())
                .load(cachedFromTcgDex)
                .fitCenter()
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .into(binding.ivCarta)
            binding.ivCarta.tag = cachedFromTcgDex
            // no continuamos con probes: ya tenemos la imagen rápida
        } else {
            // Si no hay cached, seguir con tu lógica previa (testAndLoadImage / race)
            binding.ivCarta.tag = imageCandidate

            // Si ya hay imageCandidate (del DTO o arg), cargarla primero
            if (!imageCandidate.isNullOrBlank()) {
                testAndLoadImage(binding.ivCarta, imageCandidate)
            } else {
                // no hay candidate -> race entre CDN y API
                lifecycleScope.launch {
                    binding.pbCargando.visibility = View.VISIBLE
                    val pokeKey: String? = if (BuildConfig.POKETCG_API_KEY.isNotBlank()) BuildConfig.POKETCG_API_KEY else null
                    val url = try {
                        withContext(Dispatchers.IO) { findFirstValidImageUrl(carta, pokeKey) }
                    } catch (e: Exception) {
                        Log.w(TAG, "findFirstValidImageUrl threw", e)
                        null
                    }
                    binding.pbCargando.visibility = View.GONE

                    if (!url.isNullOrBlank()) {
                        val cacheKey = carta.id ?: carta.localId ?: ""
                        if (cacheKey.isNotBlank()) {
                            memoryCache[cacheKey] = url
                            saveToPrefs(cacheKey, url)
                        }
                        Log.d(TAG, "Imagen encontrada (race) -> $url")
                        withContext(Dispatchers.Main) {
                            testAndLoadImage(binding.ivCarta, url)
                        }
                    } else {
                        Log.w(TAG, "No se encontró URL válida vía race. Mostrando placeholder.")
                        withContext(Dispatchers.Main) {
                            binding.ivCarta.setImageBitmap(createTextPlaceholder(carta.name))
                        }
                    }
                }
            }
        }
        // -------------------------------------------------------------------

        val ataques = carta.attacks
        if (ataques == null || ataques.isEmpty()) binding.tvAtaques.text = getString(R.string.no_hay_ataques) else binding.tvAtaques.text = ataques.joinToString(separator = "\n\n") { formatAtaque(it) }

        if (!carta.types.isNullOrEmpty()) binding.tvTipos.text = carta.types.joinToString(", ") else binding.tvTipos.text = "-"
        if (carta.hp != null) binding.tvHP.text = carta.hp.toString() else binding.tvHP.text = "-"
        if (!carta.localId.isNullOrBlank()) binding.tvLocalId.text = carta.localId else binding.tvLocalId.text = "-"
        if (carta.retreat != null) binding.tvRetreat.text = "Retreat: ${carta.retreat}" else binding.tvRetreat.text = "Retreat: -"
        if (!carta.set?.name.isNullOrBlank()) binding.tvSetInfo.text = carta.set?.name else binding.tvSetInfo.text = "-"
    }

    // -------------------------
    // Construye posibles URLs directas del CDN de PokeTCG para intentar antes de llamar a la API.
    // Devuelve una cadena con candidatos separados por '|' (para iterar después).
    // -------------------------
    private fun buildDirectImageUrl(carta: Carta): String? {
        val setId = carta.set?.id?.trim()
        val local = carta.localId?.trim()
        if (setId.isNullOrBlank() || local.isNullOrBlank()) return null

        val candidates = mutableListOf<String>()
        // hires first (el más probable)
        candidates.add("https://images.pokemontcg.io/$setId/${local}_hires.png")
        // variantes habituales
        candidates.add("https://images.pokemontcg.io/$setId/${local}.png")
        candidates.add("https://images.pokemontcg.io/$setId/${local}.jpg")
        // si local tiene ceros delante, intentar versión sin ceros
        val localNoLeading = local.trimStart('0')
        if (localNoLeading.isNotEmpty() && localNoLeading != local) {
            candidates.add("https://images.pokemontcg.io/$setId/${localNoLeading}_hires.png")
            candidates.add("https://images.pokemontcg.io/$setId/${localNoLeading}.png")
        }

        return candidates.joinToString("|")
    }

    // -------------------------
    // Encuentra la primera URL válida (CDN o API) ejecutando tareas en paralelo.
    // Devuelve la URL válida o null si ninguna lo es.
    // (Esta función mantiene la "race" entre direct CDN, TCGdex (API/assets) y PokeTCG)
    // -------------------------
    private suspend fun findFirstValidImageUrl(carta: Carta, pokeKey: String?): String? = coroutineScope {
        val deferreds = mutableListOf<kotlinx.coroutines.Deferred<String?>>()

        // 0) cache local fragment
        val cacheKey = carta.id ?: carta.localId ?: ""
        if (!cacheKey.isNullOrBlank()) {
            val cached = memoryCache[cacheKey]
            if (!cached.isNullOrBlank()) {
                val dCache = async(Dispatchers.IO) {
                    try {
                        if (validateUrlHead(cached) || probeUrlWithGet(cached)) cached else null
                    } catch (e: Exception) {
                        Log.w(TAG, "Cache candidate threw: $cached", e)
                        null
                    }
                }
                deferreds.add(dCache)
            }
        }

        // 1) CDN candidates (images.pokemontcg.io)
        val direct = buildDirectImageUrl(carta)
        if (!direct.isNullOrBlank()) {
            val listCandidates = direct.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            for (candidate in listCandidates) {
                val d = async(Dispatchers.IO) {
                    try {
                        if (validateUrlHead(candidate) || probeUrlWithGet(candidate)) candidate else null
                    } catch (e: Exception) {
                        Log.w(TAG, "CDN candidate threw: $candidate", e)
                        null
                    }
                }
                deferreds.add(d)
            }
        }

        // 2) TCGdex REST API candidate
        val lookupId = if (!carta.id.isNullOrBlank()) carta.id else carta.localId
        if (!lookupId.isNullOrBlank()) {
            val dTcgApi = async(Dispatchers.IO) {
                try {
                    fetchFromTcgDexApiWithRetries(lookupId)
                } catch (e: Exception) {
                    Log.w(TAG, "TCGdex API candidate threw for $lookupId", e)
                    null
                }
            }
            deferreds.add(dTcgApi)
        }

        // 3) TCGdex assets candidate
        val dTcgDexAssets = async(Dispatchers.IO) {
            try {
                fetchFromTcgDexAssets(carta)
            } catch (e: Exception) {
                Log.w(TAG, "TCGdex assets candidate threw for ${carta.id ?: carta.localId}", e)
                null
            }
        }
        deferreds.add(dTcgDexAssets)

        // 4) PokeTCG API candidate (último recurso)
        if (!lookupId.isNullOrBlank()) {
            val dPoke = async(Dispatchers.IO) {
                try {
                    fetchFromPokeTcgApiWithRetries(lookupId, pokeKey)
                } catch (e: Exception) {
                    Log.w(TAG, "PokeTCG API candidate threw for $lookupId", e)
                    null
                }
            }
            deferreds.add(dPoke)
        }

        if (deferreds.isEmpty()) return@coroutineScope null

        try {
            val remaining = deferreds.toMutableList()
            while (remaining.isNotEmpty()) {
                val result = select<String?> {
                    for (d in remaining) {
                        d.onAwait { it }
                    }
                }
                if (!result.isNullOrBlank()) {
                    remaining.filter { !it.isCompleted }.forEach { it.cancel() }
                    return@coroutineScope result
                } else {
                    remaining.removeAll { it.isCompleted }
                }
            }
            return@coroutineScope null
        } finally {
            deferreds.filter { !it.isCompleted }.forEach { it.cancel() }
        }
    }

    // -------------------------
    // PokeTCG fetch (retries + UA)
    // -------------------------
    private suspend fun fetchFromPokeTcgApiWithRetries(lookupId: String, pokeApiKey: String?): String? {
        val apiUrl = "https://api.pokemontcg.io/v2/cards/$lookupId"
        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 500L
        val userAgent = "ProyectoDAM-PokeTCG/1.0 (Android)"

        while (attempt < maxAttempts) {
            attempt++
            try {
                val builder = Request.Builder().url(apiUrl)
                builder.addHeader("User-Agent", userAgent)
                if (!pokeApiKey.isNullOrBlank()) builder.addHeader("X-Api-Key", pokeApiKey)
                val req = builder.build()

                val resp: Response = withContext(Dispatchers.IO) { httpClient.newCall(req).execute() }
                resp.use { r ->
                    if (r.isSuccessful) {
                        val body = r.body?.string()
                        if (!body.isNullOrBlank()) {
                            val root = JSONObject(body)
                            if (root.has("data")) {
                                val data = root.getJSONObject("data")
                                if (data.has("images")) {
                                    val images = data.getJSONObject("images")
                                    var candidate: String? = null
                                    if (images.has("large")) {
                                        val large = images.optString("large", null)
                                        if (!large.isNullOrBlank()) candidate = large
                                    }
                                    if (candidate == null && images.has("small")) {
                                        val small = images.optString("small", null)
                                        if (!small.isNullOrBlank()) candidate = small
                                    }
                                    if (!candidate.isNullOrBlank()) {
                                        val ok = validateUrlHead(candidate)
                                        if (ok) {
                                            return candidate
                                        } else {
                                            Log.w(TAG, "PokeTCG image candidate HEAD failed: $candidate (attempt $attempt)")
                                        }
                                    } else {
                                        Log.w(TAG, "PokeTCG response had no image candidate for $lookupId (attempt $attempt)")
                                    }
                                } else {
                                    Log.w(TAG, "PokeTCG response missing 'images' for $lookupId (attempt $attempt)")
                                }
                            } else {
                                Log.w(TAG, "PokeTCG response missing 'data' for $lookupId (attempt $attempt)")
                            }
                        } else {
                            Log.w(TAG, "PokeTCG response empty body for $lookupId (attempt $attempt)")
                        }
                    } else {
                        Log.w(TAG, "PokeTCG API returned ${r.code} for $lookupId (attempt $attempt)")
                    }
                }
            } catch (e: Exception) {
                if (e is SocketTimeoutException || e is InterruptedIOException) {
                    Log.w(TAG, "Timeout conectando a PokeTCG API for $lookupId (attempt $attempt)", e)
                } else {
                    Log.e(TAG, "Error calling PokeTCG API for $lookupId (attempt $attempt)", e)
                }
            }

            if (attempt < maxAttempts) {
                try {
                    kotlinx.coroutines.delay(backoffMs)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
                backoffMs *= 2
            }
        }

        return null
    }

    // -------------------------
    // TCGdex REST API (retries + parse image)
    // -------------------------
    private suspend fun fetchFromTcgDexApiWithRetries(lookupId: String): String? {
        val apiUrl = "https://api.tcgdex.net/v2/en/cards/$lookupId"
        var attempt = 0
        val maxAttempts = 3
        var backoffMs = 500L
        val userAgent = "ProyectoDAM-PokeTCG/1.0 (Android)"

        while (attempt < maxAttempts) {
            attempt++
            try {
                val builder = Request.Builder().url(apiUrl)
                builder.addHeader("User-Agent", userAgent)
                val req = builder.build()

                val resp: Response = withContext(Dispatchers.IO) { httpClient.newCall(req).execute() }
                resp.use { r ->
                    if (r.isSuccessful) {
                        val body = r.body?.string()
                        if (!body.isNullOrBlank()) {
                            val root = JSONObject(body)
                            val candidateBase = if (root.has("image")) root.optString("image", null) else null
                            if (!candidateBase.isNullOrBlank()) {
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
                                        try {
                                            if (validateUrlHead(url) || probeUrlWithGet(url)) {
                                                Log.d(TAG, "TCGdex API image candidate ok -> $url")
                                                return url
                                            }
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Error probing tcgdex api-derived image $url", e)
                                        }
                                    }
                                }
                                Log.w(TAG, "TCGdex API returned image base but no variant probed ok: $candidateBase")
                            } else {
                                Log.w(TAG, "TCGdex API response missing 'image' for $lookupId (attempt $attempt)")
                            }
                        } else {
                            Log.w(TAG, "TCGdex API response empty body for $lookupId (attempt $attempt)")
                        }
                    } else {
                        Log.w(TAG, "TCGdex API returned ${r.code} for $lookupId (attempt $attempt)")
                    }
                }
            } catch (e: Exception) {
                if (e is SocketTimeoutException || e is InterruptedIOException) {
                    Log.w(TAG, "Timeout conectando a TCGdex API for $lookupId (attempt $attempt)", e)
                } else {
                    Log.e(TAG, "Error calling TCGdex API for $lookupId (attempt $attempt)", e)
                }
            }

            if (attempt < maxAttempts) {
                try {
                    kotlinx.coroutines.delay(backoffMs)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
                backoffMs *= 2
            }
        }

        return null
    }

    // -------------------------
    // TCGdex assets fallback (reconstrucción y prueba)
    // -------------------------
    private suspend fun fetchFromTcgDexAssets(carta: Carta): String? = withContext(Dispatchers.IO) {
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
                        if (validateUrlHead(url) || probeUrlWithGet(url)) {
                            Log.d(TAG, "TCGdex asset ok -> $url")
                            return@withContext url
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error probing tcgdex asset $url", e)
                    }
                }
            }

            // also try using localNoLeading
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
                            if (validateUrlHead(url) || probeUrlWithGet(url)) {
                                Log.d(TAG, "TCGdex asset ok (noLeading) -> $url")
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

    // -------------------------
    // URL probing helpers
    // -------------------------
    private suspend fun validateUrlHead(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .head()
                .addHeader("User-Agent", "ProyectoDAM-PokeTCG/1.0 (Android)")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                Log.d(TAG, "HEAD ${resp.code} -> $url")
                resp.isSuccessful && resp.body != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "validateUrlHead error for $url", e)
            false
        }
    }

    private suspend fun probeUrlWithGet(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", "ProyectoDAM-PokeTCG/1.0 (Android)")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val code = resp.code
                val ct = resp.header("Content-Type") ?: "unknown"
                Log.d(TAG, "GET $code content-type=$ct -> $url")
                if (resp.isSuccessful) {
                    val lower = ct.lowercase()
                    if (lower.contains("image") || lower.contains("jpeg") || lower.contains("png") || lower.contains("webp")) {
                        true
                    } else {
                        Log.w(TAG, "GET OK pero Content-Type no es imagen: $ct -> $url")
                        false
                    }
                } else {
                    Log.w(TAG, "GET NO 2xx ($code) -> $url")
                    false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "probeUrlWithGet error for $url", e)
            false
        }
    }

    // Single loadImageWithLog used for detalle (fitCenter -> imagen completa)
    private fun loadImageWithLog(imageView: ImageView, url: String) {
        Glide.with(imageView.context)
            .load(url)
            .fitCenter() // detalle: mostrar completa
            .placeholder(R.drawable.placeholder_carta)
            .error(R.drawable.placeholder_carta)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Log.e(TAG, "Glide FALLIDA -> url: $url", e)
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "Glide OK -> url: $url")
                    return false
                }
            })
            .into(imageView)
    }

    // Probar esquema, HEAD, GET y luego cargar con Glide (usado para probar y luego mostrar en detalle)
    private fun testAndLoadImage(imageView: ImageView, url: String) {
        // Normalizar esquemas (sin I/O)
        val finalUrl = when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") || url.startsWith("https://") -> url
            else -> "https://$url"
        }

        Log.d(TAG, "testAndLoadImage: probing url='$finalUrl' (original='$url')")

        lifecycleScope.launch {
            var success = false

            // 1) HEAD
            val headOk = try {
                validateUrlHead(finalUrl)
            } catch (e: Exception) {
                Log.w(TAG, "validateUrlHead suspend threw for $finalUrl", e)
                false
            }

            if (headOk) {
                Log.d(TAG, "HEAD OK -> $finalUrl")
                success = true
            } else {
                // 2) GET probe
                val getOk = try {
                    probeUrlWithGet(finalUrl)
                } catch (e: Exception) {
                    Log.w(TAG, "probeUrlWithGet suspend threw for $finalUrl", e)
                    false
                }

                if (getOk) {
                    Log.d(TAG, "GET probe OK -> $finalUrl")
                    success = true
                } else {
                    // 3) intentar esquema alternativo (https <-> http)
                    val alternate = if (finalUrl.startsWith("https://")) finalUrl.replaceFirst("https://", "http://") else finalUrl.replaceFirst("http://", "https://")
                    if (alternate != finalUrl) {
                        val altOkHead = try { validateUrlHead(alternate) } catch (e: Exception) { false }
                        val altOkGet = if (!altOkHead) try { probeUrlWithGet(alternate) } catch (e: Exception) { false } else true
                        if (altOkHead || altOkGet) {
                            Log.d(TAG, "Alternate scheme OK -> $alternate")
                            withContext(Dispatchers.Main) {
                                loadImageWithLog(imageView, alternate)
                            }
                            return@launch
                        }
                    }
                }
            }

            // Si success true, carga con Glide en main
            if (success) {
                withContext(Dispatchers.Main) {
                    loadImageWithLog(imageView, finalUrl)
                }
            } else {
                Log.w(TAG, "URL no válida o inaccesible para imagen: $url (final='$finalUrl')")
                // poner placeholder en main
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(R.drawable.placeholder_carta)
                }
            }
        }
    }

    // ---- cache prefs helpers ----
    private fun saveToPrefs(key: String, url: String) {
        try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
            val currentJson = prefs.getString(PREFS_KEY_MAP, null)
            val obj = if (!currentJson.isNullOrBlank()) JSONObject(currentJson) else JSONObject()
            obj.put(key, url)
            prefs.edit().putString(PREFS_KEY_MAP, obj.toString()).apply()
            Log.d(TAG, "Cache prefs guardada: $key -> $url")
        } catch (e: Exception) {
            Log.w(TAG, "Error guardando cache prefs", e)
        }
    }

    private fun getCachedFromPrefs(key: String): String? {
        return try {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
            val json = prefs.getString(PREFS_KEY_MAP, null)
            if (json.isNullOrBlank()) return null
            val obj = JSONObject(json)
            val v = obj.optString(key, null)
            if (!v.isNullOrBlank()) v else null
        } catch (e: Exception) {
            Log.w(TAG, "Error leyendo cache prefs key=$key", e)
            null
        }
    }

    private fun formatAtaque(a: Ataque): String {
        val lineParts = mutableListOf<String>()
        if (!a.name.isNullOrBlank()) lineParts.add(a.name!!)
        if (!a.damage.isNullOrBlank()) lineParts.add(a.damage!!)
        val l1 = if (lineParts.isEmpty()) "" else lineParts.joinToString(" — ")
        val l2 = if (!a.effect.isNullOrBlank()) a.effect!! else ""
        if (l2.isBlank()) return l1
        if (l1.isBlank()) return l2
        return "$l1\n$l2"
    }

    private fun guardarCartaEnFirestore(cardId: String, name: String?, imageUrl: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para guardar cartas", Toast.LENGTH_SHORT).show()
            return
        }

        val doc = firestore.collection("users").document(user.uid).collection("cards").document(cardId)
        val datos = mutableMapOf<String, Any?>()
        datos["name"] = if (!name.isNullOrBlank()) name else "Sin nombre"
        datos["image"] = imageUrl
        datos["addedAt"] = Timestamp.now()

        binding.pbCargando.visibility = View.VISIBLE
        doc.set(datos)
            .addOnSuccessListener {
                binding.pbCargando.visibility = View.GONE
                Toast.makeText(requireContext(), "Carta añadida a tu colección.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                binding.pbCargando.visibility = View.GONE
                Log.e(TAG, "Error guardando carta en Firestore", e)
                Toast.makeText(requireContext(), "Error guardando la carta", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createTextPlaceholder(name: String?, width: Int = 600, height: Int = 400): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paintBg = Paint().apply { style = Paint.Style.FILL }
        val colors = listOf(0xFFB3E5FC.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFCDD2.toInt())
        val index = if (name != null) (name.hashCode().absoluteValue % colors.size) else 0
        paintBg.color = colors[index]
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val initials: String
        if (!name.isNullOrBlank()) {
            val parts = name.split("\\s+".toRegex()).filter { it.isNotBlank() }
            val chars = parts.mapNotNull { part -> part.firstOrNull()?.uppercaseChar() }
            initials = if (chars.isEmpty()) "—" else chars.take(2).joinToString("")
        } else {
            initials = "—"
        }

        val paintText = Paint().apply {
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = width * 0.14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val x = width / 2f
        val y = height / 2f - (paintText.descent() + paintText.ascent()) / 2f
        canvas.drawText(initials, x, y, paintText)

        return bmp
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
