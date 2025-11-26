package com.cesar.creamazospoketcg.data.model

data class CartaTCGdexBreve(
    val id: String,
    val name: String,
    val image: String? = null
) {
    fun aCartaLocalMinima(): Carta {
        return Carta(
            id = id,
            localId = null,
            name = name,
            superType = null,
            subTypes = null,
            types = null,
            hp = null,
            evolvesFrom = null,
            attacks = null,
            weaknesses = null,
            resistances = null,
            retreat = null,
            illustrator = null,
            rarity = null,
            set = null,
            images = ImagenesCarta(small = image, large = image),
            flavorText = null
        )
    }
}
