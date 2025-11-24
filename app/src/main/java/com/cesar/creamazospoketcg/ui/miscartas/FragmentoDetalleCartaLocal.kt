package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.data.local.RepositorioLocal
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaLocalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * FragmentoDetalleCartaLocal
 *
 * Muestra el detalle (pedimos a la API para datos completos) y ofrece:
 *  - Eliminar carta de la colección (con popup de confirmación)
 *  - Añadirla a un mazo existente (lista ficticia por ahora)
 *  - Crear un nuevo mazo con esta carta (acción placeholder)
 *  - Volver al listado (popBackStack)
 *
 * Comentarios orientados a estudiante.
 */
class FragmentoDetalleCartaLocal : Fragment() {

    private var _binding: FragmentDetalleCartaLocalBinding? = null
    private val binding get() = _binding!!

    private lateinit var repoLocal: RepositorioLocal
    private val repoRemoto = RepositorioCartas()

    private var idCarta: String? = null
    private var imageBase: String? = null

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
            requireActivity().onBackPressed()
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

    /**
     * Cargar detalle remoto de la carta por id.
     * Mostramos un ProgressBar mientras cargamos.
     */
    private fun cargarDetalleCarta(id: String) {
        binding.pbCargando.visibility = View.VISIBLE
        lifecycleScope.launch {
            val resultado = repoRemoto.obtenerCartaPorId(id)
            binding.pbCargando.visibility = View.GONE

            if (resultado.isSuccess) {
                val carta = resultado.getOrNull()!!
                // Rellenar UI con los datos (nombre, rareza, tipo, imagen...)
                binding.tvNombre.text = carta.name
                binding.tvSubtitulo.text = listOfNotNull(carta.types?.joinToString(", "), carta.rarity).joinToString(" — ")
                val imagen = carta.images?.large ?: carta.images?.small ?: imageBase
                if (!imagen.isNullOrBlank()) {
                    Glide.with(this@FragmentoDetalleCartaLocal).load(imagen).into(binding.ivCarta)
                }
                // Aquí podríamos mostrar ataques, habilidades, etc.
            } else {
                // Si falla la descarga, intentamos usar imageBase y nombre en local
                binding.tvNombre.text = repoLocal.obtenerCarta(id)?.name ?: "Detalle no disponible"
                if (!imageBase.isNullOrBlank()) Glide.with(this@FragmentoDetalleCartaLocal).load(imageBase).into(binding.ivCarta)
                Toast.makeText(requireContext(), "No se pudo cargar el detalle.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Muestra un diálogo de confirmación para eliminar la carta de la colección local.
     */
    private fun mostrarConfirmacionEliminar() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar carta")
            .setMessage("¿Seguro que quieres eliminar esta carta de tu colección?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                idCarta?.let { id ->
                    val ok = repoLocal.eliminarCarta(id)
                    if (ok) {
                        Toast.makeText(requireContext(), "Carta eliminada", Toast.LENGTH_SHORT).show()
                        // Volvemos a la lista
                        requireActivity().onBackPressed()
                    } else {
                        Toast.makeText(requireContext(), "No se pudo eliminar la carta", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    /**
     * Selector simple de mazos (placeholder).
     * En el futuro se debería leer la lista real de mazos del usuario desde persistencia.
     */
    private fun mostrarSelectorMazos() {
        // Lista de ejemplo (temporal). En producción leer de DB/SharedPrefs
        val mazos = listOf("Mazo 1", "Mazo 2 (ejemplo)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mazos)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Selecciona mazo")
            .setAdapter(adapter) { dialog, which ->
                val elegido = mazos[which]
                // Aquí añadiríamos la carta al mazo elegido (pendiente de implementar estructura de mazos)
                Toast.makeText(requireContext(), "Añadida a: $elegido (funcionalidad demo)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
