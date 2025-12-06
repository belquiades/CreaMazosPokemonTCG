package com.cesar.creamazospoketcg.ui.miscartas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.FragmentMisCartasBinding
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent


private const val TAG = "MisCartas"

class FragmentoMisCartas : Fragment() {

    private var _binding: FragmentMisCartasBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // DTO ligero para la UI (no tocar tu modelo Carta original)
    data class LocalCard(val id: String, val name: String?, val imageUrl: String?)

    // Adapter interno (ListAdapter para facilidad de updates)
    private val adapter = object : ListAdapter<LocalCard, CardVH>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val bind = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CardVH(bind)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class CardVH(private val binding: ItemCartaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LocalCard) {
            binding.tvNombreCarta.text = item.name ?: "—"
            val iv = binding.ivImagenCarta
            val url = item.imageUrl
            if (url.isNullOrBlank()) {
                iv.setImageResource(R.drawable.placeholder_carta)
            } else {
                Glide.with(iv.context)
                    .load(url)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_carta)
                    .error(R.drawable.placeholder_carta)
                    .into(iv)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LocalCard>() {
            override fun areItemsTheSame(oldItem: LocalCard, newItem: LocalCard): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: LocalCard, newItem: LocalCard): Boolean = oldItem == newItem
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMisCartasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvMisCartas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMisCartas.adapter = adapter

        // Click en item -> navegar a detalle local
        binding.rvMisCartas.addOnItemTouchListener(
            RecyclerItemClickListener(requireContext(), binding.rvMisCartas) { position ->
                val item = adapter.currentList.getOrNull(position) ?: return@RecyclerItemClickListener
                // Intent: action id según tu nav_graph: action_misCartas_to_detalleCartaLocal
                try {
                    val bundle = bundleOf(
                        "arg_id_carta" to item.id,
                        "arg_image_base" to (item.imageUrl ?: "")
                    )
                    findNavController().navigate(R.id.action_misCartas_to_detalleCartaLocal, bundle)
                } catch (e: Exception) {
                    Log.w(TAG, "Navegación a detalle local falló", e)
                    Toast.makeText(requireContext(), "No se pudo abrir detalle", Toast.LENGTH_SHORT).show()
                }
            }
        )

        attachSwipeToDelete()

        // Cargar cartas desde Firestore
        cargarCartasUsuario()
    }

    private fun cargarCartasUsuario() {
        val user = auth.currentUser
        if (user == null) {
            showEmptyState()
            return
        }

        val uid = user.uid
        firestore.collection("users")
            .document(uid)
            .collection("cards")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                val list = snap.documents.mapNotNull { doc ->
                    val id = doc.id
                    // Intentar distintos nombres de campo que puedas haber usado
                    val img = doc.getString("image")
                        ?: doc.getString("imageUrl")
                        ?: doc.getString("imageResolvedUrl")
                        ?: doc.getString("imageResolved")
                        ?: doc.getString("imageOriginalUrl")
                        ?: ""
                    val name = doc.getString("name") ?: doc.getString("title") ?: ""
                    LocalCard(id = id, name = name.ifBlank { "—" }, imageUrl = if (img.isBlank()) null else img)
                }
                adapter.submitList(list)
                binding.tvVacio.visibility = View.GONE
                binding.rvMisCartas.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user cards", e)
                Toast.makeText(requireContext(), "Error cargando cartas", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        binding.rvMisCartas.visibility = View.GONE
        binding.tvVacio.visibility = View.VISIBLE
    }

    /**
     * Swipe left (o right) to delete — pide confirmación y muestra UNDO via Snackbar.
     */
    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val item = adapter.currentList.getOrNull(pos) ?: run {
                    adapter.notifyItemChanged(pos)
                    return
                }

                // Confirm dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar carta")
                    .setMessage("¿Seguro que quieres eliminar \"${item.name ?: item.id}\" de tu colección?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        // Perform delete with backup for undo
                        deleteCardWithUndo(item, pos)
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        adapter.notifyItemChanged(pos)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(pos)
                    }
                    .show()
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(binding.rvMisCartas)
    }

    /**
     * Borra la carta en Firestore y muestra Snackbar con DESHACER que recrea el documento.
     */
    private fun deleteCardWithUndo(item: LocalCard, position: Int) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para borrar cartas", Toast.LENGTH_SHORT).show()
            adapter.notifyItemChanged(position)
            return
        }

        val uid = user.uid
        val docRef = firestore.collection("users").document(uid).collection("cards").document(item.id)

        // Leer snapshot para backup
        docRef.get()
            .addOnSuccessListener { snapshot ->
                // snapshot.data puede ser null; convertimos a Map<String, Any?>
                val backup: Map<String, Any?> = snapshot.data?.mapValues { it.value } ?: mapOf(
                    "name" to item.name,
                    "image" to item.imageUrl,
                    "addedAt" to Timestamp.now()
                )

                // Borrar doc
                docRef.delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Deleted ${item.id} from Firestore")
                        // Eliminar visualmente
                        val newList = adapter.currentList.toMutableList()
                        val index = newList.indexOfFirst { it.id == item.id }
                        if (index >= 0) {
                            newList.removeAt(index)
                            adapter.submitList(newList)
                        } else {
                            adapter.notifyItemChanged(position)
                        }

                        // Snackbar con UNDO
                        val snack = Snackbar.make(binding.root, "Carta eliminada", Snackbar.LENGTH_LONG)
                        snack.setAction("DESHACER") {
                            // recrear documento con backup
                            docRef.set(backup)
                                .addOnSuccessListener {
                                    Log.d(TAG, "UNDO: recreated ${item.id}")
                                    // recargar o reinsertar localmente
                                    val restored = LocalCard(item.id, backup["name"] as? String, backup["image"] as? String)
                                    val restoredList = adapter.currentList.toMutableList()
                                    // insert at original position if possible
                                    val insertAt = if (position <= restoredList.size) position else restoredList.size
                                    restoredList.add(insertAt, restored)
                                    adapter.submitList(restoredList)
                                    Toast.makeText(requireContext(), "Carta restaurada", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "UNDO failed for ${item.id}", e)
                                    Toast.makeText(requireContext(), "No se pudo deshacer", Toast.LENGTH_LONG).show()
                                }
                        }
                        snack.show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete ${item.id}", e)
                        Toast.makeText(requireContext(), "Error eliminando carta", Toast.LENGTH_SHORT).show()
                        adapter.notifyItemChanged(position)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to read doc before delete ${item.id}", e)
                Toast.makeText(requireContext(), "No se pudo comprobar la carta antes de borrar", Toast.LENGTH_SHORT).show()
                adapter.notifyItemChanged(position)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Helper: detecta clicks simples en RecyclerView items (sin depender de la implementación del adapter).
 * Uso: RecyclerView.addOnItemTouchListener(RecyclerItemClickListener(...){ pos -> ... })
 */

class RecyclerItemClickListener(
    context: Context,
    private val recyclerView: RecyclerView,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean = true
    })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(e)) {
            val child = rv.findChildViewUnder(e.x, e.y)
            if (child != null) {
                val vh = rv.getChildViewHolder(child)
                onItemClick(vh.bindingAdapterPosition)
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) { /* no-op */ }
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) { /* no-op */ }
}
