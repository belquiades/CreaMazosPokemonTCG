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
import androidx.lifecycle.lifecycleScope
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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

    private val httpClient by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            cartaIdArg = args.getString("arg_id_carta")
            imageBaseArg = args.getString("arg_image_base")
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

        // --- calcular imageCandidate SIN usar 'if' como expresión ---
        var computedLarge: String? = null
        if (!carta.images?.large.isNullOrBlank()) {
            computedLarge = carta.images?.large
        }

        var computedSmall: String? = null
        if (!carta.images?.small.isNullOrBlank()) {
            computedSmall = carta.images?.small
        }

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
        // ----------------------------------------------------------

        Log.d(TAG, "Computed imageCandidate = '$imageCandidate' (large='$computedLarge', small='$computedSmall', arg='$imageBaseArg')")

        binding.groupDetalle.visibility = View.VISIBLE

        // Nombre
        if (!carta.name.isNullOrBlank()) {
            binding.tvNombreCarta.text = carta.name
        } else {
            binding.tvNombreCarta.text = "—"
        }

        // Tipo — Rareza
        val tipoParts = mutableListOf<String>()
        if (!carta.types.isNullOrEmpty()) tipoParts.add(carta.types.joinToString(", "))
        if (!carta.rarity.isNullOrBlank()) tipoParts.add(carta.rarity)
        if (tipoParts.isEmpty()) {
            binding.tvTipoRarity.text = "—"
        } else {
            binding.tvTipoRarity.text = tipoParts.joinToString(" — ")
        }

        // Mostrar imagen (local primero)
        binding.ivCarta.tag = imageCandidate
        if (!imageCandidate.isNullOrBlank()) {
            loadImageWithLog(binding.ivCarta, imageCandidate)
        } else {
            // Si no hay candidato, intentar PokéTCG API en background
            binding.pbCargando.visibility = View.VISIBLE
            lifecycleScope.launch {
                val pokeKey: String? = if (BuildConfig.POKETCG_API_KEY.isNotBlank()) BuildConfig.POKETCG_API_KEY else null

                var lookupId: String? = null
                if (!carta.id.isNullOrBlank()) {
                    lookupId = carta.id
                } else {
                    if (!carta.localId.isNullOrBlank()) {
                        lookupId = carta.localId
                    }
                }

                var remoteUrl: String? = null
                if (lookupId != null) {
                    remoteUrl = withContext(Dispatchers.IO) {
                        fetchFromPokeTcgApi(lookupId, pokeKey)
                    }
                }

                binding.pbCargando.visibility = View.GONE

                if (!remoteUrl.isNullOrBlank()) {
                    binding.ivCarta.tag = remoteUrl
                    loadImageWithLog(binding.ivCarta, remoteUrl)
                    Log.d(TAG, "Usada imagen desde PokeTCG API -> $remoteUrl")
                } else {
                    val bmp = createTextPlaceholder(carta.name)
                    binding.ivCarta.setImageBitmap(bmp)
                    Log.w(TAG, "No se obtuvo imagen desde PokeTCG API para id=${carta.id} (lookupId=$lookupId)")
                }
            }
        }

        // Ataques
        val ataques = carta.attacks
        if (ataques == null || ataques.isEmpty()) {
            binding.tvAtaques.text = getString(R.string.no_hay_ataques)
        } else {
            binding.tvAtaques.text = ataques.joinToString(separator = "\n\n") { formatAtaque(it) }
        }

        // Resto de campos
        if (!carta.types.isNullOrEmpty()) binding.tvTipos.text = carta.types.joinToString(", ") else binding.tvTipos.text = "-"
        if (carta.hp != null) binding.tvHP.text = carta.hp.toString() else binding.tvHP.text = "-"
        if (!carta.localId.isNullOrBlank()) binding.tvLocalId.text = carta.localId else binding.tvLocalId.text = "-"
        if (carta.retreat != null) binding.tvRetreat.text = "Retreat: ${carta.retreat}" else binding.tvRetreat.text = "Retreat: -"
        if (!carta.set?.name.isNullOrBlank()) binding.tvSetInfo.text = carta.set?.name else binding.tvSetInfo.text = "-"
    }

    // --- PokeTCG helper (no-if-as-expression) ---
    private fun validateUrlHead(url: String): Boolean {
        return try {
            val req = Request.Builder().url(url).head().build()
            httpClient.newCall(req).execute().use { resp ->
                resp.isSuccessful && resp.body != null
            }
        } catch (e: Exception) {
            Log.w(TAG, "validateUrlHead error for $url", e)
            false
        }
    }

    private fun fetchFromPokeTcgApi(lookupId: String, pokeApiKey: String?): String? {
        try {
            val apiUrl = "https://api.pokemontcg.io/v2/cards/$lookupId"
            val builder = Request.Builder().url(apiUrl)
            if (!pokeApiKey.isNullOrBlank()) {
                builder.addHeader("X-Api-Key", pokeApiKey)
            }
            val req = builder.build()
            val resp: Response = httpClient.newCall(req).execute()
            resp.use { r ->
                if (r.isSuccessful) {
                    val body = r.body?.string()
                    if (body.isNullOrBlank()) {
                    } else {
                        val root = JSONObject(body)
                        if (!root.has("data")) {
                        } else {
                            val data = root.getJSONObject("data")
                            if (!data.has("images")) {
                            } else {
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
                                if (candidate.isNullOrBlank()) {
                                } else {
                                    val ok = validateUrlHead(candidate)
                                    if (ok) {
                                        return candidate
                                    } else {
                                        Log.w(TAG, "PokeTCG image candidate HEAD failed: $candidate")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "PokeTCG API returned ${r.code} for $lookupId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling PokeTCG API for $lookupId", e)
        }
        return null
    }

    // ---- utilities ----
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

    private fun loadImageWithLog(imageView: ImageView, url: String) {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.drawable.placeholder_carta)
            .error(R.drawable.placeholder_carta)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Log.e(TAG, "Carga FALLIDA -> url: $url", e)
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "Carga OK -> url: $url")
                    return false
                }
            })
            .into(imageView)
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
