package com.cesar.creamazospoketcg.ui.miscartas

import androidx.core.os.bundleOf
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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await

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
            .setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()

                val id = idCarta
                if (id.isNullOrBlank()) {
                    Log.w(TAG, "mostrarConfirmacionEliminar: idCarta nulo/ vacío, no se borra")
                    Toast.makeText(requireContext(), "No hay id de carta para eliminar", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                Log.d(TAG, "mostrarConfirmacionEliminar: iniciando borrado para id=$id")

                // Ejecutamos la operación en un coroutine para no bloquear la UI
                lifecycleScope.launch {
                    try {
                        // Si el borrado es local (RepositorioLocal), use su método; si es remoto, llame a Firestore
                        // Ejemplo con repoLocal (ajusta si tu método se llama distinto):
                        repoLocal.eliminarCarta(id) // <-- reemplaza por el método real de tu repoLocal

                        Log.d(TAG, "mostrarConfirmacionEliminar: carta eliminada localmente: $id")

                        // === NUEVO: BORRAR TAMBIÉN EN FIRESTORE ===
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        if (userId != null) {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .collection("cards")
                                    .document(id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Firestore: carta eliminada correctamente: $id")
                                    }
                                    .addOnFailureListener { ex ->
                                        Log.e(TAG, "Firestore: error al eliminar carta: ${ex.message}")
                                    }
                            } catch (ex: Exception) {
                                Log.e(TAG, "Exception Firestore delete: ${ex.message}")
                            }
                        } else {
                            Log.w(TAG, "No hay usuario autenticado: no se puede borrar en Firestore")
                        }

                        // Si también borras de Firestore (user collection), hazlo aquí y espera al resultado:
                        // val userId = FirebaseAuth.getInstance().currentUser?.uid
                        // if (userId != null) {
                        //     FirebaseFirestore.getInstance()
                        //         .collection("users").document(userId)
                        //         .collection("cartas").document(id).delete().await()
                        //     Log.d(TAG, "mostrarConfirmacionEliminar: carta eliminada en Firestore: $id")
                        // }

                        // Ahora volvemos al hilo principal para actualizar UI / navegar,
                        // pero SÓLO si el fragment sigue añadido y la vista existe.
                        withContext(Dispatchers.Main) {
                            if (!isAdded) {
                                Log.w(TAG, "mostrarConfirmacionEliminar: fragment no añadido al Activity, no actualizamos UI")
                                return@withContext
                            }
                            if (_binding == null) {
                                Log.w(TAG, "mostrarConfirmacionEliminar: binding es null, evitando acceso a vistas")
                            } else {
                                Toast.makeText(requireContext(), "Carta eliminada", Toast.LENGTH_SHORT).show()
                            }

                            // Si quieres que la lista se actualice en el fragment padre, lo ideal es
                            // usar snapshot listeners o que el fragment padre recargue en onResume()
                            // Aquí cerramos el detalle (popBackStack) de forma segura:
                            try {
                                findNavController().popBackStack()
                            } catch (e: Exception) {
                                Log.e(TAG, "mostrarConfirmacionEliminar: popBackStack fallo", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "mostrarConfirmacionEliminar: error borrando carta id=$id", e)
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                Toast.makeText(requireContext(), "No se pudo eliminar la carta: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
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
