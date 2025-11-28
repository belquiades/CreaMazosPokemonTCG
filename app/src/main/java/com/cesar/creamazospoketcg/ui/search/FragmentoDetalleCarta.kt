package com.cesar.creamazospoketcg.ui.search

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Ataque
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

    // Pequeña caché en memoria para no pedir muchas veces la misma imagen
    private val cardImageCache = mutableMapOf<String, String?>()

    // OkHttp client
    private val httpClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            cartaIdArg = bundle.getString("arg_id_carta")
            imageBaseArg = bundle.getString("arg_image_base")
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
            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No hay id de carta para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nombre = binding.tvNombreCarta.text.toString().takeIf { it.isNotBlank() } ?: ""
            val imagen = binding.ivCarta.tag as? String ?: imageBaseArg
            guardarCartaEnFirestore(id, nombre, imagen)
        }

        cartaIdArg?.let { id ->
            cargarDetalleCarta(id)
        } ?: run {
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
        // Logging para depurar contenido de la carta y la URL calculada
        Log.d(TAG, "Mostrar carta: $carta")
        val computedImageLarge = carta.images?.large
        val computedImageSmall = carta.images?.small
        val localImageUrl = computedImageLarge ?: computedImageSmall ?: imageBaseArg
        Log.d(TAG, "Computed localImageUrl = '$localImageUrl' (large='$computedImageLarge', small='$computedImageSmall', arg='$imageBaseArg')")

        binding.groupDetalle.visibility = View.VISIBLE

        binding.tvNombreCarta.text = carta.name ?: "—"
        binding.tvTipoRarity.text = listOfNotNull(carta.types?.joinToString(", "), carta.rarity)
            .filter { it.isNotEmpty() }
            .joinToString(" — ")

        // Guardamos la url en tag por si la usas en otros sitios
        binding.ivCarta.tag = localImageUrl

        // Si ya tenemos imagen local válida, cargarla; si no, intentar la PokéTCG API
        if (!localImageUrl.isNullOrBlank()) {
            safeLoadImage(binding.ivCarta, localImageUrl, carta.name)
        } else {
            // intenta caché primero
            val lookupId = carta.id ?: carta.localId ?: ""
            val cached = cardImageCache[lookupId]
            if (!cached.isNullOrBlank()) {
                Log.d(TAG, "Usando cached imageUrl para $lookupId -> $cached")
                safeLoadImage(binding.ivCarta, cached, carta.name)
            } else {
                // buscar en la API remota
                binding.pbCargando.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val apiKey: String? = null // si tienes Api key ponla aquí
                    val remoteUrl = fetchImageUrlFromPokeTcg(lookupId, apiKey)
                    binding.pbCargando.visibility = View.GONE
                    cardImageCache[lookupId] = remoteUrl // puede ser null
                    if (!remoteUrl.isNullOrBlank()) {
                        binding.ivCarta.tag = remoteUrl
                        safeLoadImage(binding.ivCarta, remoteUrl, carta.name)
                    } else {
                        // nada -> placeholder dinámico
                        safeLoadImage(binding.ivCarta, null, carta.name)
                    }
                }
            }
        }

        val ataques = carta.attacks
        if (ataques.isNullOrEmpty()) {
            binding.tvAtaques.text = getString(R.string.no_hay_ataques)
        } else {
            binding.tvAtaques.text = ataques.joinToString(separator = "\n\n") { formatAtaque(it) }
        }

        // Resto de campos (si quieres puedes rellenar más)
        binding.tvTipos.text = carta.types?.joinToString(", ") ?: "-"
        binding.tvHP.text = carta.hp ?: "-"
        binding.tvLocalId.text = carta.localId ?: "-"
        binding.tvRetreat.text = "Retreat: ${carta.retreat ?: "-"}"
        binding.tvSetInfo.text = carta.set?.name ?: "-"
    }

    private fun formatAtaque(a: Ataque): String {
        val linea1 = listOfNotNull(a.name, a.damage).filter { it.isNotEmpty() }.joinToString(" — ")
        val linea2 = a.effect ?: ""
        return if (linea2.isBlank()) linea1 else "$linea1\n$linea2"
    }

    private fun guardarCartaEnFirestore(cardId: String, name: String?, imageUrl: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para guardar cartas", Toast.LENGTH_SHORT).show()
            return
        }

        val doc = firestore.collection("users").document(user.uid).collection("cards").document(cardId)

        val datos = mutableMapOf<String, Any?>(
            "name" to (name ?: "Sin nombre"),
            "image" to imageUrl,
            "addedAt" to Timestamp.now()
        )

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -------------------------
    // Helper: safeLoadImage + loadImageWithLog
    // -------------------------
    private fun safeLoadImage(imageView: ImageView, url: String?, nameForPlaceholder: String? = null) {
        val finalUrl = url?.takeIf { it.isNotBlank() }
        if (finalUrl == null) {
            Log.w(TAG, "No hay URL válida, mostrando placeholder dinámico. url='$url' name='$nameForPlaceholder'")
            val bmp = createTextPlaceholder(nameForPlaceholder ?: "Cart")
            imageView.setImageBitmap(bmp)
            return
        }
        loadImageWithLog(imageView, finalUrl)
    }

    private fun loadImageWithLog(imageView: ImageView, url: String, placeholder: Int = R.drawable.placeholder_carta) {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(placeholder)
            .error(placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
                ): Boolean {
                    Log.e(TAG, "Carga FALLIDA -> url: $url", e)
                    return false // permitimos que Glide coloque el drawable de error
                }

                override fun onResourceReady(
                    resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "Carga OK -> url: $url")
                    return false
                }
            })
            .into(imageView)
    }

    // -------------------------
    // Helper: placeholder dinámico (iniciales)
    // -------------------------
    private fun createTextPlaceholder(name: String?, width: Int = 600, height: Int = 400): android.graphics.Bitmap {
        val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paintBg = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.FILL
            val colors = listOf(0xFFB3E5FC.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(), 0xFFD1C4E9.toInt(), 0xFFFFCDD2.toInt())
            val index = (name?.hashCode()?.absoluteValue ?: 0) % colors.size
            color = colors[index]
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

        val initials = name
            ?.split("\\s+".toRegex())
            ?.mapNotNull { it.takeIf { it.isNotBlank() }?.firstOrNull()?.uppercaseChar() }
            ?.take(2)
            ?.joinToString("") ?: "—"

        val paintText = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = width * 0.14f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        val x = width / 2f
        val y = height / 2f - (paintText.descent() + paintText.ascent()) / 2f
        canvas.drawText(initials, x, y, paintText)

        return bmp
    }

    // -------------------------
    // Network: fetch image URL from PokéTCG API
    // -------------------------
    private suspend fun fetchImageUrlFromPokeTcg(cardId: String?, apiKey: String? = null): String? {
        if (cardId.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.pokemontcg.io/v2/cards/${cardId}"
                val builder = Request.Builder().url(url)
                if (!apiKey.isNullOrBlank()) {
                    builder.addHeader("X-Api-Key", apiKey)
                }
                val request = builder.build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "PokeTCG API returned ${resp.code}")
                        return@withContext null
                    }
                    val body = resp.body?.string() ?: return@withContext null
                    val root = JSONObject(body)
                    if (!root.has("data")) return@withContext null
                    val data = root.getJSONObject("data")
                    if (!data.has("images")) return@withContext null
                    val images = data.getJSONObject("images")
                    val large = if (images.has("large")) images.optString("large", null) else null
                    val small = if (images.has("small")) images.optString("small", null) else null
                    return@withContext large ?: small
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching PokeTCG image for $cardId", e)
                return@withContext null
            }
        }
    }
}
