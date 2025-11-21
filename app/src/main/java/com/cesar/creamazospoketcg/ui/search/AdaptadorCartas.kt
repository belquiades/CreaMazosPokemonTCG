package com.cesar.creamazospoketcg.ui.search

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cesar.creamazospoketcg.R
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.databinding.ItemCartaBinding

/**
 * AdaptadorCartas
 *
 * Adaptador sencillo para mostrar una lista de cartas.
 * Utiliza ViewBinding (ItemCartaBinding) y Glide para cargar la imagen.
 */
class AdaptadorCartas(
    private var listaCartas: List<Carta> = emptyList(),
    private val onClick: (Carta) -> Unit = {}
) : RecyclerView.Adapter<AdaptadorCartas.CartaViewHolder>() {

    companion object {
        private const val TAG = "AdaptadorCartas"

        /**
         * Construye la URL final a partir de la ruta base de TCGdex assets.
         * Si la URL ya contiene extensión la devuelve tal cual.
         * No realiza comprobación remota; devuelve la primera combinación construida.
         */
        private fun construirUrlAsset(base: String?): String? {
            if (base.isNullOrBlank()) return null

            val lower = base.lowercase()
            // Si ya tiene extensión, devolver tal cual
            if (lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return base
            }

            // Normalizar / final
            val baseSinSlash = if (base.endsWith("/")) base.dropLast(1) else base

            // Preferencia: high.webp -> high.png -> low.webp -> low.png
            // Devolvemos la base sin extensión; en binding usaremos los fallback con Glide.
            return baseSinSlash
        }

        /**
         * Genera la URL completa dado un base y una tupla (quality, extension)
         */
        private fun urlConQuality(baseSinExt: String?, quality: String, ext: String): String? {
            if (baseSinExt.isNullOrBlank()) return null
            return "$baseSinExt/$quality.$ext"
        }
    }

    inner class CartaViewHolder(private val binding: ItemCartaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun vincular(carta: Carta) {
            binding.tvNombreCarta.text = carta.name
            val subtitulo = listOfNotNull(carta.types?.joinToString(", "), carta.rarity)
                .filter { it.isNotEmpty() }
                .joinToString(" — ")
            binding.tvSubtitulo.text = subtitulo

            // Obtener base (puede ser una URL completa o la ruta base tipo: .../fut2020/1)
            val posibleBase = carta.images?.small ?: carta.images?.large

            val baseSinExt = construirUrlAsset(posibleBase)

            // Construimos las posibles URLs en orden de preferencia
            val primary = urlConQuality(baseSinExt, "high", "webp")
            val alt1 = urlConQuality(baseSinExt, "high", "png")
            val alt2 = urlConQuality(baseSinExt, "low", "webp")
            val alt3 = urlConQuality(baseSinExt, "low", "png")

            Log.d(TAG, "bind() cartaId='${carta.id}' base='$posibleBase' primary='$primary' alt1='$alt1' alt2='$alt2' alt3='$alt3'")


            // Si la URL ya incluía extensión (construirUrlAsset la habría devuelto tal cual),
            // entonces primary será null y usaremos posibleBase directamente.
            val primerLoad = if (!posibleBase.isNullOrBlank() && (
                        posibleBase.endsWith(".png") ||
                                posibleBase.endsWith(".webp") ||
                                posibleBase.endsWith(".jpg") ||
                                posibleBase.endsWith(".jpeg")
                        )) {
                posibleBase
            } else {
                primary ?: alt1 ?: alt2 ?: alt3
            }

            // Glide con fallback encadenado usando RequestBuilder::error()
            val glide = Glide.with(binding.root)
                .load(primerLoad)
                .centerCrop()
                .placeholder(R.drawable.placeholder_carta)
                .error(R.drawable.placeholder_carta)

            // Si hay alternativas, encadenamos .error(Glide.with(...).load(alt))
            // Glide.error() acepta un RequestBuilder para fallback.
            val glideConFallback = when {
                primerLoad == primary && alt1 != null -> glide.error(
                    Glide.with(binding.root)
                        .load(alt1)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_carta)
                        .error(
                            Glide.with(binding.root)
                                .load(alt2)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_carta)
                                .error(
                                    Glide.with(binding.root)
                                        .load(alt3)
                                        .centerCrop()
                                        .placeholder(R.drawable.placeholder_carta)
                                )
                        )
                )
                primerLoad == alt1 && alt2 != null -> glide.error(
                    Glide.with(binding.root)
                        .load(alt2)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_carta)
                        .error(
                            Glide.with(binding.root)
                                .load(alt3)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_carta)
                        )
                )
                primerLoad == alt2 && alt3 != null -> glide.error(
                    Glide.with(binding.root)
                        .load(alt3)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_carta)
                )
                else -> glide
            }

            glideConFallback.into(binding.ivImagenCarta)

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
