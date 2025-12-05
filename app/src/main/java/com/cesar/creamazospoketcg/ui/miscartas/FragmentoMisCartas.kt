package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "MisCartas"

class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val adapter by lazy {
        MisCartasAdapter { cardId, imageUrl ->
            val action = R.id.action_misCartas_to_detalleCartaLocal
            val bundle = Bundle().apply {
                putString("arg_id_carta", cardId)
                putString("arg_image_base", imageUrl)
            }
            try {
                findNavController().navigate(action, bundle)
            } catch (e: Exception) {
                Log.e(TAG, "Error navegando a detalle local", e)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adapter

        cargarCartasUsuario()
    }

    private fun cargarCartasUsuario() {
        val user = auth.currentUser ?: return mostrarListaVacia()

        firestore.collection("users").document(user.uid).collection("cards")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarListaVacia()
                } else {
                    val cartas = snap.documents.map { doc ->
                        val id = doc.id
                        val img = doc.getString("resolvedImageUrl") ?: doc.getString("image")
                        CartaGuardada(id, img)
                    }
                    adapter.submitList(cartas)
                    binding.tvVacio.visibility = View.GONE
                    binding.rvMisCartas.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error al cargar cartas", it)
                mostrarListaVacia()
            }
    }

    private fun mostrarListaVacia() {
        binding.tvVacio.visibility = View.VISIBLE
        binding.rvMisCartas.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
