package com.cesar.creamazospoketcg.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding

class AdaptadorCartas(
    private var listaCartas: List<Carta> = emptyList(),
    private val onClick: (Carta) -> Unit = {}
) : RecyclerView.Adapter<AdaptadorCartas.CartaViewHolder>() {

    inner class CartaViewHolder(private val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(carta: Carta) {
            binding.tvNombreCarta.text = carta.name ?: "—"

            // Preferimos small (miniatura). Si no existe, fallback a large.
            val imageUrl = carta.images?.small ?: carta.images?.large

            android.util.Log.d("AdaptadorCartas", "bind() id='${carta.id}' imageUrl='$imageUrl'")

            // Si imageUrl es null, Glide mostrará placeholder/error que hayas puesto.
            Glide.with(binding.root)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .into(binding.ivImagenCarta)

            binding.root.setOnClickListener { onClick(carta) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaViewHolder {
        val binding = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartaViewHolder, position: Int) {
        holder.vincular(listaCartas[position])
    }

    override fun getItemCount(): Int = listaCartas.size

    fun actualizarLista(nuevaLista: List<Carta>) {
        listaCartas = nuevaLista
        notifyDataSetChanged()
    }
}
