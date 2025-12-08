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
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.data.local.RepositorioLocal
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaLocalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentoDetalleCartaLocal : Fragment() {

    private var _binding: FragmentDetalleCartaLocalBinding? = null
    private val binding get() = _binding!!

    private lateinit var repoLocal: RepositorioLocal
    private val repoRemoto = RepositorioCartas()

    private var idCarta: String? = null
    private var imageBase: String? = null

    companion object {
        private const val TAG = "FragmentoD-CartaLocal"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleCartaLocalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repoLocal = RepositorioLocal(requireContext())

        // Leer argumentos pasados desde la lista
        idCarta = arguments?.getString("arg_id_carta")
        imageBase = arguments?.getString("arg_image_base")

        // Botón volver -> PopBackStack (vuelve a MisCartas)
        binding.btnVolver.setOnClickListener {
            try {
                findNavController().popBackStack()
            } catch (e: Exception) {
                Log.w(TAG, "popBackStack fallo", e)
                requireActivity().onBackPressed()
            }
        }

        // Botón Eliminar -> pedir confirmación
        binding.btnEliminar.setOnClickListener {
            mostrarConfirmacionEliminar()
        }

        // Botón Añadir a mazo -> mostramos lista de mazos (placeholder)
        binding.btnAnadirAMazo.setOnClickListener {
            mostrarSelectorMazos()
        }

        // Botón Crear mazo -> placeholder para crear un nuevo mazo
        binding.btnCrearMazo.setOnClickListener {
            Toast.makeText(requireContext(), "Crear mazo: funcionalidad por implementar.", Toast.LENGTH_SHORT).show()
        }

        // Cargar detalle de la carta (si id disponible)
        idCarta?.let { cargarDetalleCarta(it) } ?: run {
            // Si no hay id, mostramos la imagen base mínima
            binding.tvNombre.text = "Carta desconocida"
            if (!imageBase.isNullOrBlank()) {
                Glide.with(this).load(imageBase).into(binding.ivCarta)
            }
        }
    }

    private fun cargarDetalleCarta(id: String) {
        // Mostrar progress si la vista existe
        if (isAdded && _binding != null) binding.pbCargando.visibility = View.VISIBLE

        lifecycleScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    repoRemoto.obtenerCartaPorId(id)
                } catch (e: Exception) {
                    Log.e(TAG, "Excepción pidiendo detalle carta", e)
                    Result.failure(Exception("Error repositorio remoto", e))
                }
            }

            if (isAdded && _binding != null) binding.pbCargando.visibility = View.GONE

            if (resultado.isSuccess) {
                val carta = resultado.getOrNull()
                if (carta != null) {
                    // Rellenar UI con los datos (nombre, rareza, tipo, imagen...)
                    if (isAdded && _binding != null) {
                        binding.tvNombre.text = carta.name
                        binding.tvSubtitulo.text = listOfNotNull(carta.types?.joinToString(", "), carta.rarity).joinToString(" — ")
                        val imagen = carta.images?.large ?: carta.images?.small ?: imageBase
                        if (!imagen.isNullOrBlank()) {
                            Glide.with(this@FragmentoDetalleCartaLocal).load(imagen).into(binding.ivCarta)
                            binding.ivCarta.tag = imagen
                        }
                    } else {
                        Log.d(TAG, "Vista no disponible para mostrar detalle (fragment detached).")
                    }
                } else {
                    if (isAdded && _binding != null) {
                        binding.tvNombre.text = repoLocal.obtenerCarta(id)?.name ?: "Detalle no disponible"
                        if (!imageBase.isNullOrBlank()) Glide.with(this@FragmentoDetalleCartaLocal).load(imageBase).into(binding.ivCarta)
                        Toast.makeText(requireContext(), "Detalle no disponible", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d(TAG, "Resultado success pero carta null y vista no disponible.")
                    }
                }
            } else {
                if (isAdded && _binding != null) {
                    binding.tvNombre.text = repoLocal.obtenerCarta(id)?.name ?: "Detalle no disponible"
                    if (!imageBase.isNullOrBlank()) Glide.with(this@FragmentoDetalleCartaLocal).load(imageBase).into(binding.ivCarta)
                    Toast.makeText(requireContext(), "No se pudo cargar el detalle.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Error cargando detalle y vista no disponible.")
                }
            }
        }
    }

    private fun mostrarConfirmacionEliminar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar carta")
            .setMessage("¿Seguro que quieres eliminar esta carta de tu colección?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                val cardId = idCarta?.takeIf { it.isNotBlank() }
                if (cardId.isNullOrBlank()) {
                    Log.e(TAG, "mostrarConfirmacionEliminar: no hay idCarta para eliminar (idCarta='$idCarta')")
                    if (isAdded && _binding != null) {
                        Snackbar.make(binding.root, "No se pudo identificar la carta a eliminar.", Snackbar.LENGTH_SHORT).show()
                    }
                    return@setPositiveButton
                }

                // 1) Eliminar en local
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            repoLocal.eliminarCarta(cardId)
                        }
                        Log.d(TAG, "Carta eliminada localmente: $cardId")
                        if (isAdded && _binding != null) {
                            Snackbar.make(binding.root, "Carta eliminada de la colección local.", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Log.d(TAG, "Carta eliminada localmente pero vista no disponible para mostrar Snackbar.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error eliminando carta localmente: ${e.message}", e)
                        if (isAdded && _binding != null) {
                            Snackbar.make(binding.root, "No se pudo eliminar la carta localmente.", Snackbar.LENGTH_LONG).show()
                        }
                        // continuamos para intentar borrar en la nube también
                    }

                    // 2) Intentar eliminar en Firestore si hay usuario
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (!userId.isNullOrBlank()) {
                        try {
                            if (isAdded && _binding != null) binding.pbCargando.visibility = View.VISIBLE
                            val firestore = FirebaseFirestore.getInstance()
                            firestore.collection("users")
                                .document(userId)
                                .collection("cartas")
                                .document(cardId)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d(TAG, "Carta eliminada en Firestore: $cardId (user=$userId)")
                                    if (isAdded && _binding != null) {
                                        binding.pbCargando.visibility = View.GONE
                                        Snackbar.make(binding.root, "Carta eliminada de tu colección en la nube.", Snackbar.LENGTH_SHORT).show()
                                    } else {
                                        Log.d(TAG, "Eliminada en Firestore pero vista no disponible.")
                                    }
                                }
                                .addOnFailureListener { ex ->
                                    Log.e(TAG, "Error al eliminar carta en Firestore: ${ex.message}", ex)
                                    if (isAdded && _binding != null) {
                                        binding.pbCargando.visibility = View.GONE
                                        Snackbar.make(binding.root, "No se pudo eliminar en la nube (ver logs).", Snackbar.LENGTH_LONG).show()
                                    } else {
                                        Log.d(TAG, "Error en Firestore pero vista no disponible para notificar.")
                                    }
                                }
                        } catch (e: Exception) {
                            Log.e(TAG, "Excepción intentando borrar en Firestore: ${e.message}", e)
                            if (isAdded && _binding != null) binding.pbCargando.visibility = View.GONE
                        }
                    } else {
                        Log.d(TAG, "Usuario no autenticado -> no se intenta borrar en Firestore")
                    }

                    // Finalmente volvemos al listado si el fragmento sigue añadido
                    if (isAdded) {
                        try {
                            findNavController().popBackStack()
                        } catch (e: Exception) {
                            Log.w(TAG, "popBackStack fallo tras eliminar", e)
                            requireActivity().onBackPressed()
                        }
                    } else {
                        Log.d(TAG, "No hacemos popBackStack porque fragmento ya no está añadido.")
                    }
                } // lifecycleScope.launch
            }
            .show()
    }

    private fun mostrarSelectorMazos() {
        Toast.makeText(requireContext(), "Selecciona un mazo (funcionalidad pendiente).", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
