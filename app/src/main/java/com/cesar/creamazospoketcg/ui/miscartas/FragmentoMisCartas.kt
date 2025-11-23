package com.cesar.creamazospoketcg.ui.miscartas

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.cesar.creamazospoketcg.ui.search.AdaptadorCartas
import com.cesar.creamazospoketcg.data.repository.RepositorioCartas
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.cesar.creamazospoketcg.R
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.cesar.creamazospoketcg.data.model.Carta

/**
 * FragmentoMisCartas
 *
 * Muestra todas las cartas que el usuario ha guardado en Firestore.
 * Usa el RepositorioCartas para obtener los detalles.
 */
class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repo = RepositorioCartas()

    private lateinit var adaptador: AdaptadorCartas
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Configurar RecyclerView
        adaptador = AdaptadorCartas(emptyList()) { carta ->
            abrirDetalleLocal(carta)
        }

        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adaptador

        cargarCartasUsuario()
    }

    private fun cargarCartasUsuario() {
        val uid = auth.currentUser?.uid ?: return

        binding.pbCargando.visibility = View.VISIBLE

        scope.launch {
            try {
                val snapshot = db.collection("usuarios")
                    .document(uid)
                    .collection("mis_cartas")
                    .get()
                    .await()

                val listaIds = snapshot.documents.mapNotNull { it.id }

                if (listaIds.isEmpty()) {
                    binding.tvMensaje.text = "No tienes cartas guardadas."
                    binding.tvMensaje.visibility = View.VISIBLE
                    binding.pbCargando.visibility = View.GONE
                    return@launch
                }

                val resultado = repo.obtenerCartasPorIds(listaIds)

                if (resultado.isSuccess) {
                    val cartas = resultado.getOrNull() ?: emptyList()
                    adaptador.actualizarLista(cartas)
                } else {
                    binding.tvMensaje.text = "No se pudieron cargar tus cartas."
                    binding.tvMensaje.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.tvMensaje.text = "Error cargando cartas."
                binding.tvMensaje.visibility = View.VISIBLE
            } finally {
                binding.pbCargando.visibility = View.GONE
            }
        }
    }

    private fun abrirDetalleLocal(carta: Carta) {
        val b = Bundle().apply {
            putString("id_local", carta.id)
        }
        findNavController().navigate(R.id.fragmentoDetalleCartaLocal, b)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        scope.cancel()
    }
}
