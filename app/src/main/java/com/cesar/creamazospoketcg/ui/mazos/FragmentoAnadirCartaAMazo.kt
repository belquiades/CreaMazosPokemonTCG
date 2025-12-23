package com.cesar.creamazospoketcg.ui.mazos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.data.repository.RepositorioMazos
import com.cesar.creamazospoketcg.databinding.FragmentAnadirCartaMazoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentoAnadirCartaAMazo : Fragment() {

    private var _binding: FragmentAnadirCartaMazoBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var mazoId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnadirCartaMazoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mazoId = arguments?.getString("mazoId") ?: return

        binding.recyclerCartas.layoutManager = LinearLayoutManager(requireContext())

        cargarCartasDisponibles()
    }

    private fun cargarCartasDisponibles() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("cards")
            .get()
            .addOnSuccessListener { snapshot ->

                val lista = snapshot.documents.mapNotNull { doc ->
                    val cantidad = doc.getLong("quantity") ?: 0L
                    if (cantidad > 0) {
                        doc.id
                    } else null
                }

                binding.tvInfo.text = "Cartas disponibles: ${lista.size}"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error cargando cartas", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
