package com.cesar.creamazospoketcg.data.network

import com.cesar.creamazospoketcg.data.model.Ataque
import com.cesar.creamazospoketcg.data.model.Habilidad
import com.cesar.creamazospoketcg.data.model.Carta
import com.cesar.creamazospoketcg.data.model.ImagenesCarta
import com.cesar.creamazospoketcg.data.model.SetInfo

/**
 * CartaTCGdexCompleta
 * Modelo para el endpoint de detalle /v2/{lang}/card/{id}
 * Incluye attacks y abilities que necesitamos mostrar en detalle.
 */
data class CartaTCGdexCompleta(
    val id: String,
    val name: String,
    val images: ImagenesBreves? = null,
    val types: List<String>? = null,
    val rarity: String? = null,
    val set: SetDetalle? = null,
    val attacks: List<AtaqueTCGdex>? = null,
    val abilities: List<HabilidadTCGdex>? = null
)

data class SetDetalle(val id: String? = null, val name: String? = null)
data class AtaqueTCGdex(val name: String? = null, val damage: String? = null, val text: String? = null)
data class HabilidadTCGdex(val name: String? = null, val text: String? = null)

/**
 * aCartaCompleta(): mapea CartaTCGdexCompleta -> Carta (modelo interno)
 * Asegúrate de que los modelos Ataque/Habilidad en data.model existen con esos campos.
 */
fun CartaTCGdexCompleta.aCartaCompleta(): Carta {
    val imagenes = this.images
    return Carta(
        id = this.id,
        name = this.name,
        types = this.types,
        rarity = this.rarity,
        set = this.set?.let { SetInfo(id = it.id, name = it.name) },
        images = imagenes?.let { ImagenesCarta(small = it.small, large = it.large) },
        attacks = this.attacks?.map { Ataque(name = it.name ?: "", damage = it.damage, text = it.text) },
        abilities = this.abilities?.map { Habilidad(name = it.name ?: "", text = it.text) }
    )
}
