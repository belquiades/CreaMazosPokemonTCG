package com.cesar.creamazospoketcg.ui.mazos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.databinding.FragmentDetalleMazoBinding
import com.cesar.creamazospoketcg.model.CartaEnMazo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentoDetalleMazo : Fragment() {

    private var _binding: FragmentDetalleMazoBinding? = null
    private val binding get() = _binding!!

    private lateinit var mazoId: String
    private lateinit var adapter: CartasMazoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleMazoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mazoId = requireArguments().getString("mazoId") ?: return

        adapter = CartasMazoAdapter()

        binding.recyclerCartasMazo.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerCartasMazo.adapter = adapter

        cargarMazo()
    }

    private fun cargarMazo() {
        val uid = FirebaseAuth.getInstance().uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Info del mazo
        db.collection("users")
            .document(uid)
            .collection("mazos")
            .document(mazoId)
            .get()
            .addOnSuccessListener { doc ->
                binding.tvNombreMazo.text = doc.getString("nombre") ?: ""
                binding.tvResumen.text =
                    "Pokémon: ${doc.getLong("pokemonCount") ?: 0} | " +
                            "Entrenador: ${doc.getLong("entrenadorCount") ?: 0} | " +
                            "Energía: ${doc.getLong("energiaCount") ?: 0}"
            }

        // Cartas del mazo
        db.collection("users")
            .document(uid)
            .collection("mazos")
            .document(mazoId)
            .collection("cartas")
            .get()
            .addOnSuccessListener { snapshot ->

                val lista = snapshot.documents.map { doc ->
                    CartaEnMazo(
                        cardId = doc.id,
                        nombre = doc.getString("name") ?: "",
                        tipo = doc.getString("tipo") ?: "",
                        cantidad = doc.getLong("cantidad") ?: 0L
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
