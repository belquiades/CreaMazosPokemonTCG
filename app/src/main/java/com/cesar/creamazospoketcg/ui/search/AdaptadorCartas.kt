package com.cesar.creamazospoketcg.ui.search

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding
import com.cesar.creamazospoketcg.utils.ImageResolverTcgDex

class AdaptadorCartas(
    private var listaCartas: List<Carta> = emptyList(),
    private val onClick: (Carta) -> Unit = {}
) : RecyclerView.Adapter<AdaptadorCartas.CartaViewHolder>() {

    inner class CartaViewHolder(private val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(carta: Carta) {
            binding.tvNombreCarta.text = carta.name ?: "—"

            Log.d("AdaptadorCartas", "bind() id='${carta.id}' name='${carta.name}'")

            // CARGA EXCLUSIVA DESDE TCGDEX
            ImageResolverTcgDex.loadInto(binding.ivImagenCarta, carta)

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
