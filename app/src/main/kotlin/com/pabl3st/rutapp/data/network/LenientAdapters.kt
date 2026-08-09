package com.pabl3st.rutapp.data.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson

/**
 * Adaptadores tolerantes para escalares que MySQL/PHP no serializan con el
 * tipo JSON que cabría esperar.
 *
 * CONTEXTO (ago 2026). Al migrar Moshi a adaptadores generados por KSP se
 * eliminó `KotlinJsonAdapterFactory`, que era reflexivo y **laxo**: convertía
 * silenciosamente 0/1 a Boolean y "39.47" a Double. Los adaptadores generados
 * son estrictos, así que la primera respuesta real de `delta_sync` reventó con:
 *
 *   JsonDataException: Expected a boolean but was NUMBER at path $.stops[0].pdv_open
 *
 * y como la excepción aborta el parseo de TODA la respuesta, el cliente se
 * quedaba sin rutas ni paradas ("Sin rutas") aunque el servidor las enviara.
 *
 * Desajustes reales verificados contra producción (delta_sync, 490 paradas):
 *   pdv_open        TINYINT  → 0/1        (DTO Boolean)
 *   pdv_inactive    TINYINT  → 0/1        (DTO Boolean)
 *   lat / lng       DECIMAL  → "39.47…"   (DTO Double)
 *   visit_frequency INT      → 54         (DTO String)
 *
 * Se resuelve en el cliente, no en el servidor: PDO devuelve DECIMAL como
 * string por diseño y el driver puede variar entre hostings. Tolerar ambas
 * formas es más robusto que exigir al backend una serialización concreta.
 *
 * Nota: la laxitud se limita a los campos anotados. El resto del modelo sigue
 * siendo estricto, que es justamente lo que hace que un desajuste se detecte.
 */

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class LenientBoolean

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class LenientDouble

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class LenientString

object LenientAdapters {

    /** Acepta true/false, 0/1 y "0"/"1"/"true"/"false". */
    @FromJson
    @LenientBoolean
    fun booleanFromJson(reader: JsonReader): Boolean =
        when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.NUMBER  -> reader.nextInt() != 0
            JsonReader.Token.STRING  -> reader.nextString().let {
                it == "1" || it.equals("true", ignoreCase = true)
            }
            JsonReader.Token.NULL    -> { reader.nextNull<Unit>(); false }
            else                     -> { reader.skipValue(); false }
        }

    @ToJson
    fun booleanToJson(@LenientBoolean value: Boolean): Boolean = value

    /** Acepta 39.47 y "39.47". Devuelve null si viene vacío o no es numérico. */
    @FromJson
    @LenientDouble
    fun doubleFromJson(reader: JsonReader): Double? =
        when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull()
            JsonReader.Token.NULL   -> reader.nextNull()
            else                    -> { reader.skipValue(); null }
        }

    @ToJson
    fun doubleToJson(@LenientDouble value: Double?): Double? = value

    /** Acepta "54" y 54 — MySQL devuelve INT donde el DTO espera texto. */
    @FromJson
    @LenientString
    fun stringFromJson(reader: JsonReader): String? =
        when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> reader.nextString()
            JsonReader.Token.NULL   -> reader.nextNull()
            else                    -> { reader.skipValue(); null }
        }

    @ToJson
    fun stringToJson(@LenientString value: String?): String? = value
}
