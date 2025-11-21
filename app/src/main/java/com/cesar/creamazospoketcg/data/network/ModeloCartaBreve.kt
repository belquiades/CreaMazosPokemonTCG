package com.cesar.creamazospoketcg.data.network

import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.ImagenesCarta
import com.cesar.creamazospoketcg.data.model.SetInfo

/**
 * CartaTCGdexBreve
 * Modela la estructura que devuelve /v2/{lang}/cards (CardBrief).
 * Algunos endpoints devuelven 'image' o 'images':{small,large}.
 */
data class CartaTCGdexBreve(
    val id: String,
    val localId: String? = null,
    val name: String,
    val image: String? = null,            // a veces la API usa 'image'
    val images: ImagenesBreves? = null,   // o 'images': { small, large }
    val setId: String? = null,
    val setName: String? = null
)

data class ImagenesBreves(
    val small: String? = null,
    val large: String? = null
)

/**
 * aCarta(): mapea la estructura breve al modelo interno `Carta`.
 * - Prioriza images.large, luego images.small, luego image.
 */
fun CartaTCGdexBreve.aCarta(): Carta {
    val urlGrande = this.images?.large ?: this.images?.small ?: this.image
    val urlPequena = this.images?.small ?: this.image
    return Carta(
        id = this.id,
        name = this.name,
        types = null,
        rarity = null,
        set = if (setId != null || setName != null) SetInfo(id = setId, name = setName) else null,
        images = ImagenesCarta(small = urlPequena, large = urlGrande),
        attacks = null,
        abilities = null
    )
}
