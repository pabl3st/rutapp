package com.pabl3st.rutapp.core.importer

import kotlin.math.*

/**
 * Agrupación geográfica de paradas para generar rutas automáticamente.
 *
 * Estrategias:
 *  - AUTO: elige K en base al número de paradas (1 ruta cada ~15 paradas)
 *  - FIXED_K: el usuario elige cuántas rutas quiere
 *  - RADIUS_KM: cada ruta cubre un radio máximo en km
 */
object GeoCluster {

    data class Stop(
        val index: Int,
        val name:  String,
        val lat:   Double,
        val lng:   Double,
    )

    data class ClusterResult(
        val clusters: List<List<Stop>>,
        val k:        Int,
    )

    // ── Punto de entrada principal ────────────────────────────

    fun cluster(
        stops:    List<Stop>,
        strategy: Strategy = Strategy.AUTO,
        fixedK:   Int      = 3,
        radiusKm: Double   = 10.0,
        maxIter:  Int      = 100,
    ): ClusterResult {
        if (stops.isEmpty()) return ClusterResult(emptyList(), 0)
        if (stops.size == 1) return ClusterResult(listOf(stops), 1)

        return when (strategy) {
            Strategy.AUTO     -> kMeans(stops, autoK(stops.size), maxIter)
            Strategy.FIXED_K  -> kMeans(stops, fixedK.coerceIn(1, stops.size), maxIter)
            Strategy.RADIUS   -> clusterByRadius(stops, radiusKm)
        }
    }

    enum class Strategy { AUTO, FIXED_K, RADIUS }

    // ── Auto K: 1 ruta cada 15 paradas, mínimo 1, máximo 20 ──

    fun autoK(n: Int): Int = (n / 15).coerceIn(1, 20)

    // ── K-means geográfico ─────────────────────────────────────

    private fun kMeans(stops: List<Stop>, k: Int, maxIter: Int): ClusterResult {
        // Inicialización con K-means++ para mejor convergencia
        val centroids = initKMeansPlusPlus(stops, k).toMutableList()
        var assignments = IntArray(stops.size) { 0 }

        repeat(maxIter) {
            // Asignar cada stop al centroide más cercano
            val newAssignments = IntArray(stops.size) { i ->
                centroids.indices.minByOrNull { c ->
                    haversine(stops[i].lat, stops[i].lng, centroids[c].first, centroids[c].second)
                } ?: 0
            }

            // Recalcular centroides
            val newCentroids = (0 until k).map { c ->
                val group = stops.indices.filter { newAssignments[it] == c }
                if (group.isEmpty()) centroids[c]
                else {
                    val avgLat = group.map { stops[it].lat }.average()
                    val avgLng = group.map { stops[it].lng }.average()
                    avgLat to avgLng
                }
            }

            val converged = assignments.contentEquals(newAssignments)
            assignments = newAssignments
            centroids.clear()
            centroids.addAll(newCentroids)
            if (converged) return@repeat
        }

        val clusters = (0 until k).map { c ->
            stops.indices.filter { assignments[it] == c }.map { stops[it] }
        }.filter { it.isNotEmpty() }

        return ClusterResult(clusters, clusters.size)
    }

    private fun initKMeansPlusPlus(stops: List<Stop>, k: Int): List<Pair<Double, Double>> {
        val centroids = mutableListOf<Pair<Double, Double>>()
        // Primer centroide aleatorio (índice 0 para reproducibilidad)
        centroids.add(stops[0].lat to stops[0].lng)

        repeat(k - 1) {
            // Elegir siguiente centroide con probabilidad proporcional a distancia^2
            val distances = stops.map { stop ->
                centroids.minOf { c -> haversine(stop.lat, stop.lng, c.first, c.second) }.pow(2)
            }
            val sum = distances.sum()
            if (sum == 0.0) {
                centroids.add(stops[centroids.size % stops.size].lat to stops[centroids.size % stops.size].lng)
                return@repeat
            }
            var rand = Math.random() * sum
            var chosen = 0
            for (i in distances.indices) {
                rand -= distances[i]
                if (rand <= 0) { chosen = i; break }
            }
            centroids.add(stops[chosen].lat to stops[chosen].lng)
        }
        return centroids
    }

    // ── Clustering por radio fijo ──────────────────────────────

    private fun clusterByRadius(stops: List<Stop>, radiusKm: Double): ClusterResult {
        val assigned   = BooleanArray(stops.size) { false }
        val clusters   = mutableListOf<MutableList<Stop>>()

        for (i in stops.indices) {
            if (assigned[i]) continue
            val cluster = mutableListOf(stops[i])
            assigned[i] = true
            for (j in i + 1 until stops.size) {
                if (!assigned[j] &&
                    haversine(stops[i].lat, stops[i].lng, stops[j].lat, stops[j].lng) <= radiusKm) {
                    cluster.add(stops[j])
                    assigned[j] = true
                }
            }
            clusters.add(cluster)
        }

        return ClusterResult(clusters, clusters.size)
    }

    // ── Haversine en km ───────────────────────────────────────

    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
