package com.cesar.creamazospoketcg.data.network

import com.cesar.creamazospoketcg.data.model.*

// Un único fichero para DTOs TCGdex y mapeo a data.model.Carta (firma EXACTA de Carta.kt)

/** Respuesta de lista (si la API devuelve un envoltorio) */
data class RespuestaListaCartasTCGdex(
    val total: Int = 0,
    val limit: Int = 0,
    val page: Int = 0,
    val data: List<CartaDTO> = emptyList()
)

/** DTO breve usado en listados */
data class CartaDTO(
    val id: String,
    val name: String? = null,
    val set: SetDTO? = null,
    val images: ImagenesDTO? = null,
    val types: List<String>? = null,
    val rarity: String? = null
) {
    fun aCarta(): Carta {
        return Carta(
            id = id,
            localId = null,
            name = name,
            superType = null,
            subTypes = null,
            types = types,
            hp = null,
            evolvesFrom = null,
            attacks = null,
            weaknesses = null,
            resistances = null,
            retreat = null,
            illustrator = null,
            rarity = rarity,
            set = set?.toSetInfo(),
            images = images?.toImagenesCarta(),
            flavorText = null
        )
    }
}

/** DTO detalle (cards/{id}) */
data class CartaCompletaDTO(
    val id: String,
    val localId: String? = null,
    val name: String? = null,
    val superType: String? = null,
    val subTypes: List<String>? = null,
    val types: List<String>? = null,
    val hp: Int? = null,
    val evolvesFrom: String? = null,
    val attacks: List<AtaqueDTO>? = null,
    val weaknesses: List<ElementoValorDTO>? = null,
    val resistances: List<ElementoValorDTO>? = null,
    val retreat: Int? = null,
    val illustrator: String? = null,
    val rarity: String? = null,
    val set: SetDTO? = null,
    val images: ImagenesDTO? = null,
    val flavorText: String? = null
    // NOTE: no incluimos 'abilities' porque tu Carta.kt no lo define.
) {
    fun aCartaCompleta(): Carta {
        return Carta(
            id = id,
            localId = localId,
            name = name,
            superType = superType,
            subTypes = subTypes,
            types = types,
            hp = hp,
            evolvesFrom = evolvesFrom,
            attacks = attacks?.map { it.toAtaque() },
            weaknesses = weaknesses?.map { ElementoValor(type = it.type, value = it.value) },
            resistances = resistances?.map { ElementoValor(type = it.type, value = it.value) },
            retreat = retreat,
            illustrator = illustrator,
            rarity = rarity,
            set = set?.toSetInfo(),
            images = images?.toImagenesCarta(),
            flavorText = flavorText
        )
    }
}

/* ---- DTOs auxiliares ---- */
data class AtaqueDTO(
    val name: String? = null,
    val cost: List<String>? = null,
    val damage: String? = null,
    val effect: String? = null,
    val text: String? = null
) {
    fun toAtaque(): Ataque = Ataque(
        name = name,
        cost = cost,
        damage = damage,
        effect = effect ?: text
    )
}

data class ElementoValorDTO(val type: String? = null, val value: String? = null)

data class SetDTO(
    val id: String? = null,
    val name: String? = null,
    val series: String? = null,
    val printedTotal: Int? = null,
    val total: Int? = null,
    val releaseDate: String? = null
) {
    fun toSetInfo(): SetInfo = SetInfo(id = id, name = name, series = series, printedTotal = printedTotal, total = total, releaseDate = releaseDate)
}

data class ImagenesDTO(val small: String? = null, val large: String? = null) {
    fun toImagenesCarta(): ImagenesCarta = ImagenesCarta(small = small, large = large)
}
