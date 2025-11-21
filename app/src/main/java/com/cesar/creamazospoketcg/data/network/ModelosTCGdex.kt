package com.cesar.creamazospoketcg.data.network

import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.ImagenesCarta
import com.cesar.creamazospoketcg.data.model.SetInfo

/**
 * ModelosTCGdex
 *
 * Clases para mapear la respuesta de TCGdex.
 * Los nombres están en español (RespuestaListaCartasTCGdex, CartaTCGdex...).
 * A partir de aquí transformamos a nuestro modelo interno `Carta`.
 */

data class RespuestaListaCartasTCGdex(
    val total: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,
    val data: List<CartaTCGdex> = emptyList()
)

data class CartaTCGdex(
    val id: String,
    val name: String,
    val set: SetTCGdex? = null,
    val images: ImagenesTCGdex? = null,
    val types: List<String>? = null,
    val rarity: String? = null
    // Añade más campos si los necesitas (attacks, abilities, etc.)
)

data class SetTCGdex(val id: String? = null, val name: String? = null)
data class ImagenesTCGdex(val small: String? = null, val large: String? = null)

/**
 * Función de extensión para convertir CartaTCGdex -> Carta (modelo interno)
 */
fun CartaTCGdex.aCarta(): Carta {
    return Carta(
        id = this.id,
        name = this.name,
        types = this.types,
        rarity = this.rarity,
        set = this.set?.let { SetInfo(id = it.id, name = it.name) },
        images = this.images?.let { ImagenesCarta(small = it.small, large = it.large) },
        attacks = null,
        abilities = null
    )
}
