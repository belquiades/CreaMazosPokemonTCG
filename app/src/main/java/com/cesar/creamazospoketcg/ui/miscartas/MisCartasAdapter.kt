package com.cesar.creamazospoketcg.ui.miscartas

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding
import com.cesar.creamazospoketcg.model.CartaGuardada

class MisCartasAdapter(
    private val onClick: (CartaGuardada) -> Unit
) : ListAdapter<CartaGuardada, MisCartasAdapter.MisCartasVH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<CartaGuardada>() {
        override fun areItemsTheSame(old: CartaGuardada, new: CartaGuardada) =
            old.id == new.id

        override fun areContentsTheSame(old: CartaGuardada, new: CartaGuardada) =
            old == new
    }

    inner class MisCartasVH(
        private val binding: ItemCartaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartaGuardada) {
            binding.tvNombreCarta.text = item.name.ifBlank { item.id }

            // Elegimos la mejor imagen disponible
            val imageUrl = item.imageResolvedUrl ?: item.imageOriginalUrl

            Glide.with(binding.root)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .into(binding.ivImagenCarta)

            // Mostrar cantidad solo si > 1
            if (item.quantity > 1) {
                binding.tvCantidad.visibility = View.VISIBLE
                binding.tvCantidad.text = "x${item.quantity}"
            } else {
                binding.tvCantidad.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onClick(item)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisCartasVH {
        val binding = ItemCartaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MisCartasVH(binding)
    }

    override fun onBindViewHolder(holder: MisCartasVH, position: Int) {
        holder.bind(getItem(position))
    }
}
