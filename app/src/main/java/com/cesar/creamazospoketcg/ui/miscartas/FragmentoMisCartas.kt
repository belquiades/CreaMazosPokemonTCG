package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.data.local.RepositorioLocal
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.CartaTCGdexBreve
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.cesar.creamazospoketcg.ui.search.AdaptadorCartas
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * FragmentoMisCartas
 *
 * Muestra la lista de cartas guardadas localmente (colección del usuario).
 * Si no hay cartas, muestra un FAB (+) que lleva al buscador para añadir.
 *
 * - Al pulsar una carta: navegamos a FragmentoDetalleCartaLocal enviando 'arg_id_carta' y 'arg_image_base'.
 * - Eliminar se implementa en el fragmento de detalle local (con confirmación).
 *
 * Comentarios claros para estudiantes.
 */
class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    private lateinit var repoLocal: RepositorioLocal
    private lateinit var adaptador: AdaptadorCartas

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repoLocal = RepositorioLocal(requireContext())

        // RecyclerView y Adaptador (inicialmente vacío)
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            // Al pulsar carta -> navegar a detalle local (mis cartas)
            val bundle = Bundle().apply {
                putString("arg_id_carta", carta.id)
                val imageBase = carta.images?.small ?: carta.images?.large
                if (!imageBase.isNullOrBlank()) putString("arg_image_base", imageBase)
            }
            findNavController().navigate(com.cesar.creamazospoketcg.R.id.action_misCartas_to_detalleCartaLocal, bundle)
        }

        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adaptador

        // FAB añadir: lleva al buscador para seleccionar cartas
        binding.fabAnadirCarta.setOnClickListener {
            // Navegamos al fragmento de búsqueda (usar el id definido en nav_graph)
            findNavController().navigate(com.cesar.creamazospoketcg.R.id.fragmentoBusquedaCartas)
        }

        // Si el usuario quiere limpiar la colección (ejemplo: botón en la UI), podríamos añadir aquí.
        // Cargamos la colección y actualizamos la UI
        actualizarUIDesdeRepositorio()
    }

    /**
     * Carga la colección desde RepositorioLocal y actualiza vistas.
     */
    private fun actualizarUIDesdeRepositorio() {
        val listaBreve: List<CartaTCGdexBreve> = repoLocal.cargarColeccion()
        if (listaBreve.isEmpty()) {
            binding.tvVacio.visibility = View.VISIBLE
            binding.rvMisCartas.visibility = View.GONE
        } else {
            binding.tvVacio.visibility = View.GONE
            binding.rvMisCartas.visibility = View.VISIBLE

            // Convertir CartaTCGdexBreve -> Carta (mínimo) para usar AdaptadorCartas existente
            val listaCartas: List<Carta> = listaBreve.map { it.aCartaLocalMinima() }
            adaptador.actualizarLista(listaCartas)
        }
    }

    // Extensión para mapear CartaTCGdexBreve a Carta mínima
    private fun CartaTCGdexBreve.aCartaLocalMinima(): Carta {
        // Construimos un objeto Carta mínimo con id, name e imagen (small -> ImagenesCarta)
        return Carta(
            id = this.id,
            name = this.name,
            images = com.cesar.creamazospoketcg.data.model.ImagenesCarta(
                small = this.image,
                large = this.image
            )
        )
    }

    override fun onResume() {
        super.onResume()
        // Refrescamos la lista por si se han añadido/eliminado cartas desde otra pantalla
        actualizarUIDesdeRepositorio()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
