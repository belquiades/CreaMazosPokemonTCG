package com.cesar.creamazospoketcg.ui.search

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentBusquedaCartasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

/**
 * FragmentoBusquedaCartas (mejorado)
 *
 * - Muestra un EditText para búsqueda, botón Buscar y RecyclerView con resultados.
 * - Click simple: navegar a detalle remoto (fragmentoDetalleCarta).
 * - Long-press sobre un item: mostrar diálogo para añadir la carta a la colección del usuario
 *   (Firestore -> users/{uid}/cards/{cardId}).
 *
 * Comentarios y nombres en español para facilitar el aprendizaje.
 */
class FragmentoBusquedaCartas : Fragment() {

    private var _binding: FragmentBusquedaCartasBinding? = null
    private val binding get() = _binding!!

    private val TAG = "FragmentoBusquedaCartas"

    // ViewModel si ya existe en tu proyecto (mantengo uso actual)
    private val vistaModelo: com.cesar.creamazospoketcg.vm.VistaModeloBusquedaCartas by viewModels()

    private lateinit var adaptador: AdaptadorCartas
    private var listaActual: List<Carta> = emptyList()

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Repositorio para llamadas a la API (si lo necesitas)
    private val repo by lazy { RepositorioCartas() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBusquedaCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inicializamos adaptador. El onClick abre detalle (comportamiento ya existente).
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            val bundle = Bundle().apply {
                putString("arg_id_carta", carta.id)
                val imageBaseParaDetalle = carta.images?.small ?: carta.images?.large
                if (!imageBaseParaDetalle.isNullOrBlank()) {
                    putString("arg_image_base", imageBaseParaDetalle)
                }
            }
            findNavController().navigate(R.id.fragmentoDetalleCarta, bundle)
        }

        binding.rvCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartas.adapter = adaptador

        // Instalamos detector de long-press para "añadir carta" sobre cada item
        instalarDetectorLongPressEnRecycler(binding.rvCartas)

        // Botón buscar (mantiene comportamiento)
        binding.btnBuscar.setOnClickListener { realizarBusqueda() }

        // Acción IME (teclado) -> buscar al pulsar "search"
        binding.etConsulta.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                realizarBusqueda()
                true
            } else false
        }

        // Observamos el estado del ViewModel como antes
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vistaModelo.estadoUI.collect { estado ->
                    when (estado) {
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Cargando -> {
                            binding.pbCargando.visibility = View.VISIBLE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.GONE
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Resultado -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.tvMensaje.visibility = View.GONE
                            binding.rvCartas.visibility = View.VISIBLE
                            listaActual = estado.lista
                            adaptador.actualizarLista(estado.lista)
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Error -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.VISIBLE
                            binding.tvMensaje.text = estado.mensaje
                        }
                        is com.cesar.creamazospoketcg.vm.EstadoBusqueda.Vacío -> {
                            binding.pbCargando.visibility = View.GONE
                            binding.rvCartas.visibility = View.GONE
                            binding.tvMensaje.visibility = View.VISIBLE
                            binding.tvMensaje.text = "Introduce una consulta y pulsa Buscar"
                        }
                    }
                }
            }
        }

        // Mensaje diagnóstico en log (opcional)
        Log.d(TAG, "onViewCreated - inicio")
    }

    /**
     * Lanza la búsqueda en el ViewModel (misma lógica que ya tenías).
     */
    private fun realizarBusqueda() {
        val consulta = binding.etConsulta.text.toString().takeIf { it.isNotBlank() }
        vistaModelo.buscarCartas(consulta)
    }

    /**
     * Instala un detector de long-press sobre el RecyclerView.
     * Cuando se detecta long-press sobre un item se abre un diálogo que permite:
     *  - Añadir la carta a users/{uid}/cards/{cardId}
     */
    private fun instalarDetectorLongPressEnRecycler(rv: RecyclerView) {
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return
                val position = rv.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION) return
                val carta = listaActual.getOrNull(position) ?: return

                // Mostrar diálogo de confirmación para añadir
                AlertDialog.Builder(requireContext())
                    .setTitle("Añadir carta")
                    .setMessage("¿Deseas añadir '${carta.name}' a tu colección?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Añadir") { _, _ ->
                        añadirCartaAUsuario(carta)
                    }
                    .show()
            }
        })

        rv.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // no-op
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // no-op
            }
        })
    }

    /**
     * Añade la carta a Firestore bajo users/{uid}/cards/{cardId}.
     *
     * Guardamos el nombre, una imagen de referencia y la fecha (timestamp) para ordenar
     * posteriormente. Si quieres guardar el objeto completo puedes mapear Carta -> Map y hacer set().
     */
    private fun añadirCartaAUsuario(carta: Carta) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para guardar cartas", Toast.LENGTH_SHORT).show()
            return
        }

        val userDocRef = firestore.collection("users").document(user.uid)
        val cardDocRef = userDocRef.collection("cards").document(carta.id)

        // Mapa mínimo para almacenar la carta
        val datosCarta = mapOf(
            "name" to carta.name,
            "image" to (carta.images?.small ?: carta.images?.large),
            "addedAt" to com.google.firebase.Timestamp.now()
        )

        // Ejecutamos una transacción: si la carta NO existe -> la creamos y aumentamos totalCards en 1.
        firestore.runTransaction { transaction ->
            val cardSnapshot = transaction.get(cardDocRef)
            if (cardSnapshot.exists()) {
                // La carta ya está en la colección del usuario -> no hacemos nada (no incrementamos)
                // Para indicar al hilo exterior que no se añadió, devolvemos false
                false
            } else {
                // Guardamos la carta (merge no es estrictamente necesario porque no existe,
                // pero lo ponemos por seguridad si otros campos se añadieran)
                transaction.set(cardDocRef, datosCarta, SetOptions.merge())

                // Incrementamos (crea el campo si no existía)
                transaction.update(userDocRef, "totalCards", FieldValue.increment(1))

                // Devolvemos true indicando que hemos añadido la carta
                true
            }
        }.addOnSuccessListener { added ->
            if (added == true) {
                Toast.makeText(requireContext(), "Carta añadida a tu colección", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "La carta ya está en tu colección", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error añadiendo carta o actualizando contador", e)
            Toast.makeText(requireContext(), "No se pudo añadir la carta", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
