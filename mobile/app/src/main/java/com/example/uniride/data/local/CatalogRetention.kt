package com.example.uniride.data.local

/**
 * Retention rule of the trip catalog (preentrega 11.2), written once so `TripDao.replaceCatalog`
 * applies it inside a `@Transaction` when Room (E1) is ready.
 *
 * The catalog is a copy of what the server lists, except for the trips the phone must not lose:
 * the ones with a change not synced yet and the ones where the user has an active reservation
 * (they are the "Mi viaje" of RF04, and the server stops listing them once they leave).
 */
object CatalogRetention {

    /**
     * Full refresh (`GET /trips` without `updatedSince`): what the server lists, plus the
     * [mustKeep] ones of [current] that the server no longer lists.
     */
    fun <T, K> replace(
        current: Collection<T>,
        fromServer: Collection<T>,
        id: (T) -> K,
        mustKeep: (T) -> Boolean,
    ): List<T> {
        val serverIds = fromServer.mapTo(HashSet(), id)
        return fromServer.toList() + current.filter { it.let(id) !in serverIds && mustKeep(it) }
    }

    /**
     * Partial refresh (`GET /trips?updatedSince=…`): only the changed trips come, including the
     * ones that were finished or cancelled. Those leave the catalog unless they are [mustKeep];
     * the rest are updated or added.
     */
    fun <T, K> merge(
        current: Collection<T>,
        changed: Collection<T>,
        id: (T) -> K,
        isListed: (T) -> Boolean,
        mustKeep: (T) -> Boolean,
    ): List<T> {
        val result = LinkedHashMap<K, T>()
        current.forEach { result[id(it)] = it }
        changed.forEach { trip ->
            if (isListed(trip) || mustKeep(trip)) result[id(trip)] = trip else result.remove(id(trip))
        }
        return result.values.toList()
    }
}
