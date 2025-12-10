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
            // Ajusta la acción si tu navigation id es distinto
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

        // Si quieres soporte swipe-to-delete con ItemTouchHelper, lo añadimos aquí.
        // (Dejé esa parte fuera por simplicidad; avísame si quieres que la incorpore.)
        cargarCartasUsuario()
    }

    override fun onResume() {
        super.onResume()
        // Refrescar cuando volvamos del detalle (por ejemplo tras borrar una carta)
        cargarCartasUsuario()
    }

    /**
     * Carga las cartas del usuario desde Firestore y las pasa al adapter.
     * Asume estructura: users/{uid}/cards (document id = cardId), con campo de imagen opcional.
     */
    private fun cargarCartasUsuario() {
        val user = auth.currentUser
        if (user == null) {
            Log.w(TAG, "Usuario no logueado - mostrando lista vacía")
            return mostrarListaVacia()
        }

        firestore.collection("users")
            .document(user.uid)
            .collection("cards")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    mostrarListaVacia()
                } else {
                    val cartas = snap.documents.map { doc ->
                        val id = doc.id
                        // Ajusta nombres de campos según tu Firestore real
                        val resolved = doc.getString("imageResolvedUrl")
                        val original = doc.getString("imageOriginalUrl")
                        val alt = doc.getString("resolvedImageUrl") ?: doc.getString("image") ?: doc.getString("imageUrl")
                        val img = resolved ?: original ?: alt
                        // Nota: usamos la clase CartaGuardada definida en MisCartasAdapter.kt
                        CartaGuardada(id, img)
                    }
                    // convertimos a MutableList para que coincida con la firma esperada por tu adapter
                    adapter.submitList(cartas.toMutableList())
                    binding.tvVacio.visibility = View.GONE
                    binding.rvMisCartas.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exc ->
                Log.e(TAG, "Error al cargar cartas desde Firestore", exc)
                mostrarListaVacia()
            }
    }

    private fun mostrarListaVacia() {
        binding.tvVacio.visibility = View.VISIBLE
        binding.rvMisCartas.visibility = View.GONE
        adapter.submitList(mutableListOf())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
