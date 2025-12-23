package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.data.repository.RepositorioMazos
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaLocalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class FragmentoDetalleCartaLocal : Fragment() {

    private var _binding: FragmentDetalleCartaLocalBinding? = null
    private val binding get() = _binding!!

    private var idCarta: String? = null
    private var imageBase: String? = null

    companion object {
        private const val TAG = "DetalleCartaLocal"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleCartaLocalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        idCarta = arguments?.getString("arg_id_carta")
        imageBase = arguments?.getString("arg_image_base")

        binding.btnVolver.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEliminar.setOnClickListener {
            confirmarEliminarUnaCarta()
        }
        binding.btnAnadirAMazo.setOnClickListener {
            val cardId = idCarta ?: return@setOnClickListener

            seleccionarMazo { mazoId ->
                RepositorioMazos.añadirCartaAMazo(
                    mazoId = mazoId,
                    cardId = cardId,
                    nombre = binding.tvNombre.text.toString(),
                    tipo = "POKEMON" // luego lo haremos dinámico
                )
            }
        }
        binding.btnQuitarDeMazo.setOnClickListener {
            val cardId = idCarta ?: return@setOnClickListener

            seleccionarMazo { mazoId ->
                RepositorioMazos.quitarCartaDelMazo(
                    mazoId = mazoId,
                    cardId = cardId,
                    tipo = "POKEMON"
                )
            }
        }

        cargarDetalleDesdeFirestore()
    }

    private fun cargarDetalleDesdeFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cardId = idCarta ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    Toast.makeText(requireContext(), "Carta no encontrada", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val nombre = doc.getString("name") ?: cardId
                val imageResolved = doc.getString("imageResolvedUrl")
                val imageOriginal = doc.getString("imageOriginalUrl")
                val quantity = doc.getLong("quantity") ?: 1L

                binding.tvNombre.text = nombre
                binding.tvSubtitulo.text = "Carta en tu colección"

                // Imagen
                val imageToLoad = imageResolved ?: imageOriginal ?: imageBase
                if (!imageToLoad.isNullOrBlank()) {
                    Glide.with(this)
                        .load(imageToLoad)
                        .fitCenter()
                        .into(binding.ivCarta)
                }

                // Cantidad overlay
                if (quantity > 1) {
                    binding.tvCantidadOverlay.visibility = View.VISIBLE
                    binding.tvCantidadOverlay.text = "x$quantity"
                } else {
                    binding.tvCantidadOverlay.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Error cargando detalle", it)
            }
    }

    private fun confirmarEliminarUnaCarta() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar carta")
            .setMessage("¿Quieres eliminar una copia de esta carta?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarUnaUnidad()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarUnaUnidad() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val cardId = idCarta ?: return

        val docRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)

        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snap = tx.get(docRef)
            val qty = snap.getLong("quantity") ?: 1L

            if (qty <= 1) {
                tx.delete(docRef)
            } else {
                tx.update(docRef, "quantity", qty - 1)
            }
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Carta actualizada", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }.addOnFailureListener {
            Log.e(TAG, "Error eliminando carta", it)
        }
    }

    private fun mostrarSelectorMazosParaAñadir() {

        val uid = FirebaseAuth.getInstance().uid ?: return

        RepositorioMazos.obtenerMazos()
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No tienes mazos", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val nombres = snapshot.documents.map { it.getString("nombre") ?: "Sin nombre" }
                val ids = snapshot.documents.map { it.id }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Añadir carta a mazo")
                    .setItems(nombres.toTypedArray()) { _, index ->

                        val mazoId = ids[index]

                        RepositorioMazos.añadirCartaAMazoDesdeColeccion(
                            mazoId = mazoId,
                            cartaId = idCarta!!,
                            nombre = binding.tvNombre.text.toString(),
                            tipo = "POKEMON" // más adelante lo calculamos
                        )

                        Toast.makeText(requireContext(), "Carta añadida al mazo", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
    }

    private fun seleccionarMazo(onMazoSeleccionado: (String) -> Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().uid ?: return

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("mazos")
            .get()
            .addOnSuccessListener { snapshot ->

                val nombres = snapshot.documents.map { it.getString("nombre") ?: "Sin nombre" }
                val ids = snapshot.documents.map { it.id }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Selecciona un mazo")
                    .setItems(nombres.toTypedArray()) { _, index ->
                        onMazoSeleccionado(ids[index])
                    }
                    .show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
