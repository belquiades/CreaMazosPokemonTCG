package com.cesar.creamazospoketcg.ui.miscartas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding

data class CartaGuardada(
    val id: String,
    val imageUrl: String?
)

class MisCartasAdapter(
    private val onClick: (String, String?) -> Unit
) : ListAdapter<CartaGuardada, MisCartasAdapter.MisCartasVH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<CartaGuardada>() {
        override fun areItemsTheSame(old: CartaGuardada, new: CartaGuardada) = old.id == new.id
        override fun areContentsTheSame(old: CartaGuardada, new: CartaGuardada) = old == new
    }

    inner class MisCartasVH(private val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartaGuardada) {
            binding.tvNombreCarta.text = item.id

            Glide.with(binding.root)
                .load(item.imageUrl)
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)
                .into(binding.ivImagenCarta)

            binding.root.setOnClickListener {
                onClick(item.id, item.imageUrl)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisCartasVH {
        val binding = ItemCartaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MisCartasVH(binding)
    }

    override fun onBindViewHolder(holder: MisCartasVH, position: Int) {
        holder.bind(getItem(position))
    }
}
