package com.cesar.creamazospoketcg.ui.mazos

import com.cesar.creamazospoketcg.model.Mazo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.databinding.FragmentListaMazosBinding
import com.cesar.creamazospoketcg.data.repository.RepositorioMazos

class FragmentoListaMazos : Fragment() {

    private var _binding: FragmentListaMazosBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MazosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaMazosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = MazosAdapter()

        binding.recyclerMazos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMazos.adapter = adapter

        cargarMazos()
    }

    private fun cargarMazos() {
        RepositorioMazos.obtenerMazos()
            .addOnSuccessListener { snapshot ->
                val lista = snapshot.documents.map { doc ->
                    Mazo(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        creadoEn = doc.getLong("creadoEn") ?: 0L,
                        totalCartas = doc.getLong("totalCartas") ?: 0L,
                        pokemonCount = doc.getLong("pokemonCount") ?: 0L,
                        entrenadorCount = doc.getLong("entrenadorCount") ?: 0L,
                        energiaCount = doc.getLong("energiaCount") ?: 0L
                    )
                }
                adapter.submitList(lista)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
