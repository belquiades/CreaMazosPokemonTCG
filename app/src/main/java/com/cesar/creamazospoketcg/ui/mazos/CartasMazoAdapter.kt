package com.cesar.creamazospoketcg.ui.mazos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cesar.creamazospoketcg.databinding.ItemCartaMazoBinding
import com.cesar.creamazospoketcg.model.CartaEnMazo

class CartasMazoAdapter :
    ListAdapter<CartaEnMazo, CartasMazoAdapter.CartaVH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<CartaEnMazo>() {
        override fun areItemsTheSame(old: CartaEnMazo, new: CartaEnMazo) =
            old.cardId == new.cardId

        override fun areContentsTheSame(old: CartaEnMazo, new: CartaEnMazo) =
            old == new
    }

    inner class CartaVH(private val binding: ItemCartaMazoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(carta: CartaEnMazo) {
            binding.tvNombreCarta.text = carta.nombre
            binding.tvCantidad.text = "x${carta.cantidad}"
            binding.tvTipo.text = carta.tipo
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartaVH {
        val binding = ItemCartaMazoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartaVH(binding)
    }

    override fun onBindViewHolder(holder: CartaVH, position: Int) {
        holder.bind(getItem(position))
    }
}
