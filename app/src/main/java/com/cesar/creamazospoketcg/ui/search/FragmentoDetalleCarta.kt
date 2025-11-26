package com.cesar.creamazospoketcg.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentoDetalleCarta : Fragment() {

    private var _binding: FragmentDetalleCartaBinding? = null
    private val binding get() = _binding!!

    private val TAG = "DetalleCarta"

    private val repo by lazy { RepositorioCartas() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var cartaIdArg: String? = null
    private var imageBaseArg: String? = null
    private var cartaEnPantalla: Carta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cartaIdArg = it.getString("arg_id_carta")
            imageBaseArg = it.getString("arg_image_base")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleCartaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.pbCargando.visibility = View.GONE

        binding.btnVolverDetalle.setOnClickListener {
            val popOk = findNavController().popBackStack()
            if (!popOk) findNavController().navigate(R.id.perfilFragment)
        }

        binding.btnAnadirMiColeccion.setOnClickListener {
            val id = cartaEnPantalla?.id ?: cartaIdArg
            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No hay id de carta para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val nombre = cartaEnPantalla?.name ?: binding.tvNombreCarta.text.toString()
            val imagen = cartaEnPantalla?.images?.large ?: cartaEnPantalla?.images?.small ?: imageBaseArg
            guardarCartaEnFirestore(id, nombre, imagen)
        }

        cartaIdArg?.let { cargarDetalleCarta(it) } ?: run {
            binding.tvNombreCarta.text = getString(R.string.error_carta_no_encontrada)
        }
    }

    private fun cargarDetalleCarta(id: String) {
        binding.pbCargando.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.Main).launch {
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
                    cartaEnPantalla = carta
                    mostrarCartaEnUI(carta)
                } else {
                    binding.tvNombreCarta.text = getString(R.string.error_carta_no_encontrada)
                }
            } else {
                binding.tvNombreCarta.text = getString(R.string.error_carta_no_encontrada)
                Log.e(TAG, "Error obteniendo carta: ${resultado.exceptionOrNull()?.message}")
            }
        }
    }

    private fun mostrarCartaEnUI(carta: Carta) {
        // Título y subtítulo
        binding.tvNombreCarta.text = carta.name ?: "—"
        val tipo = carta.types?.joinToString(", ")
        val rarity = carta.rarity
        binding.tvTipoRarity.text = listOfNotNull(tipo, rarity).joinToString(" — ")

        binding.tvHP.text = "HP: ${carta.hp ?: "-"}"
        binding.tvLocalId.text = "No: ${carta.localId ?: "-"}"
        binding.tvRetreat.text = "Retreat: ${carta.retreat ?: "-"}"

        binding.tvTipos.text = "Tipos: ${carta.types?.joinToString(", ") ?: "-"}"
        binding.tvEvoluciona.text = "Evoluciona de: ${carta.evolvesFrom ?: "-"}"

        // Weaknesses / Resistances
        val weak = carta.weaknesses?.joinToString { "${it.type ?: "-"} ${it.value ?: ""}" }
        val res = carta.resistances?.joinToString { "${it.type ?: "-"} ${it.value ?: ""}" }
        binding.tvWeakRes.text = "Debilidades: ${weak ?: "-"}\nResistencias: ${res ?: "-"}"

        // Ataques
        if (carta.attacks.isNullOrEmpty()) {
            binding.tvAtaques.text = getString(R.string.no_hay_ataques)
        } else {
            val ataquesText = carta.attacks.joinToString(separator = "\n\n") { atk ->
                val cost = atk.cost?.joinToString(", ") ?: ""
                val linea1 = listOfNotNull(atk.name, atk.damage).filter { it.isNotEmpty() }.joinToString(" — ")
                val linea2 = if (cost.isNotEmpty()) "Cost: $cost" else ""
                val efecto = atk.effect ?: ""
                listOfNotNull(linea1, linea2, efecto).filter { it.isNotBlank() }.joinToString("\n")
            }
            binding.tvAtaques.text = ataquesText
        }

        binding.tvIllustrator.text = "Ilustrador: ${carta.illustrator ?: "-"}"
        binding.tvFlavor.text = carta.flavorText ?: ""

        // Set info
        val set = carta.set
        binding.tvSetInfo.text = if (set != null) {
            "Set: ${set.series ?: "-"} — ${set.name ?: "-"} (${set.id ?: "-"}) • ${carta.localId ?: "-"} / ${set.total ?: "-"}"
        } else {
            "Set: -"
        }

        // Imagen (reconstrucción similar al adaptador)
        val posibleBaseRaw = carta.images?.large ?: carta.images?.small ?: imageBaseArg
        val imageToLoad = construirUrlPreferida(posibleBaseRaw)
        binding.ivCarta.tag = imageToLoad
        Log.d(TAG, "mostrarCartaEnUI -> posibleBaseRaw='$posibleBaseRaw' imageToLoad='$imageToLoad'")

        binding.ivCarta.post {
            val glide = Glide.with(binding.root)
                .load(imageToLoad)
                .centerCrop()
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)

            // No encadenamos todos los fallbacks aquí (Adaptador ya tiene lógica). Si quieres,
            // puedes copiar la lógica completa de fallbacks del AdaptadorCartas.
            glide.listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e(TAG, "Glide onLoadFailed. model=$model image=$imageToLoad", e)
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "Glide onResourceReady. image=$imageToLoad dataSource=$dataSource")
                    return false
                }
            }).into(binding.ivCarta)
        }
    }

    private fun construirUrlPreferida(base: String?): String? {
        if (base.isNullOrBlank()) return null
        val lower = base.lowercase()
        if (lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return base
        }
        val baseSinSlash = if (base.endsWith("/")) base.dropLast(1) else base
        return "$baseSinSlash/high.webp"
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
}
