package com.cesar.creamazospoketcg.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentBusquedaCartasBinding
import com.cesar.creamazospoketcg.vm.VistaModeloBusquedaCartas
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * FragmentoBusquedaCartas
 *
 * Fragment que muestra un EditText para introducir una query, un botón de buscar,
 * y un RecyclerView con los resultados.
 *
 * La navegación hacia el detalle usa Navigation Component: findNavController().navigate(...)
 * En el Bundle enviamos:
 *  - "arg_id_carta"     -> id de la carta (String)
 *  - "arg_image_base"   -> ruta base de la miniatura (String?) usada como fallback en detalle
 */
class FragmentoBusquedaCartas : Fragment() {

    private var _binding: FragmentBusquedaCartasBinding? = null
    private val binding get() = _binding!!

    // ViewModel para la búsqueda
    private val vistaModelo: VistaModeloBusquedaCartas by viewModels()

    // Adaptador del RecyclerView (se inicializa en onViewCreated)
    private lateinit var adaptador: AdaptadorCartas

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBusquedaCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inicializar RecyclerView y adaptador (callback recibe la carta seleccionada)
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            // Aquí preparamos los argumentos para el fragmento de detalle.
            // Usamos las claves que espera FragmentoDetalleCarta: "arg_id_carta" y "arg_image_base"
            val bundle = Bundle().apply {
                putString("arg_id_carta", carta.id) // id de la carta
                // pasamos la miniatura como fallback si el detalle no incluye imágenes
                val imageBaseParaDetalle = carta.images?.small ?: carta.images?.large
                if (!imageBaseParaDetalle.isNullOrBlank()) {
                    putString("arg_image_base", imageBaseParaDetalle)
                }
            }

            // Navegar al fragmento de detalle usando NavController.
            // El id de destino debe coincidir con el id del fragmento en nav_graph.xml
            findNavController().navigate(R.id.fragmentoDetalleCarta, bundle)
        }

        binding.rvCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartas.adapter = adaptador

        // Botón buscar
        binding.btnBuscar.setOnClickListener {
            realizarBusqueda()
        }

        // Acción IME (teclado) -> buscar al pulsar "search"
        binding.etConsulta.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                realizarBusqueda()
                true
            } else false
        }

        // Observamos el estado del ViewModel de forma segura con repeatOnLifecycle
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vistaModelo.estadoUI.collect { estado ->
                    when (estado) {
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Cargando -> {
                            binding.pbCargando.visibility = View.VISIBLE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.GONE
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Resultado -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.tvMensaje.visibility = View.GONE
                            binding.rvCartas.visibility = View.VISIBLE
                            adaptador.actualizarLista(estado.lista)
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Error -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.VISIBLE
                            binding.tvMensaje.text = estado.mensaje
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Vacío -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.VISIBLE
                            binding.tvMensaje.text = "Introduce una consulta y pulsa Buscar"
                        }
                    }
                }
            }
        }
    }

    private fun realizarBusqueda() {
        val consulta = binding.etConsulta.text.toString().takeIf { it.isNotBlank() }
        vistaModelo.buscarCartas(consulta)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
