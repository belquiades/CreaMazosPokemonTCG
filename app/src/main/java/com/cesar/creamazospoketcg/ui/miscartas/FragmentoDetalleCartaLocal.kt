package com.cesar.creamazospoketcg.ui.miscartas

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentDetalleCartaLocalBinding
import com.cesar.creamazospoketcg.data.model.Ataque
import com.cesar.creamazospoketcg.data.model.Carta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * FragmentoDetalleCartaLocal
 *
 * Muestra el detalle completo de una carta obtenida por id (local / API).
 * Permite:
 *  - Eliminar la carta de la colección del usuario
 *  - Añadir la carta a un mazo existente
 *  - Crear un nuevo mazo que contenga esta carta
 *  - Volver al listado
 *
 * Comentarios orientados a estudiantes para entender cada paso.
 */
class FragmentoDetalleCartaLocal : Fragment(), CoroutineScope {

    private var _binding: FragmentDetalleCartaLocalBinding? = null
    private val binding get() = _binding!!

    // Firestore / Auth
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Repositorio para pedir detalle de cartas (usa API tcgdex)
    private val repo = RepositorioCartas()

    // Coroutine scope
    private val job = Job()
    override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main + job

    // id de la carta que recibimos por argumentos (bundle)
    private var idCarta: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recuperamos argumento (clave usada en FragmentoMisCartas al abrir)
        idCarta = arguments?.getString("id_local")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalleCartaLocalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón volver
        binding.btnVolver.setOnClickListener {
            findNavController().popBackStack()
        }

        // Botón acciones que abre las opciones (eliminar, añadir a mazo, crear mazo)
        binding.btnAcciones.setOnClickListener {
            mostrarDialogoAcciones()
        }

