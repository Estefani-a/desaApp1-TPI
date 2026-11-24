package com.example.uniride.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.uniride.data.local.database.UniRideDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusDaoTest {

    private lateinit var database: UniRideDatabase
    private lateinit var dao: CampusDao

    @Before
    fun setUp() {
        database = inMemoryDatabase()
        dao = database.campusDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `sin sedes guardadas la lista esta vacia`() = runTest {
        assertEquals(emptyList<Any>(), dao.observeAll().first())
    }

    @Test
    fun `las sedes se observan ordenadas por nombre`() = runTest {
        dao.replaceAll(
            listOf(
                campusEntity("RECOLETA", "Recoleta", "Puerta principal"),
                campusEntity("MONSERRAT", "Monserrat", "Salida Lima 717"),
            ),
        )

        assertEquals(listOf("Monserrat", "Recoleta"), dao.observeAll().first().map { it.name })
    }

    @Test
    fun `los puntos de encuentro con comas y comillas se guardan y se recuperan iguales`() = runTest {
        val campus = campusEntity("MONSERRAT", "Monserrat", "Lima 717, esquina", "Puerta \"norte\"")

        dao.replaceAll(listOf(campus))

        assertEquals(listOf(campus), dao.observeAll().first())
    }

    @Test
    fun `una sede sin puntos de encuentro se guarda con la lista vacia`() = runTest {
        val campus = campusEntity("MONSERRAT", "Monserrat")

        dao.replaceAll(listOf(campus))

        assertEquals(emptyList<String>(), dao.observeAll().first().single().meetingPoints)
    }

    @Test
    fun `replaceAll reemplaza la lista completa y borra las sedes que ya no vienen`() = runTest {
        dao.replaceAll(
            listOf(
                campusEntity("MONSERRAT", "Monserrat", "Salida Lima 717"),
                campusEntity("RECOLETA", "Recoleta", "Puerta principal"),
            ),
        )

        dao.replaceAll(
            listOf(
                campusEntity("MONSERRAT", "Monserrat", "Salida Lima 717", "Lima 757"),
                campusEntity("TIGRE", "Tigre", "Acceso sur"),
            ),
        )

        val saved = dao.observeAll().first()
        assertEquals(listOf("MONSERRAT", "TIGRE"), saved.map { it.code })
        assertEquals(listOf("Salida Lima 717", "Lima 757"), saved.first().meetingPoints)
    }

    @Test
    fun `replaceAll con una lista vacia deja la tabla vacia`() = runTest {
        dao.replaceAll(listOf(campusEntity("MONSERRAT", "Monserrat")))

        dao.replaceAll(emptyList())

        assertEquals(emptyList<Any>(), dao.observeAll().first())
    }
}
