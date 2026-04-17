package com.example.batmaps

object ComuniDatabase {
    // Mappa dei comuni con (Provincia, Latitudine, Longitudine)
    val database = mapOf(
        "vicenza" to Triple("VI", 45.5479, 11.5446),
        "padova" to Triple("PD", 45.4064, 11.8768),
        "verona" to Triple("VR", 45.4384, 10.9916),
        "venezia" to Triple("VE", 45.4408, 12.3155),
        "treviso" to Triple("TV", 45.6669, 12.2431),
        "belluno" to Triple("BL", 46.1425, 12.2167),
        "rovigo" to Triple("RO", 45.0711, 11.7907),
        "modena" to Triple("MO", 44.6471, 10.9252),
        "milano" to Triple("MI", 45.4642, 9.1900),
        "roma" to Triple("RM", 41.9028, 12.4964),
        "torino" to Triple("TO", 45.0703, 7.6869),
        "bologna" to Triple("BO", 44.4949, 11.3426),
        "brescia" to Triple("BS", 45.5416, 10.2118),
        "trento" to Triple("TN", 46.0679, 11.1211),
        "bergamo" to Triple("BG", 45.6983, 9.6773),

        // Comuni Vicentini
        "arzignano" to Triple("VI", 45.5210, 11.3323),
        "bassano del grappa" to Triple("VI", 45.7665, 11.7342),
        "schio" to Triple("VI", 45.7124, 11.3541),
        "thiene" to Triple("VI", 45.7073, 11.4776),
        "valdagno" to Triple("VI", 45.6486, 11.3023),
        "montecchio maggiore" to Triple("VI", 45.5039, 11.4116),
        "lonigo" to Triple("VI", 45.3881, 11.3869),
        "malo" to Triple("VI", 45.6562, 11.4150),
        "cassola" to Triple("VI", 45.7364, 11.7825),
        "rosà" to Triple("VI", 45.7175, 11.7708),
        "marostica" to Triple("VI", 45.7441, 11.6575),
        "chiampo" to Triple("VI", 45.5428, 11.2797),
        "sandrigo" to Triple("VI", 45.6599, 11.5907),
        "isola vicentina" to Triple("VI", 45.6173, 11.4452),
        "asiago" to Triple("VI", 45.8758, 11.5096),
        "castelgomberto" to Triple("VI", 45.5902, 11.3962),
        "san vito di leguzzano" to Triple("VI", 45.6883, 11.3853),
        "arcugnano" to Triple("VI", 45.4988, 11.5361),
        "marano vicentino" to Triple("VI", 45.6931, 11.4243),
        "breganze" to Triple("VI", 45.7073, 11.5644),
        "costabissara" to Triple("VI", 45.5861, 11.4842),
        "carrè" to Triple("VI", 45.7485, 11.4589),
        "santorso" to Triple("VI", 45.7347, 11.3908),
        "sossano" to Triple("VI", 45.3582, 11.5113),

        // Padova e comuni
        "san martino di lupari" to Triple("PD", 45.6558, 11.8601),
        "cittadella" to Triple("PD", 45.6483, 11.7836),
        "camposampiero" to Triple("PD", 45.5714, 11.9314),
        "vigonza" to Triple("PD", 45.4497, 11.9841),
        "albignasego" to Triple("PD", 45.3477, 11.8619),

        // Treviso e comuni
        "cappella maggiore" to Triple("TV", 45.9723, 12.3619),
        "conegliano" to Triple("TV", 45.8859, 12.2965),
        "vittorio veneto" to Triple("TV", 45.9814, 12.2974)
    )

    fun cercaDati(comune: String, localita: String, provincia: String): Triple<String, Double, Double> {
        val p = provincia.uppercase().trim()
        val c = comune.lowercase().trim()
        val l = localita.lowercase().trim()

        // 1. Priorità: Provincia esplicita (solo sigla, coordinate capoluogo come base)
        if (p.length == 2) {
            val capoluogo = database.entries.find { it.key == getCapoluogo(p) }?.value
            if (capoluogo != null) {
                // Se abbiamo anche il comune, cerchiamo le sue coordinate precise
                database[c]?.let { return it }
                return Triple(p, capoluogo.second, capoluogo.third)
            }
        }

        // 2. Priorità: Comune
        database[c]?.let { return it }

        // 3. Priorità: Località (cerca match nel DB comuni)
        for ((nome, dati) in database) {
            if (l.contains(nome)) return dati
        }

        // Default: Vicenza
        return Triple("VI", 45.5479, 11.5446)
    }

    private fun getCapoluogo(prov: String): String {
        return when(prov) {
            "PD" -> "padova"; "VI" -> "vicenza"; "VE" -> "venezia"; "VR" -> "verona"; "TV" -> "treviso"
            "BL" -> "belluno"; "RO" -> "rovigo"; "MO" -> "modena"; "MI" -> "milano"; "RM" -> "roma"
            else -> "vicenza"
        }
    }
}
