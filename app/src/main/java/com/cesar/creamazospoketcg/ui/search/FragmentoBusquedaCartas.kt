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
import com.cesar.creamazospoketcg.databinding.FragmentBusquedaCartasBinding
import com.cesar.creamazospoketcg.vm.VistaModeloBusquedaCartas
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

    // ViewBinding: _binding puede ser null cuando la vista se destruye
    private var _binding: FragmentBusquedaCartasBinding? = null
    private val binding get() = _binding!!

    // ViewModel para búsquedas
    private val vistaModelo: VistaModeloBusquedaCartas by viewModels()

    // Adaptador de cartas (usa el adaptador existente)
    private lateinit var adaptador: AdaptadorCartas

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflamos el layout mediante ViewBinding
        _binding = FragmentBusquedaCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializamos el adaptador con callback para abrir detalle
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            val bundle = Bundle().apply {
                putString("arg_id_carta", carta.id)
                val imageBaseParaDetalle = carta.images?.small ?: carta.images?.large
                if (!imageBaseParaDetalle.isNullOrBlank()) {
                    putString("arg_image_base", imageBaseParaDetalle)
                }
            }
            findNavController().navigate(com.cesar.creamazospoketcg.R.id.fragmentoDetalleCarta, bundle)
        }

        // Configuramos RecyclerView usando binding (¡no usar rvCartas directo!)
        binding.rvCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartas.adapter = adaptador

        // Botón buscar
        binding.btnBuscar.setOnClickListener { realizarBusqueda() }

        // Acción IME del teclado (buscar)
        binding.etConsulta.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                realizarBusqueda()
                true
            } else false
        }

        // Observamos el estado del ViewModel con repeatOnLifecycle
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
                            // El adaptador que tienes no es ListAdapter; usa actualizarLista()
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
        // Evitamos memory leaks liberando binding
        _binding = null
    }
}