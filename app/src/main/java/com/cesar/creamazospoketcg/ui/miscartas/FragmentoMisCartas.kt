package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.cesar.creamazospoketcg.model.CartaGuardada
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MisCartasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = MisCartasAdapter { carta ->
            val bundle = Bundle().apply {
                putString("arg_id_carta", carta.id)
                putString("arg_image_base", carta.imageResolvedUrl ?: carta.imageOriginalUrl)
            }
            findNavController().navigate(
                R.id.fragmentoDetalleCartaLocal,
                bundle
            )
        }

        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adapter

        cargarCartasDesdeFirestore()
    }

    private fun cargarCartasDesdeFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("cards")
            .get()
            .addOnSuccessListener { snapshot ->

                val lista = snapshot.documents.map { doc ->
                    CartaGuardada(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        imageResolvedUrl = doc.getString("imageResolvedUrl"),
                        imageOriginalUrl = doc.getString("imageOriginalUrl"),
                        quantity = doc.getLong("quantity") ?: 1L
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

