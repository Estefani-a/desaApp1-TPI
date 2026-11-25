package com.example.uniride.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

/** Retention rule of the catalog (preentrega 11.2). */
class CatalogRetentionTest {

    private data class Row(val id: String, val listed: Boolean = true, val mine: Boolean = false, val version: Int = 1)

    private fun replace(current: List<Row>, server: List<Row>) =
        CatalogRetention.replace(current, server, Row::id, Row::mine)

    private fun merge(current: List<Row>, changed: List<Row>) =
        CatalogRetention.merge(current, changed, Row::id, Row::listed, Row::mine)

    @Test
    fun `el refresco completo deja lo que lista el servidor`() {
        val result = replace(current = listOf(Row("viejo"), Row("a")), server = listOf(Row("a", version = 2), Row("b")))

        assertEquals(listOf(Row("a", version = 2), Row("b")), result)
    }

    @Test
    fun `el refresco completo conserva los viajes con reserva activa aunque el servidor ya no los liste`() {
        val result = replace(current = listOf(Row("mi-viaje", mine = true), Row("ajeno")), server = listOf(Row("b")))

        assertEquals(setOf("b", "mi-viaje"), result.map { it.id }.toSet())
    }

    @Test
    fun `el refresco parcial actualiza y agrega sin tocar el resto`() {
        val result = merge(current = listOf(Row("a"), Row("b")), changed = listOf(Row("b", version = 2), Row("c")))

        assertEquals(listOf(Row("a"), Row("b", version = 2), Row("c")), result)
    }

    @Test
    fun `el refresco parcial saca los finalizados o cancelados`() {
        val result = merge(current = listOf(Row("a"), Row("b")), changed = listOf(Row("b", listed = false)))

        assertEquals(listOf(Row("a")), result)
    }

    @Test
    fun `el refresco parcial no saca un viaje propio aunque deje de listarse`() {
        val cancelledButMine = Row("b", listed = false, mine = true, version = 2)

        val result = merge(current = listOf(Row("a"), Row("b", mine = true)), changed = listOf(cancelledButMine))

        assertEquals(listOf(Row("a"), cancelledButMine), result)
    }
}
