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
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.cesar.creamazospoketcg.databinding.FragmentBusquedaCartasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

class FragmentoBusquedaCartas : Fragment() {

    private var _binding: FragmentBusquedaCartasBinding? = null
    private val binding get() = _binding!!

    private val TAG = "FragmentoBusquedaCartas"

    // ViewModel de búsqueda
    private val vistaModelo: com.cesar.creamazospoketcg.vm.VistaModeloBusquedaCartas by viewModels()

    private lateinit var adaptador: AdaptadorCartas
    private var listaActual: List<Carta> = emptyList()

    // Firebase
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBusquedaCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Adaptador sin long-press, sólo onClick para abrir detalle
        adaptador = AdaptadorCartas(
            emptyList(),
            onClick = { carta ->
                val bundle = Bundle().apply {
                    putString("arg_id_carta", carta.id)
                    val imageBaseParaDetalle = carta.images?.small ?: carta.images?.large
                    if (!imageBaseParaDetalle.isNullOrBlank()) {
                        putString("arg_image_base", imageBaseParaDetalle)
                    }
                }
                findNavController().navigate(R.id.fragmentoDetalleCarta, bundle)
            }
        )

        binding.rvCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartas.adapter = adaptador

        // Botón buscar
        binding.btnBuscar.setOnClickListener { realizarBusqueda() }

        // Teclado: acción buscar
        binding.etConsulta.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                realizarBusqueda()
                true
            } else false
        }

        // Observador del ViewModel
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
                            adaptador.actualizarLista(listaActual)
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

        Log.d(TAG, "onViewCreated - inicio")
    }

    private fun realizarBusqueda() {
        val consulta = binding.etConsulta.text.toString().takeIf { it.isNotBlank() }
        vistaModelo.buscarCartas(consulta)
    }

    /**
     * Método para añadir cartas del botón “Añadir carta”
     */
    fun añadirCartaAUsuario(carta: Carta) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val userDocRef = firestore.collection("users").document(user.uid)
        val cardDocRef = userDocRef.collection("cards").document(carta.id)

        val datosCarta = mapOf(
            "name" to carta.name,
            "image" to (carta.images?.small ?: carta.images?.large),
            "addedAt" to com.google.firebase.Timestamp.now()
        )

        firestore.runTransaction { tx ->
            val snapshot = tx.get(cardDocRef)

            if (snapshot.exists()) {
                false // No se añade porque ya existe
            } else {
                tx.set(cardDocRef, datosCarta, SetOptions.merge())
                tx.update(userDocRef, "totalCards", FieldValue.increment(1))
                true
            }
        }.addOnSuccessListener { added ->
            if (added == true) {
                Toast.makeText(requireContext(), "Carta añadida", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Ya tienes esta carta", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error añadiendo carta", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error añadiendo carta", it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
