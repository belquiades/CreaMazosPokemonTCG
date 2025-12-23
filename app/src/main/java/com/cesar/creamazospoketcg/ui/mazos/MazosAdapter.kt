package com.cesar.creamazospoketcg.ui.mazos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cesar.creamazospoketcg.databinding.ItemMazoBinding
import com.cesar.creamazospoketcg.model.Mazo

class MazosAdapter :
    ListAdapter<Mazo, MazosAdapter.MazoVH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Mazo>() {
        override fun areItemsTheSame(old: Mazo, new: Mazo) = old.id == new.id
        override fun areContentsTheSame(old: Mazo, new: Mazo) = old == new
    }

    inner class MazoVH(private val binding: ItemMazoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mazo: Mazo) {
            binding.tvNombreMazo.text = mazo.nombre
            binding.tvTotal.text = "Total: ${mazo.totalCartas}"
            binding.tvPokemon.text = "Pokémon: ${mazo.pokemonCount}"
            binding.tvEntrenador.text = "Entrenador: ${mazo.entrenadorCount}"
            binding.tvEnergia.text = "Energía: ${mazo.energiaCount}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MazoVH {
        val binding = ItemMazoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MazoVH(binding)
    }

    override fun onBindViewHolder(holder: MazoVH, position: Int) {
        holder.bind(getItem(position))
    }
}
