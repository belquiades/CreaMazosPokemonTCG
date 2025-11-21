package com.cesar.creamazospoketcg.ui.search

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.DrawableCompat

class FragmentoDetalleCarta : Fragment() {

    private var _binding: FragmentDetalleCartaBinding? = null
    private val binding get() = _binding!!
    private val repositorio = RepositorioCartas()

    // fallback image base pasado desde la lista
    private var imageBaseArg: String? = null

    companion object {
        private const val ARG_ID_CARTA = "arg_id_carta"
        private const val ARG_IMAGE_BASE = "arg_image_base"
        private const val TAG = "FragmentoDetalleCarta"

        fun newInstance(idCarta: String, imageBase: String? = null): FragmentoDetalleCarta {
            val f = FragmentoDetalleCarta()
            val args = Bundle()
            args.putString(ARG_ID_CARTA, idCarta)
            if (!imageBase.isNullOrBlank()) args.putString(ARG_IMAGE_BASE, imageBase)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleCartaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pbDetalle.visibility = View.VISIBLE
        binding.tvMensajeDetalle.visibility = View.GONE
        binding.tvNombreDetalle.text = ""

        // leer argumento imageBase
        imageBaseArg = arguments?.getString(ARG_IMAGE_BASE)

        val idCarta = arguments?.getString(ARG_ID_CARTA)
        if (idCarta == null) {
            binding.pbDetalle.visibility = View.GONE
            binding.tvNombreDetalle.text = getString(R.string.error_carta_no_encontrada)
            return
        }

        // Petición al repositorio en background
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Solicitando detalle para id=$idCarta")
                val resultado = repositorio.obtenerCartaPorId(idCarta)
                binding.pbDetalle.visibility = View.GONE

                if (resultado.isSuccess) {
                    val carta = resultado.getOrNull()!!
                    mostrarCarta(carta)
                } else {
                    val ex = resultado.exceptionOrNull()
                    Log.e(TAG, "Error al obtener carta id=$idCarta", ex)
                    binding.tvMensajeDetalle.visibility = View.VISIBLE
                    binding.tvMensajeDetalle.text = "Error al obtener detalle: ${ex?.message ?: "desconocido"}"
                    binding.tvNombreDetalle.text = getString(R.string.error_carta_no_encontrada)
                }
            } catch (e: Exception) {
                binding.pbDetalle.visibility = View.GONE
                Log.e(TAG, "Excepción al pedir detalle carta id=$idCarta", e)
                binding.tvMensajeDetalle.visibility = View.VISIBLE
                binding.tvMensajeDetalle.text = "Error al obtener detalle: ${e.message}"
                binding.tvNombreDetalle.text = getString(R.string.error_carta_no_encontrada)
            }
        }

        binding.btnAnadirMazo.setOnClickListener {
            val id = arguments?.getString(ARG_ID_CARTA) ?: "?"
            android.widget.Toast.makeText(requireContext(), "Añadida la carta $id al mazo (temporal)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Convierte una ruta base de assets en una lista de candidatos:
     * high.webp, high.png, low.webp, low.png
     * Si la base ya tiene extensión, devuelve esa URL como único candidato.
     */
    private fun baseToAssetCandidates(base: String?): List<String> {
        if (base.isNullOrBlank()) return emptyList()
        val lower = base.lowercase()
        if (lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return listOf(base)
        }
        val baseSinSlash = if (base.endsWith("/")) base.dropLast(1) else base
        return listOf(
            "$baseSinSlash/high.webp",
            "$baseSinSlash/high.png",
            "$baseSinSlash/low.webp",
            "$baseSinSlash/low.png"
        )
    }

    /**
     * Intenta cargar secuencialmente los candidatos y loguea fallos/éxitos.
     */
    private fun loadImageSequentially(candidatos: List<String>, target: android.widget.ImageView) {
        if (candidatos.isEmpty()) {
            target.setImageResource(R.drawable.placeholder_carta)
            Log.d(TAG, "loadImageSequentially: no hay candidatos, se muestra placeholder")
            return
        }

        fun intentar(index: Int) {
            if (index >= candidatos.size) {
                Log.e(TAG, "loadImageSequentially: ningún candidato válido, mostrando placeholder")
                target.setImageResource(R.drawable.placeholder_carta)
                return
            }

            val url = candidatos[index]
            Log.d(TAG, "loadImageSequentially: intentando [$index] -> $url")

            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        targetGlide: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.w(TAG, "Glide fallo al cargar $url -> ${e?.message ?: "sin mensaje"}")
                        intentar(index + 1)
                        return true // manejamos el fallo y probamos siguiente
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        targetGlide: Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "Glide cargó correctamente: $url (dataSource=${dataSource})")
                        return false // permitir que Glide coloque la imagen
                    }
                })
                .into(target)
        }

        intentar(0)
    }

    private fun mostrarCarta(carta: Carta) {
        // Logs para depuración
        Log.d(TAG, "mostrarCarta id='${carta.id}' name='${carta.name}' images.large='${carta.images?.large}' images.small='${carta.images?.small}' imageBaseArg='$imageBaseArg'")


        // Nombre
        binding.tvNombreDetalle.text = carta.name

        // IMAGEN GRANDE: preferimos imágenes del detalle, si no están usamos la imageBase pasada desde la lista
        val posibleBase = carta.images?.large ?: carta.images?.small ?: imageBaseArg
        val candidatos = baseToAssetCandidates(posibleBase)
        Log.d(TAG, "Candidatos imagen grande para id='${carta.id}': $candidatos")

        loadImageSequentially(candidatos, binding.ivImagenGrande)

        // Tipos y rareza
        val tiposTexto = carta.types?.joinToString(", ") ?: getString(R.string.desconocido)
        val rarezaTexto = carta.rarity ?: getString(R.string.desconocido)
        binding.tvTipoRareza.text = "$tiposTexto — $rarezaTexto"

        // Set
        binding.tvSet.text = carta.set?.name ?: getString(R.string.desconocido)

        // Ataques
        binding.llAtaques.removeAllViews()
        if (!carta.attacks.isNullOrEmpty()) {
            carta.attacks.forEach { ataque ->
                val tv = TextView(requireContext()).apply {
                    text = "${ataque.name} — ${ataque.damage ?: ""}\n${ataque.text ?: ""}"
                    setPadding(0, 8, 0, 8)
                }
                binding.llAtaques.addView(tv)
            }
        } else {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.no_hay_ataques)
                setPadding(0, 8, 0, 8)
            }
            binding.llAtaques.addView(tv)
        }

        // Habilidades (si aplica)
        binding.llHabilidades.removeAllViews()
    }

    private fun colorPorTipo(tipo: String): Int {
        return when (tipo.lowercase()) {
            "fire" -> Color.parseColor("#FF6B6B")
            "water" -> Color.parseColor("#4D9FFF")
            "grass" -> Color.parseColor("#63C766")
            "psychic" -> Color.parseColor("#C96BFF")
            "fighting" -> Color.parseColor("#C97A56")
            "lightning" -> Color.parseColor("#FFD93B")
            "darkness" -> Color.parseColor("#595260")
            "metal" -> Color.parseColor("#AFAFAF")
            "dragon" -> Color.parseColor("#C8A951")
            "fairy" -> Color.parseColor("#FF77D6")
            else -> Color.parseColor("#DDDDDD")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
