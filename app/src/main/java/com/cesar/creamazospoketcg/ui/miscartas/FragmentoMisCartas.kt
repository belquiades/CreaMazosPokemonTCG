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
        attachSwipeToDelete()
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
    private fun attachSwipeToDelete() {
        val itemTouchCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
            androidx.recyclerview.widget.ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]

                borrarCartaEnFirestore(item.id) {
                    // Solo actualiza UI si delete fue OK
                    val mutable = adapter.currentList.toMutableList()
                    mutable.removeAt(position)
                    adapter.submitList(mutable)
                }
            }
        }

        androidx.recyclerview.widget.ItemTouchHelper(itemTouchCallback)
            .attachToRecyclerView(binding.rvMisCartas)
    }
    private fun borrarCartaEnFirestore(cardId: String, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: run {
            Log.w(TAG, "No hay usuario autenticado")
            return
        }

        val ref = firestore.collection("users")
            .document(user.uid)
            .collection("cards")
            .document(cardId)

        ref.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Carta eliminada correctamente: $cardId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error eliminando carta $cardId", e)
            }
    }

}
