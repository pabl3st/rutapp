package com.pabl3st.rutapp.core

import java.util.Locale
import kotlin.math.roundToInt

/** Distancia legible: por debajo de 1 km se da en metros para que un recorrido corto no salga como "0,0 km". */
fun formatKm(km: Double): String =
    if (km < 1.0) "${(km * 1000).roundToInt()} m"
    else "%.1f km".format(Locale("es"), km)

/** Duracion en "1h 05m". */
fun formatDuration(ms: Long): String {
    val h = ms / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return "%dh %02dm".format(h, m)
}