        // Si no tenemos id, mostramos mensaje y volvemos
        val id = idCarta
        if (id.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.error_carta_no_encontrada), Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        // Cargar datos de la carta (detalle)
        cargarDetalleCarta(id)
    }

    /**
     * Carga detalle de la carta llamando al repositorio.
     * Actualiza la UI con la información recibida.
     */
    private fun cargarDetalleCarta(id: String) {
        binding.pbCargando.visibility = View.VISIBLE
        launch {
            val resultado = withContext(Dispatchers.IO) {
                try {
                    repo.obtenerCartaPorId(id)
                } catch (e: Exception) {
                    kotlin.runCatching { Result.failure<Carta>(e) }.getOrNull()
                }
            }

            if (resultado != null && resultado.isSuccess) {
                val carta = resultado.getOrNull()
                if (carta != null) {
                    mostrarCartaEnUI(carta)
                } else {
                    mostrarError("Carta no encontrada")
                }
            } else {
                val mensaje = resultado?.exceptionOrNull()?.localizedMessage ?: "Error cargando carta"
                mostrarError(mensaje)
            }
            binding.pbCargando.visibility = View.GONE
        }
    }

    /**
     * Pinta los datos de la carta en las vistas.
     */
    private fun mostrarCartaEnUI(carta: Carta) {
        // Nombre
        binding.tvNombreCarta.text = carta.name

        // Subtítulo: tipos — rareza
        val subtitulo = listOfNotNull(carta.types?.joinToString(", "), carta.rarity)
            .filter { it.isNotEmpty() }
            .joinToString(" — ")
        binding.tvSubtitulo.text = if (subtitulo.isBlank()) getString(R.string.desconocido) else subtitulo

        // Imagen: intentamos large -> small -> placeholder
        val imgUrl = carta.images?.large ?: carta.images?.small
        if (!imgUrl.isNullOrBlank()) {
            Glide.with(this)
                    .load(imgUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_carta)
                    .error(R.drawable.placeholder_carta)
                    .into(binding.ivImagenCarta)
        } else {
            binding.ivImagenCarta.setImageResource(R.drawable.placeholder_carta)
        }

        // Ataques
        val ataques = carta.attacks ?: emptyList()
        if (ataques.isEmpty()) {
            binding.tvAtaques.text = getString(R.string.no_hay_ataques)
        } else {
            binding.tvAtaques.text = ataques.joinToString("\n\n") { formatearAtaque(it) }
        }

        // Guardamos en tag la carta para usar en acciones
        binding.root.tag = carta
    }

    private fun formatearAtaque(a: Ataque): String {
        val nombre = a.name ?: ""
        val dano = a.damage ?: ""
        val texto = a.text ?: ""
        return listOf(nombre, dano).filter { it.isNotBlank() }.joinToString(" — ") +
        if (texto.isNotBlank()) "\n$texto" else ""
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    /**
     * Muestra un diálogo con las opciones de acción sobre la carta:
     * - Eliminar
     * - Añadir a mazo existente
     * - Crear nuevo mazo con esta carta
     */
    private fun mostrarDialogoAcciones() {
        val opciones = arrayOf(
                "Eliminar carta de mi colección",
                "Añadir a un mazo existente",
                "Crear un nuevo mazo con esta carta",
                "Cancelar"
        )
        AlertDialog.Builder(requireContext())
                .setTitle("Acciones")
                .setItems(opciones) { dialog, which ->
                when (which) {
            0 -> confirmarYEliminarCarta()
            1 -> seleccionarMazoYAñadir()
            2 -> crearMazoConCarta()
                    else -> dialog.dismiss()
        }
        }
            .show()
    }

    /**
     * Eliminar: confirmación y borrado del documento en la colección usuarios/{uid}/mis_cartas/{id}
     */
    private fun confirmarYEliminarCarta() {
        val id = idCarta ?: return
                AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar carta")
                        .setMessage("¿Estás seguro de que deseas eliminar esta carta de tu colección?")
                        .setPositiveButton("Eliminar") { _, _ ->
                // Borrado en background
                launch {
            try {
                val uid = auth.currentUser?.uid ?: throw Exception("No autenticado")
                db.collection("usuarios")
                        .document(uid)
                        .collection("mis_cartas")
                        .document(id)
                        .delete()
                        .await()
                Toast.makeText(requireContext(), "Carta eliminada", Toast.LENGTH_SHORT).show()
                // Volver al listado
                findNavController().popBackStack()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error eliminando carta", Toast.LENGTH_SHORT).show()
            }
        }
        }
            .setNegativeButton("Cancelar", null)
                .show()
    }

    /**
     * Muestra los mazos existentes y permite seleccionar uno para añadir la carta (arrayUnion).
     */
    private fun seleccionarMazoYAñadir() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "No autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        binding.pbCargando.visibility = View.VISIBLE
        launch {
            try {
                // Obtenemos documentos de la colección 'mazos' del usuario
                val snapshot = db.collection("usuarios")
                        .document(uid)
                        .collection("mazos")
                        .get()
                        .await()

                val docs = snapshot.documents
                if (docs.isEmpty()) {
                    binding.pbCargando.visibility = View.GONE
                    Toast.makeText(requireContext(), "No tienes mazos. Crea uno primero.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Preparar lista de nombres y ids
                val nombres = docs.map { it.getString("nombre") ?: "Sin nombre" }.toTypedArray()
                val ids = docs.map { it.id }

                binding.pbCargando.visibility = View.GONE

                var seleccionado = -1
                AlertDialog.Builder(requireContext())
                        .setTitle("Selecciona un mazo")
                        .setSingleChoiceItems(nombres, -1) { _, which ->
                        seleccionado = which
                }
                    .setPositiveButton("Añadir") { _, _ ->
                    if (seleccionado < 0) {
                        Toast.makeText(requireContext(), "Selecciona un mazo", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val idMazo = ids[seleccionado]
                    añadirCartaAMazo(uid, idMazo)
                }
                    .setNegativeButton("Cancelar", null)
                        .show()

            } catch (e: Exception) {
                binding.pbCargando.visibility = View.GONE
                Toast.makeText(requireContext(), "Error al obtener mazos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Añade la carta (por id) a la colección 'mazos/{id}/cartas' mediante arrayUnion.
     */
    private fun añadirCartaAMazo(uid: String, idMazo: String) {
        val id = idCarta ?: return

                launch {
            try {
                val mazoRef = db.collection("usuarios")
                        .document(uid)
                        .collection("mazos")
                        .document(idMazo)

                // Usamos arrayUnion para evitar duplicados
                mazoRef.update("cartas", FieldValue.arrayUnion(id)).await()
                Toast.makeText(requireContext(), "Carta añadida al mazo", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Si el campo no existía, lo creamos con set
                try {
                    val mazoRef = db.collection("usuarios")
                            .document(uid)
                            .collection("mazos")
                            .document(idMazo)
                    mazoRef.set(mapOf("cartas" to listOf(id)), com.google.firebase.firestore.SetOptions.merge()).await()
                    Toast.makeText(requireContext(), "Carta añadida al mazo", Toast.LENGTH_SHORT).show()
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), "Error añadiendo carta al mazo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Crea un nuevo mazo y añade la carta en su campo 'cartas'.
     */
    private fun crearMazoConCarta() {
        val id = idCarta ?: return

                // Pedimos nombre del mazo con un EditText en un AlertDialog
                val input = EditText(requireContext())
        input.hint = "Nombre del mazo"
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        AlertDialog.Builder(requireContext())
                .setTitle("Crear nuevo mazo")
                .setView(input)
                .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
            if (nombre.isBlank()) {
                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            // Crear doc en Firestore bajo usuarios/{uid}/mazos
            launch {
                try {
                    val uid = auth.currentUser?.uid ?: throw Exception("No autenticado")
                    val nuevo = mapOf(
                            "nombre" to nombre,
                            "cartas" to listOf(id)
                    )
                    db.collection("usuarios").document(uid)
                            .collection("mazos")
                            .add(nuevo)
                            .await()
                    Toast.makeText(requireContext(), "Mazo creado y carta añadida", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error creando mazo", Toast.LENGTH_SHORT).show()
                }
            }
        }
            .setNegativeButton("Cancelar", null)
                .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job.cancel()
        _binding = null
    }
}
