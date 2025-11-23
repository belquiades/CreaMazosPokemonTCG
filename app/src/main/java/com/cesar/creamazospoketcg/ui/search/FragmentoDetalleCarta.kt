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
import com.cesar.creamazospoketcg.data.model.Ataque
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

/**
 * FragmentoDetalleCarta
 *
 * Muestra el detalle de una carta obtenida de la API (RepositorioCartas.obtenerCartaPorId).
 * Permite:
 *  - Añadir la carta a la colección del usuario (Firestore: users/{uid}/cards/{cardId})
 *  - Volver (si procede) usando NavController
 *
 * Todo el código y comentarios están en español para que lo entienda un estudiante.
 */
class FragmentoDetalleCarta : Fragment() {

    private var _binding: FragmentDetalleCartaBinding? = null
    private val binding get() = _binding!!

    private val TAG = "DetalleCarta"

    private val repo by lazy { RepositorioCartas() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Id de la carta que recibimos como argumento (nav)
    private var cartaIdArg: String? = null
    // Fallback de imagen (si detalle no trae imágenes)
    private var imageBaseArg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtenemos argumentos (si vienen)
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
        // Inicial UI
        binding.pbCargando.visibility = View.GONE
        binding.groupDetalle.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        // Botón volver local (vuelve al fragmento anterior)
        binding.btnVolverDetalle.setOnClickListener {
            // Intentamos hacer popBackStack, si no hay nada se va al perfil
            val popOk = findNavController().popBackStack()
            if (!popOk) {
                findNavController().navigate(R.id.perfilFragment)
            }
        }

        // Botón añadir a mi colección
        binding.btnAnadirMiColeccion.setOnClickListener {
            // Si ya tenemos la carta cargada en UI guardamos; si no, pedimos al repo antes
            val id = cartaIdArg
            if (id.isNullOrBlank()) {
                Toast.makeText(requireContext(), "No hay id de carta para guardar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Si ya hay datos en pantalla, los usamos; si no, pedimos detalle y luego guardamos.
            val nombre = binding.tvNombreCarta.text.toString().takeIf { it.isNotBlank() } ?: ""
            val imagen = binding.ivCarta.tag as? String ?: imageBaseArg

            guardarCartaEnFirestore(id, nombre, imagen)
        }

        // Si nos han pasado id -> cargamos detalle
        cartaIdArg?.let { id ->
            cargarDetalleCarta(id)
        } ?: run {
            // No hay id: mostrar error (puede ocurrir si se navega mal)
            binding.tvError.visibility = View.VISIBLE
            binding.tvError.text = getString(R.string.error_carta_no_encontrada)
        }
    }

    /**
     * Pide al repositorio el detalle de la carta por id y lo muestra en pantalla.
     */
    private fun cargarDetalleCarta(id: String) {
        binding.pbCargando.visibility = View.VISIBLE
        binding.groupDetalle.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        // Lanzamos coroutine para red (no en hilo principal)
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
                    mostrarCartaEnUI(carta)
                } else {
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = getString(R.string.error_carta_no_encontrada)
                }
            } else {
                // Si falla la petición, intentamos mostrar fallback si recibimos imageBaseArg
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = getString(R.string.error_carta_no_encontrada)
            }
        }
    }

    /**
     * Rellena vistas con la información de la carta.
     * También guardamos la url de la imagen en el tag de la ImageView para poder guardarla después.
     */
    private fun mostrarCartaEnUI(carta: Carta) {
        binding.groupDetalle.visibility = View.VISIBLE

        binding.tvNombreCarta.text = carta.name
        binding.tvTipoRarity.text = listOfNotNull(carta.types?.joinToString(", "), carta.rarity).filter { it.isNotEmpty() }.joinToString(" — ")

        // Cargar imagen con Glide (si hay varias opciones, usamos la que venga)
        val imageUrl = carta.images?.large ?: carta.images?.small ?: imageBaseArg
        binding.ivCarta.tag = imageUrl // guardamos la url en tag como referencia
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_carta)
            .error(R.drawable.placeholder_carta)
            .into(binding.ivCarta)

        // Mostrar ataques (si hay)
        val ataques = carta.attacks
        if (ataques.isNullOrEmpty()) {
            binding.tvAtaques.text = getString(R.string.no_hay_ataques)
        } else {
            binding.tvAtaques.text = ataques.joinToString(separator = "\n\n") { formatAtaque(it) }
        }
    }

    private fun formatAtaque(a: Ataque): String {
        // Ejemplo simple: "Nombre — daño\nDescripción"
        val linea1 = listOfNotNull(a.name, a.damage).filter { it.isNotEmpty() }.joinToString(" — ")
        val linea2 = a.text ?: ""
        return if (linea2.isBlank()) linea1 else "$linea1\n$linea2"
    }

    /**
     * Guarda la carta en Firestore bajo users/{uid}/cards/{cardId}.
     * name e imagen son opcionales pero recomendables para el listado rápido.
     */
    private fun guardarCartaEnFirestore(cardId: String, name: String?, imageUrl: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para guardar cartas", Toast.LENGTH_SHORT).show()
            return
        }

        // Documento por id de carta (evita duplicados)
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
