package com.cesar.creamazospoketcg.ui.miscartas

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.CartaTCGdexBreve
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.cesar.creamazospoketcg.ui.search.AdaptadorCartas
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * FragmentoMisCartas
 *
 * Muestra la colección de cartas guardadas por el usuario.
 *
 * Almacena una lista reducida (id, name, image) en SharedPreferences como JSON.
 *
 * Funcionalidades:
 *  - Mostrar lista (RecyclerView)
 *  - Si está vacía, mostrar vista vacía con FAB grande que navega al buscador
 *  - FAB en esquina para añadir (navega al buscador)
 *  - Al pulsar una carta, abrimos detalle local (usamos action definido: action_misCartas_to_detalleCartaLocal)
 *
 * Comentarios en español y fáciles para un estudiante.
 */
class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    // Nombre de SharedPreferences donde guardamos la colección
    private val PREFS = "prefs_mis_cartas"
    private val KEY_COLECCION = "coleccion_cartas"

    // Gson para serializar / deserializar
    private val gson = Gson()

    // Adaptador que reutiliza AdaptadorCartas (usa modelo Carta). Convertimos breves -> Carta mínimo.
    private lateinit var adaptador: AdaptadorCartas

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Configuramos RecyclerView
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            // Al hacer click en una carta de "Mis Cartas" navegamos al detalle local.
            // Pasamos el id como argumento para que FragmentoDetalleCartaLocal lo cargue.
            val bundle = Bundle().apply {
                putString("arg_id_carta_local", carta.id)
                // También podemos pasar la miniatura como fallback
                carta.images?.small?.let { putString("arg_image_base", it) }
            }
            // usa la acción que definiste en el nav_graph
            findNavController().navigate(R.id.action_misCartas_to_detalleCartaLocal, bundle)
        }

        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adaptador

        // Clicks: FAB vacío y FAB añadir llevan al buscador para elegir cartas
        binding.fabVacioAgregar.setOnClickListener {
            navegarAlBuscador()
        }
        binding.fabAnadir.setOnClickListener {
            navegarAlBuscador()
        }

        // Cargar la colección guardada
        mostrarColeccionCargada()
    }

    /**
     * Navega de forma segura al fragmento de búsqueda de cartas.
     * El usuario elegirá cartas en la búsqueda; la lógica para "añadir" desde búsqueda
     * deberá llamar a la función pública 'guardarCartaLocal(...)' o enviar resultado al nav.
     */
    private fun navegarAlBuscador() {
        try {
            // La acción desde Perfil/Login se usó antes; aquí navegamos directamente al fragmento de búsqueda
            findNavController().navigate(R.id.fragmentoBusquedaCartas)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el buscador: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Carga la colección desde SharedPreferences y actualiza la UI.
     */
    private fun mostrarColeccionCargada() {
        val coleccion = cargarColeccion()
        if (coleccion.isEmpty()) {
            // Mostrar estado vacío
            binding.layoutVacio.visibility = View.VISIBLE
            binding.rvMisCartas.visibility = View.GONE
        } else {
            // Convertir CartaTCGdexBreve -> Carta (mínimo necesario para el adaptador)
            val listaCartas: List<Carta> = coleccion.map { breve ->
                Carta(
                    id = breve.id,
                    name = breve.name,
                    images = com.cesar.creamazospoketcg.data.model.ImagenesCarta(small = breve.image, large = breve.image)
                )
            }
            adaptador.actualizarLista(listaCartas)
            binding.layoutVacio.visibility = View.GONE
            binding.rvMisCartas.visibility = View.VISIBLE
        }
    }

    /**
     * Guarda una carta localmente en la colección del usuario.
     * - Si la carta ya existe (mismo id) no la duplica.
     * - Se guarda la versión "breve" (id,name,image) por simplicidad.
     *
     * Se puede llamar desde otro fragment (por ejemplo: FragmentoDetalleCartaLocal) si éste obtiene la carta completa.
     */
    fun guardarCartaLocal(carta: Carta) {
        try {
            // Construimos el objeto "breve"
            val imagen = carta.images?.small ?: carta.images?.large
            val breve = CartaTCGdexBreve(id = carta.id, localId = null, name = carta.name, image = imagen)

            val listaActual = cargarColeccion().toMutableList()
            // evitar duplicados
            if (listaActual.any { it.id == breve.id }) {
                Toast.makeText(requireContext(), "La carta ya está en tu colección", Toast.LENGTH_SHORT).show()
                return
            }
            listaActual.add(0, breve) // añadimos al inicio
            salvarColeccion(listaActual)
            Toast.makeText(requireContext(), "Carta añadida a tu colección", Toast.LENGTH_SHORT).show()
            mostrarColeccionCargada()
        } catch (e: Exception) {
            Log.e("FragmentoMisCartas", "Error guardando carta local", e)
            Toast.makeText(requireContext(), "Error al guardar carta", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Elimina una carta por id de la colección local. Muestra confirmación por Toast.
     */
    fun eliminarCartaLocal(idCarta: String) {
        try {
            val lista = cargarColeccion().toMutableList()
            val borrado = lista.removeAll { it.id == idCarta }
            if (borrado) {
                salvarColeccion(lista)
                Toast.makeText(requireContext(), "Carta eliminada", Toast.LENGTH_SHORT).show()
                mostrarColeccionCargada()
            } else {
                Toast.makeText(requireContext(), "No se encontró la carta", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FragmentoMisCartas", "Error eliminando carta local", e)
            Toast.makeText(requireContext(), "Error al eliminar carta", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cargar lista de CartaTCGdexBreve desde SharedPreferences (JSON).
     */
    private fun cargarColeccion(): List<CartaTCGdexBreve> {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COLECCION, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CartaTCGdexBreve>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e("FragmentoMisCartas", "Error parseando coleccion JSON", e)
            emptyList()
        }
    }

    /**
     * Guardar lista de CartaTCGdexBreve en SharedPreferences (JSON).
     */
    private fun salvarColeccion(lista: List<CartaTCGdexBreve>) {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(lista)
        editor.putString(KEY_COLECCION, json).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
